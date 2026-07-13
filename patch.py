import re

with open('app/src/main/java/com/example/viewmodel/GenerativeAiViewModel.kt', 'r') as f:
    content = f.read()

# AgentResponse update
content = re.sub(
    r'data class AgentResponse\(\s*val reasoning: String,\s*val changes: List<FileChange>\s*\)',
    'data class AgentResponse(\n    val reasoning: String,\n    val changes: List<FileChange> = emptyList(),\n    val actions: List<AgentAction> = emptyList()\n)\n\n@Serializable\ndata class AgentAction(\n    val actionType: String,\n    val parameters: Map<String, String>\n)',
    content
)

# Gemini properties update
content = content.replace(
    'putJsonArray("required") { add(JsonPrimitive("reasoning")); add(JsonPrimitive("changes")) }',
    '''putJsonObject("actions") {
                                    put("type", "ARRAY")
                                    putJsonObject("items") {
                                        put("type", "OBJECT")
                                        putJsonObject("properties") {
                                            putJsonObject("actionType") {
                                                put("type", "STRING")
                                                put("description", "Action: 'create_file', 'delete_file', 'shell_command', 'web_search'")
                                            }
                                            putJsonObject("parameters") {
                                                put("type", "OBJECT")
                                            }
                                        }
                                    }
                                }
                            }
                            putJsonArray("required") { add(JsonPrimitive("reasoning")) }'''
)

# OpenAI and Local update
old_json = '''{
                "reasoning": "string",
                "changes": [
                    {
                        "filePath": "string",
                        "newContent": "string"
                    }
                ]
            }'''
new_json = '''{
                "reasoning": "string",
                "changes": [
                    {
                        "filePath": "string",
                        "newContent": "string"
                    }
                ],
                "actions": [
                    {
                        "actionType": "create_file | delete_file | shell_command | web_search",
                        "parameters": {
                        }
                    }
                ]
            }'''
content = content.replace(old_json, new_json)

# applyChanges
old_apply = '''fun applyChanges(context: Context, changes: List<FileChange>) {
        viewModelScope.launch {
            _state.value = AgentState.Loading
            try {
                withContext(Dispatchers.IO) {
                    changes.forEach { change ->
                        val targetFile = currentFiles.find { it.filePath == change.filePath }
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
                        }
                    }
                }'''
new_apply = '''fun applyChanges(context: Context, response: AgentResponse) {
        viewModelScope.launch {
            _state.value = AgentState.Loading
            try {
                withContext(Dispatchers.IO) {
                    response.changes.forEach { change ->
                        val targetFile = currentFiles.find { it.filePath == change.filePath }
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
                        } else {
                            if (currentRootPath != null) {
                                val file = File(currentRootPath!!, change.filePath)
                                file.parentFile?.mkdirs()
                                file.writeText(change.newContent)
                            } else if (currentTreeUri != null) {
                                val rootDoc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, currentTreeUri!!)
                                val parts = change.filePath.split("/")
                                var currentDoc = rootDoc
                                for (i in 0 until parts.size - 1) {
                                    val dirName = parts[i]
                                    var nextDoc = currentDoc?.findFile(dirName)
                                    if (nextDoc == null) {
                                        nextDoc = currentDoc?.createDirectory(dirName)
                                    }
                                    currentDoc = nextDoc
                                }
                                var newFile = currentDoc?.findFile(parts.last())
                                if (newFile == null) {
                                    newFile = currentDoc?.createFile("text/plain", parts.last())
                                }
                                newFile?.uri?.let { uri ->
                                    context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                                        output.bufferedWriter().use { writer ->
                                            writer.write(change.newContent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    response.actions.forEach { action ->
                        when (action.actionType) {
                            "delete_file" -> {
                                val filePath = action.parameters["filePath"]
                                if (filePath != null && currentRootPath != null) {
                                    File(currentRootPath, filePath).delete()
                                }
                            }
                            "shell_command" -> {
                                val command = action.parameters["command"]
                                if (command != null) {
                                    try {
                                        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                                        process.waitFor()
                                    } catch (e: Exception) {}
                                }
                            }
                        }
                    }
                }'''
content = content.replace(old_apply, new_apply)

with open('app/src/main/java/com/example/viewmodel/GenerativeAiViewModel.kt', 'w') as f:
    f.write(content)
