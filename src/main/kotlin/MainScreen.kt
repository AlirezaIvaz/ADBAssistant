import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ADB Assistant",
    ) {
        Main()
    }
}

@Composable
fun Main() {
    var screenState by remember { mutableStateOf<Screen>(Screen.Home) }
    AppTheme {
        when (screenState) {
            is Screen.Home -> HomeScreen { screenState = it }
            is Screen.Devices -> DevicesScreen { screenState = it }
            is Screen.DevicePair -> DevicePairScreen { screenState = it }
            is Screen.DeviceConnect -> DeviceConnectScreen { screenState = it }
            is Screen.Restart -> RestartScreen { screenState = it }
        }
    }
}

suspend fun runCommand(command: String): String {
    val os = System.getProperty("os.name").lowercase(Locale.getDefault())
    val processBuilder = if (os.contains("win")) {
        ProcessBuilder("cmd.exe", "/c", command)
    } else {
        ProcessBuilder("bash", "-c", command)
    }
//    val processBuilder = ProcessBuilder()
//    processBuilder.command("bash", "-c", command)
    return try {
        withContext(Dispatchers.IO) {
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readText()
            process.waitFor()
            line
        }
    } catch (e: Exception) {
        "Command failed"
    }
}

sealed class Screen {
    object Home : Screen()
    object Devices : Screen()
    object DevicePair : Screen()
    object DeviceConnect : Screen()
    object Restart : Screen()
}
