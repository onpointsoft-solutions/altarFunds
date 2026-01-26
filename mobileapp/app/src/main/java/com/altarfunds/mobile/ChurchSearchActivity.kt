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
                // Load nearby churches (default search)
                val response = ApiService.getApiInterface().searchChurches(
                    query = "",
                    location = null
                )
                
                if (response.isSuccessful) {
                    churches = (response.body()?.churches ?: emptyList()) as List<ChurchSearchResult>
                    updateChurchList(churches)
                    updateSearchResults(churches.size)
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
                    val searchResults = response.body()?.churches ?: emptyList()
                    updateChurchList(searchResults)
                    updateSearchResults(searchResults.size)
                    
                    if (searchResults.isEmpty()) {
                        showNoResults()
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
        // TODO: Implement join by code dialog
        Toast.makeText(this, "Join by code feature coming soon", Toast.LENGTH_SHORT).show()
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
