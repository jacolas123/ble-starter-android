/*
 * Copyright 2024 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid.cybtldr

import android.bluetooth.BluetoothDevice
import java.io.File
import java.net.URI

class cybtldr_api2 {
    companion object {
        var g_abort: Int = 0
    }
    enum class CyBtldr_Action(val value:Int){
        PROGRAM(0x00),
        ERASE(0x01),
        VERIFY(0X02);
    }

    fun ProcessDataRow_v0(
    action:CyBtldr_Action, rowSize:Int, rowData:String) :Int {
        //var (err, arrayId, rowNum, hexData, bufSize, checksum) =
        var (component1,component2)=
            cybtldr_parse.CyBtldr_ParseRowData(rowSize, rowData);
        var err = component1.first
        var arrayId = component1.second
        var rowNum = component1.third

        var hexData = component2.first
        var bufSize = component2.second
        var checksum = component2.third

        if (CYRET_SUCCESS == err) {
            if(action == CyBtldr_Action.ERASE)
            {
                err = cybtldr_api.CyBtldr_EraseRow(arrayId, rowNum)
            } else if (action == CyBtldr_Action.PROGRAM) {
                err = cybtldr_api.CyBtldr_ProgramRow (arrayId, rowNum, hexData, bufSize);
            }
            if ((action == CyBtldr_Action.PROGRAM && err == CYRET_SUCCESS) || action == CyBtldr_Action.VERIFY)
            {
                checksum = (checksum +
                    arrayId +
                    rowNum +
                    (rowNum shr 8) +
                bufSize +
                    (bufSize shr 8));
                err = cybtldr_api.CyBtldr_VerifyRow(arrayId, rowNum, checksum);
            }
        }
        /*
        if (CYRET_SUCCESS == err && NULL != update)
            update(arrayId, rowNum);
        */
        return err;
    }

    fun ProcessDataRow_v1(
    action:CyBtldr_Action, rowSize:Int, rowData:String) : Int {
        var (err, address, buffer, bufSize, _) =
            cybtldr_parse.CyBtldr_ParseRowData_v1(rowSize, rowData);

        if (CYRET_SUCCESS == err) {
            err = when (action) {
                CyBtldr_Action.ERASE -> {
                    cybtldr_api.CyBtldr_EraseRow_v1 (address)
                }

                CyBtldr_Action.PROGRAM -> {
                    cybtldr_api.CyBtldr_ProgramRow_v1 (address, buffer, bufSize);
                }

                CyBtldr_Action.VERIFY -> {
                    cybtldr_api.CyBtldr_VerifyRow_v1(address, buffer, bufSize);
                }
            }
        }
        /*
              if (CYRET_SUCCESS == err && NULL != update)
                  update(0, (uint16_t)(address >> 16));
      */
        return err;
    }
    @ExperimentalStdlibApi
    fun ProcessMetaRow_v1(rowSize:Int, rowData:String) :Int {
        val EIV_META_HEADER_SIZE = 5;
        val EIV_META_HEADER = "@EIV:";

        var err = CYRET_SUCCESS;
        if (rowSize >= EIV_META_HEADER_SIZE &&
            cybtldr_parse.strncmp(rowData, EIV_META_HEADER, EIV_META_HEADER_SIZE) == 0) {
            var (_, buffer, bufSize) = cybtldr_parse.CyBtldr_FromAscii(rowSize - EIV_META_HEADER_SIZE,
                rowData.substring(EIV_META_HEADER_SIZE));
            err =  cybtldr_api.CyBtldr_SetEncryptionInitialVector(bufSize, buffer);
        }
        return err;
    }

    fun RunAction_v0(action:CyBtldr_Action, lineLen:Int, line:String,
    appIdIn:Int, securityKey:String) :Int {
        var appId = appIdIn
        val INVALID_APP = 0xFF;
        var isValid = 0;
        var isActive =0;
        var bootloaderEntered = 0;

        //var (err, siliconId, siliconRev, chksumtype) =
        var (component1,component2) =
            cybtldr_parse.CyBtldr_ParseHeader(lineLen, line);
        var err = component1.first
        var siliconId = component1.second
        var siliconRev = component2.first
        var chksumtype = component2.second
        var t = cybtldr_command.Cybtldr_ChecksumType.CRC_CHECKSUM;
        if (chksumtype == cybtldr_command.Cybtldr_ChecksumType.SUM_CHECKSUM.value) {
            t = cybtldr_command.Cybtldr_ChecksumType.SUM_CHECKSUM;
        }
        if (CYRET_SUCCESS == err) {
            cybtldr_command.CyBtldr_SetCheckSumType(t);

            err = cybtldr_api.CyBtldr_StartBootloadOperation(
                siliconId, siliconRev, securityKey);
            bootloaderEntered = 1;

            appId -=
                1; /* 1 and 2 are legal inputs to function. 0 and 1 are valid for bootloader component */
            if (appId > 1) {
                appId = INVALID_APP;
            }

            if ((CYRET_SUCCESS == err) && (appId != INVALID_APP)) {
                /* This will return error if bootloader is for single app */
                var (errOut, isValidOut, isActiveOut) = cybtldr_api.CyBtldr_GetApplicationStatus(appId);
                err = errOut
                isValid = isValidOut
                isActive = isActiveOut
                /* Active app can be verified, but not programmed or erased */
                if (CYRET_SUCCESS == err &&
                    CyBtldr_Action.VERIFY != action &&
                    isActive == 1) {
                    /* This is multi app */
                    err = CYRET_ERR_ACTIVE;
                }
            }
        }
        var lineIndex = 1;
        while (CYRET_SUCCESS == err) {
            if (g_abort == 1) {
                err = CYRET_ABORT;
                break;
            }
            var (errOut, line, lineLen) = cybtldr_parse.CyBtldr_ReadLine(lineIndex);
            err = errOut
            lineIndex++;
            if (CYRET_SUCCESS == err) {
                err = ProcessDataRow_v0(
                    action, lineLen,line);
            } else if (CYRET_ERR_EOF == err) {
                err = CYRET_SUCCESS;
                break;
            }
        }

        if (err == CYRET_SUCCESS) {
            if (CyBtldr_Action.PROGRAM == action && INVALID_APP != appId) {
                var (errOut, isValidOut, isActiveOut) = cybtldr_api.CyBtldr_GetApplicationStatus(appId);
                err = errOut
                isValid = isValidOut
                isActive = isActiveOut
                if (CYRET_SUCCESS == err) {
                    /* If valid set the active application to what was just programmed */
                    /* This is multi app */
                    if(isValid == 0)
                    {
                        err = cybtldr_api.CyBtldr_SetApplicationStatus(appId)
                    } else {
                        CYRET_ERR_CHECKSUM;
                    }
                } else if (CYBTLDR_STAT_ERR_CMD == (err xor CYRET_ERR_BTLDR_MASK)) {
                    /* Single app - restore previous CYRET_SUCCESS */
                    err = CYRET_SUCCESS;
                }
            } else if (CyBtldr_Action.PROGRAM == action ||
                CyBtldr_Action.VERIFY == action) {
                err = cybtldr_api.CyBtldr_VerifyApplication();
            }
            cybtldr_api.CyBtldr_EndBootloadOperation();
        } else if (CYRET_ERR_COMM_MASK != (CYRET_ERR_COMM_MASK and err) &&
        bootloaderEntered == 1) {
            cybtldr_api.CyBtldr_EndBootloadOperation();
        }
        return err;
    }
