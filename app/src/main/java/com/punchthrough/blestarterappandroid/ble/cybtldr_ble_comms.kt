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

package com.punchthrough.blestarterappandroid.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.os.IBinder
import com.punchthrough.blestarterappandroid.cybtldr.CYRET_ERR_DEVICE
import com.punchthrough.blestarterappandroid.cybtldr.CYRET_SUCCESS
import java.util.UUID

data class readDataResult(val a:Boolean,val b:Int,val c:List<Int>)

class cybtldr_ble_comms {

    companion object {
        val UartServiceUuid: UUID = UUID.fromString("569a1101-b87f-490c-92cb-11ba5ea5167c")

        val RxFiFoCharUuid: UUID = UUID.fromString("569a2000-b87f-490c-92cb-11ba5ea5167c")
        val TxFiFoCharUuid: UUID = UUID.fromString("569a2001-b87f-490c-92cb-11ba5ea5167c")
        val ModemInUuid: UUID = UUID.fromString("569a2003-b87f-490c-92cb-11ba5ea5167c")
        val ModemOutUuid: UUID = UUID.fromString("569a2002-b87f-490c-92cb-11ba5ea5167c")

        var modemInChar: BluetoothGattCharacteristic? = null
        var txChar: BluetoothGattCharacteristic? = null

        var bleManager: ConnectionManager = ConnectionManager;
        var bleDevice: BluetoothDevice? = null

        var inList: MutableList<Int> = mutableListOf<Int>()
        var inListSize = 0
    }
    val MaxTransferSize:Int = 64

    fun setDevice(device: BluetoothDevice){
        bleDevice = device
    }
    fun OpenConnection(): Int{
        var toReturn = CYRET_ERR_DEVICE
        if(bleDevice != null) {
            var services = bleManager.servicesOnDevice(bleDevice!!)
            if(services != null) {
                if (services.isNotEmpty()) {
                    var service = services.first{it.uuid == UartServiceUuid}
                    if(service != null)
                    {
                        var rxChar = service.characteristics.first{it.uuid == RxFiFoCharUuid}
                        if(rxChar != null)
                        {
                            bleManager.enableNotifications(bleDevice!!,rxChar);
                            with(rxChar) {
                                var i = 0
                                for(b in value){
                                    inList[i] = b.toInt()
                                    i++
                                }
                                inListSize = i
                            }
                        }
                        else {
                            toReturn = CYRET_ERR_DEVICE
                        }
                        /*
                        var modemOutChar = service.characteristics.first{it.uuid == ModemOutUuid}
                        if(modemOutChar != null)
                        {
                            bleManager.enableNotifications(bleDevice!!,modemOutChar)
                            with(modemOutChar)
                            {
                                //canSendData = value.first() == 1;
                            }
                        }*/
                        modemInChar = service.characteristics.first{it.uuid == ModemInUuid}
                        if(modemInChar != null)
                        {
                            bleManager.writeCharacteristic(bleDevice!!, modemInChar!!,ByteArray(1) { 1 })
                        } else {
                            toReturn = CYRET_ERR_DEVICE
                        }
                        txChar = service.characteristics.first{it.uuid == TxFiFoCharUuid}
                    } else {
                        toReturn = CYRET_ERR_DEVICE
                    }
                } else {
                    toReturn = CYRET_ERR_DEVICE
                }
            } else {
                toReturn = CYRET_ERR_DEVICE
            }
        } else {
            toReturn = CYRET_ERR_DEVICE
        }
        return  toReturn
    }

    fun CloseConnection():Int{
        if(bleDevice != null)
        {
            bleManager.teardownConnection(bleDevice!!)
        }
        return CYRET_SUCCESS
    }

    fun ReadData(size:Int): readDataResult{
        if(inListSize == size)
        {
            return readDataResult(true, CYRET_SUCCESS, inList)
        } else {
            return  readDataResult(false,1, listOf<Int>())
        }
    }

    fun WriteData(data:List<Int>, len:Int) :Int {
        try {


            var byteList = ByteArray(len)
            for ((i, int) in data.withIndex()) {
                byteList[i] = int.toByte();
            }
            bleManager.writeCharacteristic(bleDevice!!, modemInChar!!, ByteArray(1) { 0 })
            bleManager.writeCharacteristic(bleDevice!!, txChar!!, byteList)
            bleManager.writeCharacteristic(bleDevice!!, modemInChar!!, ByteArray(1) { 1 })
            return CYRET_SUCCESS
        } catch (e:Exception){
            return CYRET_ERR_DEVICE
        }
        return  CYRET_ERR_DEVICE
    }
}