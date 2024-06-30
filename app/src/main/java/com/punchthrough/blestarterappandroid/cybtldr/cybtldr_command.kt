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

val MAX_COMMAND_SIZE = 512;

//STANDARD PACKET FORMAT:
// Multi byte entries are encoded in LittleEndian.
/// *****************************************************************************
/// [1-byte] [1-byte ] [2-byte] [n-byte] [ 2-byte ] [1-byte]
/// [ SOP  ] [Command] [ Size ] [ Data ] [Checksum] [ EOP  ]
///*****************************************************************************

/* The first byte of any boot loader command. */
val CMD_START = 0x01;
/* The last byte of any boot loader command. */
val CMD_STOP = 0x17;
/* The minimum number of bytes in a bootloader command. */
val BASE_CMD_SIZE = 0x07;

/* Command identifier for verifying the checksum value of the bootloadable project. */
val CMD_VERIFY_CHECKSUM = 0x31;
/* Command identifier for getting the number of flash rows in the target device. */
val CMD_GET_FLASH_SIZE = 0x32;
/* Command identifier for getting info about the app status. This is only supported on multi app bootloader. */
val CMD_GET_APP_STATUS = 0x33;
/* Command identifier for erasing a row of flash data from the target device. */
val CMD_ERASE_ROW = 0x34;
/* Command identifier for making sure the bootloader host and bootloader are in sync. */
val CMD_SYNC = 0x35;
/* Command identifier for setting the active application. This is only supported on multi app bootloader. */
val CMD_SET_ACTIVE_APP = 0x36;
/* Command identifier for sending a block of data to the bootloader without doing anything with it yet. */
val CMD_SEND_DATA = 0x37;
/* Command identifier for starting the boot loader.  All other commands ignored until this is sent. */
val CMD_ENTER_BOOTLOADER = 0x38;
/* Command identifier for programming a single row of flash. */
val CMD_PROGRAM_ROW = 0x39;
/* Command identifier for verifying the contents of a single row of flash. */
val CMD_GET_ROW_CHECKSUM = 0x3A;
/* Command identifier for exiting the bootloader and restarting the target program. */
val CMD_EXIT_BOOTLOADER = 0x3B;
/* Command to erase data */
val CMD_ERASE_DATA = 0x44;
/* Command to program data. */
val CMD_PROGRAM_DATA = 0x49;
/* Command to verify data */
val CMD_VERIFY_DATA = 0x4A;
/* Command to set application metadata in bootloader SDK */
val CMD_SET_METADATA = 0x4C;
/* Command to set encryption initial vector */
val CMD_SET_EIV = 0x4D;

data class enterBootLoaderCmdData(val a:Int,val b:Int,val c:List<Int>,val d:Int)
data class bootLoaderCmdResultData(val a:Int,val b:Int,val c:Int, val d: Int,val e:Int)
data class exitBootLoaderCmdData(val a:Int,val b:Int,val c:List<Int>,val d: Int)
data class createProgramRowCmdData(val a:Int,val b: Int,val c:List<Int>,val data: Int)
data class createVerifyRowCmdData(val a:Int,val b:Int,val c:List<Int>,val d:Int)
data class createEraseRowCmdData(val a:Int,val b:Int,val c:List<Int>,val d: Int)
data class createVerifyChecksumCmdData(val a:Int,val b:Int,val c:List<Int>,val d:Int)
data class createGetFlashSizeCmdData(val a:Int,val b:Int,val c:List<Int>,val d:Int)
data class parseGetFlashSizeCmdResultData(val a:Int,val b:Int,val c:Int,val d:Int)
data class createSendDataCmdData(val a:Int,val b:Int,val c:List<Int>,val d:Int)
data class createGetAppStatusCmdData(val a:Int,val b:Int,val c:List<Int>,val d:Int)
data class parseGetAppStatusCmdResultData(val a:Int,val b:Int,val c:Int,val d:Int)
data class createSetActiveAppCmdData(val a:Int,val b:Int,val c:List<Int>,val d:Int)
data class createProgramDataCmdData(val a:Int,val b:Int,val c:List<Int>,val d:Int)
data class createVerifyDataCmdData(val a:Int,val b:Int,val c:List<Int>,val d:Int)
data class createEraseDataCmdData(val a:Int,val b:Int,val c:List<Int>,val d:Int)
data class createVerifyChecksumCmd_v1Data(val a:Int,val b:Int,val c:List<Int>,val d:Int)
data class createSetApplicationMetadataCmdData(val a:Int,val b:Int,val c:List<Int>,val d:Int)
data class createSetEncryptionInitialVectorCmdData(val a:Int,val b:Int,val c:List<Int>,val d:Int)

