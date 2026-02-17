package com.altarfunds.member.ui.church

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.member.MemberApp
import com.altarfunds.member.adapters.ChurchAdapter
import com.altarfunds.member.data.mappers.toEntity
import com.altarfunds.member.data.mappers.toModel
import com.altarfunds.member.databinding.ActivityChurchSearchBinding
import com.altarfunds.member.utils.*
import kotlinx.coroutines.flow.firstOrNull
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
                resultIntent.putExtra("church_code", church.code)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                val intent = Intent(this, ChurchDetailsActivity::class.java)
                intent.putExtra("id", church.id)
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
            // First, try to load from cache
            val cachedChurches = app.database.churchDao().searchChurches(query).firstOrNull()
            if (!cachedChurches.isNullOrEmpty()) {
                val churches = cachedChurches.map { it.toModel() }
                churchAdapter.submitList(churches)
                binding.tvEmpty.gone()
                binding.rvChurches.visible()
            }
            
            // Check network availability
            if (!NetworkUtils.isNetworkAvailable(this@ChurchSearchActivity)) {
                binding.progressBar.gone()
                if (!cachedChurches.isNullOrEmpty()) {
                    showToast("ℹ Offline mode - Showing cached churches for '$query'")
                } else {
                    showToast("✗ No internet connection and no cached churches found")
                    binding.tvEmpty.visible()
                    binding.rvChurches.gone()
                }
                return@launch
            }
            
            // Fetch from network
            try {
                val response = app.apiService.getChurches(search = query, page = 1)
                
                if (response.isSuccessful && response.body() != null) {
                    val churches = response.body()!!.results
                    
                    // Cache the churches
                    if (churches.isNotEmpty()) {
                        app.database.churchDao().insertChurches(churches.map { it.toEntity() })
                    }
                    
                    churchAdapter.submitList(churches)
                    
                    if (churches.isEmpty()) {
                        binding.tvEmpty.visible()
                        binding.rvChurches.gone()
                        showToast("ℹ No churches found matching '$query'")
                    } else {
                        binding.tvEmpty.gone()
                        binding.rvChurches.visible()
                        showToast("✓ Found ${churches.size} church(es)")
                    }
                } else {
                    if (cachedChurches.isNullOrEmpty()) {
                        val errorMessage = when (response.code()) {
                            400 -> "✗ Invalid search query. Please try different keywords."
                            403 -> "✗ You don't have permission to search churches."
                            500 -> "✗ Server error. Please try again later."
                            else -> "✗ Search failed: ${response.message() ?: "Unknown error"}"
                        }
                        showToast(errorMessage)
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                e.printStackTrace()
                if (cachedChurches.isNullOrEmpty()) {
                    showToast("✗ No internet connection. Please check your network.")
                }
            } catch (e: java.net.SocketTimeoutException) {
                e.printStackTrace()
                if (cachedChurches.isNullOrEmpty()) {
                    showToast("✗ Connection timeout. Please try again.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (cachedChurches.isNullOrEmpty()) {
                    showToast("✗ Search error: ${e.message ?: "Network error"}")
                }
            } finally {
                binding.progressBar.gone()
            }
        }
    }
}
