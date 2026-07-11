package com.example.konektto.konektto.widgets

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat

/**
 * An EditText that accepts content (GIFs, images) committed directly from
 * a keyboard's built-in search -- Gboard and SwiftKey both support this
 * via Android's standard commitContent API. This is the whole mechanism
 * behind Konektto's GIF support: no third-party API, no API key to manage,
 * no custom search screen to build and maintain. The keyboard the person
 * already has installed does the searching; all this class does is accept
 * what it hands back and forward it to whoever is listening.
 */
class GifSupportEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    /** (contentUri, mimeType) -> Unit */
    var onContentCommitted: ((Uri, String) -> Unit)? = null

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {

        val inputConnection = super.onCreateInputConnection(outAttrs) ?: return null

        EditorInfoCompat.setContentMimeTypes(
            outAttrs,
            arrayOf("image/gif", "image/png", "image/jpeg")
        )

        val callback = InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, _ ->

            try {

                if (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION != 0) {
                    inputContentInfo.requestPermission()
                }

                val mimeType = if (inputContentInfo.description.mimeTypeCount > 0) {
                    inputContentInfo.description.getMimeType(0)
                } else {
                    "image/gif"
                }

                onContentCommitted?.invoke(inputContentInfo.contentUri, mimeType)

                true

            } catch (e: Exception) {

                false

            }

        }

        return InputConnectionCompat.createWrapper(inputConnection, outAttrs, callback)

    }

}