class cybtldr_command{
    enum class Cybtldr_ChecksumType(val value:Int){
        SUM_CHECKSUM(0x00),
        CRC_CHECKSUM(0x01);
    }

    var CyBtldr_Checksum:
        Cybtldr_ChecksumType = Cybtldr_ChecksumType.SUM_CHECKSUM
    fun fillData16(data:Int):List<Int> {
        val buf = mutableListOf<Int>()
        buf[0] = data and 0xff.toInt()
        buf[1] = data shr 8
        return buf.toList();
    }

    fun fillData32(data:Int):List<Int> {
        var ret = mutableListOf<Int>();
        var ret1 = fillData16(data);
        ret[0] = ret1[0];
        ret[1] = ret1[1];
        var sec = fillData16((data shr 16));
        ret[2] = sec[0];
        ret[3] = sec[1];
        return ret;
    }

    fun CyBtldr_ComputeChecksum16bit(buf:List<Int>, sizeIn:Int) : Int {
        var bufIndex = 0;
        var size = sizeIn
        if (CyBtldr_Checksum == Cybtldr_ChecksumType.CRC_CHECKSUM) {
            var crc = 0xffff;

            var tmp = 0;

            if (size == 0) return (crc.inv());

            do {
                tmp = 0x00ff and buf[bufIndex++];
                for (i in 0..8){
                    tmp = tmp shr 1
                    if (((crc and 0x0001) xor (tmp and 0x0001)) > 0) {
                    crc = (crc shr 1) xor 0x8408
                } else {
                    crc = crc shr 1;
                }
                }
            } while ((--size) > 0);

            crc = crc.inv();
            tmp = crc;
            crc = (crc shl 8) or (tmp shr 8 and 0xFF);

            return crc;
        } else /* SUM_CHECKSUM */
        {
            var sum = 0;
            while (size-- > 0) {
                sum += buf[bufIndex++];
            }

            return (1 + sum.inv());
        }
    }

    fun CyBtldr_ComputeChecksum32bit(buf:List<Int>, sizeIn:Int) : Int {
        var size = sizeIn

        val g0 = 0x82F63B78;
        val g1 = (g0 shr 1) and 0x7fffffff;
        val g2 = (g0 shr 2) and 0x3fffffff;
        val g3 = (g0 shr 3) and 0x1fffffff;
        val table = listOf(
            0,
            g3,
            g2,
            (g2 xor g3),
        g1,
        (g1 xor g3),
        (g1 xor g2),
        (g1 xor g2 xor g3),
        g0,
        (g0 xor g3),
        (g0 xor g2),
        (g0 xor g2 xor g3),
        (g0 xor g1),
        (g0 xor g1 xor g3),
        (g0 xor g1 xor g2),
        (g0 xor g1 xor g2 xor g3),
        );

        var bufIndex = 0;
        var crc = 0xFFFFFFFF;
        while (size != 0) {
            size = size -1;
            crc = crc xor (buf[bufIndex].toLong());
            bufIndex++;
            for (i in 0..1) {
                crc = (crc shr 4) xor table[(crc and 0xF).toInt()];
            }
        }
        return crc.inv().toInt();
    }

    fun CyBtldr_SetCheckSumType(chksumType:Cybtldr_ChecksumType) {
        CyBtldr_Checksum = chksumType;
    }

    fun ParseGenericCmdResult(cmdBuf:List<Int>,dataSize:Int,expectedSize:Int): Pair<Int,Int> {
        var err = CYRET_SUCCESS;
        val cmdSize = dataSize + BASE_CMD_SIZE;
        var status = cmdBuf[1];
        if (cmdSize != expectedSize) {
            err = CYRET_ERR_LENGTH;
        } else if (status != CYRET_SUCCESS) {
            err = CYRET_ERR_BTLDR_MASK or (status);
        } else if (cmdBuf[0] != CMD_START ||
            cmdBuf[2] != (dataSize) ||
            cmdBuf[3] != ((dataSize shr 8)) ||
        cmdBuf[cmdSize - 1] != CMD_STOP) {
            err = CYRET_ERR_DATA;
        }
        return Pair(err, status);
    }

