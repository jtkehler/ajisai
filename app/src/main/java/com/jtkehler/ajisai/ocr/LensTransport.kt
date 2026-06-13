package com.jtkehler.ajisai.ocr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class LensRequest(
    val endpoint: String,
    val headers: Map<String, String>,
    val body: ByteArray,
)

data class LensResponse(
    val statusCode: Int,
    val body: ByteArray,
)

interface LensTransport {
    suspend fun execute(request: LensRequest): LensResponse
}

class OkHttpLensTransport(
    client: OkHttpClient = OkHttpClient(),
) : LensTransport {
    private val client = client.newBuilder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun execute(request: LensRequest): LensResponse = withContext(Dispatchers.IO) {
        val httpRequest = Request.Builder()
            .url(request.endpoint)
            .apply { request.headers.forEach { (name, value) -> header(name, value) } }
            .post(request.body.toRequestBody(PROTOBUF_MEDIA_TYPE))
            .build()
        try {
            client.newCall(httpRequest).execute().use { response ->
                LensResponse(response.code, response.body?.bytes() ?: byteArrayOf())
            }
        } catch (error: IOException) {
            throw OcrException(OcrErrorType.NETWORK, "Google Lens could not be reached.", error)
        }
    }

    private companion object {
        val PROTOBUF_MEDIA_TYPE = "application/x-protobuf".toMediaType()
    }
}
