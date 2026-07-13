package com.example.viewmodel

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

data class LocalModel(val name: String, val size: String, val url: String, val filename: String)

sealed class DownloadState {
    object NotDownloaded : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    object Downloaded : DownloadState()
}

object ModelManager {
    val availableModels = listOf(
        LocalModel("Gemma 2B IT (CPU, int4)", "1.3 GB", "https://storage.googleapis.com/mediapipe-models/llm/gemma-2b-it-cpu-int4/float32/latest/gemma-2b-it-cpu-int4.task", "gemma-2b-it-cpu-int4.task"),
        LocalModel("Gemma 2B IT (CPU, int8)", "2.6 GB", "https://storage.googleapis.com/mediapipe-models/llm/gemma-2b-it-cpu-int8/float32/latest/gemma-2b-it-cpu-int8.task", "gemma-2b-it-cpu-int8.task"),
        LocalModel("Gemma 2B IT (GPU, int4)", "1.5 GB", "https://storage.googleapis.com/mediapipe-models/llm/gemma-2b-it-gpu-int4/float32/latest/gemma-2b-it-gpu-int4.task", "gemma-2b-it-gpu-int4.task"),
        LocalModel("Gemma 2B IT (GPU, int8)", "2.8 GB", "https://storage.googleapis.com/mediapipe-models/llm/gemma-2b-it-gpu-int8/float32/latest/gemma-2b-it-gpu-int8.task", "gemma-2b-it-gpu-int8.task"),
        LocalModel("Gemma 2B IT (GPU, fp16)", "5.2 GB", "https://storage.googleapis.com/mediapipe-models/llm/gemma-2b-it-gpu-fp16/float32/latest/gemma-2b-it-gpu-fp16.task", "gemma-2b-it-gpu-fp16.task"),
        LocalModel("Phi-2 (CPU, int4)", "1.6 GB", "https://storage.googleapis.com/mediapipe-models/llm/phi-2/float32/latest/phi-2-cpu-int4.task", "phi-2-cpu-int4.task"),
        LocalModel("Phi-2 (GPU, int4)", "1.6 GB", "https://storage.googleapis.com/mediapipe-models/llm/phi-2/float32/latest/phi-2-gpu-int4.task", "phi-2-gpu-int4.task"),
        LocalModel("Falcon 1B (CPU, int4)", "800 MB", "https://storage.googleapis.com/mediapipe-models/llm/falcon-1b/float32/latest/falcon-1b-cpu-int4.task", "falcon-1b-cpu-int4.task"),
        LocalModel("Falcon 1B (GPU, int4)", "800 MB", "https://storage.googleapis.com/mediapipe-models/llm/falcon-1b/float32/latest/falcon-1b-gpu-int4.task", "falcon-1b-gpu-int4.task"),
        LocalModel("Llama 2 7B (CPU, int4)", "4.0 GB", "https://storage.googleapis.com/mediapipe-models/llm/llama-2-7b/float32/latest/llama-2-7b-cpu-int4.task", "llama-2-7b-cpu-int4.task"),
        LocalModel("Llama 3 8B (CPU, int4)", "4.5 GB", "https://storage.googleapis.com/mediapipe-models/llm/llama-3-8b/float32/latest/llama-3-8b-cpu-int4.task", "llama-3-8b-cpu-int4.task"),
        LocalModel("Mistral 7B (CPU, int4)", "4.0 GB", "https://storage.googleapis.com/mediapipe-models/llm/mistral-7b/float32/latest/mistral-7b-cpu-int4.task", "mistral-7b-cpu-int4.task")
    )

    private val downloadIds = mutableMapOf<String, Long>()

    fun downloadModel(context: Context, model: LocalModel) {
        val request = DownloadManager.Request(Uri.parse(model.url))
            .setTitle("Downloading ${model.name}")
            .setDescription("Downloading model for AI File Agent")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, model.filename)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = downloadManager.enqueue(request)
        downloadIds[model.filename] = id
    }

    fun getModelPath(context: Context, model: LocalModel): String {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), model.filename)
        return file.absolutePath
    }

    fun isModelDownloaded(context: Context, model: LocalModel): Boolean {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), model.filename)
        return file.exists()
    }

    fun observeDownload(context: Context, model: LocalModel): Flow<DownloadState> = flow {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        while (true) {
            if (isModelDownloaded(context, model)) {
                emit(DownloadState.Downloaded)
                break
            }

            val id = downloadIds[model.filename]
            if (id == null) {
                // Try to find if it's currently downloading
                val query = DownloadManager.Query()
                val cursor = downloadManager.query(query)
                var found = false
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI)
                        if (uriIndex != -1) {
                            val uri = cursor.getString(uriIndex)
                            if (uri == model.url) {
                                val idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
                                if (idIndex != -1) {
                                    downloadIds[model.filename] = cursor.getLong(idIndex)
                                    found = true
                                }
                            }
                        }
                    }
                    cursor.close()
                }
                
                if (!found) {
                    emit(DownloadState.NotDownloaded)
                }
            }
            
            val activeId = downloadIds[model.filename]
            if (activeId != null) {
                val query = DownloadManager.Query().setFilterById(activeId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    
                    if (statusIndex != -1 && bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                        val status = cursor.getInt(statusIndex)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            emit(DownloadState.Downloaded)
                            cursor.close()
                            break
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            emit(DownloadState.NotDownloaded)
                            downloadIds.remove(model.filename)
                        } else {
                            val bytesDownloaded = cursor.getInt(bytesDownloadedIndex)
                            val bytesTotal = cursor.getInt(bytesTotalIndex)
                            val progress = if (bytesTotal > 0) bytesDownloaded.toFloat() / bytesTotal else 0f
                            emit(DownloadState.Downloading(progress))
                        }
                    }
                    cursor.close()
                } else {
                    cursor?.close()
                    emit(DownloadState.NotDownloaded)
                }
            }
            
            delay(1000)
        }
    }
}
