package com.altarfunds.member.ui.suggestions

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.member.MemberApp
import com.altarfunds.member.R
import com.altarfunds.member.adapters.SuggestionAdapter
import com.altarfunds.member.databinding.ActivitySuggestionsBinding
import com.altarfunds.member.utils.*
import kotlinx.coroutines.launch

class SuggestionsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySuggestionsBinding
    private val app by lazy { MemberApp.getInstance() }
    private lateinit var suggestionAdapter: SuggestionAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuggestionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadSuggestions()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Suggestions"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupRecyclerView() {
        suggestionAdapter = SuggestionAdapter()
        binding.rvSuggestions.apply {
            layoutManager = LinearLayoutManager(this@SuggestionsActivity)
            adapter = suggestionAdapter
        }
    }
    
    private fun setupListeners() {
        binding.fabNewSuggestion.setOnClickListener {
            CreateSuggestionDialog().show(supportFragmentManager, "create_suggestion")
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            loadSuggestions()
        }
    }
    
    private fun loadSuggestions() {
        binding.swipeRefresh.isRefreshing = true
        
        lifecycleScope.launch {
            try {
                val response = app.apiService.getSuggestions()
                
                if (response.isSuccessful && response.body() != null) {
                    val suggestions = response.body()!!
                    
                    if (suggestions.isEmpty()) {
                        binding.rvSuggestions.gone()
                        binding.tvEmpty.visible()
                    } else {
                        binding.rvSuggestions.visible()
                        binding.tvEmpty.gone()
                        suggestionAdapter.submitList(suggestions)
                    }
                } else {
                    showToast("Failed to load suggestions")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Network error")
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    fun refreshSuggestions() {
        loadSuggestions()
    }
}
