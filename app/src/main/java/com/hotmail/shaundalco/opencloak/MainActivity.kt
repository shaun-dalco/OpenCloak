package com.hotmail.shaundalco.opencloak

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hotmail.shaundalco.opencloak.ui.theme.OpenCloakTheme

class MainActivity : ComponentActivity() {
    // Define request code for VPN permission
    val REQUEST_CODE_VPN_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenCloakTheme {
                // Surface container with background
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    VpnServiceControl(this)
                }
            }
        }
    }

    // Handle result of permission request (for VPN)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Check if the request code matches the VPN permission request
        if (requestCode == REQUEST_CODE_VPN_PERMISSION) {
            if (resultCode == Activity.RESULT_OK) {
                // Start VPN service when permission is granted
                val vpnServiceIntent = Intent(this, MyVpnService::class.java)
                startService(vpnServiceIntent)
            } else {
                // Handle VPN permission denial
                Log.e("VpnService", "VPN permission denied")
            }
        }
    }
}

@Composable
fun VpnServiceControl(activity: Activity) {
    // Track whether VPN is active
    var isVpnActive by remember { mutableStateOf(false) }

    // UI Layout with Button to toggle VPN status
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = if (isVpnActive) "VPN is Active" else "VPN is Inactive")
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (isVpnActive) {
                    // Stop VPN service
                    stopVpnService(activity)
                } else {
                    // Start VPN service with permission check
                    startVpnService(activity)
                }
                isVpnActive = !isVpnActive
            }
        ) {
            Text(text = if (isVpnActive) "Stop VPN" else "Start VPN")
        }
    }
}

private fun startVpnService(activity: Activity) {
    // Check if VPN permission is granted using VpnService.prepare()
    val intent = VpnService.prepare(activity)
    if (intent != null) {
        // If permission hasn't been granted, ask the user to allow it
        activity.startActivityForResult(intent, (activity as MainActivity).REQUEST_CODE_VPN_PERMISSION)
    } else {
        // Permission is already granted, start the VPN service directly
        val vpnServiceIntent = Intent(activity, MyVpnService::class.java)
        activity.startService(vpnServiceIntent)
    }
}

private fun stopVpnService(activity: Activity) {
    // Stop the VPN service
    val vpnServiceIntent = Intent(activity, MyVpnService::class.java)
    activity.stopService(vpnServiceIntent)
}