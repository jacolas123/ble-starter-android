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

import com.punchthrough.blestarterappandroid.ble.cybtldr_ble_comms

/* The highest number of memory arrays for any device. This includes flash and EEPROM arrays */
val MAX_DEV_ARRAYS = 0x80;
/* The default value if a flash array has not yet received data */
val NO_FLASH_ARRAY_DATA = 0;
/* The maximum number of flash arrays */
val MAX_FLASH_ARRAYS = 0x40;
/* The minimum array id for EEPROM arrays. */
val MIN_EEPROM_ARRAY = 0x40;

class cybtldr_api {
    companion object {

        var g_validRows: MutableList<Int> = mutableListOf<Int>()
        var g_comm : cybtldr_ble_comms = cybtldr_ble_comms()

        fun min_int(a: Int, b: Int): Int {
            return if (a < b) {
                a
            } else {
                b
            }
        }

        fun CyBtldr_TransferData(
            inBuf: List<Int>, inSize: Int, outSize: Int
        ): Pair<Int, List<Int>> {

            var err = g_comm.WriteData(inBuf, inSize);
            var outbuf = mutableListOf<Int>()
            if (CYRET_SUCCESS == err) {
                var hasAllData = false;
                var errCount = 0;
                while (!hasAllData && errCount < 100) {
                    var (hasAllDataOut, errOut, outbufOut) = g_comm.ReadData(outSize);
                    hasAllData = hasAllDataOut
                    err = errOut
                    outbuf = outbufOut.toMutableList()
                    if (!hasAllData) {
                        errCount++;
                    }
                    //await Future . delayed (const Duration (milliseconds: 1));
                }
            }

            if (CYRET_SUCCESS != err) {
                err = err or CYRET_ERR_COMM_MASK
            }

            return Pair(err, outbuf);
        }

        fun CyBtldr_ValidateRow(arrayId: Int, rowNum: Int): Int {
            var inSize = 0
            var outSize = 0
            var minRow = 0;
            var maxRow = 0;
            var inBuf = mutableListOf<Int>();
            var outBuf = mutableListOf<Int>();
            var status = CYRET_SUCCESS;
            var err = CYRET_SUCCESS;

            if (arrayId < MAX_FLASH_ARRAYS) {
                if (NO_FLASH_ARRAY_DATA == g_validRows[arrayId]) {
                    var (err, inSizeOut, inBufOut, outSize) = cybtldr_command.CyBtldr_CreateGetFlashSizeCmd(
                        arrayId
                    );
                    inSize = inSizeOut
                    inBuf = inBufOut.toMutableList()
                    if (CYRET_SUCCESS == err) {
                        var (errOut, outBufOut) = CyBtldr_TransferData(inBuf, inSize, outSize);
                        err = errOut
                        outBufOut = outBuf
                    }
                    if (CYRET_SUCCESS == err) {
                        var (errOut, minRowOut, maxRowOut, statusOut) =
                            cybtldr_command.CyBtldr_ParseGetFlashSizeCmdResult(outBuf, outSize);
                        err = errOut
                        minRow = minRowOut
                        maxRow = maxRowOut
                        status = statusOut
                    }
                    if (CYRET_SUCCESS != status) {
                        err = status or CYRET_ERR_BTLDR_MASK;
                    }
                    if (CYRET_SUCCESS == err) {
                        if (CYRET_SUCCESS == status) {
                            g_validRows[arrayId] = (minRow shl 16) + maxRow;
                        } else {
                            err = status or CYRET_ERR_BTLDR_MASK;
                        }
                    }
                }
                if (CYRET_SUCCESS == err) {
                    minRow = (g_validRows[arrayId] shr 16);
                    maxRow = g_validRows[arrayId];
                    if (rowNum < minRow || rowNum > maxRow) {
                        err = CYRET_ERR_ROW;
                    }
                }
            } else {
                err = CYRET_ERR_ARRAY;
            }

            return err;
        }

        fun CyBtldr_StartBootloadOperation(
        expSiId:Int, expSiRev:Int, securityKeyBuf:String):Int
        {
            val SUPPORTED_BOOTLOADER = 0x010000;
            val BOOTLOADER_VERSION_MASK = 0xFF0000;
            var inSize = 0;
            var outSize = 0;
            var siliconId = 0;
            var inBuf = mutableListOf<Int>();
            var outBuf = mutableListOf<Int>()
            var siliconRev = 0;
            var status = CYRET_SUCCESS;

            var blVer = 0.toLong();
            for (i in 0..MAX_FLASH_ARRAYS) {
                g_validRows[i] = NO_FLASH_ARRAY_DATA;
            }

            var err =  cybtldr_api.g_comm.OpenConnection ();
            //await Future . delayed (const Duration (seconds: 3));
            if (CYRET_SUCCESS != err) {
                err  = err or CYRET_ERR_COMM_MASK;
            }

            if (CYRET_SUCCESS == err) {
                var (errOut, inSizeOut, inBufOut, outSizeOut) =
                cybtldr_command.CyBtldr_CreateEnterBootLoaderCmd(securityKeyBuf);
                err = errOut
                inSize = inSizeOut
                inBuf = inBufOut.toMutableList()
                outSize = outSizeOut
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, outBufOut) = CyBtldr_TransferData(inBuf, inSize, outSize)
                err = errOut
                outBuf = outBufOut.toMutableList()
            }
            if (CYRET_SUCCESS == err) {
               var (errOut, siliconIdOut, siliconRevOut, blVerOut, statusOut) =
                cybtldr_command.CyBtldr_ParseEnterBootLoaderCmdResult(outBuf, outSize);
                err = errOut
                siliconId = siliconIdOut
                siliconRev = siliconRevOut
                blVer = blVerOut
                status = statusOut
            } else {
                var (errOut, statusOut) = cybtldr_command.CyBtldr_TryParseParketStatus(outBuf, outSize);
                err = errOut
                status = statusOut
                if (err == CYRET_SUCCESS) {
                    err = status or
                    CYRET_ERR_BTLDR_MASK; //if the response we get back is a valid packet override the err with the response's status
                }
            }

            if (CYRET_SUCCESS == err) {
                if (CYRET_SUCCESS != status) {
                    err = status or CYRET_ERR_BTLDR_MASK;
                }
                if (expSiId != siliconId || expSiRev != siliconRev) {
                    err = CYRET_ERR_DEVICE;
                } else if ((blVer and BOOTLOADER_VERSION_MASK.toLong()) != SUPPORTED_BOOTLOADER.toLong()) {
                    err = CYRET_ERR_VERSION;
                }
            }

            return err;
        }
/*
        fun CyBtldr_StartBootloadOperation_v1(
        expSiId:Long, expSiRev:Int, blVer:Int, productID:Long): Int
        {

            var inSize = 0;
            var outSize = 0;
            var siliconId = 0;
            var inBuf = mutableListOf<Int>();
            var outBuf = mutableListOf<Int>();
            var siliconRev = 0;
            var status = CYRET_SUCCESS;

            for (i in 0..MAX_FLASH_ARRAYS) {
                g_validRows[i] = NO_FLASH_ARRAY_DATA;
            }

            var err = await g_comm . OpenConnection ();
            if (CYRET_SUCCESS != err) {
                err = err or CYRET_ERR_COMM_MASK;
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, inSizeOut, inBufOut, outSizeOut) = cybtldr_command.CyBtldr_CreateEnterBootLoaderCmd_v1(productID);
                err = errOut
                inSize = inSizeOut
                inBuf = inBufOut.toMutableList()
                outSize = outSizeOut
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, outBufOut) = CyBtldr_TransferData(inBuf, inSize, outSize);
                err = errOut
                outBuf = outBufOut.toMutableList()
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, siliconIdOut, siliconRevOut, blVerOut, productIDOut) =
                cybtldr_command.CyBtldr_ParseEnterBootLoaderCmdResult(outBuf, outSize)
                err = errOut
                siliconId = siliconIdOut
                siliconRev = siliconRevOut
                blVer = blVerOut
                productID = productIDOut
            } else {
                var (err, status) = cybtldr_command.CyBtldr_TryParseParketStatus(outBuf, outSize);
                if (err == CYRET_SUCCESS) {
                    err = status or
                    CYRET_ERR_BTLDR_MASK; //if the response we get back is a valid packet override the err with the response's status
                }
            }

            if (CYRET_SUCCESS == err) {
                if (CYRET_SUCCESS != status) {
                    err = status or CYRET_ERR_BTLDR_MASK;
                }
                if (expSiId != siliconId || expSiRev != siliconRev) {
                    err = CYRET_ERR_DEVICE;
                }
            }

            return err;
        }
*/
        fun CyBtldr_GetApplicationStatus(appID:Int) :Triple<Int,Int,Int>
        {
            var outBuf = mutableListOf<Int>()
            var status = CYRET_SUCCESS;
            var isValid = 0;
            var isActive = 0;

            var (err, inSize, inBuf, outSize) = cybtldr_command.CyBtldr_CreateGetAppStatusCmd(appID);
            if (CYRET_SUCCESS == err) {
                var (errOut, outBufOut) = CyBtldr_TransferData(inBuf, inSize, outSize);
                err =errOut
                outBuf = outBufOut.toMutableList()
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, isValidOut, isActiveOut, statusOut) =
                cybtldr_command.CyBtldr_ParseGetAppStatusCmdResult(outBuf, outSize)
                err = errOut
                isValid = isValidOut
                isActive = isActiveOut
                status = statusOut
            } else {
                var (errOut, status) = cybtldr_command.CyBtldr_TryParseParketStatus(outBuf, outSize);
                err = errOut
                if (err == CYRET_SUCCESS) {
                    err = status or
                    CYRET_ERR_BTLDR_MASK; //if the response we get back is a valid packet override the err with the response's status
                }
            }

