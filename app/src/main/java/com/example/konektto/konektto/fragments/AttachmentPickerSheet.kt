package com.example.konektto.konektto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.konektto.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AttachmentPickerSheet(
    private val onPickImage: () -> Unit,
    private val onPickVoiceNote: () -> Unit,
    private val onPickFile: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.sheet_attachment_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.optionImage).setOnClickListener {
            onPickImage()
            dismiss()
        }

        view.findViewById<View>(R.id.optionVoice).setOnClickListener {
            onPickVoiceNote()
            dismiss()
        }

        view.findViewById<View>(R.id.optionFile).setOnClickListener {
            onPickFile()
            dismiss()
        }

    }

}
