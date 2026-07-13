import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

old_idle = """                        Button(onClick = { dirPickerLauncher.launch(null) }) {
                            Text("Select Workspace Folder")
                        }
                    }
                }
                is AgentState.Loading -> {"""

new_idle = """                        Button(onClick = { dirPickerLauncher.launch(null) }) {
                            Text("Select Workspace Folder (SAF)")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.loadWorkspaceFromPath("/storage/emulated/0") }) {
                            Text("Load Full Storage (/sdcard)")
                        }
                    }
                }
                is AgentState.Loading -> {"""
content = content.replace(old_idle, new_idle)

old_workspace = """        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Workspace: ${files.size} files loaded",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onSelectNewFolder) {
                Text("Change")
            }
        }"""

new_workspace = """        Row(
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
        }"""
content = content.replace(old_workspace, new_workspace)

# We need to add onSelectFullStorage to WorkspaceScreen signature and call
old_workspace_sig = """fun WorkspaceScreen(
    files: List<FileItem>,
    settingsManager: SettingsManager,
    onAnalyze: (String) -> Unit,
    onSelectNewFolder: () -> Unit
) {"""

new_workspace_sig = """fun WorkspaceScreen(
    files: List<FileItem>,
    settingsManager: SettingsManager,
    onAnalyze: (String) -> Unit,
    onSelectNewFolder: () -> Unit,
    onSelectFullStorage: () -> Unit
) {"""
content = content.replace(old_workspace_sig, new_workspace_sig)

old_workspace_call = """                    WorkspaceScreen(
                        files = s.files,
                        settingsManager = settingsManager,
                        onAnalyze = { prompt -> viewModel.analyzeAndPropose(context, prompt, settingsManager) },
                        onSelectNewFolder = { dirPickerLauncher.launch(null) }
                    )"""

new_workspace_call = """                    WorkspaceScreen(
                        files = s.files,
                        settingsManager = settingsManager,
                        onAnalyze = { prompt -> viewModel.analyzeAndPropose(context, prompt, settingsManager) },
                        onSelectNewFolder = { dirPickerLauncher.launch(null) },
                        onSelectFullStorage = { viewModel.loadWorkspaceFromPath("/storage/emulated/0") }
                    )"""
content = content.replace(old_workspace_call, new_workspace_call)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
