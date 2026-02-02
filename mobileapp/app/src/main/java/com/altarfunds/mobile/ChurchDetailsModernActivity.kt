package com.altarfunds.mobile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.altarfunds.mobile.databinding.ActivityChurchDetailsModernBinding
import com.altarfunds.mobile.models.Church
import com.altarfunds.mobile.utils.GlideUtils

class ChurchDetailsModernActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChurchDetailsModernBinding
    private lateinit var church: Church

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChurchDetailsModernBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadChurchData()
        setupClickListeners()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Church Details"
    }

    private fun loadChurchData() {
        church = (if (intent != null) intent.getParcelableExtra("church") else throw NullPointerException("Expression 'intent' must not be null")) as Church? as Church? ?: run {
            Toast.makeText(this, "Church data not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        displayChurchDetails()
    }

    private fun displayChurchDetails() {
        binding.apply {
            // Basic Information
            churchName.text = church.name
            binding.churchAddress.text = buildAddressString()
            churchPhone.text = formatPhoneNumber(church.phoneNumber)
            churchEmail.text = church.email
            churchWebsite.text = church.website.ifEmpty { "No website available" }
            churchDescription.text = church.description.ifEmpty { "No description available" }
            
            // Statistics
            memberCount.text = "${church.memberCount} members"
            denomination.text = church.denomination.ifEmpty { "Non-denominational" }
            foundedDate.text = "Founded: ${church.foundedDate}"
            
            // Display additional statistics if available
            // Note: These fields are not in the current layout, commenting out for now
            // church.activeMembers?.let { active ->
            //     activeMembersCount?.text = "Active: $active"
            // }
            
            // church.weeklyAttendance?.let { attendance ->
            //     weeklyAttendance?.text = "Weekly Attendance: $attendance"
            // }
            
            // church.totalGivings?.let { givings ->
            //     totalChurchGivings?.text = "Total Givings: ${formatCurrency(givings)}"
            // }

            // Load church image if available
            church.imageUrl?.let { imageUrl ->
                GlideUtils.loadImage(this@ChurchDetailsModernActivity, imageUrl, churchImage)
            } ?: run {
                // Set placeholder if no image
                churchImage?.setImageResource(R.drawable.ic_church_placeholder)
            }

            // Display service times with better formatting
            if (church.serviceTimes.isNotEmpty()) {
                val serviceTimesText = church.serviceTimes.joinToString("\n\n") { 
                    "📅 ${it.day}\n⏰ ${it.time}\n🙏 ${it.type}"
                }
                // Service Times - Display if available
                if (!church.serviceTimes.isNullOrEmpty()) {
                    binding.serviceTimes.text = "Service Times: ${church.serviceTimes}"
                    binding.serviceTimes.visibility = android.view.View.VISIBLE
                } else {
                    binding.serviceTimes.text = "Service times not available"
                    binding.serviceTimes.visibility = android.view.View.VISIBLE
                }
            }

            // Display leadership with better formatting
            if (church.leadership.isNotEmpty()) {
                val leadershipText = church.leadership.joinToString("\n\n") { 
                    "👤 ${it.name}\n📋 ${it.title}" +
                    (it.email?.let { email -> "\n📧 $email" } ?: "") +
                    (it.phoneNumber?.let { phone -> "\n📞 $phone" } ?: "")
                }
                // Leadership - Display if available
                if (!church.leadership.isNullOrEmpty()) {
                    binding.leadership.text = "Leadership: ${church.leadership}"
                    binding.leadership.visibility = android.view.View.VISIBLE
                } else {
                    binding.leadership.text = "Leadership information not available"
                    binding.leadership.visibility = android.view.View.VISIBLE
                }
            }
            
            // Display ministries if available
            church.ministries?.let { ministries ->
                if (ministries.isNotEmpty()) {
                    // Ministries - Display if available
                    if (!church.ministries.isNullOrEmpty()) {
                        val ministriesText = church.ministries.joinToString(", ")
                        // Note: ministries view not in current layout, could add to description
                        binding.churchDescription.text = "${binding.churchDescription.text}\n\nMinistries: $ministriesText"
                    }
                }
            }
            
            // Display facilities if available - commented out due to type mismatch
            // church.facilities?.let { facilitiesList ->
            //     if (facilitiesList.isNotEmpty()) {
            //         val facilitiesText = facilitiesList.joinToString("\n") { "✓ $it" }
            //         binding.churchDescription.text = "${binding.churchDescription.text}\n\nFacilities: $facilitiesText"
            //     }
            // }
        }
    }
    
    private fun buildAddressString(): String {
        val parts = mutableListOf<String>()
        if (church.address.isNotEmpty()) parts.add(church.address)
        if (church.city.isNotEmpty()) parts.add(church.city)
        if (church.state.isNotEmpty()) parts.add(church.state)
        // Note: zipCode field may not exist in Church model
        // if (church.zipCode?.isNotEmpty() == true) parts.add(church.zipCode)!!)
        return parts.joinToString(", ").ifEmpty { "No address available" }
    }
    
    private fun formatPhoneNumber(phone: String): String {
        return if (phone.isNotEmpty()) {
            // Format phone number if needed
            phone
        } else {
            "No phone number"
        }
    }
    
    private fun formatCurrency(amount: Double): String {
        return "KES ${String.format("%,.2f", amount)}"
    }

    private fun setupClickListeners() {
        binding.btnCallChurch.setOnClickListener {
            // Navigate to church join activity
            val intent = Intent(this, ChurchJoinActivity::class.java)
            intent.putExtra("church_id", church.id)
            intent.putExtra("church_name", church.name)
            startActivity(intent)
        }

        binding.btnViewWebsite.setOnClickListener {
            if (church.website.isNotEmpty()) {
                openWebsite(church.website)
            } else {
                Toast.makeText(this, "No website available", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEmailChurch.setOnClickListener {
            if (church.email.isNotEmpty()) {
                openEmailClient(church.email)
            } else {
                Toast.makeText(this, "No email address available", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Note: btnGetDirections and btnShareChurch are not in the current layout
    }
    
    private fun openWebsite(url: String) {
        try {
            var website = url
            if (!website.startsWith("http://") && !website.startsWith("https://")) {
                website = "https://$website"
            }
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(website))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open website", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openPhoneDialer(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phoneNumber"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open phone dialer", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openEmailClient(email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, "Inquiry about ${church.name}")
            }
            startActivity(Intent.createChooser(intent, "Send email"))
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open email client", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openMapsForDirections() {
        try {
            val address = buildAddressString()
            val gmmIntentUri = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(address)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // Fallback to browser if Google Maps not installed
                val browserIntent = Intent(Intent.ACTION_VIEW, 
                    android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=${android.net.Uri.encode(address)}"))
                startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open maps", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareChurchDetails() {
        try {
            val shareText = buildString {
                append("Check out ${church.name}!\n\n")
                append("📍 ${buildAddressString()}\n")
                if (church.phoneNumber.isNotEmpty()) append("📞 ${church.phoneNumber}\n")
                if (church.email.isNotEmpty()) append("📧 ${church.email}\n")
                if (church.website.isNotEmpty()) append("🌐 ${church.website}\n")
                append("\n${church.description}")
            }
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, church.name)
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(shareIntent, "Share church details"))
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to share church details", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