    fun CyBtldr_ParseDefaultCmdResult(cmdBuf:List<Int>, cmdSize:Int): Pair<Int,Int> {
        return ParseGenericCmdResult(cmdBuf, 0, cmdSize);
    }

// NOTE: If the cmd contains data bytes, make sure to call this after setting data bytes.
// Otherwise the checksum here will not include the data bytes.
   fun CreateCmd(cmdBufIn:List<Int>, cmdSize:Int, cmdCode:Int): Pair<Int,List<Int>> {
        var cmdBuf = cmdBufIn.toMutableList()
        var checksum = 0;
        cmdBuf[0] = CMD_START;
        cmdBuf[1] = cmdCode;
        val t1 = fillData16(cmdSize - BASE_CMD_SIZE);
        cmdBuf[2] = t1[0];
        cmdBuf[3] = t1[1];
        checksum = CyBtldr_ComputeChecksum16bit(cmdBuf, cmdSize - 3);
        val t2 = fillData16(checksum);
        cmdBuf[cmdSize - 3] = t2[0];
        cmdBuf[cmdSize - 2] = t2[1];
        cmdBuf[cmdSize - 1] = CMD_STOP;
        return Pair(CYRET_SUCCESS, cmdBuf);
    }

    fun CyBtldr_CreateEnterBootLoaderCmd(securityKeyBuf:List<Int>): enterBootLoaderCmdData {
        val RESULT_DATA_SIZE = 8;
        val BOOTLOADER_SECURITY_KEY_SIZE = 6;
        var commandDataSize = 0;
        val resSize = BASE_CMD_SIZE + RESULT_DATA_SIZE;

        var cmdBuf = mutableListOf<Int>();

        if (securityKeyBuf.isNotEmpty()) {
            commandDataSize = BOOTLOADER_SECURITY_KEY_SIZE;
        } else {
            commandDataSize = 0;
        }
        var cmdSize = BASE_CMD_SIZE + commandDataSize;

        for (i in 0..commandDataSize) {
            cmdBuf[i + 4] = securityKeyBuf[i];
        }
        var (err, cmdBufT) = CreateCmd(cmdBuf, cmdSize, CMD_ENTER_BOOTLOADER)
        cmdBuf = cmdBufT.toMutableList()
        return enterBootLoaderCmdData(err, cmdSize, cmdBuf, resSize);
    }

    fun CyBtldr_CreateEnterBootLoaderCmd_v1(productID:Int):enterBootLoaderCmdData {
        val COMMAND_DATA_SIZE = 6;
        val RESULT_DATA_SIZE = 8;
        val resSize = BASE_CMD_SIZE + RESULT_DATA_SIZE;
        val cmdSize = BASE_CMD_SIZE + COMMAND_DATA_SIZE;
        var cmdBuf = mutableListOf<Int>();

        var fill1 = fillData32(productID);
        cmdBuf[0] = fill1[0];
        cmdBuf[1] = fill1[1];
        cmdBuf[2] = fill1[2];
        cmdBuf[3] = fill1[3];

        cmdBuf[8] = 0;
        cmdBuf[9] = 0;
        var (err, cmdBufT) = CreateCmd(cmdBuf, cmdSize, CMD_ENTER_BOOTLOADER);
        cmdBuf = cmdBufT.toMutableList()
        return enterBootLoaderCmdData(err, cmdSize, cmdBuf, resSize);
    }

