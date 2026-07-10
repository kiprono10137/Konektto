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

object CloudinaryUploader {

    private const val CLOUD_NAME = "dayafm4jt"
    private const val UPLOAD_PRESET = "konektto_app"

    private val client = OkHttpClient()

    fun uploadImage(
        imageBytes: ByteArray,
        callback: (String?) -> Unit
    ) {

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "profile.jpg",
                imageBytes.toRequestBody(
                    "image/jpeg".toMediaType()
                )
            )
            .addFormDataPart(
                "upload_preset",
                UPLOAD_PRESET
            )
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
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
                    Log.d("Cloudinary", "Raw Response = $body")

                    if (!response.isSuccessful || body == null) {

                        callback(null)
                        return

                    }

                    try {

                        val json = JSONObject(body)

                        Log.d(
                            "Cloudinary",
                            "Formatted JSON:\n${json.toString(4)}"
                        )

                        val secureUrl =
                            json.optString("secure_url", "")

                        Log.d(
                            "Cloudinary",
                            "Secure URL = $secureUrl"
                        )

                        if (secureUrl.isNotEmpty()) {

                            callback(secureUrl)

                        } else {

                            callback(null)

                        }

                    } catch (e: Exception) {

                        Log.e(
                            "Cloudinary",
                            "JSON Parse Error",
                            e
                        )

                        callback(null)

                    }

                }

            }

        })

    }

}