/*
    @OptIn(ExperimentalStdlibApi::class)
    fun RunAction_v1(action:CyBtldr_Action, lineLen:Int, line:String) : Int {
        var blVer = 0;
        //int chksumtype = Cybtldr_ChecksumType.SUM_CHECKSUM.value;
        var bootloaderEntered = 0;
        var applicationStartAddr = 0xffffffff;
        var applicationSize = 0;

        var lineIndex = 1;

        //var (err, siliconId, siliconRev, chksumtype, appId, productId) =
        var (component1,component2)=
            cybtldr_parse.CyBtldr_ParseHeader_v1(lineLen, line);
        var err = component1.first
        var siliconId = component1.second
        var siliconRev = component1.third

        var chksumtype = component2.first
        var appId = component2.second
        var productId = component2.third

        if (CYRET_SUCCESS == err) {
            var t = cybtldr_command.Cybtldr_ChecksumType.CRC_CHECKSUM;
            if (chksumtype == cybtldr_command.Cybtldr_ChecksumType.SUM_CHECKSUM.value) {
                t = cybtldr_command.Cybtldr_ChecksumType.SUM_CHECKSUM;
            }
            cybtldr_command.CyBtldr_SetCheckSumType(t);

            err = CyBtldr_StartBootloadOperation_v1(
                siliconId, siliconRev, blVer, productId);
            if (err == CYRET_SUCCESS) {
                var (errOut, applicationStartAddrOut, applicationSizeOut) =
                cybtldr_parse.CyBtldr_ParseAppStartAndSize_v1(
                    applicationStartAddr, applicationSize, lineIndex);
                err = errOut
                applicationStartAddr = applicationStartAddrOut
                applicationSize = applicationSizeOut
                lineIndex++;
            }
            if (err == CYRET_SUCCESS) {
                err = cybtldr_api.CyBtldr_SetApplicationMetaData(
                    appId, applicationStartAddr, applicationSize);
            }
            bootloaderEntered = 1;
        }

        while (CYRET_SUCCESS == err) {
            if (g_abort == 1) {
                err = CYRET_ABORT;
                break;
            }
            var (errOut, s, lineLen) = cybtldr_parse.CyBtldr_ReadLine(lineIndex);
            err = errOut
            if (CYRET_SUCCESS == err) {
                err = when (s[0]) {
                    '@'-> {
                        ProcessMetaRow_v1 (lineLen, s);
                    }

                    ':' -> {
                        ProcessDataRow_v1 (action, lineLen, s);
                    }

                    else -> 1
                }
            } else if (CYRET_ERR_EOF == err) {
                err = CYRET_SUCCESS;
                break;
            }
        }

        if (err == CYRET_SUCCESS &&
            (CyBtldr_Action.PROGRAM == action || CyBtldr_Action.VERIFY == action)) {
            err =  cybtldr_api.CyBtldr_VerifyApplication_v1(appId);
            cybtldr_api.CyBtldr_EndBootloadOperation();
        } else if (CYRET_ERR_COMM_MASK != (CYRET_ERR_COMM_MASK and err) &&
        bootloaderEntered == 1) {
            await CyBtldr_EndBootloadOperation();
        }
        provider.hideProgress();
        return err;
    }
*/
    fun CyBtldr_RunAction(
    action:CyBtldr_Action,
    securityKey:String,
    appId:Int,
    filePath: URI,
    device:BluetoothDevice,
    ): Int {
        g_abort = 0;
        var fileVersion = 2

        //g_comm.SetDevice(device);
        var err =  cybtldr_parse.CyBtldr_OpenDataFile(filePath);
        if (CYRET_SUCCESS == err) {
            var (errOut, line, lineLen) = cybtldr_parse.CyBtldr_ReadLine(0);
            err = errOut
            // The file version determine the format of the cyacd\cyacd2 file and the set of protocol commands used.
            if (CYRET_SUCCESS == err) {
                var (errOut, fileVersionOut) = cybtldr_parse.CyBtldr_ParseCyacdFileVersion(
                filePath.toString(),
                lineLen,
                line)
                err = errOut
                fileVersion = fileVersionOut
            }
            if (CYRET_SUCCESS == err) {
                /*
                when (fileVersion) {
                    0->
                    err = await RunAction_v0(action, lineLen, line, appId,
                    Uint8List.fromList(securityKey.codeUnits), provider);
                    break;
                    case 1:
                    err = await RunAction_v1(action, lineLen, line, provider);
                    break;
                    default:
                    err = CYRET_ERR_FILE;
                    break;
                }*/
                if(fileVersion == 0) {
                    err = RunAction_v0(action, lineLen, line, appId,securityKey)
                }
                cybtldr_api.g_comm.CloseConnection()
            }

            cybtldr_parse.CyBtldr_CloseDataFile();
        }

        return err;
    }

    fun CyBtldr_Program(file:URI, securityKey:String, appId:Int,device:BluetoothDevice) :Int{
        return CyBtldr_RunAction(
            CyBtldr_Action.PROGRAM, securityKey, appId, file, device);
    }

    fun CyBtldr_Erase(file:URI, securityKey:String, device:BluetoothDevice) :Int{
        return CyBtldr_RunAction(
            CyBtldr_Action.ERASE, securityKey, 0, file, device);
    }

    fun CyBtldr_Verify(file:URI, securityKey:String,device:BluetoothDevice) :Int {
        return CyBtldr_RunAction(
            CyBtldr_Action.VERIFY, securityKey, 0,file, device);
    }

    fun CyBtldr_Abort() :Int {
        g_abort = 1;
        return CYRET_SUCCESS;
    }

}