    fun CyBtldr_ParseEnterBootLoaderCmdResult(
    cmdBuf:List<Int>,
    cmdSize:Int
    ): bootLoaderCmdResultData {
        val RESULT_DATA_SIZE = 8;

        var siliconId = 0;
        var siliconRev = 0;
        var blVersion = 0;
        var (err, status) = ParseGenericCmdResult(cmdBuf, RESULT_DATA_SIZE, cmdSize);

        if (CYRET_SUCCESS == err) {
            siliconId =
                (cmdBuf[7] shl  24) or (cmdBuf[6] shl 16) or (cmdBuf[5] shl 8) or cmdBuf[4];
            siliconRev = cmdBuf[8];
            blVersion = (cmdBuf[11] shl 16) or (cmdBuf[10] shl 8) or cmdBuf[9];
        }
        return bootLoaderCmdResultData(err, siliconId, siliconRev, blVersion, status);
    }

    fun CyBtldr_CreateExitBootLoaderCmd():exitBootLoaderCmdData {
        val cmdSize = BASE_CMD_SIZE;
        val resSize = BASE_CMD_SIZE;
        var cmdBuf = mutableListOf<Int>();
        var (err, cmdBufOut) = CreateCmd(cmdBuf, cmdSize, CMD_EXIT_BOOTLOADER);
        return exitBootLoaderCmdData(err, cmdSize, cmdBufOut, resSize);
    }

    fun CyBtldr_CreateProgramRowCmd(arrayId:Int, rowNum:Int, buf:List<Int>, size:Int, cmdBufIn:List<Int>):createProgramRowCmdData {
        val COMMAND_DATA_SIZE = 3;
        val resSize = BASE_CMD_SIZE;
        val cmdSize = BASE_CMD_SIZE + COMMAND_DATA_SIZE + size;
        val cmdBuf = cmdBufIn.toMutableList()
        cmdBuf[4] = arrayId;
        var fill1 = fillData16(rowNum);
        cmdBuf[5] = fill1[0];
        cmdBuf[6] = fill1[1];
        for (i in 0..size) {
            cmdBuf[i + 7] = buf[i];
        }
        var (err, cmdBufOut) = CreateCmd(cmdBuf, cmdSize, CMD_PROGRAM_ROW);
        return createProgramRowCmdData(err, cmdSize, cmdBufOut, resSize);
    }

    fun CyBtldr_ParseProgramRowCmdResult(cmdBuf:List<Int>, cmdSize:Int): Pair<Int,Int> {
        return CyBtldr_ParseDefaultCmdResult(cmdBuf, cmdSize);
    }

    fun CyBtldr_CreateVerifyRowCmd(arrayId:Int, rowNum:Int):createVerifyRowCmdData  {
        val RESULT_DATA_SIZE = 1;
        val COMMAND_DATA_SIZE = 3;
        var resSize = BASE_CMD_SIZE + RESULT_DATA_SIZE;
        var cmdSize = BASE_CMD_SIZE + COMMAND_DATA_SIZE;
        var cmdBuf = mutableListOf<Int>();
        cmdBuf[4] = arrayId;
        val fill1 = fillData16(rowNum);
        cmdBuf[5] = fill1[0];
        cmdBuf[6] = fill1[1];

        var (err, cmdBufOut) = CreateCmd(cmdBuf, cmdSize, CMD_GET_ROW_CHECKSUM);

        return createVerifyRowCmdData(err, cmdSize, cmdBufOut, resSize);
    }

    fun CyBtldr_ParseVerifyRowCmdResult( cmdBuf:List<Int>, cmdSize:Int): Triple<Int,Int,Int> {
        val RESULT_DATA_SIZE = 1;
        var checksum = 0;
        var (err, status) = ParseGenericCmdResult(cmdBuf, RESULT_DATA_SIZE, cmdSize);
        if (CYRET_SUCCESS == err) {
            checksum = cmdBuf[4];
        }
        return Triple(err, checksum, status);
    }

    fun CyBtldr_CreateEraseRowCmd(arrayId:Int, rowNum:Int): createEraseRowCmdData {
        val COMMAND_DATA_SIZE = 3;
        var resSize = BASE_CMD_SIZE;
        var cmdSize = BASE_CMD_SIZE + COMMAND_DATA_SIZE;
        var cmdBuf = mutableListOf<Int>();
        cmdBuf[4] = arrayId;
        val fill = fillData16(rowNum);
        cmdBuf[5] = fill[0];
        cmdBuf[6] = fill[1];
        var (err, cmdBufOut) = CreateCmd(cmdBuf, cmdSize, CMD_ERASE_ROW);
        return createEraseRowCmdData(err, cmdSize, cmdBufOut, resSize);
    }

