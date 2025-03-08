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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

val servers = listOf(
    Server("USA", "https://flagcdn.com/us.png", "us.ovpn", "freeopenvpn", "889369906"),
    Server("UK", "https://flagcdn.com/gb.png", "japan.ovpn"),
    Server("Germany", "https://flagcdn.com/de.png", "sweden.ovpn")
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            VPNMapScreen()
            ConnectButton()
        }
    }
}

@Composable
fun ConnectButton() {
    val pulseAnimation = rememberInfiniteTransition()
    val context = LocalContext.current
    val scale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Button(
            onClick = { startVpn(context, servers[0]) },
            modifier = Modifier
                .padding(bottom = 50.dp)
                .size((150 * scale).dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
            shape = CircleShape
        ) {
            Text("CONNECT", color = Color.White, fontSize = 18.sp)
        }
    }
}

@Composable
fun VPNMapScreen() {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .verticalScroll(verticalScrollState)
                .size(width = screenWidth * 3, height = screenHeight)
        ) {
            Image(
                painter = painterResource(id = R.drawable.bluemap), // Replace with your world map drawable
                contentDescription = "World Map",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            // Overlay nodes
            NodeOverlay()
        }
    }
}

@Composable
fun NodeOverlay() {
    val nodes = listOf(
        NodeData("USA", 200.dp, 150.dp),
        NodeData("UK", 500.dp, 100.dp),
        NodeData("Australia", 800.dp, 400.dp),
        NodeData("Japan", 900.dp, 200.dp),
        NodeData("Germany", 600.dp, 120.dp)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        nodes.forEach { node ->
            Node(node)
        }
    }
}

@Composable
fun Node(node: NodeData) {
    var selected by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .absoluteOffset(x = node.x, y = node.y)
            .size(30.dp)
    ) {
        Button(
            onClick = { selected = !selected },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected) Color.Green else Color.Red
            ),
            modifier = Modifier.size(30.dp)
        ) {}
    }
}

data class NodeData(val country: String, val x: Dp, val y: Dp)


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