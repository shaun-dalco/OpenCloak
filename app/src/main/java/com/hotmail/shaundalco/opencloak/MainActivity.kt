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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.hotmail.shaundalco.opencloak.model.Server
import de.blinkt.openvpn.OpenVpnApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Convert target Dp values to pixels
    val targetX = with(density) { 1000.dp.toPx().toInt() }
    val targetY = with(density) { 500.dp.toPx().toInt() }
    val screenWidthPx = with(density) { screenWidth.toPx().toInt() }
    val screenHeightPx = with(density) { screenHeight.toPx().toInt() }

    // Center the initial scroll position around (200.dp, 150.dp)
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            delay(100L) // Small delay to allow UI composition
            horizontalScrollState.scrollTo(targetX - screenWidthPx / 2 + 250)
            verticalScrollState.scrollTo(targetY - screenHeightPx / 2 + 250)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .verticalScroll(verticalScrollState)
                .size(width = screenWidth * 4, height = screenHeight * 2)
        ) {
            Image(
                painter = painterResource(id = R.drawable.bluemap2), // Replace with your world map drawable
                contentDescription = "World Map",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            // Overlay nodes
            NodeOverlay()
            println("currentVpn: $currentVpn")
        }
    }
}

var currentVpn by mutableStateOf<Int?>(0) // Global variable to track selected node index

@Composable
fun NodeOverlay() {
    val nodes = listOf(
        NodeData("RUSSIA", 1000.dp, 500.dp),
        NodeData("CANADA", 400.dp, 500.dp),
        NodeData("Australia", 1100.dp, 900.dp),
        NodeData("Japan", 900.dp, 200.dp),
        NodeData("Germany", 600.dp, 120.dp)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        nodes.forEachIndexed { index, node ->
            Node(node, index)
        }
    }
}

@Composable
fun Node(node: NodeData, index: Int) {
    val selected = currentVpn == index // Check if this node is the selected one

    val dotColor by animateColorAsState(
        targetValue = if (selected) Color.Green else Color.Red,
        animationSpec = tween(durationMillis = 300)
    )

    val pulse by animateFloatAsState(
        targetValue = if (selected) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val rotation by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .absoluteOffset(x = node.x, y = node.y)
            .size(40.dp),
    ) {
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            val strokeWidth = 3.dp.toPx()
            val radius = size.minDimension / 2 - strokeWidth / 2

            withTransform({
                rotate(rotation, pivot = Offset(size.width / 2, size.height / 2)) // Rotating around center
            }) {
                // Draw four arc segments with gaps at top, bottom, left, right
                for (angle in listOf(0f, 90f, 180f, 270f)) {
                    drawArc(
                        color = Color.Gray,
                        startAngle = angle - 30f, // Small cut space
                        sweepAngle = 60f, // Arc length
                        useCenter = false,
                        style = Stroke(width = strokeWidth),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(
                            (size.width - radius * 2) / 2,
                            (size.height - radius * 2) / 2
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size((20 * pulse).dp)
                .clip(CircleShape)
                .background(dotColor)
                .align(Alignment.Center)
                .clickable { currentVpn = if (selected) null else index } // Update global state
        )
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