    fun CyBtldr_ParseEraseRowCmdResult(cmdBuf:List<Int>, cmdSize:Int): Pair<Int,Int> {
        return CyBtldr_ParseDefaultCmdResult(cmdBuf, cmdSize);
    }

    fun CyBtldr_CreateVerifyChecksumCmd(): createVerifyChecksumCmdData {
        val RESULT_DATA_SIZE = 1;
        val cmdSize = BASE_CMD_SIZE;
        val resSize = BASE_CMD_SIZE + RESULT_DATA_SIZE;
        var cmdBuf = mutableListOf<Int>();

        var (err, cmdBufOut) = CreateCmd(cmdBuf, cmdSize, CMD_VERIFY_CHECKSUM);
        return createVerifyChecksumCmdData(err, cmdSize, cmdBuf, resSize);
    }

    fun CyBtldr_ParseVerifyChecksumCmdResult(
     cmdBuf:List<Int>, cmdSize:Int): Triple<Int,Int,Int> {
        val RESULT_DATA_SIZE = 1;
        var (err, status) = ParseGenericCmdResult(cmdBuf, RESULT_DATA_SIZE, cmdSize);
        var checksumValid = 0;
        if (CYRET_SUCCESS == err) {
            checksumValid = cmdBuf[4];
        }
        return Triple(err, checksumValid, status);
    }

    fun CyBtldr_CreateGetFlashSizeCmd(arrayId:Int): createGetFlashSizeCmdData {
        val RESULT_DATA_SIZE = 4;
        val COMMAND_DATA_SIZE = 1;
        val resSize = BASE_CMD_SIZE + RESULT_DATA_SIZE;
        val cmdSize = BASE_CMD_SIZE + COMMAND_DATA_SIZE;
        val cmdBuf = mutableListOf<Int>();
        cmdBuf[4] = arrayId;

        var (err, cmdBufOut) = CreateCmd(cmdBuf, cmdSize, CMD_GET_FLASH_SIZE);

        return createGetFlashSizeCmdData(err, cmdSize, cmdBufOut, resSize);
    }

    fun CyBtldr_ParseGetFlashSizeCmdResult(cmdBuf:List<Int>, cmdSize:Int): parseGetFlashSizeCmdResultData{
        val RESULT_DATA_SIZE = 4;
        var (err, status) = ParseGenericCmdResult(cmdBuf, RESULT_DATA_SIZE, cmdSize);
        var startRow = 0;
        var endRow = 0;
        if (CYRET_SUCCESS == err) {
            startRow = (cmdBuf[5] shl  8) or cmdBuf[4];
            endRow = (cmdBuf[7] shl  8) or cmdBuf[6];
        }
        return parseGetFlashSizeCmdResultData(err, startRow, endRow, status);
    }

    fun CyBtldr_CreateSendDataCmd(
    buf:List<Int>, size:Int, cmdBuf:MutableList<Int>): createSendDataCmdData {
        val resSize = BASE_CMD_SIZE;
        val cmdSize = size + BASE_CMD_SIZE;

        for (i in 0..size) {
            cmdBuf[i + 4] = buf[i];
        }
        var (err, cmdBuf) = CreateCmd(cmdBuf, cmdSize, CMD_SEND_DATA);
        return createSendDataCmdData(err, cmdSize, cmdBuf, resSize);
    }

    fun CyBtldr_ParseSendDataCmdResult(cmdBuf:List<Int>, cmdSize:Int): Pair<Int,Int> {
        return CyBtldr_ParseDefaultCmdResult(cmdBuf, cmdSize);
    }
    /*
    (int, int, int) CyBtldr_CreateSyncBootLoaderCmd(Uint8List cmdBuf) {
      int cmdSize = BASE_CMD_SIZE;
      int resSize = BASE_CMD_SIZE;

      return (CreateCmd(cmdBuf, cmdSize, CMD_SYNC), cmdSize, resSize);
    }*/

