import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.MobileOff
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PhonelinkErase
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    onNavigate: (target: Screen) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("Waiting...") }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDevice by remember { mutableStateOf("") }
    var showDisconnectDialog by remember { mutableStateOf(false) }

    scope.launch(Dispatchers.IO) {
        text = runCommand("adb devices")
        delay(500)
        isLoading = false
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text("Connected devices")
                },
                navigationIcon = {
                    IconButton({ onNavigate(Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row {
                        IconButton(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    isLoading = true
                                    text = runCommand("adb devices")
                                    delay(1000)
                                    isLoading = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                contentColor = MaterialTheme.colorScheme.primary,
                onClick = {
                    onNavigate(Screen.DeviceConnect)
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Connect to device")
            }
        }
    ) {
        if (showDisconnectDialog) {
            showAlertDialog(
                icon = Icons.Outlined.PhonelinkErase,
                title = "Disconnect",
                text = "Are you sure you want to disconnect from $selectedDevice?",
                dismissText = "Cancel",
                onConfirmation = {
                    scope.launch(Dispatchers.IO) {
                        isLoading = true
                        runCommand("adb disconnect $selectedDevice")
                        text = runCommand("adb devices")
                        selectedDevice = ""
                        showDisconnectDialog = false
                        delay(500)
                        isLoading = false
                    }
                },
                onDismissRequest = {
                    showDisconnectDialog = false
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(it)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier
//                            .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                ) {
                    val parsed = parseAdbDevicesOutput(text)
                    if (parsed.isEmpty()) {
                        Text("Nothing was found!")
                    } else {
                        parseAdbDevicesOutput(text).forEach {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .padding(
                                        16.dp,
                                        4.dp,
                                    )
                                    .fillMaxWidth(),
                                onClick = {
                                    selectedDevice = it.serial
                                    showDisconnectDialog = true
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(it.serial)
                                    if (it.isEmulator) {
                                        Icon(Icons.Outlined.Devices, contentDescription = "Emulator")
                                    } else {
                                        if (it.isOnline) {
                                            Icon(Icons.Outlined.PhoneAndroid, contentDescription = "Physical")
                                        } else {
                                            Icon(Icons.Outlined.MobileOff, contentDescription = "Physical Offline")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun showAlertDialog(
    icon: ImageVector,
    title: String,
    text: String,
    confirmText: String = "Confirm",
    dismissText: String = "Dismiss",
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    AlertDialog(
        icon = { Icon(icon, contentDescription = "title") },
        title = {
            Text(text = title)
        },
        text = {
            Text(text = text)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(dismissText)
            }
        }
    )
}

fun parseAdbDevicesOutput(output: String): List<DeviceInfo> {
    val lines = output.lines()

    if (lines.size < 2) {
        // No devices attached
        return emptyList()
    }

    // Remove the first line ("List of devices attached")
    val deviceLines = lines.subList(1, lines.size)

    val devices = deviceLines.mapNotNull { line ->
        val words = line.split("\\s+".toRegex())
        if (words.isNotEmpty()) {
            val serial = words[0]
            val isEmulator = serial.startsWith("emulator")
            val isOnline = words.last() == "device"

            if (serial.isNotEmpty()) {
                DeviceInfo(serial = serial, isEmulator = isEmulator, isOnline = isOnline)
            } else {
                null
            }
        } else {
            null
        }
    }
    return devices
}

data class DeviceInfo(val serial: String, val isEmulator: Boolean, val isOnline: Boolean)
