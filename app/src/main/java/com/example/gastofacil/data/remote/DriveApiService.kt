package com.example.gastofacil.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class DriveApiService(private val accessToken: String) {

    private val client = OkHttpClient()
    private val fileName = "GastoFacil_Backup.json"

    // 1. Procura se já existe um backup guardado no Drive
    suspend fun findBackupFileId(): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files?q=name='$fileName' and trashed=false"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val files = json.getJSONArray("files")
                if (files.length() > 0) {
                    return@withContext files.getJSONObject(0).getString("id")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    // 2. Faz upload ou atualiza o ficheiro de backup no Drive de forma segura
    suspend fun uploadBackup(jsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val existingFileId = findBackupFileId()
            val metadata = JSONObject().apply {
                put("name", fileName)
                put("mimeType", "application/json")
            }

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata",
                    null,
                    metadata.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .addFormDataPart(
                    "file",
                    fileName,
                    jsonContent.toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .build()

            val url = if (existingFileId == null) {
                "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
            } else {
                "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=multipart"
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")

            val request = if (existingFileId == null) {
                requestBuilder.post(multipartBody).build()
            } else {
                requestBuilder.patch(multipartBody).build()
            }

            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 3. Descarrega o conteúdo do backup do Google Drive
    suspend fun downloadBackup(): String? = withContext(Dispatchers.IO) {
        try {
            val fileId = findBackupFileId() ?: return@withContext null
            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                return@withContext response.body?.string()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}