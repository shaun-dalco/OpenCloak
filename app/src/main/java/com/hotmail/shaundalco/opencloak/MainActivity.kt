package com.hotmail.shaundalco.opencloak

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hotmail.shaundalco.opencloak.ui.theme.OpenCloakTheme
import de.blinkt.openvpn.OpenVPNService

class MainActivity : ComponentActivity() {
    private val REQUEST_CODE_VPN_PERMISSION = 1

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_VPN_PERMISSION) {
            if (resultCode == RESULT_OK) {
                // Proceed to start the OpenVPN service
                startOpenVpnService()
            } else {
                // Handle case where permission is denied
                Log.e("OpenVPN", "VPN permission denied")
            }
        }
    }

    private fun startOpenVpnService() {
        // Configure the OpenVPN intent with the .ovpn configuration file
        val openvpnIntent = Intent(this, OpenVPNService::class.java)
        openvpnIntent.putExtra("config", "/path/to/your/openvpn/config.ovpn")
        startService(openvpnIntent)
    }
}

@Composable
fun VpnServiceControl(activity: MainActivity) {
    var isVpnActive by remember { mutableStateOf(false) }

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
                    stopOpenVpnService(activity)
                } else {
                    startOpenVpnService(activity)
                }
                isVpnActive = !isVpnActive
            }
        ) {
            Text(text = if (isVpnActive) "Stop VPN" else "Start VPN")
        }
    }
}

private fun startOpenVpnService(activity: MainActivity) {
    // Check if OpenVPN permission is granted
    val intent = OpenVPNService.prepare(activity)
    if (intent != null) {
        // If permission hasn't been granted, start the activity to ask for permission
        activity.startActivityForResult(intent, activity.REQUEST_CODE_VPN_PERMISSION)
    } else {
        // Permission granted, start OpenVPN service directly
        activity.startOpenVpnService()
    }
}

private fun stopOpenVpnService(activity: MainActivity) {
    // Stop the OpenVPN service
    val stopIntent = Intent(activity, OpenVPNService::class.java)
    activity.stopService(stopIntent)
}
