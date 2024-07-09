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

import java.io.File
import java.net.URI

data class RowData_v1Ret(val x: Int, val y: Long, val z: List<Int>, val a: Int, val b: Int)

@OptIn(ExperimentalStdlibApi::class)
class cybtldr_parse {
    companion object {

        val MAX_BUFFER_SIZE = 768

        var dataFileData = mutableListOf<String?>(null)

        var dataFilePath = ""

        fun parse2ByteValueBigEndian(buf: List<Int>): Int {
            var toReturn = (buf[0] shl 8)
            return toReturn or (buf[1])
        }

        fun parse4ByteValueBigEndian(buf: List<Int>): Int {
            return ((parse2ByteValueBigEndian(buf)) shl 16) or
                (parse2ByteValueBigEndian(
                    buf.subList(2, 4)
                ))
        }

        fun parse2ByteValueLittleEndian(buf: List<Int>): Int {
            return (buf[0]) or ((buf[1]) shl 8)
        }

        fun parse4ByteValueLittleEndian(buf: List<Int>): Long {
            return (parse2ByteValueLittleEndian(buf)).toLong() or
                ((parse2ByteValueLittleEndian(
                    buf.subList(2, 4)
                )) shl
                    16).toLong()
        }

        @ExperimentalStdlibApi
        fun CyBtldr_FromHex(value: String): Int {
            return value.hexToInt()
        }

        //(int, Uint8List, int) CyBtldr_FromAscii(int bufSize, Uint8List buffer) {
        @ExperimentalStdlibApi
        fun CyBtldr_FromAscii(bufSize: Int, buffer: String): Triple<Int, List<Int>, Int> {
            var i = 0
            var rowSize = 0
            val hexData = mutableListOf<Int>()
            var err = CYRET_SUCCESS

            if ((bufSize and 1) == 1) // Make sure even number of bytes
            {
                err = CYRET_ERR_LENGTH;
            } else {
                /*for (i = 0; i < bufSize / 2; i++) {
            hexData[i] =
                (int.parse(String.fromCharCode(buffer[i * 2]), radix: 16) << 4) |
            int.parse(String.fromCharCode(buffer[i * 2 + 1]), radix: 16);
        }*/
                for (index in 0..bufSize / 2) {
                    hexData[index] =
                        (buffer[i * 2].toString().hexToInt() shl 4) or buffer[(i * 2) + 1].toString().hexToInt()
                    i += 1
                }

                rowSize = i
            }

            return Triple(err, hexData, rowSize);
        }

        //(int, String, int) CyBtldr_ReadLine(int lineIndex) {
        fun CyBtldr_ReadLine(lineI: Int): Triple<Int, String, Int> {
            var lineIndex = lineI
            var err = CYRET_SUCCESS
            var lineFound = false
            var toReturn = "";
            // line that start with '#' are assumed to be comments, continue reading if we read a comment

            while (!lineFound) {
                if (dataFileData.isNotEmpty()) {
                    if (lineIndex < dataFileData.size) {
                        val firstChar = dataFileData[lineIndex]?.get(0)
                        if (firstChar != '#' || firstChar != null) {
                            val strLen = dataFileData[lineIndex]!!.length;
                            toReturn = dataFileData[lineIndex]!!;
                            if (toReturn[strLen - 1] == '\n' && toReturn[strLen - 2] == '\r') {
                                toReturn = toReturn.substring(0, strLen - 2);
                            }
                            lineFound = true;
                        } else {
                            lineIndex++;
                        }
                    } else {
                        err = CYRET_ERR_EOF;
                        lineFound = true;
                    }
                } else {
                    err = CYRET_ERR_FILE;
                    lineFound = true;
                }
            }
            return Triple(err, toReturn, toReturn.length);
        }

        //Future<int> CyBtldr_OpenDataFile(File file) async {
        fun CyBtldr_OpenDataFile(fileName: URI): Int {
            try {
                File(fileName).useLines { dataFileData = it.toList().toMutableList() }
            } catch (e: Exception) {
                return CYRET_ERR_FILE;
            }
            return CYRET_SUCCESS;
        }

        //(int, int) CyBtldr_ParseCyacdFileVersion(
        //String fileName, int bufSize, Uint8List header) {
        fun CyBtldr_ParseCyacdFileVersion(
            fileName: String,
            bufSize: Int,
            header: String
        ): Pair<Int, Int> {
            // check file extension of the file, if extension is cyacd, version 0
            var index = fileName.length
            var err = CYRET_SUCCESS

            var version = 0
            if (bufSize == 0) {
                err = CYRET_ERR_FILE
            }
            while (CYRET_SUCCESS == err && fileName[--index] != '.') {
                if (index == 0) {
                    err = CYRET_ERR_FILE
                }
            }
            if (fileName.substring(index).lowercase() == ".cyacd2") {
                if (bufSize < 2) {
                    err = CYRET_ERR_FILE
                }
                // .cyacd2 file stores version information in the first byte of the file header.
                if (CYRET_SUCCESS == err) {
                    version = (header[0].toString().hexToInt() shl 4) or header[1].toString().hexToInt();
                    if (version == 0) {
                        err = CYRET_ERR_DATA
                    }
                }
            } else if (fileName.substring(index).lowercase() == ".cyacd") {
                // .cyacd file does not contain version information
                version = 0;
            } else {
                err = CYRET_ERR_FILE
            }
            return Pair(err, version)
        }

        //(int, int, int, int) CyBtldr_ParseHeader(int bufSize, Uint8List buffer) {
        fun CyBtldr_ParseHeader(
            bufSize: Int,
            buffer: String
        ): Pair<Pair<Int, Int>, Pair<Int, Int>> {
            var err = CYRET_SUCCESS
            var chksum = 0
            var siliconId = 0
            var siliconRev = 0
            var rowSize = 0
            var rowData = listOf<Int>()
            if (CYRET_SUCCESS == err) {
                var (errT, rowDataT, rowSizeT) = CyBtldr_FromAscii(bufSize, buffer)
                err = errT
                rowData = rowDataT
                rowSize = rowSizeT
            }
            if (CYRET_SUCCESS == err) {
                if (rowSize > 5) {
                    chksum = rowData[5];
                } else {
                    chksum = 0;
                }
                if (rowSize > 4) {
                    siliconId = parse4ByteValueBigEndian(rowData);
                    siliconRev = rowData[4];
                } else {
                    err = CYRET_ERR_LENGTH;
                }
            }
            return Pair(Pair(err, siliconId), Pair(siliconRev, chksum))
        }

        //(int, int, int, int, int, int) CyBtldr_ParseHeader_v1(
        //int bufSize, Uint8List buffer) {
        fun CyBtldr_ParseHeader_v1(
            bufSize: Int,
            buffer: String
        ): Pair<Triple<Int, Long, Int>, Triple<Int, Int, Long>> {
            var err = CYRET_SUCCESS
            var siliconId = 0.toLong();
            var siliconRev = 0;
            var chksum = 0;
            var appID = 0;
            var productID = 0.toLong();

            var rowSize = 0;
            var rowData = listOf<Int>()
            if (CYRET_SUCCESS == err) {
                var (errT, rowDataT, rowSizeT) = CyBtldr_FromAscii(bufSize, buffer)
                err = errT
                rowData = rowDataT
                rowSize = rowSizeT
            }
            if (CYRET_SUCCESS == err) {
                if (rowSize == 12) {
                    siliconId = parse4ByteValueLittleEndian(
                        rowData.subList(1, 5)
                    )
                    siliconRev = rowData[5]
                    chksum = rowData[6]
                    appID = rowData[7]
                    productID = parse4ByteValueLittleEndian(
                        rowData.subList(8, 13)
                    );
                } else {
                    err = CYRET_ERR_LENGTH;
                }
            }
            return Pair(Triple(err, siliconId, siliconRev), Triple(chksum, appID, productID))
        }

        //(int, int, int, Uint8List, int, int) CyBtldr_ParseRowData(
        //int bufSize, Uint8List buffer) {
        fun CyBtldr_ParseRowData(
            bufSize: Int,
            buffer: String
        ): Pair<Triple<Int, Int, Int>, Triple<List<Int>, Int, Int>> {
            val MIN_SIZE = 6; //1-array, 2-addr, 2-size, 1-checksum
            val DATA_OFFSET = 5;

            var i = 0;
            var err = CYRET_SUCCESS;
            var rowNum = 0;
            var arrayId = 0;
            var size = 0;
            var checksum = 0;
            var rowData = mutableListOf<Int>();

            if (bufSize <= MIN_SIZE) {
                err = CYRET_ERR_LENGTH;
            } else if (buffer[0] == ':') {
                var (errT, hexData, hexSize) = CyBtldr_FromAscii(
                    bufSize - 1,
                    buffer.substring(1, buffer.length)
                )
                err = errT

                if (err == CYRET_SUCCESS) {
                    arrayId = hexData[0];
                    rowNum = parse2ByteValueBigEndian(hexData.subList(1, 3))
                    size = parse2ByteValueBigEndian(hexData.subList(3, 5))
                    checksum = (hexData[hexSize - 1]);

                    if ((size + MIN_SIZE) == hexSize) {
                        for (i in 0..size) {
                            rowData[i] = (hexData[DATA_OFFSET + i]);
                        }
                    } else {
                        err = CYRET_ERR_DATA;
                    }
                }
            } else {
                err = CYRET_ERR_CMD;
            }

            return Pair(Triple(err, arrayId, rowNum), Triple(rowData.toList(), size, checksum))
        }

        //(int, int, Uint8List, int, int) CyBtldr_ParseRowData_v1(
        //int bufSize, String buffer) {
        fun CyBtldr_ParseRowData_v1(bufSize: Int, buffer: String): RowData_v1Ret {
            val MIN_SIZE = 4; //4-addr
            val DATA_OFFSET = 4;
            var err = CYRET_SUCCESS;
            var address = 0.toLong();
            var rowData = mutableListOf<Int>();
            var checksum = 0;
            var size = 0;

            if (bufSize <= MIN_SIZE) {
                err = CYRET_ERR_LENGTH;
            } else if (buffer[0] == ':') {
                var (errOut, hexData, hexSize) = CyBtldr_FromAscii(
                    bufSize - 1,
                    buffer.substring(1, 2)
                );
                err = errOut

                if (CYRET_SUCCESS == err) {
                    address = parse4ByteValueLittleEndian(hexData);
                    checksum = 0;

                    if (MIN_SIZE < hexSize) {
                        size = hexSize - MIN_SIZE;
                        for (i in 0..size) {
                            rowData[i] = (hexData[DATA_OFFSET + i]);
                            checksum += rowData[i];
                        }
                    } else {
                        err = CYRET_ERR_DATA;
                    }
                }
            } else {
                err = CYRET_ERR_CMD;
            }

            return RowData_v1Ret(err, address, rowData, size, checksum)
        }

        //(int, int, int) CyBtldr_ParseAppStartAndSize_v1(
        //int appStart, int appSize, int lineIndex) {
        fun CyBtldr_ParseAppStartAndSize_v1(
            appStartIn: Long,
            appSizeIn: Int,
            lineIndex: Int
        ): Triple<Int, Long, Int> {
            val APPINFO_META_HEADER_SIZE = 11;
            val APPINFO_META_HEADER = "@APPINFO:0x";
            val APPINFO_META_SEPERATOR_SIZE = 3;
            val APPINFO_META_SEPERATOR = ",0x";
            val APPINFO_META_SEPERATOR_START = ",";

            var appStart = appStartIn
            var appSize = appSizeIn

            //long fp = ftell(dataFile);
            appStart = 0xffffffff;
            appSize = 0;
            var seperatorIndex = 0;
            var err = CYRET_SUCCESS;
            var i = APPINFO_META_HEADER_SIZE;
            do {
                var (e, s, rowLength) = CyBtldr_ReadLine(lineIndex);
                err = e;
                if (err == CYRET_SUCCESS) {
                    if (s[0] == ':') {
                        var (_, addr, _, rowSize, _) = CyBtldr_ParseRowData_v1(rowLength, s);

                        if (addr < appStart) {
                            appStart = addr;
                        }
                        appSize += rowSize;
                    } else if (rowLength >= APPINFO_META_HEADER_SIZE &&
                        strncmp(s, APPINFO_META_HEADER, APPINFO_META_HEADER_SIZE) == 0
                    ) {
                        // find seperator index
                        seperatorIndex = s.indexOf(APPINFO_META_SEPERATOR_START);
                        if (strncmp(
                                s + seperatorIndex.toString(), APPINFO_META_SEPERATOR,
                                APPINFO_META_SEPERATOR_SIZE
                            ) ==
                            0
                        ) {
                            appStart = 0;
                            appSize = 0;
                            for (index in APPINFO_META_HEADER_SIZE..seperatorIndex) {
                                appStart = appStart shl 4;
                                appStart += CyBtldr_FromHex(s[i].toString());
                                i++
                            }
                            for (index in seperatorIndex + APPINFO_META_SEPERATOR_SIZE..rowLength) {
                                appSize = appSize shl 4;
                                appSize += CyBtldr_FromHex(s[i].toString());
                            }
                        } else {
                            err = CYRET_ERR_FILE;
                        }
                        break;
                    }
                }
            } while (err == CYRET_SUCCESS);
            if (err == CYRET_ERR_EOF) {
                err = CYRET_SUCCESS;
            }
            // reset to the file to where we were
            if (err == CYRET_SUCCESS) {
                //fseek(dataFile, fp, SEEK_SET);
            }
            return Triple(err, appStart, appSize);
        }

        fun CyBtldr_CloseDataFile(): Int {
            return CYRET_SUCCESS;
        }

        fun strncmp(s1: String, s2: String, num: Int): Int {
            var con = true;
            var index = 0;
            while (con) {
                if (index < num) {
                    if (s1.length > index && s2.length > index) {
                        if (!s1[index].equals(s2)) {
                            return 0;
                        } else {
                            index++;
                        }
                    } else {
                        con = false;
                    }
                } else {
                    con = false;
                }
            }
            return 1;
        }
    }

}