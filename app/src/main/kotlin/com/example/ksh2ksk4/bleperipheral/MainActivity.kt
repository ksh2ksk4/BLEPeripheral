package com.example.ksh2ksk4.bleperipheral

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.nio.ByteBuffer
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        private val TAG = MainActivity::class.java.name
        private val REQUEST_ENABLE_BLUETOOTH = 1
        private val ADVERTISING_TIMEOUT = 12500
        private val IBEACON_UUID = "7694505b-707b-484c-82ce-6917eac8191e"
        private val IBEACON_MAJOR = 8311
        private val IBEACON_MINOR = 1
    }

    lateinit private var textViewMessage: TextView
    private var editTextUuid: EditText? = null
    private var editTextMajor: EditText? = null
    private var editTextMinor: EditText? = null
    private var pref: SharedPreferences? = null

    lateinit private var bluetoothLeAdvertiser: BluetoothLeAdvertiser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewMessage = findViewById(R.id.textViewMessage) as TextView
        editTextUuid = findViewById(R.id.editTextUuid) as EditText
        editTextMajor = findViewById(R.id.editTextMajor) as EditText
        editTextMinor = findViewById(R.id.editTextMinor) as EditText

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
            Log.d(TAG, "This device does not support facility of peripheral.")
            Toast.makeText(this, "This device does not support facility of peripheral.", Toast.LENGTH_SHORT).show()

            finishAndRemoveTask()
        }

        // 共有プリファレンスからUUID/Major/Minorを取得
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        var uuid = pref!!.getString("UUID", IBEACON_UUID)
        var major = pref!!.getInt("MAJOR", IBEACON_MAJOR)
        var minor = pref!!.getInt("MINOR", IBEACON_MINOR)

        editTextUuid!!.setText(uuid)
        editTextMajor!!.setText(major.toString())
        editTextMinor!!.setText(minor.toString())

        advertiseData = buildAdvertiseData(convertUUID(uuid), major, minor)
    }

    fun onStartButtonTapped(view: View) {
        Log.d(TAG, "Start button tapped.")
        textViewMessage.append("Start button tapped.\n")

        startAdvertising()
    }

    fun onStopButtonTapped(view: View) {
        Log.d(TAG, "Stop button tapped.")
        textViewMessage.append("Stop button tapped.\n")

        stopAdvertising()
    }

    fun onUpdateButtonTapped(view: View) {
        Log.d(TAG, "Update button tapped.")
        textViewMessage.append("Update button tapped.\n")

        updateAdvertising()
    }

    /**
     * Advertising
     */
    private val advertiseSettings = buildAdvertiseSettings()
    private var advertiseData: AdvertiseData? = null
    private val advertiseCallbackIBeacon = AdvertiseCallbackIBeacon()

    private fun convertUUID(uuidString: String): ByteArray {
        val uuid = UUID.fromString(uuidString)
        val byteBuffer = ByteBuffer.wrap(ByteArray(16))
        byteBuffer.putLong(uuid.mostSignificantBits)
        byteBuffer.putLong(uuid.leastSignificantBits)

        return byteBuffer.array()
    }

    private var isAdvertising = false

    private fun buildAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(false)
                .setTimeout(ADVERTISING_TIMEOUT)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()
    }

    private fun buildAdvertiseData(uuid: ByteArray, major: Int, minor: Int): AdvertiseData {
        val manufacturerData = byteArrayOf(
                // 0x02: iBeacon
                0x02,
                // 0x15: 21Bytes
                0x15,
                // Proximity UUID(16Bytes)
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                // Major
                major.ushr(8).toByte(),
                major.toByte(),
                // Minor
                minor.ushr(8).toByte(),
                minor.toByte(),
                // Measured Power
                (AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM - 41).toByte())
        System.arraycopy(uuid, 0, manufacturerData, 2, 16)

        Log.d(TAG, "manufacturerData: " + manufacturerData.toString())

        return AdvertiseData.Builder()
                // 0x004c: Apple
                .addManufacturerData(0x004c, manufacturerData)
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(true)
                .build()
    }

    private inner class AdvertiseCallbackIBeacon: AdvertiseCallback() {
        override fun onStartSuccess(advertiseSettings: AdvertiseSettings) {
            super.onStartSuccess(advertiseSettings)

            Log.d(TAG, "Advertising succeeded.")
            textViewMessage.append("Advertising succeeded.\n")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)

            Log.d(TAG, "Advertising failed.")
            textViewMessage.append("Advertising failed.\n")
        }
    }

    private fun startAdvertising() {
        Log.d(TAG, "Advertising started.")
        textViewMessage.append("Advertising started.\n")

        isAdvertising = true

        val thread = Thread(Runnable {
            while (isAdvertising) {
                Log.d(TAG, "waking")

                bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallbackIBeacon)

                try {
                    Log.d(TAG, "sleeping")

                    Thread.sleep((ADVERTISING_TIMEOUT + 1000).toLong())
                } catch (e: InterruptedException) {
                    Log.d(TAG, e.toString())
                }
            }
        })

        thread.start()
    }

    private fun stopAdvertising() {
        Log.d(TAG, "Advertising stopped.")
        textViewMessage.append("Advertising stopped.\n")

        bluetoothLeAdvertiser.stopAdvertising(advertiseCallbackIBeacon)

        isAdvertising = false
    }

    private fun updateAdvertising() {
        Log.d(TAG, "Advertising updated.")
        textViewMessage.append("Advertising updated.\n")

        val uuid = convertUUID(editTextUuid!!.text.toString())
        val major = editTextMajor!!.text.toString().toInt()
        val minor = editTextMinor!!.text.toString().toInt()

        val editor = pref!!.edit()
        //editor.clear()
        //editor.putInt("MINOR", (Math.random() * 9999).toInt())
        editor.putInt("MAJOR", major)
        editor.putInt("MINOR", minor)
        editor.commit()

        stopAdvertising()
        advertiseData = buildAdvertiseData(uuid, major, minor)
        startAdvertising()
    }
}
