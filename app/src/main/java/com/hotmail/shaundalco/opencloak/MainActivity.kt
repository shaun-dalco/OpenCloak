package com.hotmail.shaundalco.opencloak

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hotmail.shaundalco.opencloak.model.Server
import com.hotmail.shaundalco.opencloak.ui.theme.OpenCloakTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenCloakTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val serverList = remember { getServerList() }
    var selectedServer by remember { mutableStateOf<Server?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(serverList) { server ->
                selectedServer = server
                coroutineScope.launch { drawerState.close() }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("OpenCloak VPN") },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) {
            selectedServer?.let { server ->
                ServerDetailScreen(server)
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a server from the menu")
                }
            }
        }
    }
}


@Composable
fun DrawerContent(serverList: List<Server>, onServerSelected: (Server) -> Unit) {
    LazyColumn {
        items(serverList) { server ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onServerSelected(server) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.test),
                    contentDescription = server.country,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                server.country?.let { Text(text = it) }
            }
        }
    }
}

@Composable
fun ServerDetailScreen(server: Server) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.test),
            contentDescription = server.country,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Connected to ${server.country}", style = MaterialTheme.typography.headlineLarge)
    }
}

fun getServerList(): List<Server> {
    return listOf(
        Server("United States", "", "us.ovpn", "freeopenvpn", "416248023"),
        Server("Japan", "", "japan.ovpn", "vpn", "vpn"),
        Server("Sweden", "", "sweden.ovpn", "vpn", "vpn"),
        Server("Korea", "", "korea.ovpn", "vpn", "vpn")
    )
}

