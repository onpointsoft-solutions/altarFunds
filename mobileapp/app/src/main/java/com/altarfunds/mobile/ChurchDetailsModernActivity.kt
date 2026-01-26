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
            churchName.text = church.name
            churchAddress.text = "${church.address}, ${church.city}, ${church.state}"
            churchPhone.text = church.phoneNumber
            churchEmail.text = church.email
            churchWebsite.text = church.website
            churchDescription.text = church.description
            memberCount.text = "${church.memberCount} members"
            denomination.text = church.denomination
            foundedDate.text = church.foundedDate

            // Load church image if available
            church.imageUrl?.let { imageUrl ->
                GlideUtils.loadImage(this@ChurchDetailsModernActivity, imageUrl, churchImage)
            }

            // Display service times
            val serviceTimesText = church.serviceTimes.joinToString("\n") { 
                "${it.day}: ${it.time} (${it.type})" 
            }
            serviceTimes.text = serviceTimesText

            // Display leadership
            val leadershipText = church.leadership.joinToString("\n") { 
                "${it.name} - ${it.title}"
            }
            leadership.text = leadershipText
        }
    }

    private fun setupClickListeners() {
        binding.btnJoinChurch.setOnClickListener {
            // Navigate to church join activity
            val intent = Intent(this, ChurchJoinActivity::class.java)
            intent.putExtra("church_id", church.id)
            intent.putExtra("church_name", church.name)
            startActivity(intent)
        }

        binding.btnViewWebsite.setOnClickListener {
            if (church.website.isNotEmpty()) {
                // TODO: Open website in browser
                Toast.makeText(this, "Opening website: ${church.website}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCallChurch.setOnClickListener {
            if (church.phoneNumber.isNotEmpty()) {
                // TODO: Open phone dialer
                Toast.makeText(this, "Calling: ${church.phoneNumber}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEmailChurch.setOnClickListener {
            if (church.email.isNotEmpty()) {
                // TODO: Open email client
                Toast.makeText(this, "Emailing: ${church.email}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
