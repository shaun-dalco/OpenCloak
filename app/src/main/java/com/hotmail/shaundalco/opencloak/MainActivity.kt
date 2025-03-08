package com.hotmail.shaundalco.opencloak

import android.annotation.SuppressLint
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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.chihsuanwu.freescroll.freeScroll
import com.chihsuanwu.freescroll.rememberFreeScrollState
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
            MainScreen()
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
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Define the target dimensions for the image
    val imageWidth = screenWidth * 4
    val imageHeight = screenHeight * 2

    // Initialize freeScroll state
    val freeScrollState = rememberFreeScrollState()

    // Convert target Dp values to pixels
    val targetX = with(density) { 1000.dp.toPx().toInt() }
    val targetY = with(density) { 500.dp.toPx().toInt() }
    val screenWidthPx = with(density) { screenWidth.toPx().toInt() }
    val screenHeightPx = with(density) { screenHeight.toPx().toInt() }

    // Center the initial scroll position
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            delay(100L) // Small delay to allow UI composition
            freeScrollState.scrollBy(
                Offset(
                    x = (targetX - screenWidthPx / 2 + 250).toFloat(),
                    y = (targetY - screenHeightPx / 2 + 250).toFloat()
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .freeScroll(state = freeScrollState) // Enables free diagonal scrolling
    ) {
        Box(
            modifier = Modifier
                .size(imageWidth, imageHeight) // Ensures the image is larger than the screen
        ) {
            Image(
                painter = painterResource(id = R.drawable.bluemap2),
                contentDescription = "World Map",
                modifier = Modifier
                    .matchParentSize(), // Ensures the image fills the large box
                contentScale = ContentScale.FillBounds // Makes sure the image fills without stretching
            )

            // Overlay nodes
            NodeOverlay()
            println("currentVpn: $currentVpn")
        }
    }
}



var currentVpn by mutableStateOf<Int?>(0) // Global variable to track selected node index

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val nodePositions = remember { mutableMapOf<Int, Dp>() } // Stores node Y positions
    var showSettingsMenu by remember { mutableStateOf(false) } // Controls the settings menu

    val nodes = listOf(
        NodeData("RUSSIA", 1000.dp, 500.dp),
        NodeData("CANADA", 400.dp, 500.dp),
        NodeData("Australia", 1100.dp, 900.dp),
        NodeData("Japan", 900.dp, 200.dp),
        NodeData("Germany", 600.dp, 120.dp)
    )

    val selectedCountry by derivedStateOf { nodes[currentVpn!!].country } // Observe changes

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(drawerState, scrollState, nodePositions, nodes)
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("VPN: $selectedCountry", color = Color.White) }, // White text
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Drawer", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettingsMenu = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }

                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Enable Dark Mode") },
                                onClick = {
                                    showSettingsMenu = false
                                    // TODO: Implement dark mode toggle logic
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Set Custom VPN") },
                                onClick = {
                                    showSettingsMenu = false
                                    // TODO: Implement custom VPN selection
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("About") },
                                onClick = {
                                    showSettingsMenu = false
                                    // TODO: Show an About dialog
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.DarkGray, // Dark background
                        titleContentColor = Color.White, // White title text
                        navigationIconContentColor = Color.White, // White drawer icon
                        actionIconContentColor = Color.White // White settings icon
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                VPNMapScreen() // Background map
                NodeOverlay() // Nodes on top
            }
        }
    }
}

@Composable
fun NavigationDrawerContent(
    drawerState: DrawerState,
    scrollState: ScrollState,
    nodePositions: MutableMap<Int, Dp>,
    nodes: List<NodeData>
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerWidthFraction = 0.25f // 1/4 of the screen width

    ModalDrawerSheet(
        drawerContainerColor = Color.DarkGray,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(drawerWidthFraction) // Set drawer width to 1/4 of screen width
    ) {
        Text(
            text = "Select a VPN",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn {
            itemsIndexed(nodes) { index, node ->
                Text(
                    text = node.country,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable {
                            currentVpn = index
                            coroutineScope.launch {
                                delay(100)
                                scrollState.animateScrollBy(nodePositions[index]?.value ?: 0f)
                                drawerState.close()
                            }
                        }
                        .background(if (currentVpn == index) Color.Gray else Color.Transparent),
                    color = Color.White
                )
            }
        }
    }
}




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
    val selected = currentVpn == index
    val density = LocalDensity.current

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
            .size(40.dp)
            .onGloballyPositioned { coordinates ->
                val yPosition = with(density) { coordinates.positionInParent().y.toDp() }
            },
    ) {
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            val strokeWidth = 3.dp.toPx()
            val radius = size.minDimension / 2 - strokeWidth / 2

            withTransform({
                rotate(rotation, pivot = Offset(size.width / 2, size.height / 2))
            }) {
                for (angle in listOf(0f, 90f, 180f, 270f)) {
                    drawArc(
                        color = Color.Gray,
                        startAngle = angle - 30f,
                        sweepAngle = 60f,
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
                .clickable { currentVpn = index }
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