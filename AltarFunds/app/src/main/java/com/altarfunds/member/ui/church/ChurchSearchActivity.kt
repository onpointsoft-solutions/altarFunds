package com.altarfunds.member.ui.church

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.member.MemberApp
import com.altarfunds.member.adapters.ChurchAdapter
import com.altarfunds.member.databinding.ActivityChurchSearchBinding
import com.altarfunds.member.utils.*
import kotlinx.coroutines.launch

class ChurchSearchActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChurchSearchBinding
    private val app by lazy { MemberApp.getInstance() }
    private lateinit var churchAdapter: ChurchAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChurchSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupSearchView()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Search Churches"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupRecyclerView() {
        churchAdapter = ChurchAdapter { church ->
            if (intent.getBooleanExtra("SELECT_MODE", false)) {
                val resultIntent = Intent()
                resultIntent.putExtra("CHURCH_CODE", church.code)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                val intent = Intent(this, ChurchDetailsActivity::class.java)
                intent.putExtra("CHURCH_ID", church.id)
                startActivity(intent)
            }
        }
        
        binding.rvChurches.apply {
            layoutManager = LinearLayoutManager(this@ChurchSearchActivity)
            adapter = churchAdapter
        }
    }
    
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchChurches(it) }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    churchAdapter.submitList(emptyList())
                    binding.tvEmpty.visible()
                    binding.rvChurches.gone()
                }
                return true
            }
        })
    }
    
    private fun searchChurches(query: String) {
        binding.progressBar.visible()
        
        lifecycleScope.launch {
            try {
                val response = app.apiService.getChurches(search = query, page = 1)
                
                if (response.isSuccessful && response.body() != null) {
                    val churches = response.body()!!.results
                    churchAdapter.submitList(churches)
                    
                    if (churches.isEmpty()) {
                        binding.tvEmpty.visible()
                        binding.rvChurches.gone()
                    } else {
                        binding.tvEmpty.gone()
                        binding.rvChurches.visible()
                    }
                } else {
                    showToast("Failed to search churches")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Network error")
            } finally {
                binding.progressBar.gone()
            }
        }
    }
}