            if (CYRET_SUCCESS == err) {
                if (CYRET_SUCCESS != status) {
                    err = status or CYRET_ERR_BTLDR_MASK;
                }
            }

            return Triple(err, isValid, isActive);
        }

        fun CyBtldr_SetApplicationStatus(appID:Int):Int
        {

            var outBuf = mutableListOf<Int>()
            var status = CYRET_SUCCESS;

            var (err, inSize, inBuf, outSize) = cybtldr_command.CyBtldr_CreateSetActiveAppCmd(appID);
            if (CYRET_SUCCESS == err) {
                var (errOut, outBufOut) =  CyBtldr_TransferData(inBuf, inSize, outSize);
                err = errOut
                outBuf = outBufOut.toMutableList()
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, statusOut) = cybtldr_command.CyBtldr_ParseSetActiveAppCmdResult(outBuf, outSize);
                err = errOut
                status = statusOut
            }

            if (CYRET_SUCCESS == err) {
                if (CYRET_SUCCESS != status) {
                    err = status or CYRET_ERR_BTLDR_MASK;
                }
            }

            return err;
        }

        fun CyBtldr_EndBootloadOperation():Int
        {
            var (err, inSize, inBuf, _) = cybtldr_command.CyBtldr_CreateExitBootLoaderCmd();
            if (CYRET_SUCCESS == err) {
                err =  g_comm.WriteData(inBuf, inSize);

                if (CYRET_SUCCESS == err) {
                    err = g_comm.CloseConnection();
                }

                if (CYRET_SUCCESS != err) {
                    err  = err or CYRET_ERR_COMM_MASK;
                }
            }

            return err;
        }

        fun SendData(
        buf:List<Int>, size:Int, maxRemainingDataSize:Int, outBuf:List<Int>): Triple<Int,Int,List<Int>>
        {
            var offset = 0;
            var status = CYRET_SUCCESS;
            var inBuf = mutableListOf<Int>()
            var outBufOut = mutableListOf<Int>()
            // size is the total bytes of data to transfer.
            // offset is the amount of data already transfered.
            // a is maximum amount of data allowed to be left over when this function ends.
            // (we can leave some data for caller (programRow, VerifyRow,...) to send.
            // TRANSFER_HEADER_SIZE is the amount of bytes this command header takes up.
            val TRANSFER_HEADER_SIZE = 7;
            val subBufSize =
            min_int((cybtldr_api.g_comm.MaxTransferSize - TRANSFER_HEADER_SIZE), size);
            var err = CYRET_SUCCESS;
            //Break row into pieces to ensure we don't send too much for the transfer protocol
            while ((CYRET_SUCCESS == err) && ((size - offset) > maxRemainingDataSize)) {
                var (errOut, inSize, inBufOut, outSize) = cybtldr_command.CyBtldr_CreateSendDataCmd(
                buf.subList(offset, buf.size),
                subBufSize,
                inBuf)
                err = errOut
                inBuf = inBufOut.toMutableList()

                if (CYRET_SUCCESS == err) {
                    var (errOut, outBufT) = CyBtldr_TransferData(inBuf, inSize, outSize);
                    err = errOut
                    outBufOut = outBufT.toMutableList()
                }
                if (CYRET_SUCCESS == err) {
                    var (errOut, statusOut) = cybtldr_command.CyBtldr_ParseSendDataCmdResult(outBufOut, outSize);
                    err = errOut
                    status = statusOut
                }
                if (CYRET_SUCCESS != status) {
                    err = status or CYRET_ERR_BTLDR_MASK;
                }
                offset += subBufSize;
            }
            return Triple(err, offset, inBuf);
        }

        fun CyBtldr_ProgramRow(
        arrayID:Int, rowNum:Int, buf:List<Int>, size:Int): Int
        {
            val TRANSFER_HEADER_SIZE = 10;

            var inBuf = mutableListOf<Int>()
            var outBuf = mutableListOf<Int>()
            var inSize = 0;
            var outSize = 0;
            var offset = 0;
            var subBufSize = 0;
            var status = CYRET_SUCCESS;
            var err = CYRET_SUCCESS;

            if (arrayID < MAX_FLASH_ARRAYS) {
                err = CyBtldr_ValidateRow (arrayID, rowNum);
            }

            if (CYRET_SUCCESS == err) {
                var (errOut, offsetOut, inBufOut) = SendData(
                buf, size, (g_comm.MaxTransferSize-TRANSFER_HEADER_SIZE), outBuf);
                err = errOut
                offset = offsetOut
                inBuf = inBufOut.toMutableList()
            }

            if (CYRET_SUCCESS == err) {
                subBufSize = size - offset;

                var (errOut, inSizeOut, buf, outSizeOut) = cybtldr_command.CyBtldr_CreateProgramRowCmd(
                arrayID,
                rowNum,
                buf.subList(offset, buf.size),
                subBufSize,
                inBuf)
                err = errOut
                inSize = inSizeOut
                outSize = outSizeOut
                if (CYRET_SUCCESS == err) {
                    var (errOut, outBufOut) = CyBtldr_TransferData(inBuf, inSize, outSize);
                    err = errOut
                    outBuf = outBufOut.toMutableList()
                }
                if (CYRET_SUCCESS == err) {
                    var (errOut, statusOut) = cybtldr_command.CyBtldr_ParseProgramRowCmdResult(outBuf, outSize);
                    err = errOut
                    status = statusOut
                }
                if (CYRET_SUCCESS != status) {
                    err = status or CYRET_ERR_BTLDR_MASK;
                }
            }

            return err;
        }

        fun CyBtldr_EraseRow(arrayID:Int, rowNum:Int) :Int
        {
            var inBuf = mutableListOf<Int>()
            var outBuf = mutableListOf<Int>()
            var inSize = 0;
            var outSize = 0;
            var status = CYRET_SUCCESS;
            var err = CYRET_SUCCESS;

            if (arrayID < MAX_FLASH_ARRAYS) {
                err = CyBtldr_ValidateRow (arrayID, rowNum);
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, inSizeOut, inBufOut, outSizeOut) = cybtldr_command.CyBtldr_CreateEraseRowCmd(arrayID, rowNum);
                err = errOut
                inSize = inSizeOut
                inBuf = inBufOut.toMutableList()
                outSize = outSizeOut
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, outBufOut) = CyBtldr_TransferData(inBuf, inSize, outSize);
                err = errOut
                outBuf = outBufOut.toMutableList()
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, statusOut) = cybtldr_command.CyBtldr_ParseEraseRowCmdResult(outBuf, outSize);
                err = errOut
                status = statusOut
            }
            if (CYRET_SUCCESS != status) {
                err = status or CYRET_ERR_BTLDR_MASK;
            }

            return err;
        }

        fun CyBtldr_VerifyRow(arrayID:Int, rowNum:Int, checksum:Int) : Int
        {
            var inBuf = mutableListOf<Int>();
            var outBuf = mutableListOf<Int>()
            var inSize = 0;
            var outSize = 0;
            var rowChecksum = 0;
            var status = CYRET_SUCCESS;
            var err = CYRET_SUCCESS;

            if (arrayID < MAX_FLASH_ARRAYS) {
                err =  CyBtldr_ValidateRow (arrayID, rowNum);
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, inSizeOut, inBufOut, outSizeOut) = cybtldr_command.CyBtldr_CreateVerifyRowCmd(arrayID, rowNum);
                err = errOut
                inSize = inSizeOut
                inBuf = inBufOut.toMutableList()
                outSize = outSizeOut

            }
            if (CYRET_SUCCESS == err) {
                var (errOut, outBufOut) = CyBtldr_TransferData(inBuf, inSize, outSize);
                err = errOut
                outBuf = outBufOut.toMutableList()
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, rowChecksumOut, statusOut) =
                cybtldr_command.CyBtldr_ParseVerifyRowCmdResult(outBuf, outSize);
                err = errOut
                rowChecksum = rowChecksumOut
                status = statusOut
            }
            if (CYRET_SUCCESS != status) {
                err = status or CYRET_ERR_BTLDR_MASK;
            }
            if ((CYRET_SUCCESS == err) && (rowChecksum != checksum)) {
                err = CYRET_ERR_CHECKSUM;
            }

            return err;
        }

        fun CyBtldr_VerifyApplication() :Int
        {
            var outBuf = mutableListOf<Int>()
            var checksumValid = 0;
            var status = CYRET_SUCCESS;

            var (err, inSize, inBuf, outSize) = cybtldr_command.CyBtldr_CreateVerifyChecksumCmd();
            if (CYRET_SUCCESS == err) {
                var (errOut, outBufOut) = CyBtldr_TransferData(inBuf, inSize, outSize);
                err = errOut
                outBuf = outBufOut.toMutableList()
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, checksumValidOut, statusOut) =
                cybtldr_command.CyBtldr_ParseVerifyChecksumCmdResult(outBuf, outSize);
                err = errOut
                checksumValid = checksumValidOut
                status = statusOut
            }
            if (CYRET_SUCCESS != status) {
                err = status or CYRET_ERR_BTLDR_MASK;
            }
            if ((CYRET_SUCCESS == err) && (checksumValid != 1)) {
                err = CYRET_ERR_CHECKSUM;
            }

            return err;
        }

        fun CyBtldr_ProgramRow_v1(address:Long, buf:List<Int>, size:Int):Int
        {
            val TRANSFER_HEADER_SIZE = 15;

            var inBuf = mutableListOf<Int>()
            var outBuf = mutableListOf<Int>()
            var offset = 0;
            var err = CYRET_SUCCESS;
            var status = CYRET_SUCCESS;
            var chksum = cybtldr_command.CyBtldr_ComputeChecksum32bit (buf, size);

            if (CYRET_SUCCESS == err) {
                var (errOut, offsetOut, inBufOut) = SendData(
                buf, size, (g_comm.MaxTransferSize-TRANSFER_HEADER_SIZE), outBuf);
                inBuf = inBufOut.toMutableList()
                err = errOut
                offset = offsetOut
            }

            if (CYRET_SUCCESS == err) {
                val subBufSize = size - offset;

                var (errOut, inSize, inBufOut, outSize) = cybtldr_command.CyBtldr_CreateProgramDataCmd(
                address,
                chksum,
                buf.subList(offset, buf.size),
                subBufSize,
                inBuf);
                err = errOut
                inBuf = inBufOut.toMutableList()

                if (CYRET_SUCCESS == err) {
                    var (errOut, outBufOut) = CyBtldr_TransferData(inBuf, inSize, outSize);
                    err = errOut
                    outBuf = outBufOut.toMutableList()
                }
                if (CYRET_SUCCESS == err) {
                    var (errOut, statusOut) = cybtldr_command.CyBtldr_ParseDefaultCmdResult(outBuf, outSize);
                    err = errOut
                    status = statusOut
                }
                if (CYRET_SUCCESS != status) {
                    err = status or CYRET_ERR_BTLDR_MASK;
                }
            }

            return err;
        }

        fun CyBtldr_EraseRow_v1(address:Long):Int
        {
            var  outBuf = mutableListOf<Int>()
            var status = CYRET_SUCCESS;
            var (err, inSize, inBufOut, outSize) = cybtldr_command.CyBtldr_CreateEraseDataCmd(address)
            var inBuf = inBufOut.toMutableList()
            if (CYRET_SUCCESS == err) {
                var (errOut, outBufOut) = CyBtldr_TransferData(inBuf, inSize, outSize);
                err = errOut
                outBuf = outBufOut.toMutableList()
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, statusOut) = cybtldr_command.CyBtldr_ParseEraseRowCmdResult(outBuf, outSize);
                err = errOut
                status = statusOut
            }
            if (CYRET_SUCCESS != status) {
                err = status or CYRET_ERR_BTLDR_MASK;
            }

            return err;
        }

        fun CyBtldr_VerifyRow_v1(address:Long, buf:List<Int>, size:Int): Int
        {
            val TRANSFER_HEADER_SIZE = 15;

            var outBuf = mutableListOf<Int>()
            var subBufSize = 0;

            var chksum = cybtldr_command.CyBtldr_ComputeChecksum32bit (buf, size);
            var (err, offset, inBufOut) = SendData(
            buf, size, (g_comm.MaxTransferSize-TRANSFER_HEADER_SIZE), outBuf)
            var inBuf = inBufOut.toMutableList()

            if (CYRET_SUCCESS == err) {
                subBufSize = size - offset;

                var (errOut, inSize, inBufOut, outSize) = cybtldr_command.CyBtldr_CreateVerifyDataCmd(
                address,
                chksum,buf.subList(offset, buf.size),
                subBufSize);
                err = errOut
                inBuf = inBufOut.toMutableList()
                var status = 0;
                if (CYRET_SUCCESS == err) {
                    var (errOut, outBufOut) = CyBtldr_TransferData(inBuf, inSize, outSize);
                    err = errOut
                    outBuf = outBufOut.toMutableList()
                }
                if (CYRET_SUCCESS == err) {
                    var (errOut, statusOut) = cybtldr_command.CyBtldr_ParseDefaultCmdResult(outBuf, outSize);
                    err = errOut
                    status = statusOut;
                }
                if (CYRET_SUCCESS != status) {
                    err = status or CYRET_ERR_BTLDR_MASK;
                }
            }

            return err;
        }

        fun CyBtldr_VerifyApplication_v1(appId:Int) :Int
        {
            var outBuf = mutableListOf<Int>();
            var checksumValid = 0;
            var status = CYRET_SUCCESS;

            var (err, inSize, inBuf, outSize) = cybtldr_command.CyBtldr_CreateVerifyChecksumCmd_v1(appId);
            if (CYRET_SUCCESS == err) {
                var (errOut, outBufOut) = CyBtldr_TransferData(inBuf, inSize, outSize)
                err = errOut
                outBuf = outBufOut.toMutableList()
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, checksumValidOut, statusOut) =
                    cybtldr_command.CyBtldr_ParseVerifyChecksumCmdResult(outBuf, outSize);
                err = errOut
                checksumValid = checksumValidOut
                status = statusOut
            }
            if (CYRET_SUCCESS != status) {
                err = status or CYRET_ERR_BTLDR_MASK;
            }
            if ((CYRET_SUCCESS == err) && (checksumValid != 1)) {
                err = CYRET_ERR_CHECKSUM;
            }

            return err;
        }

        fun CyBtldr_SetApplicationMetaData(
        appId:Int, appStartAddr:Long, appSize:Int) :Int
        {
            var outBuf = mutableListOf<Int>()
            var status = CYRET_SUCCESS;

            var metadata = mutableListOf<Int>()
            metadata[0] = (appStartAddr and 0xFF).toInt()
            metadata[1] = ((appStartAddr shr  8) and 0xFF).toInt()
            metadata[2] = ((appStartAddr shr 16) and 0xFF).toInt()
            metadata[3] = ((appStartAddr shr 24) and 0xFF).toInt()
            metadata[4] = appSize and 0xFF
            metadata[5] = ((appSize shr 8) and 0xFF)
            metadata[6] = ((appSize shr 16) and 0xFF)
            metadata[7] = ((appSize shr 24) and 0xFF)
            var (err, inSize, inBuf, outSize) =
                cybtldr_command.CyBtldr_CreateSetApplicationMetadataCmd(appId, metadata);
            if (CYRET_SUCCESS == err) {
                var(errOut, outBufOut) = CyBtldr_TransferData(inBuf, inSize, outSize);
                err = errOut
                outBuf = outBufOut.toMutableList()
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, statusOut) = cybtldr_command.CyBtldr_ParseDefaultCmdResult(outBuf, outSize);
                err = errOut
                status = statusOut
            }
            if (CYRET_SUCCESS != status) {
                err = status or CYRET_ERR_BTLDR_MASK;
            }

            return err;
        }

        fun CyBtldr_SetEncryptionInitialVector(size:Int, buf:List<Int>) : Int
        {
            var outBuf = mutableListOf<Int>()

            var status = CYRET_SUCCESS

            var (err, inSize, inBuf, outSize) =
                cybtldr_command.CyBtldr_CreateSetEncryptionInitialVectorCmd(buf, size);
            if (CYRET_SUCCESS == err) {
                var (errOut, outBufOut) = CyBtldr_TransferData(inBuf, inSize, outSize)
                err =errOut
                outBuf = outBufOut.toMutableList()
            }
            if (CYRET_SUCCESS == err) {
                var (errOut, statusOut) = cybtldr_command.CyBtldr_ParseDefaultCmdResult(outBuf, outSize);
                err = errOut
                status = statusOut
            }
            if (CYRET_SUCCESS != status) {
                err = status or CYRET_ERR_BTLDR_MASK;
            }

            return err;
        }
    }
}