package com.hotmail.shaundalco.opencloak

import android.content.Context
import android.os.Bundle
import android.os.RemoteException
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hotmail.shaundalco.opencloak.model.Server
import de.blinkt.openvpn.OpenVpnApi
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.security.AccessController.getContext


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
                    startVpn(context, server)
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
        // Read .ovpn file
        val inputStream = server.ovpn?.let { context.assets.open(it) }
        val reader = BufferedReader(InputStreamReader(inputStream))
        val config = buildString {
            reader.forEachLine { append(it).append("\n") }
        }
        reader.close()

        // Start VPN
        OpenVpnApi.startVpn(
            context,
            config,
            server.country,
            server.ovpnUserName,
            server.ovpnUserPassword
        )

    } catch (e: IOException) {
        e.printStackTrace()
    } catch (e: RemoteException) {
        e.printStackTrace()
    }
}
