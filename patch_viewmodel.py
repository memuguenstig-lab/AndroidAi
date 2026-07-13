import sys

with open("app/src/main/java/com/example/viewmodel/GenerativeAiViewModel.kt", "r") as f:
    content = f.read()

new_methods = """
    private var currentRootPath: String? = null

    fun loadWorkspaceFromPath(path: String) {
        viewModelScope.launch {
            _state.value = AgentState.Loading
            try {
                currentTreeUri = null
                currentRootPath = path
                val root = File(path)
                if (root.exists() && root.isDirectory) {
                    currentFiles = withContext(Dispatchers.IO) { traverseFolderDirect(root, "") }
                    _state.value = AgentState.Success(currentFiles)
                } else {
                    _state.value = AgentState.Error("Directory not found: $path")
                }
            } catch (e: Exception) {
                _state.value = AgentState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun traverseFolderDirect(root: File, path: String, depth: Int = 0): List<FileItem> {
        val result = mutableListOf<FileItem>()
        if (depth > 5) return result // Prevent too deep recursion
        
        val files = root.listFiles() ?: return result
        
        // Skip hidden folders and common large directories to prevent freezing
        for (file in files) {
            if (file.name.startsWith(".")) continue
            if (file.name == "Android" && depth == 0) continue
            
            val filePath = if (path.isEmpty()) file.name else "$path/${file.name}"
            if (file.isDirectory) {
                result.addAll(traverseFolderDirect(file, filePath, depth + 1))
            } else if (file.isFile && isTextual(file.name)) {
                try {
                    // Limit file size to 100KB to prevent OOM
                    if (file.length() < 100_000) {
                        val fileContent = file.readText()
                        result.add(FileItem(Uri.fromFile(file), filePath, fileContent))
                    }
                } catch (e: Exception) {
                    // Ignore unreadable files
                }
            }
        }
        return result
    }
"""

# Insert new_methods into the class
content = content.replace("private fun traverseFolder(context: Context, root: DocumentFile, path: String = \"\"): List<FileItem> {", new_methods + "\n    private fun traverseFolder(context: Context, root: DocumentFile, path: String = \"\"): List<FileItem> {")

# Update applyChanges to handle java.io.File directly
old_apply = """                        val targetFile = currentFiles.find { it.filePath == change.filePath }
                        if (targetFile != null) {
                            context.contentResolver.openOutputStream(targetFile.uri, "wt")?.use { output ->
                                output.bufferedWriter().use { writer ->
                                    writer.write(change.newContent)
                                }
                            }
                        }"""

new_apply = """                        val targetFile = currentFiles.find { it.filePath == change.filePath }
                        if (targetFile != null) {
                            if (targetFile.uri.scheme == "file") {
                                File(targetFile.uri.path!!).writeText(change.newContent)
                            } else {
                                context.contentResolver.openOutputStream(targetFile.uri, "wt")?.use { output ->
                                    output.bufferedWriter().use { writer ->
                                        writer.write(change.newContent)
                                    }
                                }
                            }
                        }"""
content = content.replace(old_apply, new_apply)

# Reload logic update
old_reload = """                // Reload workspace
                currentTreeUri?.let { loadWorkspace(context, it) }"""

new_reload = """                // Reload workspace
                if (currentTreeUri != null) {
                    loadWorkspace(context, currentTreeUri!!)
                } else if (currentRootPath != null) {
                    loadWorkspaceFromPath(currentRootPath!!)
                }"""
content = content.replace(old_reload, new_reload)

with open("app/src/main/java/com/example/viewmodel/GenerativeAiViewModel.kt", "w") as f:
    f.write(content)
