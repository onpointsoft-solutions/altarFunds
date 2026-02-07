package com.altarfunds.member.ui.suggestions

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.R
import com.altarfunds.member.databinding.DialogCreateSuggestionBinding
import com.altarfunds.member.models.SuggestionRequest
import com.altarfunds.member.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class CreateSuggestionDialog : DialogFragment() {
    
    private var _binding: DialogCreateSuggestionBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { MemberApp.getInstance() }
    private var alertDialog: androidx.appcompat.app.AlertDialog? = null
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCreateSuggestionBinding.inflate(layoutInflater)
        
        setupCategorySpinner()
        setupListeners()
        
        alertDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle("Submit Suggestion")
            .setPositiveButton("Submit", null)
            .setNegativeButton("Cancel", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        submitSuggestion()
                    }
                }
            }
        return alertDialog!!
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }
    
    private fun setupCategorySpinner() {
        val categories = arrayOf(
            "General",
            "Worship",
            "Ministry",
            "Facilities",
            "Events",
            "Finance",
            "Other"
        )
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }
    
    private fun setupListeners() {
        // Nothing specific needed here
    }
    
    private fun submitSuggestion() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val categoryPosition = binding.spinnerCategory.selectedItemPosition
        val isAnonymous = binding.cbAnonymous.isChecked
        
        // Validation
        if (title.isEmpty()) {
            binding.tilTitle.error = "Title is required"
            return
        }
        
        if (description.isEmpty()) {
            binding.tilDescription.error = "Description is required"
            return
        }
        
        // Clear errors
        binding.tilTitle.error = null
        binding.tilDescription.error = null
        
        // Map category position to API value
        val categoryMap = mapOf(
            0 to "general",
            1 to "worship",
            2 to "ministry",
            3 to "facilities",
            4 to "events",
            5 to "finance",
            6 to "other"
        )
        
        val category = categoryMap[categoryPosition] ?: "general"
        
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val request = SuggestionRequest(
                    title = title,
                    description = description,
                    category = category,
                    isAnonymous = isAnonymous
                )
                
                val response = app.apiService.createSuggestion(request)
                
                if (response.isSuccessful) {
                    requireContext().showToast("Suggestion submitted successfully!")
                    (activity as? SuggestionsActivity)?.refreshSuggestions()
                    dismiss()
                } else {
                    requireContext().showToast("Failed to submit suggestion")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                requireContext().showToast("Network error")
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        if (show) {
            binding.progressBar.visible()
            alertDialog?.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        } else {
            binding.progressBar.gone()
            alertDialog?.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
