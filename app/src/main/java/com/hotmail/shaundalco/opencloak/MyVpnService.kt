package com.hotmail.shaundalco.opencloak

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class MyVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start VPN connection
        Log.d(TAG, "Starting VPN service")
        startVpn()
        return START_STICKY
    }

    override fun onDestroy() {
        // Clean up VPN resources when service is destroyed
        Log.d(TAG, "Stopping VPN service")
        stopVpn()
        super.onDestroy()
    }

    private fun startVpn() {
        try {
            val builder: Builder = Builder()
            builder.setSession("My VPN")
                .setMtu(1500)
                .addAddress("192.168.0.1", 24) // Example local IP address
                .addRoute("0.0.0.0", 0) // Route all traffic through VPN
                .setBlocking(true) // Block to wait for the VPN connection

            vpnInterface = builder.establish() // Establish VPN interface
            Log.d(TAG, "VPN established")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN", e)
        }
    }

    private fun stopVpn() {
        if (vpnInterface != null) {
            try {
                vpnInterface!!.close()
                Log.d(TAG, "VPN stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping VPN", e)
            }
        }
    }

    companion object {
        private const val TAG = "MyVpnService"
    }
}