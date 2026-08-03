package io.beldex.bchat.groups

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.beldex.bchat.databinding.FragmentClosedGroupEditBottomSheetBinding

class ClosedGroupEditingOptionsBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentClosedGroupEditBottomSheetBinding
    var onRemoveTapped: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentClosedGroupEditBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.removeFromGroup.setOnClickListener { onRemoveTapped?.invoke() }
    }

    override fun onStart() {
        super.onStart()
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) {
            dialog?.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.55).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        dialog?.let { d ->
            val sheet = d as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            sheet.behavior.skipCollapsed = true
        }
    }

    override fun onStop() {
        super.onStop()
        if (isAdded) {
            dismissAllowingStateLoss()
        }
    }
}