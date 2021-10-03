package de.pacheco.bleplayground

import android.bluetooth.le.ScanResult
import android.os.Build
import timber.log.Timber

class LogHelper {
    companion object {
        fun printIt(item: Any) {
            Timber.e(item.toString())
        }

        fun discoverDevice(result: ScanResult) {
            try {
                printIt(result)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    printIt(result.advertisingSid)
                    printIt(result.dataStatus)
                    printIt(result.periodicAdvertisingInterval)
                    printIt(result.primaryPhy)
                    printIt(result.secondaryPhy)
                    printIt(result.txPower)
                }
                printIt(result.rssi)
                val device = result.device
                printIt(device)
                val scanRecord = result.scanRecord
                scanRecord?.let { printIt(it) }
                printIt(device.address)
                printIt(device.bluetoothClass)
                printIt(device.bondState)
                printIt(device.name)
                printIt(device.type)
                printIt("createBond=" + device.createBond())
                printIt(device.uuids)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}