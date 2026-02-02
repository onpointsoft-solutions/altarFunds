package com.altarfunds.mobile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.mobile.api.ApiService
import com.altarfunds.mobile.databinding.ActivityChurchSearchBinding
import com.altarfunds.mobile.models.Church
import com.altarfunds.mobile.models.ChurchSearchResult
import com.altarfunds.mobile.ui.adapters.ChurchSearchAdapter
import kotlinx.coroutines.launch

class ChurchSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChurchSearchBinding
    private lateinit var churchAdapter: ChurchSearchAdapter
    private var churches: List<ChurchSearchResult> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChurchSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        loadInitialData()
    }

    private fun setupUI() {
        // Set toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Find a Church"

        // Setup RecyclerView
        churchAdapter = ChurchSearchAdapter(emptyList()) { churchSearchResult ->
            navigateToChurchDetails(churchSearchResult)
        }
        binding.recyclerChurches.apply {
            layoutManager = LinearLayoutManager(this@ChurchSearchActivity)
            adapter = churchAdapter
        }
    }

    private fun setupListeners() {
        // Search text watcher
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchChurches(s?.toString() ?: "")
            }
        })

        // Location button
        binding.btnUseCurrentLocation.setOnClickListener {
            getCurrentLocationAndSearch()
        }

        // Filter button
        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        // Search button
        binding.btnSearch.setOnClickListener {
            performSearch()
        }

        // Join by code button
        binding.btnJoinByCode.setOnClickListener {
            showJoinByCodeDialog()
        }
    }

    private fun loadInitialData() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // Load all churches
                val response = ApiService.getApiInterface().searchChurches(
                    query = "",
                    location = null
                )
                
                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    if (searchResponse?.success == true) {
                        churches = searchResponse.churches
                        updateChurchList(churches)
                        updateSearchResults(churches.size)
                    } else {
                        showError(searchResponse?.message ?: "Failed to load churches")
                    }
                } else {
                    showError("Failed to load churches")
                }
                
                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showError("Error loading churches: ${e.message}")
            }
        }
    }

    private fun searchChurches(query: String) {
        if (query.isEmpty()) {
            updateChurchList(churches)
            return
        }

        val filteredChurches = churches.filter { church ->
            church.name.contains(query, ignoreCase = true) ||
            church.location.contains(query, ignoreCase = true)
        }
        
        updateChurchList(filteredChurches)
        updateSearchResults(filteredChurches.size)
    }

    private fun getCurrentLocationAndSearch() {
        // TODO: Implement location services
        Toast.makeText(this, "Location feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun performSearch() {
        val query = binding.editSearch.text.toString().trim()
        val location = binding.editLocation.text.toString().trim()
        
        if (query.isEmpty() && location.isEmpty()) {
            Toast.makeText(this, "Please enter a search term or location", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val response = ApiService.getApiInterface().searchChurches(
                    query = query,
                    location = location.ifEmpty { null }
                )
                
                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    if (searchResponse?.success == true) {
                        val searchResults = searchResponse.churches
                        updateChurchList(searchResults)
                        updateSearchResults(searchResults.size)
                        
                        if (searchResults.isEmpty()) {
                            showNoResults()
                        }
                    } else {
                        showError(searchResponse?.message ?: "Search failed")
                    }
                } else {
                    showError("Search failed")
                }
                
                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showError("Error searching: ${e.message}")
            }
        }
    }

    private fun showFilterDialog() {
        // TODO: Implement filter dialog
        Toast.makeText(this, "Filter feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showJoinByCodeDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(android.R.layout.simple_list_item_1, null)
        
        // Create a custom view with EditText for church code
        val editText = android.widget.EditText(this)
        editText.hint = "Enter Church Code"
        editText.inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        
        builder.setTitle("Join Church by Code")
        builder.setMessage("Enter the church code provided by your church administrator")
        builder.setView(editText)
        
        builder.setPositiveButton("Join") { dialog, _ ->
            val churchCode = editText.text.toString().trim().uppercase()
            if (churchCode.isEmpty()) {
                Toast.makeText(this, "Please enter a church code", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            joinChurchByCode(churchCode)
            dialog.dismiss()
        }
        
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
    }
    
    private fun joinChurchByCode(churchCode: String) {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // First, search for church by code
                val response = ApiService.getApiInterface().searchChurches(
                    query = churchCode,
                    location = null
                )
                
                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    if (searchResponse?.success == true) {
                        val churches = searchResponse.churches
                        val church = churches.find { it.church_code.equals(churchCode, ignoreCase = true) }
                        
                        if (church != null) {
                            // Found church by code, now join it
                            joinChurch(church.id.toInt(), church.name)
                        } else {
                            // Try to find by name if code not found
                            val churchByName = churches.find { it.name.contains(churchCode, ignoreCase = true) }
                            if (churchByName != null) {
                                joinChurch(churchByName.id.toInt(), churchByName.name)
                            } else {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@ChurchSearchActivity, "Church not found with code: $churchCode", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@ChurchSearchActivity, searchResponse?.message ?: "Failed to search for church", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@ChurchSearchActivity, "Failed to search for church", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@ChurchSearchActivity, "Error joining church: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun joinChurch(churchId: Int, churchName: String) {
        lifecycleScope.launch {
            try {
                val response = ApiService.getApiInterface().joinChurchBackend(churchId)
                
                if (response.isSuccessful) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@ChurchSearchActivity, "Successfully joined $churchName!", Toast.LENGTH_LONG).show()
                    
                    // Update user profile to reflect church membership
                    updateProfileWithChurch(churchId, churchName)
                    
                    // Navigate to main dashboard
                    val intent = Intent(this@ChurchSearchActivity, MemberDashboardModernActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    binding.progressBar.visibility = View.GONE
                    val errorMessage = response.body()?.message ?: "Failed to join church"
                    Toast.makeText(this@ChurchSearchActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@ChurchSearchActivity, "Error joining church: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateProfileWithChurch(churchId: Int, churchName: String) {
        lifecycleScope.launch {
            try {
                // The backend should automatically update the user's church membership
                // We can refresh the user profile to ensure the UI is updated
                ApiService.getUserProfile()
            } catch (e: Exception) {
                // Silently fail - the main join operation was successful
            }
        }
    }

    private fun updateChurchList(churchList: List<ChurchSearchResult>) {
        churchAdapter.updateData(churchList)
        
        if (churchList.isEmpty()) {
            binding.recyclerChurches.visibility = View.GONE
            binding.textNoResults.visibility = View.VISIBLE
        } else {
            binding.recyclerChurches.visibility = View.VISIBLE
            binding.textNoResults.visibility = View.GONE
        }
    }

    private fun updateSearchResults(count: Int) {
        binding.textResultsCount.text = "$count churches found"
    }

    private fun showNoResults() {
        binding.textNoResult.text = "No churches found. Try adjusting your search criteria."
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToChurchDetails(church: ChurchSearchResult) {
        val intent = Intent(this, ChurchDetailsModernActivity::class.java).apply {
            putExtra("church_id", church.id)
            putExtra("church_name", church.name)
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
