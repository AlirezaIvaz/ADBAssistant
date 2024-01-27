import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePairScreen(
    onNavigate: (target: Screen) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var result by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(PairState.Waiting) }
    var ipAddress by remember { mutableStateOf("192.168.1.100") }
    var port by remember { mutableStateOf("") }
    var pairCode by remember { mutableStateOf("") }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text("Pair wireless device")
                },
                navigationIcon = {
                    IconButton({ onNavigate(Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(it)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(it)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = ipAddress,
                    onValueChange = { value ->
                        ipAddress = value
                    },
                    label = { Text("IP Address") },
                    isError = status == PairState.Failed || status == PairState.IpInvalid || ipAddress.isEmpty(),
                    leadingIcon = {
                        Icon(Icons.Outlined.Router, contentDescription = "IP Address")
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = port,
                    onValueChange = { value ->
                        port = value
                    },
                    isError = status == PairState.Failed || status == PairState.PortInvalid || port.isEmpty(),
                    label = { Text("Port") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Lan, contentDescription = "Port")
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = pairCode,
                    onValueChange = { value ->
                        pairCode = value
                    },
                    isError = status == PairState.Failed || status == PairState.CodeInvalid || pairCode.isEmpty(),
                    label = { Text("Pairing code") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Password, contentDescription = "Pairing code")
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    enabled = !isLoading,
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            when {
                                ipAddress.isEmpty() && port.isEmpty() -> {
                                    status = PairState.Failed
                                }

                                ipAddress.isEmpty() -> {
                                    status = PairState.IpInvalid
                                }

                                port.isEmpty() -> {
                                    status = PairState.PortInvalid
                                }

                                pairCode.isEmpty() -> {
                                    status = PairState.CodeInvalid
                                }

                                else -> {
                                    isLoading = true
                                    result = runCommand("adb pair $ipAddress:$port $pairCode")
                                    status = if (result.contains("Successfully paired to")) {
                                        PairState.Paired
                                    } else {
                                        PairState.Failed
                                    }
                                    isLoading = false
                                    snackbarHostState.showSnackbar(result.replace("\n", ""))
                                }
                            }
                        }
                    }) {
                    Text("Connect")
                }
            }
        }
    }
}

enum class PairState {
    Waiting,
    Paired,
    IpInvalid,
    PortInvalid,
    CodeInvalid,
    Failed
}
