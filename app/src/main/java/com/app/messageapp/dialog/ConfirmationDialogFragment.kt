package com.app.messageapp.dialog

import android.os.Bundle
import android.security.ConfirmationCallback
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.app.messageapp.R
import com.app.messageapp.databinding.DialogConfirmationBinding
import com.app.myapplication.utils.Constant

class ConfirmationDialogFragment(private val type: String, private val callback: com.app.messageapp.chat.ConfirmationCallback) : DialogFragment() {
	lateinit var binding: DialogConfirmationBinding
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		binding = DialogConfirmationBinding.inflate(inflater, container, false)
		return binding.root
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		dialog?.setCancelable(true)

		when(type) {
			Constant.MessageType.Block.toString() -> {
				binding.tvDialogTitle.text = getString(R.string.block_message)
				binding.tvDialogMessage.text =
					getString(R.string.you_are_sure_you_want_to_block_conversation)
				binding.btnConfirm.text = getString(R.string.block)
			}
			Constant.MessageType.UnBlock.toString() -> {
				binding.tvDialogTitle.text = getString(R.string.un_block_user)
				binding.tvDialogMessage.text =
					getString(R.string.you_are_sure_you_want_to_un_block_conversation)
				binding.btnConfirm.text = getString(R.string.unblock)
			}
			Constant.MessageType.Delete.toString() -> {
				binding.tvDialogTitle.text = getString(R.string.delete_message)
				binding.tvDialogMessage.text =
					getString(R.string.you_are_sure_you_want_to_delete_conversation)
				binding.btnConfirm.text = getString(R.string.delete)
			}
			Constant.MessageType.Archive.toString() -> {
				binding.tvDialogTitle.text = getString(R.string.archive_message)
				binding.tvDialogMessage.text = getString(R.string.you_are_sure_you_want_to_archive_1_conversation)
				binding.btnConfirm.text = getString(R.string.archive)
			}
			Constant.MessageType.UnArchive.toString() -> {
				binding.tvDialogTitle.text = getString(R.string.un_archive)
				binding.tvDialogMessage.text = getString(R.string.you_are_sure_you_want_to_un_archive_1_conversation)
				binding.btnConfirm.text = getString(R.string.done)
			}


		}
		binding.btnCancel.setOnClickListener { dialog?.dismiss() }
		
		binding.btnConfirm.setOnClickListener {
			callback.confirmData(type)
			dialog?.dismiss()
		}
	}

	override fun onStart() {
		super.onStart()
		dialog?.window?.setLayout(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
		)
	}
}