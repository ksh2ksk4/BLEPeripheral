package com.example.ksh2ksk4.bleperipheral

import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    companion object {
        private val TAG = MainActivity::class.java.name
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // デバイスがBLEをサポートしているかチェック
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.d(TAG, "This device does not support BLE.")
            Toast.makeText(this, "This device does not support BLE.", Toast.LENGTH_SHORT).show()

            finishAndRemoveTask()
        }
    }
}
