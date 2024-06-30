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

/// ****************************************************************************
///    HOST ERROR CODES
///*****************************************************************************
///
/// Different return codes from the bootloader host.  Functions are not
/// limited to these values, but are encouraged to use them when returning
/// standard error values.
///
/// 0 is successful, all other values indicate a failure.
///***************************************************************************
/* Completed successfully */
var CYRET_SUCCESS = 0x00

/* File is not accessible */
var CYRET_ERR_FILE = 0x01

/* Reached the end of the file */
var CYRET_ERR_EOF = 0x02

/* The amount of data available is outside the expected range */
var CYRET_ERR_LENGTH = 0x03

/* The data is not of the proper form */
var CYRET_ERR_DATA = 0x04

/* The command is not recognized */
var CYRET_ERR_CMD = 0x05

/* The expected device does not match the detected device */
var CYRET_ERR_DEVICE = 0x06

/* The bootloader version detected is not supported */
var CYRET_ERR_VERSION = 0x07

/* The checksum does not match the expected value */
var CYRET_ERR_CHECKSUM = 0x08

/* The flash array is not valid */
var CYRET_ERR_ARRAY = 0x09

/* The flash row is not valid */
var CYRET_ERR_ROW = 0x0A

/* The bootloader is not ready to process data */
var CYRET_ERR_BTLDR = 0x0B

/* The application is currently marked as active */
var CYRET_ERR_ACTIVE = 0x0C

/* An unknown error occurred */
var CYRET_ERR_UNK = 0x0F

/* The operation was aborted */
var CYRET_ABORT = 0xFF

/* The communications object reported an error */
var CYRET_ERR_COMM_MASK = 0x2000

/* The bootloader reported an error */
var CYRET_ERR_BTLDR_MASK = 0x4000

/// ****************************************************************************
///    BOOTLOADER STATUS CODES
///*****************************************************************************
///
/// Different return status codes from the bootloader.
///
/// 0 is successful, all other values indicate a failure.
///***************************************************************************
/* Completed successfully */
var CYBTLDR_STAT_SUCCESS = 0x00

/* The provided key does not match the expected value */
var CYBTLDR_STAT_ERR_KEY = 0x01

/* The verification of flash failed */
var CYBTLDR_STAT_ERR_VERIFY = 0x02

/* The amount of data available is outside the expected range */
var CYBTLDR_STAT_ERR_LENGTH = 0x03

/* The data is not of the proper form */
var CYBTLDR_STAT_ERR_DATA = 0x04

/* The command is not recognized */
var CYBTLDR_STAT_ERR_CMD = 0x05

/* The expected device does not match the detected device */
var CYBTLDR_STAT_ERR_DEVICE = 0x06

/* The bootloader version detected is not supported */
var CYBTLDR_STAT_ERR_VERSION = 0x07

/* The checksum does not match the expected value */
var CYBTLDR_STAT_ERR_CHECKSUM = 0x08

/* The flash array is not valid */
var CYBTLDR_STAT_ERR_ARRAY = 0x09

/* The flash row is not valid */
var CYBTLDR_STAT_ERR_ROW = 0x0A

/* The flash row is protected and can not be programmed */
var CYBTLDR_STAT_ERR_PROTECT = 0x0B

/* The application is not valid and cannot be set as active */
var CYBTLDR_STAT_ERR_APP = 0x0C

/* The application is currently marked as active */
var CYBTLDR_STAT_ERR_ACTIVE = 0x0D

/* An unknown error occurred */
var CYBTLDR_STAT_ERR_UNK = 0x0F

/// ****************************************************************************
///    VERSION INFORMATION
///*****************************************************************************
///
/// Major � Used to indicate binary compatibility.  If a change is incompatible
///         in any way with the prior release, the major version number will be
///         updated.
/// Minor � Used to indicate feature set.  If a new feature or functionality is
///         added beyond what was available in a prior release, the this number
///         will be updated.
/// Patch � Used to indicate very minor fixes.  If the code was modified to fix
///         a defect or to improve the quality in any way that does not add new
///         functionality or change APIs this version number will be updated.
///
/// 1.0   - Original (PSoC Creator 1.0 Beta 5)
/// 1.1   - Add checksum option (PSoC Creator 1.0 Production)
/// 1.2   - Add support for Multi Application Bootloaders
///
///***************************************************************************
var VERSION_MAJOR = 0x01
var VERSION_MINOR = 0x02
var VERSION_PATCH = 0x00
