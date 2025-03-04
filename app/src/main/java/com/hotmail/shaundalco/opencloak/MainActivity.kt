package com.hotmail.shaundalco.opencloak

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.hotspot2.ConfigParser
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hotmail.shaundalco.opencloak.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

import de.blinkt.openvpn.OpenVpnApi;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.OpenVPNThread;
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VpnStatus;


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val servers = listOf(
            Server("USA", "https://flagcdn.com/us.png", "us.ovpn"),
            Server("UK", "https://flagcdn.com/gb.png", "japan.ovpn"),
            Server("Germany", "https://flagcdn.com/de.png", "sweden.ovpn")
        )

        setContent {
            VPNServerListScreen(servers)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VPNServerListScreen(servers: List<Server>) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("OpenCloak VPN") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(servers) { server ->
                VPNServerItem(server) {
                    startVpnConnection(context, server)
                }
            }
        }
    }
}

@Composable
fun VPNServerItem(server: Server, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(server.flagUrl),
            contentDescription = "Flag",
            modifier = Modifier
                .size(40.dp)
                .padding(end = 8.dp)
        )
        Text(
            text = server.country ?: "Unknown",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = { onClick() }) {
            Text("Connect")
        }
    }
}

/**
 * Reads an OVPN file from assets and returns its content as a string.
 */
suspend fun getOvpnConfigFromAssets(context: Context, ovpnFileName: String): String {
    return withContext(Dispatchers.IO) {
        val inputStream = context.assets.open(ovpnFileName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val content = reader.readText()
        reader.close()
        content
    }
}

/**
 * Starts a VPN connection using ics-openvpn.
 */
fun startVpnConnection(context: Context, server: Server) {
    val profileManager = ProfileManager.getInstance(context)
    val vpnService = VpnStatus.getLastConnectedVPN()

    if (vpnService != null) {
        VpnStatus.logInfo("Disconnecting existing VPN...")
        VpnStatus.updateState("Disconnecting", "Disconnecting current VPN...", 0, VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED)
        OpenVPNThread.stopVPN()
    }

    // Load OVPN from assets
    GlobalScope.launch(Dispatchers.Main) {
        val ovpnConfig = getOvpnConfigFromAssets(context, server.ovpn!!)

        // Create VPN profile
        val vpnProfile = ConfigParser().parseConfig(ovpnConfig.byteInputStream())
        vpnProfile.mName = server.country ?: "VPN Server"

        // Save and connect
        profileManager.addProfile(vpnProfile)
        profileManager.saveProfile(context, vpnProfile)

        OpenVPNService.startVpn(context, vpnProfile)
    }
}