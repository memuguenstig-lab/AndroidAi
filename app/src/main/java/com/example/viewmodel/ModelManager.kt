package com.example.viewmodel

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

data class LocalModel(val name: String, val url: String, val filename: String)

object ModelManager {
    val availableModels = listOf(
        LocalModel("Gemma 2B IT (CPU, int4)", "https://storage.googleapis.com/mediapipe-models/llm/gemma-2b-it-cpu-int4/float32/latest/gemma-2b-it-cpu-int4.task", "gemma-2b-it-cpu-int4.task"),
        LocalModel("Gemma 2B IT (CPU, int8)", "https://storage.googleapis.com/mediapipe-models/llm/gemma-2b-it-cpu-int8/float32/latest/gemma-2b-it-cpu-int8.task", "gemma-2b-it-cpu-int8.task"),
        LocalModel("Gemma 2B IT (GPU, int4)", "https://storage.googleapis.com/mediapipe-models/llm/gemma-2b-it-gpu-int4/float32/latest/gemma-2b-it-gpu-int4.task", "gemma-2b-it-gpu-int4.task"),
        LocalModel("Gemma 2B IT (GPU, int8)", "https://storage.googleapis.com/mediapipe-models/llm/gemma-2b-it-gpu-int8/float32/latest/gemma-2b-it-gpu-int8.task", "gemma-2b-it-gpu-int8.task"),
        LocalModel("Gemma 2B IT (GPU, fp16)", "https://storage.googleapis.com/mediapipe-models/llm/gemma-2b-it-gpu-fp16/float32/latest/gemma-2b-it-gpu-fp16.task", "gemma-2b-it-gpu-fp16.task"),
        LocalModel("Phi-2 (CPU, int4)", "https://storage.googleapis.com/mediapipe-models/llm/phi-2/float32/latest/phi-2-cpu-int4.task", "phi-2-cpu-int4.task"),
        LocalModel("Phi-2 (GPU, int4)", "https://storage.googleapis.com/mediapipe-models/llm/phi-2/float32/latest/phi-2-gpu-int4.task", "phi-2-gpu-int4.task"),
        LocalModel("Falcon 1B (CPU, int4)", "https://storage.googleapis.com/mediapipe-models/llm/falcon-1b/float32/latest/falcon-1b-cpu-int4.task", "falcon-1b-cpu-int4.task"),
        LocalModel("Falcon 1B (GPU, int4)", "https://storage.googleapis.com/mediapipe-models/llm/falcon-1b/float32/latest/falcon-1b-gpu-int4.task", "falcon-1b-gpu-int4.task"),
        LocalModel("Llama 2 7B (CPU, int4)", "https://storage.googleapis.com/mediapipe-models/llm/llama-2-7b/float32/latest/llama-2-7b-cpu-int4.task", "llama-2-7b-cpu-int4.task"),
        LocalModel("Llama 3 8B (CPU, int4)", "https://storage.googleapis.com/mediapipe-models/llm/llama-3-8b/float32/latest/llama-3-8b-cpu-int4.task", "llama-3-8b-cpu-int4.task"),
        LocalModel("Mistral 7B (CPU, int4)", "https://storage.googleapis.com/mediapipe-models/llm/mistral-7b/float32/latest/mistral-7b-cpu-int4.task", "mistral-7b-cpu-int4.task")
    )

    fun downloadModel(context: Context, model: LocalModel) {
        val request = DownloadManager.Request(Uri.parse(model.url))
            .setTitle("Downloading ${model.name}")
            .setDescription("Downloading model for AI File Agent")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, model.filename)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }

    fun getModelPath(context: Context, model: LocalModel): String {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), model.filename)
        return file.absolutePath
    }

    fun isModelDownloaded(context: Context, model: LocalModel): Boolean {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), model.filename)
        return file.exists()
    }
}
