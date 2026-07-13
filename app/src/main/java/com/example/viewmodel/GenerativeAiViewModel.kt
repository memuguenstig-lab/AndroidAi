package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.network.*
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.add
import java.io.File

data class FileItem(
    val uri: Uri,
    val filePath: String,
    val content: String
)

@Serializable
data class FileChange(
    val filePath: String,
    val newContent: String
)

@Serializable
data class AgentResponse(
    val reasoning: String,
    val changes: List<FileChange> = emptyList(),
    val actions: List<AgentAction> = emptyList()
)

@Serializable
data class AgentAction(
    val actionType: String,
    val parameters: Map<String, String>
)

sealed class AgentState {
    object Idle : AgentState()
    object Loading : AgentState()
    data class Success(val files: List<FileItem>) : AgentState()
    data class ProposedChanges(val files: List<FileItem>, val response: AgentResponse) : AgentState()
    data class Error(val message: String) : AgentState()
}

class GenerativeAiViewModel : ViewModel() {
    private val _state = MutableStateFlow<AgentState>(AgentState.Idle)
    val state: StateFlow<AgentState> = _state.asStateFlow()

    private var currentFiles = listOf<FileItem>()
    private var currentTreeUri: Uri? = null

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private var llmInference: LlmInference? = null
    private var currentModelPath: String? = null

    override fun onCleared() {
        super.onCleared()
        llmInference?.close()
    }

