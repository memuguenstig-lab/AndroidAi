package com.example

import android.content.Intent
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.viewmodel.ModelManager
import com.example.viewmodel.DownloadState
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AgentState
import com.example.viewmodel.FileItem
import com.example.viewmodel.GenerativeAiViewModel
import com.example.viewmodel.ModelProvider
import com.example.viewmodel.SettingsManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    val state by viewModel.state.collectAsState()
    val settingsManager = remember { SettingsManager(context) }
    
    var showSettings by remember { mutableStateOf(false) }

    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            viewModel.loadWorkspace(context, uri)
        }
    }

    if (showSettings) {
        SettingsDialog(
            settingsManager = settingsManager,
            context = context,
            onDismiss = { showSettings = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI File Agent") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val s = state) {
                is AgentState.Idle -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Folder",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Select a workspace to analyze",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { dirPickerLauncher.launch(null) }) {
                            Text("Select Workspace Folder (SAF)")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.loadWorkspaceFromPath("/storage/emulated/0") }) {
                            Text("Load Full Storage (/sdcard)")
                        }
                    }
                }
                is AgentState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AgentState.Success -> {
                    WorkspaceScreen(
                        files = s.files,
                        settingsManager = settingsManager,
                        onAnalyze = { prompt -> viewModel.analyzeAndPropose(context, prompt, settingsManager) },
                        onSelectNewFolder = { dirPickerLauncher.launch(null) },
                        onSelectFullStorage = { viewModel.loadWorkspaceFromPath("/storage/emulated/0") }
                    )
                }
                is AgentState.ProposedChanges -> {
                    ProposedChangesScreen(
                        state = s,
                        onApply = { viewModel.applyChanges(context, s.response.changes) },
                        onCancel = { viewModel.cancelChanges() }
                    )
                }
                is AgentState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(s.message, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { dirPickerLauncher.launch(null) }) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(settingsManager: SettingsManager, context: android.content.Context, onDismiss: () -> Unit) {
    var geminiKey by remember { mutableStateOf(settingsManager.geminiApiKey) }
    var groqKey by remember { mutableStateOf(settingsManager.groqApiKey) }
    var localPath by remember { mutableStateOf(settingsManager.localModelPath) }
    var erfinderMode by remember { mutableStateOf(settingsManager.erfinderMode) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Erfinder Mode (Inventor Mode)")
                    Switch(checked = erfinderMode, onCheckedChange = { erfinderMode = it })
                }
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    label = { Text("Gemini API Key") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = groqKey,
                    onValueChange = { groqKey = it },
                    label = { Text("Groq API Key") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = localPath,
                    onValueChange = { localPath = it },
                    label = { Text("Local Model Path (.task)") },
                    singleLine = false
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Available Local Models:", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(ModelManager.availableModels) { model ->
                        val downloadState by ModelManager.observeDownload(context, model).collectAsState(initial = DownloadState.NotDownloaded)
                        
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(model.name)
                                    Text("Size: ${model.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                when (downloadState) {
                                    is DownloadState.Downloaded -> {
                                        TextButton(onClick = { localPath = ModelManager.getModelPath(context, model) }) {
                                            Text("Select")
                                        }
                                    }
                                    is DownloadState.Downloading -> {
                                        Text("${((downloadState as DownloadState.Downloading).progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                                    }
                                    is DownloadState.NotDownloaded -> {
                                        IconButton(onClick = { ModelManager.downloadModel(context, model) }) {
                                            Icon(Icons.Default.Download, contentDescription = "Download")
                                        }
                                    }
                                }
                            }
                            if (downloadState is DownloadState.Downloading) {
                                val progress = (downloadState as DownloadState.Downloading).progress
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    settingsManager.geminiApiKey = geminiKey
                    settingsManager.groqApiKey = groqKey
                    settingsManager.localModelPath = localPath
                    settingsManager.erfinderMode = erfinderMode
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun WorkspaceScreen(
    files: List<FileItem>,
    settingsManager: SettingsManager,
    onAnalyze: (String) -> Unit,
    onSelectNewFolder: () -> Unit,
    onSelectFullStorage: () -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var currentProvider by remember { mutableStateOf(settingsManager.provider) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Files: ${files.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row {
                TextButton(onClick = onSelectNewFolder) {
                    Text("SAF")
                }
                TextButton(onClick = onSelectFullStorage) {
                    Text("SD Card")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            items(files) { file ->
                Text(
                    text = file.filePath,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Model Provider Selection
        Text("AI Provider", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ModelProvider.values().forEach { provider ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = currentProvider == provider,
                        onClick = {
                            currentProvider = provider
                            settingsManager.provider = provider
                        }
                    )
                    Text(provider.name)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("What should the AI do?") },
            placeholder = { Text("e.g. Find all TODOs and fix them") },
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onAnalyze(prompt) },
            modifier = Modifier.fillMaxWidth(),
            enabled = prompt.isNotBlank()
        ) {
            Icon(Icons.Default.AutoFixHigh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Analyze & Propose Changes")
        }
    }
}

@Composable
fun ProposedChangesScreen(
    state: AgentState.ProposedChanges,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("AI Reasoning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = state.response.reasoning,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Proposed Changes (${state.response.changes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(state.response.changes) { change ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "File: ${change.filePath}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = change.newContent.take(200) + if (change.newContent.length > 200) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onApply,
                modifier = Modifier.weight(1f)
            ) {
                Text("Apply Changes")
            }
        }
    }
}
