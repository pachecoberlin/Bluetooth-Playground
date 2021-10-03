package de.pacheco.bleplayground

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import de.pacheco.bleplayground.databinding.BleActivityMainBinding
import timber.log.Timber

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2
private const val SCAN_PERIOD = 10000L

class BleMainActivity : Activity() {
    private val bluetoothAdapter by lazy { (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter }
    private val bleScanner by lazy { bluetoothAdapter.bluetoothLeScanner }
    private val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    private lateinit var optionsMenu: Menu
    private var mainLooperHandler = Handler(Looper.getMainLooper())
    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread { updateScanButton(optionsMenu) }
        }
    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            if (isScanning) {
                stopScanning()
            }
            with(result.device) {
                Timber.w("Connecting to $address")
//               TODO connect
            }
        }
    }

    private fun updateScanButton(menu: Menu) {
        menu.findItem(R.id.menu_stop_scan).isVisible = isScanning
        menu.findItem(R.id.menu_start_scan).isVisible = !isScanning
        if (isScanning) menu.findItem(R.id.menu_working).setActionView(R.layout.scanning_progress_loop) else menu.findItem(R.id.menu_working).actionView = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        val inflate = BleActivityMainBinding.inflate(layoutInflater)
        val scanResultsRecyclerView = inflate.scanResultsRecyclerView
        setupRecyclerView(scanResultsRecyclerView)
        setContentView(inflate.root)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_ble_main, menu)
        optionsMenu = menu
        updateScanButton(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_start_scan -> startScanning()
            R.id.menu_stop_scan -> stopScanning()
        }
        return true
    }

    private fun startScanning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted) {
            requestLocationPermission()
            return
        }
        mainLooperHandler.postDelayed({
            isScanning = false
            bleScanner.stopScan(scanCallback)
        }, SCAN_PERIOD)
        isScanning = true
        scanResults.clear()
        scanResultAdapter.notifyDataSetChanged()
        bleScanner.startScan(null, scanSettings, scanCallback)
    }

    private fun stopScanning() {
        isScanning = false
        bleScanner.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {
                    Timber.i("Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                }
                scanResults.add(result)
                discoverDevice(result)
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.e("onScanFailed: code $errorCode")
        }
    }

    override fun onResume() {
        super.onResume()
//       TODO need of ConnectionListener?
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestLocationPermission()
                } else {
                    startScanning()
                }
            }
        }
    }

    private fun setupRecyclerView(scanResultsRecyclerView: RecyclerView) {
        scanResultsRecyclerView.apply {
            adapter = scanResultAdapter
            layoutManager = LinearLayoutManager(this@BleMainActivity, RecyclerView.VERTICAL, false)
            isNestedScrollingEnabled = false
        }

        val animator = scanResultsRecyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    private fun requestLocationPermission() {
        if (!isLocationPermissionGranted) {
            runOnUiThread { requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_REQUEST_CODE) }
        }
    }

    private val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) == PackageManager.PERMISSION_GRANTED
    }

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

    private fun discoverDevice(result: ScanResult) {
//        TODO()
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
            printIt(device.connectGatt(this, true, gattCallback))
            printIt(device.uuids)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            printIt("onCharacteristicChanged")
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            printIt("onCharacteristicRead")
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            printIt("onCharacteristicWrite")
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            printIt("onConnectionStateChange")
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            printIt("onDescriptorRead")
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            printIt("onDescriptorWrite")
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            printIt("onMtuChanged")
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            printIt("onPhyRead")
        }

        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            printIt("onPhyUpdate")
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            printIt("onReadRemoteRssi")
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
            printIt("onReliableWriteCompleted")
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            printIt("onServicesDiscovered")
        }
    }

    private fun printIt(item: Any) {
        Timber.e(item.toString())
    }

}