    fun CyBtldr_CreateGetAppStatusCmd(appId:Int): createGetAppStatusCmdData {
        val RESULT_DATA_SIZE = 2;
        val COMMAND_DATA_SIZE = 1;
        val resSize = BASE_CMD_SIZE + RESULT_DATA_SIZE;
        val cmdSize = BASE_CMD_SIZE + COMMAND_DATA_SIZE;
        var cmdBuf = mutableListOf<Int>();

        cmdBuf[4] = appId;
        var (err, cmdBufOut) = CreateCmd(cmdBuf, cmdSize, CMD_GET_APP_STATUS);

        return createGetAppStatusCmdData(err, cmdSize, cmdBufOut, resSize);
    }

    fun CyBtldr_ParseGetAppStatusCmdResult(
     cmdBuf:List<Int>, cmdSize:Int):parseGetAppStatusCmdResultData {
        val RESULT_DATA_SIZE = 2;
        var (err, status) = ParseGenericCmdResult(cmdBuf, RESULT_DATA_SIZE, cmdSize);
        var isValid = 0;
        var isActive = 0;
        if (CYRET_SUCCESS == err) {
            isValid = cmdBuf[4];
            isActive = cmdBuf[5];
        }
        return parseGetAppStatusCmdResultData(err, isValid, isActive, status);
    }

    fun CyBtldr_CreateSetActiveAppCmd(appId:Int): createSetActiveAppCmdData {
        val COMMAND_DATA_SIZE = 1;
        val resSize = BASE_CMD_SIZE;
        val cmdSize = BASE_CMD_SIZE + COMMAND_DATA_SIZE;

        val cmdBuf = mutableListOf<Int>();

        cmdBuf[4] = appId;

        var (err, cmdBufOut) = CreateCmd(cmdBuf, cmdSize, CMD_SET_ACTIVE_APP);
        return createSetActiveAppCmdData(err, cmdSize, cmdBufOut, resSize);
    }

    fun CyBtldr_ParseSetActiveAppCmdResult(cmdBuf:List<Int>, cmdSize:Int): Pair<Int,Int> {
        return CyBtldr_ParseDefaultCmdResult(cmdBuf, cmdSize);
    }

    fun CyBtldr_CreateProgramDataCmd(
    address:Int, chksum:Int, buf:List<Int>, size:Int, cmdBuf:MutableList<Int>): createProgramDataCmdData {
        val COMMAND_DATA_SIZE = 8;
        val resSize = BASE_CMD_SIZE;
        val cmdSize = BASE_CMD_SIZE + COMMAND_DATA_SIZE + size;

        val fill1 = fillData32(address);
        cmdBuf[0] = fill1[0];
        cmdBuf[1] = fill1[1];
        cmdBuf[2] = fill1[2];
        cmdBuf[3] = fill1[3];
        val fill2 = fillData32(chksum);
        cmdBuf[4] = fill2[0];
        cmdBuf[5] = fill2[1];
        cmdBuf[6] = fill2[2];
        cmdBuf[7] = fill2[3];
        for (i in 0..size) {
            cmdBuf[i + 4 + COMMAND_DATA_SIZE] = buf[i];
        }
        var (err, cmdBuf) = CreateCmd(cmdBuf, cmdSize, CMD_PROGRAM_DATA);
        return createProgramDataCmdData(err, cmdSize, cmdBuf, resSize);
    }

    fun CyBtldr_CreateVerifyDataCmd(
    address:Int, chksum:Int, buf:List<Int>, size:Int): createVerifyDataCmdData {
        val COMMAND_DATA_SIZE = 8;
        val resSize = BASE_CMD_SIZE;
        val cmdSize = BASE_CMD_SIZE + COMMAND_DATA_SIZE + size;
        var cmdBuf = mutableListOf<Int>()

        var fill1 = fillData32(address);
        cmdBuf[0] = fill1[0];
        cmdBuf[1] = fill1[1];
        cmdBuf[2] = fill1[2];
        cmdBuf[3] = fill1[3];

        var fill2 = fillData32(chksum);
        cmdBuf[4] = fill2[0];
        cmdBuf[5] = fill2[1];
        cmdBuf[6] = fill2[2];
        cmdBuf[7] = fill2[3];

        for (i in 0..size) {
            cmdBuf[i + 4 + COMMAND_DATA_SIZE] = buf[i];
        }

        var (err, cmdBufOut) = CreateCmd(cmdBuf, cmdSize, CMD_VERIFY_DATA);

        return createVerifyDataCmdData(err, cmdSize, cmdBufOut, resSize);
    }

