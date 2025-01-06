package com.app.messageapp.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.app.messageapp.databinding.DialogConfirmationBinding

class ConfirmationDialogFragment : DialogFragment() {
	lateinit var binding: DialogConfirmationBinding
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		binding = DialogConfirmationBinding.inflate(inflater, container, false)
		return binding.root
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		dialog?.setCancelable(true)

		binding.btnCancel.setOnClickListener { dialog?.dismiss() }
		
		binding.btnConfirm.setOnClickListener { dialog?.dismiss() }
	}

	override fun onStart() {
		super.onStart()
		dialog?.window?.setLayout(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
		)
	}
}