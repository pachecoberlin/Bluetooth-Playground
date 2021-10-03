package de.pacheco.bleplayground

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import java.util.*

class BleGattCallBack(val bleGattActivity: BleGattActivity) : BluetoothGattCallback() {
    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
        LogHelper.printIt("onCharacteristicChanged")
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        LogHelper.printIt("onCharacteristicRead")
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        LogHelper.printIt("onCharacteristicWrite")
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        LogHelper.printIt("onConnectionStateChange")
        gatt?.discoverServices()
    }

    override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        LogHelper.printIt("onDescriptorRead")
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        LogHelper.printIt("onDescriptorWrite")
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        LogHelper.printIt("onMtuChanged")
    }

    override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
        LogHelper.printIt("onPhyRead")
    }

    override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
        LogHelper.printIt("onPhyUpdate")
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        LogHelper.printIt("onReadRemoteRssi")
    }

    override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
        LogHelper.printIt("onReliableWriteCompleted")
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        LogHelper.printIt("onServicesDiscovered")
        bleGattActivity.showServices(gatt?.services ?: LinkedList())
        TODO("gatt.services is empty")
    }
}