import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

new_imports = """import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.viewmodel.ModelManager"""

content = content.replace("import android.content.Intent\nimport android.os.Bundle", "import android.content.Intent\nimport android.os.Bundle\n" + new_imports)

old_main_screen = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GenerativeAiViewModel = viewModel()) {"""

new_main_screen = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GenerativeAiViewModel = viewModel()) {
    val context = LocalContext.current
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission responses if needed
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
            }
        } else {
            val permissionsToRequest = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (permissionsToRequest.isNotEmpty()) {
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        }
    }
"""

content = content.replace(old_main_screen, new_main_screen)

content = content.replace("SettingsDialog(\n            settingsManager = settingsManager,\n            onDismiss = { showSettings = false }\n        )", "SettingsDialog(\n            settingsManager = settingsManager,\n            context = context,\n            onDismiss = { showSettings = false }\n        )")

old_settings_dialog = """@Composable
fun SettingsDialog(settingsManager: SettingsManager, onDismiss: () -> Unit) {
    var geminiKey by remember { mutableStateOf(settingsManager.geminiApiKey) }
    var groqKey by remember { mutableStateOf(settingsManager.groqApiKey) }
    var localPath by remember { mutableStateOf(settingsManager.localModelPath) }

    AlertDialog(
        onDismissRequest = onDismiss,"""

new_settings_dialog = """@Composable
fun SettingsDialog(settingsManager: SettingsManager, context: android.content.Context, onDismiss: () -> Unit) {
    var geminiKey by remember { mutableStateOf(settingsManager.geminiApiKey) }
    var groqKey by remember { mutableStateOf(settingsManager.groqApiKey) }
    var localPath by remember { mutableStateOf(settingsManager.localModelPath) }

    AlertDialog(
        onDismissRequest = onDismiss,"""

content = content.replace(old_settings_dialog, new_settings_dialog)


old_settings_fields = """                OutlinedTextField(
                    value = localPath,
                    onValueChange = { localPath = it },
                    label = { Text("Local Model Path (.task)") },
                    singleLine = false
                )
            }
        },"""

new_settings_fields = """                OutlinedTextField(
                    value = localPath,
                    onValueChange = { localPath = it },
                    label = { Text("Local Model Path (.task)") },
                    singleLine = false
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Available Local Models:", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(ModelManager.availableModels) { model ->
                        val isDownloaded = ModelManager.isModelDownloaded(context, model)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(model.name, modifier = Modifier.weight(1f))
                            if (isDownloaded) {
                                TextButton(onClick = { localPath = ModelManager.getModelPath(context, model) }) {
                                    Text("Select")
                                }
                            } else {
                                IconButton(onClick = { ModelManager.downloadModel(context, model) }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Download")
                                }
                            }
                        }
                    }
                }
            }
        },"""

content = content.replace(old_settings_fields, new_settings_fields)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
