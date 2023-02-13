package com.example.sunmiscaleadapter

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber

class MainActivity : Activity() {
    private enum class UsbPermission {
        Unknown, Requested, Granted, Denied
    }

    private val tag = "MainActivity1234"
    private val waitMs = 1000
    private val intentActionGrantUsb = BuildConfig.APPLICATION_ID + ".GRANT_USB"
    private var connected = false
    private lateinit var usbSerialPort : UsbSerialPort
    private var usbPermission : UsbPermission = UsbPermission.Unknown
    private lateinit var button: Button

    // Usb Port Parameters
    private val baudRate = 9600
    private val dataBits = 7
    private val stopBits = UsbSerialPort.STOPBITS_1
    private val parity = UsbSerialPort.PARITY_EVEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        registerReceiver(mUsbReceiver, IntentFilter(intentActionGrantUsb))

        button = findViewById(R.id.button1)

        if (usbPermission === UsbPermission.Unknown || usbPermission === UsbPermission.Denied) {
            button.text = getString(R.string.request_permission)
        }

        button.setOnClickListener { handleButtonPress() }

        if(usbPermission == UsbPermission.Unknown || usbPermission == UsbPermission.Granted) {
            connect()
        }
    }

    private fun handleButtonPress() {
        Log.d(tag, "Handle button press")

        connect()

        if (usbPermission === UsbPermission.Unknown || usbPermission === UsbPermission.Denied) {
            Log.d(tag, "Permission not yet granted")
            button.text = getString(R.string.request_permission)
        }

        if (usbPermission == UsbPermission.Granted){
            var response = ""
            var totalBytes = 0
            usbSerialPort.write("W\r\n".toByteArray(), waitMs);
            var invalidPacket = true

            while (invalidPacket) {
                val res = ByteArray(20)
                val len = usbSerialPort.read(res, waitMs);
                totalBytes += len

                response += String(res).slice(IntRange(0, len-1))
                Log.d(tag, "Current Packet: $response | Length: ${response.length}")

                if (response.length == 4 && response[1] == '?' && response[2] == 'd') {
                    Log.d(tag, "Underweight error detected!")
                    invalidPacket = false
                } else if (response.length == 8) {
                    invalidPacket = false
                }

            }

            Log.d(tag, "Complete Packet: $response | Length: $totalBytes")
            Toast.makeText(this, response, Toast.LENGTH_LONG).show() // Show weight
        }

    }

    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (intentActionGrantUsb == action) {
                Log.d(tag, "Requesting USB permission")
                synchronized(this) {
                    usbPermission = if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d(tag, "Permission granted")
                        UsbPermission.Granted
                    } else {
                        Log.d(tag, "Permission denied")
                        UsbPermission.Denied
                    }

                    if (usbPermission == UsbPermission.Granted) {
                        connect()
                    }


                }
            }
        }
    }

    private fun connect() {
        Log.d(tag, "Starting USB connection sequence")

        val usbManager = getSystemService(USB_SERVICE) as UsbManager

        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            usbPermission = UsbPermission.Denied
            Log.d(tag, "Connection Failed: No available drivers")
            return
        }

        val driver = availableDrivers[0] // Use first available driver

        if (driver == null) {
            usbPermission = UsbPermission.Denied
            Log.d(tag, "Connection Failed: No driver for device")
            return
        }

        val device: UsbDevice? = driver.device
        if (device == null) {
            usbPermission = UsbPermission.Denied
            Log.d(tag, "Connection Failed: Device not found")
            return
        }

        usbSerialPort = driver.ports[0]
        val usbConnection = if (usbManager.hasPermission(driver.device)) {
            usbManager.openDevice(driver.device)
        } else {
            null
        }

        if (usbConnection == null && (usbPermission === UsbPermission.Unknown || usbPermission === UsbPermission.Denied) && !usbManager.hasPermission(driver.device)) {
            usbPermission = UsbPermission.Requested
            val usbPermissionIntent = PendingIntent.getBroadcast(this, 0, Intent(intentActionGrantUsb), 0)
            usbManager.requestPermission(driver.device, usbPermissionIntent)
            Log.d(tag, "Connection Failed: No USB permission granted")
            return
        }
        if (usbConnection == null) {
            if (!usbManager.hasPermission(driver.device)) {
                Log.d(tag,"Connection Failed: Permission denied")
            } else {
                Log.d(tag, "Connection Failed: Open failed")
            }
            return
        }
        try {
            usbSerialPort.open(usbConnection)
            usbSerialPort.setParameters(baudRate, dataBits, stopBits, parity)

            connected = true
            usbPermission = UsbPermission.Granted
            Log.d(tag,"Connection Successful!")
            button.text = getString(R.string.query_weight)
        } catch (e: Exception) {
            Log.d(tag, "Connection Failed: " + e.message)
            //disconnect()
        }
    }
}