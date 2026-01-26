package com.altarfunds.mobile

import android.R
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.altarfunds.mobile.api.ApiService
import com.altarfunds.mobile.databinding.ActivityChurchJoinBinding
import com.altarfunds.mobile.models.ChurchJoinRequest
import com.altarfunds.mobile.models.EmergencyContact
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChurchJoinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChurchJoinBinding
    private var churchId: String = ""
    private var churchName: String = ""
    private var selectedSkills: MutableSet<String> = mutableSetOf()
    private var selectedInterests: MutableSet<String> = mutableSetOf()

    companion object {
        const val EXTRA_CHURCH_ID = "church_id"
        const val EXTRA_CHURCH_NAME = "church_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChurchJoinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        churchId = intent.getStringExtra(EXTRA_CHURCH_ID) ?: ""
        churchName = intent.getStringExtra(EXTRA_CHURCH_NAME) ?: ""

        setupUI()
        setupListeners()
        loadChurchInfo()
    }

    private fun setupUI() {
        // Set toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Join $churchName"

        // Setup membership type spinner
        val membershipTypes = arrayOf("Regular Member", "Associate Member", "Visitor")
        val membershipSpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, membershipTypes)
        membershipSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        //binding.spinnerMembershipType.adapter = membershipSpinnerAdapter

        // Setup skill chips
        setupSkillChips()
        
        // Setup interest chips
        setupInterestChips()
    }

    private fun setupSkillChips() {
        val skills = arrayOf(
            "Music", "Teaching", "Youth Ministry", "Children Ministry", 
            "Administration", "IT/Technology", "Photography", "Videography",
            "Hospitality", "Prayer Ministry", "Outreach", "Maintenance"
        )

        binding.chipGroupSkills.removeAllViews()
        skills.forEach { skill ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = skill
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedSkills.add(skill)
                    } else {
                        selectedSkills.remove(skill)
                    }
                }
            }
            binding.chipGroupSkills.addView(chip)
        }
    }

    private fun setupInterestChips() {
        val interests = arrayOf(
            "Bible Study", "Prayer Groups", "Men's Ministry", "Women's Ministry",
            "Family Ministry", "Singles Ministry", "Senior Ministry", "College Ministry",
            "Missions", "Community Service", "Social Justice", "Environmental Stewardship"
        )

        binding.chipGroupInterests.removeAllViews()
        interests.forEach { interest ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = interest
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedInterests.add(interest)
                    } else {
                        selectedInterests.remove(interest)
                    }
                }
            }
            binding.chipGroupInterests.addView(chip)
        }
    }

    private fun setupListeners() {
        // Previous church checkbox
        binding.checkboxPreviousChurch.setOnCheckedChangeListener { _, isChecked ->
            binding.editPreviousChurch.isEnabled = isChecked
            binding.editPreviousChurch.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Terms and conditions
        binding.checkboxTerms.setOnCheckedChangeListener { _, isChecked ->
            validateForm()
        }

        // Background check
        binding.checkboxBackgroundCheck.setOnCheckedChangeListener { _, isChecked ->
            validateForm()
        }

        // Submit button
        binding.btnSubmit.setOnClickListener {
            submitApplication()
        }

        // Clear button
        binding.btnClear.setOnClickListener {
            clearForm()
        }
    }

    private fun loadChurchInfo() {
        binding.textChurchName.text = churchName
        binding.textApplicationDate.text = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            .format(Date())
    }

    private fun validateForm(): Boolean {
        val isValid = binding.editName.text.toString().trim().isNotEmpty() &&
                binding.editEmail.text.toString().trim().isNotEmpty() &&
                binding.editPhone.text.toString().trim().isNotEmpty() &&
                binding.editAddress.text.toString().trim().isNotEmpty() &&
                binding.editReason.text.toString().trim().isNotEmpty() &&
                binding.editEmergencyName.text.toString().trim().isNotEmpty() &&
                binding.editEmergencyPhone.text.toString().trim().isNotEmpty() &&
                binding.checkboxTerms.isChecked &&
                binding.checkboxBackgroundCheck.isChecked

        binding.btnSubmit.isEnabled = isValid
        return isValid
    }

    private fun submitApplication() {
        if (!validateForm()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        val joinRequest = ChurchJoinRequest(
            church_name = churchName,
            church_code = churchId,
            user_id = (application as AltarFundsApp).preferencesManager.userId.toString(),
            previousChurch = if (binding.checkboxPreviousChurch.isChecked) 
                binding.editPreviousChurch.text.toString().trim() else null,
            reason = binding.editReason.text.toString().trim(),
            skills = selectedSkills.toList()
        )

        lifecycleScope.launch {
            try {
                val response = ApiService.getApiInterface().joinChurch(joinRequest)
                
                if (response.isSuccessful) {
                    val joinResponse = response.body()
                    if (joinResponse?.success == true) {
                        showSuccessDialog(joinResponse)
                    } else {
                        showError(joinResponse?.message ?: "Application failed")
                    }
                } else {
                    showError("Failed to submit application")
                }
            } catch (e: Exception) {
                showError("Error submitting application: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmit.isEnabled = true
            }
        }
    }

    private fun showSuccessDialog(response: com.altarfunds.mobile.models.ChurchJoinResponse) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Application Submitted!")
            .setMessage("Your membership application has been submitted successfully.\n\n" +
                    "Status: ${response.status}\n" +
                    "Application ID: ${response.applicationId}\n\n" +
                    "Next Steps:\n${response.nextSteps.joinToString("\n")}")
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun clearForm() {
        binding.editName.text?.clear()
        binding.editEmail.text?.clear()
        binding.editPhone.text?.clear()
        binding.editAddress.text?.clear()
        binding.editReason.text?.clear()
        binding.editPreviousChurch.text?.clear()
        binding.editEmergencyName.text?.clear()
        binding.editEmergencyRelationship.text?.clear()
        binding.editEmergencyPhone.text?.clear()
        binding.editEmergencyEmail.text?.clear()
        
        binding.checkboxPreviousChurch.isChecked = false
        binding.checkboxTerms.isChecked = false
        binding.checkboxBackgroundCheck.isChecked = false
        
        // Clear chip selections
        binding.chipGroupSkills.clearCheck()
        binding.chipGroupInterests.clearCheck()
        selectedSkills.clear()
        selectedInterests.clear()
        
        validateForm()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
