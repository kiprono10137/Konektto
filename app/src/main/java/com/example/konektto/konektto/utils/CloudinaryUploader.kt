package com.example.konektto.konektto.utils

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object CloudinaryUploader {

    private const val CLOUD_NAME = "dayafm4jt"
    private const val UPLOAD_PRESET = "konektto_app"

    // Chat attachments (especially voice notes and files) can be larger
    // and slower than a profile picture upload; the default OkHttpClient
    // timeouts are tuned for quick API calls, not multi-second uploads
    // over a mediocre connection.
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Kept for backward compatibility with existing callers
     * (EditProfileActivity) -- behaves exactly as before.
     */
    fun uploadImage(
        imageBytes: ByteArray,
        callback: (String?) -> Unit
    ) {
        uploadFile(
            bytes = imageBytes,
            filename = "profile.jpg",
            mimeType = "image/jpeg",
            resourceType = "image",
            callback = callback
        )
    }

    /**
     * Generic upload covering everything Phase 4 needs:
     *  - resourceType "image"  -> photos AND gifs (Cloudinary stores an
     *                             animated gif as-is under the image
     *                             resource type; no special handling needed)
     *  - resourceType "video"  -> Cloudinary's convention for audio files
     *                             too, not just video -- there is no
     *                             separate "audio" resource type
     *  - resourceType "raw"    -> arbitrary documents/files
     */
    fun uploadFile(
        bytes: ByteArray,
        filename: String,
        mimeType: String,
        resourceType: String,
        callback: (String?) -> Unit
    ) {

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                filename,
                bytes.toRequestBody(mimeType.toMediaType())
            )
            .addFormDataPart(
                "upload_preset",
                UPLOAD_PRESET
            )
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/$resourceType/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(
                call: Call,
                e: IOException
            ) {

                Log.e("Cloudinary", "Upload failed", e)
                callback(null)

            }

            override fun onResponse(
                call: Call,
                response: Response
            ) {

                response.use {

                    val body = response.body?.string()

                    Log.d("Cloudinary", "HTTP Code = ${response.code}")

                    if (!response.isSuccessful || body == null) {

                        callback(null)
                        return

                    }

                    try {

                        val json = JSONObject(body)

                        val secureUrl =
                            json.optString("secure_url", "")

                        if (secureUrl.isNotEmpty()) {

                            callback(secureUrl)

                        } else {

                            callback(null)

                        }

                    } catch (e: Exception) {

                        Log.e("Cloudinary", "JSON Parse Error", e)
                        callback(null)

                    }

                }

            }

        })

    }

}
