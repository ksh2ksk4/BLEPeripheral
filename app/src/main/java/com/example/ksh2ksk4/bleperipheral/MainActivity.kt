package com.example.ksh2ksk4.bleperipheral

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    companion object {
        private val TAG = MainActivity::class.java.name
        private val REQUEST_ENABLE_BLUETOOTH = 1
    }

    lateinit private var bluetoothLeAdvertiser: BluetoothLeAdvertiser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // デバイスがBLEをサポートしているかチェック
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.d(TAG, "This device does not support BLE.")
            Toast.makeText(this, "This device does not support BLE.", Toast.LENGTH_SHORT).show()

            finishAndRemoveTask()
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        // Bluetoothが有効になっているかどうかチェック
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }

        //fixme アプリ開始前にBluetoothが有効になっていないと、ここでNPEが発生
        bluetoothLeAdvertiser = bluetoothAdapter!!.bluetoothLeAdvertiser

        // peripheralの機能をサポートしているかチェック
        if (Build.VERSION.SDK_INT <= 20 ||
                bluetoothLeAdvertiser == null ||
                !bluetoothAdapter.isMultipleAdvertisementSupported ||
                !bluetoothAdapter.isOffloadedFilteringSupported ||
                !bluetoothAdapter.isOffloadedScanBatchingSupported) {
            Log.d(TAG, "This device does not support facility of peripheral")
            Toast.makeText(this, "This device does not support facility of peripheral", Toast.LENGTH_SHORT).show()

            finishAndRemoveTask()
        }
    }
}
