package de.pacheco.bleplayground

import android.app.Activity
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.widget.ExpandableListView
import android.widget.SimpleExpandableListAdapter
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import de.pacheco.bleplayground.LogHelper.Companion.printIt
import de.pacheco.bleplayground.databinding.BleGattActivityBinding
import java.util.ArrayList
import java.util.HashMap

class BleGattActivity : Activity() {
    private var scanResultMaybe: ScanResult? = null
    private val scanResult: ScanResult by lazy {
        if (scanResultMaybe == null) {
            Toast.makeText(this, "No ScanResult passed!", LENGTH_LONG)
            finish()
        }
        scanResultMaybe!!
    }
    private lateinit var gattServicesView: ExpandableListView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        scanResultMaybe = intent.getParcelableExtra(SCAN_RESULT)
        val inflate = BleGattActivityBinding.inflate(layoutInflater)
        inflate.macAddress.text = scanResult.device.address
        gattServicesView = inflate.gattServices
        actionBar?.title = scanResult.device.name
        actionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(inflate.root)
    }

    override fun onResume() {
        super.onResume()
        printIt(scanResult.device.connectGatt(this, true, BleGattCallBack(this)))
    }

    private val LIST_NAME = "NAME"
    private val LIST_UUID = "UUID"
    fun showServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String?
        val noName = "no name"
        val gattServiceData = ArrayList<HashMap<String, String?>>()
        val gattCharacteristicData = ArrayList<ArrayList<HashMap<String, String?>>>()
        val gattCharacteristicsList = ArrayList<ArrayList<BluetoothGattCharacteristic>>()
        for (gattService in gattServices) {
            val currentServiceData = HashMap<String, String?>()
            uuid = gattService.uuid.toString()
            currentServiceData[LIST_NAME] = noName
            currentServiceData[LIST_UUID] = uuid
            gattServiceData.add(currentServiceData)
            val gattCharacteristicGroupData = ArrayList<HashMap<String, String?>>()
            val gattCharacteristics = gattService.characteristics
            val charas = ArrayList<BluetoothGattCharacteristic>()
            for (gattCharacteristic in gattCharacteristics) {
                charas.add(gattCharacteristic)
                val currentCharaData = HashMap<String, String?>()
                uuid = gattCharacteristic.uuid.toString()
                currentCharaData[LIST_NAME] = noName
                currentCharaData[LIST_UUID] = uuid
                gattCharacteristicGroupData.add(currentCharaData)
            }
            gattCharacteristicsList.add(charas)
            gattCharacteristicData.add(gattCharacteristicGroupData)
        }
        val gattServiceAdapter = SimpleExpandableListAdapter(
            this,
            gattServiceData,
            android.R.layout.simple_expandable_list_item_2,
            arrayOf(LIST_NAME, LIST_UUID),
            intArrayOf(android.R.id.text1, android.R.id.text2),
            gattCharacteristicData,
            android.R.layout.simple_expandable_list_item_2,
            arrayOf(LIST_NAME, LIST_UUID),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        )
        runOnUiThread {
            gattServicesView.setAdapter(gattServiceAdapter)
        }
    }
}