    fun loadWorkspace(context: Context, treeUri: Uri) {
        viewModelScope.launch {
            _state.value = AgentState.Loading
            try {
                currentTreeUri = treeUri
                val root = DocumentFile.fromTreeUri(context, treeUri)
                if (root != null) {
                    currentFiles = withContext(Dispatchers.IO) { traverseFolder(context, root) }
                    _state.value = AgentState.Success(currentFiles)
                } else {
                    _state.value = AgentState.Error("Could not open directory")
                }
            } catch (e: Exception) {
                _state.value = AgentState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun analyzeAndPropose(context: Context, prompt: String, settings: SettingsManager) {
        if (currentFiles.isEmpty()) {
            _state.value = AgentState.Error("No files loaded")
            return
        }
        viewModelScope.launch {
            _state.value = AgentState.Loading
            try {
                val filesContext = currentFiles.joinToString("\n\n") { 
                    "--- File: ${it.filePath} ---\n${it.content}"
                }
                
                var systemInstructions = ""
                if (settings.erfinderMode) {
                    systemInstructions = """
                        You are in 'Erfinder mode' (Inventor mode). 
                        Your goal is to invent, research, and develop new technologies, hardware, or concepts step-by-step.
                        Instead of just writing code, act as an inventor and scientist.
                        Think deeply and iterate until the goal is achieved.
                        If the task involves real-world hardware or physics, give the user step-by-step instructions on what to test in real life.
                        Wait for the user to report back the results in the next prompt, and iterate based on those results.
                        You must still output valid JSON. Write your step-by-step instructions, thoughts, and questions for the user into a file like 'INVENTION_LOG.md' or 'TEST_PLAN.md'.
                        
                    """.trimIndent()
                }

                val fullPrompt = "${systemInstructions}User Prompt: $prompt\n\nWorkspace Files:\n$filesContext"
                val responseText = when (settings.provider) {
                    ModelProvider.GEMINI -> callGemini(fullPrompt, settings)
                    ModelProvider.GROQ -> callOpenAi(fullPrompt, settings, "https://api.groq.com/openai/v1/chat/completions", "llama3-70b-8192", settings.groqApiKey)
                    ModelProvider.LOCAL_ON_DEVICE -> callLocalOnDevice(context, fullPrompt, settings)
                }

                if (responseText != null) {
                    val cleanResponse = responseText.substringAfter("```json").substringBeforeLast("```").trim()
                    val agentResponse = json.decodeFromString<AgentResponse>(cleanResponse)
                    _state.value = AgentState.ProposedChanges(currentFiles, agentResponse)
                } else {
                    _state.value = AgentState.Error("Empty response from AI")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = AgentState.Error(e.message ?: "Error calling API")
            }
        }
    }

    private suspend fun callGemini(fullPrompt: String, settings: SettingsManager): String? {
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = fullPrompt)))),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(
                        mimeType = "application/json",
                        schema = buildJsonObject {
                            put("type", "OBJECT")
                            putJsonObject("properties") {
                                putJsonObject("reasoning") {
                                    put("type", "STRING")
                                    put("description", "Explanation of the changes")
                                }
                                putJsonObject("changes") {
                                    put("type", "ARRAY")
                                    putJsonObject("items") {
                                        put("type", "OBJECT")
                                        putJsonObject("properties") {
                                            putJsonObject("filePath") {
                                                put("type", "STRING")
                                            }
                                            putJsonObject("newContent") {
                                                put("type", "STRING")
                                            }
                                        }
                                        putJsonArray("required") { add(JsonPrimitive("filePath")); add(JsonPrimitive("newContent")) }
                                    }
                                }
                                putJsonObject("actions") {
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
                                        putJsonArray("required") { add(JsonPrimitive("actionType")); add(JsonPrimitive("parameters")) }
                                    }
                                }
                            }
                            putJsonArray("required") { add(JsonPrimitive("reasoning")); add(JsonPrimitive("changes")); add(JsonPrimitive("actions")) }
                        }
                    )
                )
            )
        )
        val response = RetrofitClient.service.generateContent(settings.geminiApiKey, request)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
    }

    private suspend fun callOpenAi(fullPrompt: String, settings: SettingsManager, url: String, model: String, apiKey: String): String? {
        val systemPrompt = """
            You are an AI coding agent. You must respond in valid JSON matching this schema exactly:
            {
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
            }
        """.trimIndent()
        val request = OpenAiRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(role = "system", content = systemPrompt),
                OpenAiMessage(role = "user", content = fullPrompt)
            ),
            response_format = OpenAiResponseFormat(type = "json_object"),
            temperature = 0.1f
        )
        val authHeader = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"
        val response = RetrofitClient.openAiService.generateContent(url, authHeader, request)
        return response.choices?.firstOrNull()?.message?.content
    }

    private suspend fun callLocalOnDevice(context: Context, fullPrompt: String, settings: SettingsManager): String? {
        return withContext(Dispatchers.IO) {
            val modelPath = settings.localModelPath
            if (!File(modelPath).exists()) {
                throw Exception("Local model file not found at $modelPath. Please download a MediaPipe .task model (e.g., Gemma) and put it there.")
            }

            if (llmInference == null || currentModelPath != modelPath) {
                llmInference?.close()
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(1024)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                currentModelPath = modelPath
            }

            val systemPrompt = """
                You are an AI coding agent. You must respond in valid JSON matching this schema exactly:
                {
                    "reasoning": "string",
                    "changes": [
                        {
                            "filePath": "string",
                            "newContent": "string"
                        }
                    ]
                }
            """.trimIndent()

            val combinedPrompt = "$systemPrompt\n\n$fullPrompt\n\nReturn JSON ONLY:"
            llmInference!!.generateResponse(combinedPrompt)
        }
    }

    fun applyChanges(context: Context, response: AgentResponse) {
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
                }
                // Reload workspace
                if (currentTreeUri != null) {
                    loadWorkspace(context, currentTreeUri!!)
                } else if (currentRootPath != null) {
                    loadWorkspaceFromPath(currentRootPath!!)
                }
            } catch (e: Exception) {
                _state.value = AgentState.Error("Failed to apply changes: ${e.message}")
            }
        }
    }

    fun cancelChanges() {
        _state.value = AgentState.Success(currentFiles)
    }

    
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

    private fun traverseFolderDirect(root: File, path: String, depth: Int = 0, maxFiles: Int = 5000, result: MutableList<FileItem> = mutableListOf()): List<FileItem> {
        if (depth > 15 || result.size >= maxFiles) return result
        
        val files = root.listFiles() ?: return result
        
        for (file in files) {
            if (result.size >= maxFiles) break
            val name = file.name
            val filePath = if (path.isEmpty()) name else "$path/$name"
            if (file.isDirectory) {
                traverseFolderDirect(file, filePath, depth + 1, maxFiles, result)
            } else if (file.isFile) {
                try {
                    var content = ""
                    val size = file.length()
                    if (isTextual(name)) {
                        if (size < 250_000) {
                            content = file.readText()
                        } else {
                            content = "[Text file too large: ${size / 1024} KB]"
                        }
                    } else {
                        content = "[Binary or unsupported file: ${size / 1024} KB]"
                    }
                    result.add(FileItem(Uri.fromFile(file), filePath, content))
                } catch (e: Exception) {
                    result.add(FileItem(Uri.fromFile(file), filePath, "[Error reading file: ${e.message}]"))
                }
            }
        }
        return result
    }

    private fun traverseFolder(context: Context, root: DocumentFile, path: String = "", depth: Int = 0, maxFiles: Int = 5000, result: MutableList<FileItem> = mutableListOf()): List<FileItem> {
        if (depth > 15 || result.size >= maxFiles) return result

        root.listFiles().forEach { file ->
            if (result.size >= maxFiles) return@forEach
            val name = file.name ?: ""
            val filePath = if (path.isEmpty()) name else "$path/$name"
            if (file.isDirectory) {
                traverseFolder(context, file, filePath, depth + 1, maxFiles, result)
            } else if (file.isFile) {
                try {
                    var content = ""
                    val size = file.length()
                    if (isTextual(name)) {
                        if (size < 250_000) {
                            content = context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() } ?: ""
                        } else {
                            content = "[Text file too large: ${size / 1024} KB]"
                        }
                    } else {
                        content = "[Binary or unsupported file: ${size / 1024} KB]"
                    }
                    result.add(FileItem(file.uri, filePath, content))
                } catch (e: Exception) {
                    result.add(FileItem(file.uri, filePath, "[Error reading file: ${e.message}]"))
                }
            }
        }
        return result
    }

    private fun isTextual(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        val textExtensions = setOf("txt", "md", "kt", "java", "xml", "json", "gradle", "kts", "csv", "html", "js", "css", "py", "c", "cpp", "h")
        return textExtensions.contains(ext) || ext.isEmpty()
    }
}
