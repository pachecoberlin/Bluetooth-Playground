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
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import de.pacheco.bleplayground.LogHelper.Companion.discoverDevice
import de.pacheco.bleplayground.databinding.BleActivityMainBinding
import timber.log.Timber

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2
private const val SCAN_PERIOD = 10000L
const val SCAN_RESULT = "SCAN_RESULT"

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
            stopScanning()
            val intent = Intent(this, BleGattActivity::class.java)
            intent.putExtra(SCAN_RESULT, result)
            startActivity(intent)
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
        startScanning()
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
        requestLocationPermission()
        enableBluetooth()
        if (!bluetoothAdapter.isEnabled || !isLocationPermissionGranted) {
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
        if (!isScanning) return
        isScanning = false
        bleScanner.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) {
                scanResults[indexQuery] = result
                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                result.device.run { Timber.i("New Device\nname: ${name ?: "unknown"}\naddress: $address") }
                scanResults.add(result)
                discoverDevice(result)
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.e("onScanFailed: code $errorCode")
        }
    }

    private fun enableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ENABLE_BLUETOOTH_REQUEST_CODE && resultCode != RESULT_OK) {
            Toast.makeText(this, "Without Bluetooth no playing with Bluetooth ;)", LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startScanning()
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
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) || Build.VERSION.SDK_INT < Build.VERSION_CODES.M

    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) == PackageManager.PERMISSION_GRANTED
    }

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }
}