    fun CyBtldr_CreateEraseDataCmd(address:Int): createEraseDataCmdData {
        val COMMAND_DATA_SIZE = 4;
        val resSize = BASE_CMD_SIZE;
        val cmdSize = BASE_CMD_SIZE + COMMAND_DATA_SIZE;
        val cmdBuf = mutableListOf<Int>();

        var fill = fillData32(address);
        cmdBuf[0] = fill[0];
        cmdBuf[1] = fill[1];
        cmdBuf[2] = fill[2];
        cmdBuf[3] = fill[3];

        var (err, cmdBufOut) = CreateCmd(cmdBuf, cmdSize, CMD_ERASE_DATA);

        return createEraseDataCmdData(err, cmdSize, cmdBufOut, resSize);
    }

    fun CyBtldr_CreateVerifyChecksumCmd_v1(appId:Int): createVerifyChecksumCmd_v1Data {
        val COMMAND_DATA_SIZE = 1;
        val resSize = BASE_CMD_SIZE + 1;
        val cmdSize = BASE_CMD_SIZE + COMMAND_DATA_SIZE;
        val cmdBuf = mutableListOf<Int>();
        cmdBuf[4] = appId;
        var (err, cmdBufOut) = CreateCmd(cmdBuf, cmdSize, CMD_VERIFY_CHECKSUM);
        return createVerifyChecksumCmd_v1Data(err, cmdSize, cmdBufOut, resSize);
    }

    fun CyBtldr_CreateSetApplicationMetadataCmd(
    appID:Int, buf:List<Int>): createSetApplicationMetadataCmdData {
        val BTDLR_SDK_METADATA_SIZE = 8;
        val COMMAND_DATA_SIZE = BTDLR_SDK_METADATA_SIZE + 1;
        val resSize = BASE_CMD_SIZE;
        val cmdSize = BASE_CMD_SIZE + COMMAND_DATA_SIZE;
        val cmdBuf = mutableListOf<Int>();
        cmdBuf[4] = appID;
        for (i in 0..BTDLR_SDK_METADATA_SIZE) {
            cmdBuf[5 + i] = buf[i];
        }
        var (err, cmdBufOut) = CreateCmd(cmdBuf, cmdSize, CMD_SET_METADATA);
        return createSetApplicationMetadataCmdData(err, cmdSize, cmdBufOut, resSize);
    }

    fun CyBtldr_CreateSetEncryptionInitialVectorCmd(
    buf:List<Int>, size:Int): createSetEncryptionInitialVectorCmdData {
        val resSize = BASE_CMD_SIZE;
        val cmdSize = BASE_CMD_SIZE + size;
        val cmdBuf = mutableListOf<Int>();
        for (i in 0..size) {
            cmdBuf[4 + i] = buf[i];
        }
        var (err, cmdBufOut) = CreateCmd(cmdBuf, cmdSize, CMD_SET_EIV);
        return createSetEncryptionInitialVectorCmdData(err, cmdSize, cmdBuf, resSize);
    }

//Try to parse a packet to determine its validity, if valid then return set the status param to the packet's status.
//Used to generate useful error messages. return 1 on success 0 otherwise.
    fun CyBtldr_TryParseParketStatus(packet:List<Int>, packetSize:Int): Pair<Int,Int> {
        if (packet.isEmpty() || packetSize < BASE_CMD_SIZE || packet[0] != CMD_START) {
            return Pair(CYBTLDR_STAT_ERR_UNK, 0);
        }
        val status = packet[1];
        val dataSize = packet[2] or (packet[3] shl  8);

        val readChecksum = packet[dataSize + 4] or (packet[dataSize + 5] shl  8);
        val computedChecksum =
            CyBtldr_ComputeChecksum16bit(packet, BASE_CMD_SIZE + dataSize - 3);

        if (packet[dataSize + BASE_CMD_SIZE - 1] != CMD_STOP ||
            readChecksum != computedChecksum) {
            return Pair(CYBTLDR_STAT_ERR_UNK, status);
        }
        return Pair(CYRET_SUCCESS, status);
    }

}