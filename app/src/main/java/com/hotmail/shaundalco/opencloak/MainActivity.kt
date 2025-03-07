package com.hotmail.shaundalco.opencloak

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hotmail.shaundalco.opencloak.model.Server
import de.blinkt.openvpn.OpenVpnApi
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.security.AccessController.getContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val servers = listOf(
            Server("USA", "https://flagcdn.com/us.png", "us.ovpn", "freeopenvpn", "889369906"),
            Server("UK", "https://flagcdn.com/gb.png", "japan.ovpn"),
            Server("Germany", "https://flagcdn.com/de.png", "sweden.ovpn")
        )

        try {
            println("🔹 Loading OpenVPN binary...")
            System.loadLibrary("openvpn") // This loads libopenvpn.so from lib/arm64-v8a/
            System.loadLibrary("jbcrypto")
            System.loadLibrary("opvpnutil")
            System.loadLibrary("ovpnexec")
            System.loadLibrary("ovpnutil")
            println("✅ OpenVPN binary loaded successfully!")
        } catch (e: UnsatisfiedLinkError) {
            println("❌ Failed to load OpenVPN binary: ${e.localizedMessage}")
        }

        setContent {
            VPNServerListScreen(servers)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VPNServerListScreen(servers: List<Server>) {
    val context = LocalContext.current
    var vpnPermissionGranted by remember { mutableStateOf(VpnService.prepare(context) == null) }

    // Launcher to request VPN permission
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        vpnPermissionGranted = result.resultCode == Activity.RESULT_OK
    }

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
                    if (vpnPermissionGranted) {
                        startVpn(context, server)
                    } else {
                        val intent = VpnService.prepare(context)
                        if (intent != null) {
                            vpnPermissionLauncher.launch(intent)
                        } else {
                            vpnPermissionGranted = true
                            startVpn(context, server)
                        }
                    }
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

fun startVpn(context: Context, server: Server) {

    try {
        // Ensure OVPN file is provided
        val ovpnFileName = server.ovpn ?: return
        val inputStream = context.assets.open(ovpnFileName)
        val reader = BufferedReader(InputStreamReader(inputStream))

        // Read the OpenVPN configuration
        val config = reader.use { it.readText() }

        // Debugging: Log the loaded config
        //println("Loaded VPN Config: $config")

        // Validate the configuration before attempting to start
        if (config.isBlank()) {
            println("Error: OpenVPN configuration is empty!")
            return
        }

        // Start OpenVPN
        OpenVpnApi.startVpn(
            context,
            config,
            server.country,
            server.ovpnUserName,
            server.ovpnUserPassword
        )

        println("VPN Started Successfully!")

    } catch (e: IOException) {
        e.printStackTrace()
        println("Error reading VPN config file: ${e.localizedMessage}")
    } catch (e: RemoteException) {
        e.printStackTrace()
        println("Error starting VPN service: ${e.localizedMessage}")
    } catch (e: Exception) {
        e.printStackTrace()
        println("Unexpected error starting VPN: ${e.localizedMessage}")
    }
}