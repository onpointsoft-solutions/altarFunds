package com.altarfunds.mobile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.altarfunds.mobile.databinding.ActivityCreateBudgetBinding
import com.altarfunds.mobile.models.Budget
import com.altarfunds.mobile.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

class CreateBudgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateBudgetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create Budget"
    }

    private fun setupClickListeners() {
        binding.btnSaveBudget.setOnClickListener {
            saveBudget()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun saveBudget() {
        val name = binding.etBudgetName.text.toString().trim()
        val category = binding.etCategory.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().trim()
        val period = binding.spinnerPeriod.selectedItem.toString()

        if (name.isEmpty()) {
            binding.etBudgetName.error = "Budget name is required"
            return
        }

        if (category.isEmpty()) {
            binding.etCategory.error = "Category is required"
            return
        }

        if (amountStr.isEmpty()) {
            binding.etAmount.error = "Amount is required"
            return
        }

        try {
            val amount = amountStr.toDouble()
            val budget = Budget(
                id = UUID.randomUUID().toString(),
                name = name,
                category = category,
                amount = amount,
                spent = 0.0,
                remaining = amount,
                period = period,
                startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                endDate = calculateEndDate(period),
                isActive = true
            )

            // TODO: Save budget to API
            Toast.makeText(this, "Budget created successfully", Toast.LENGTH_SHORT).show()
            finish()

        } catch (e: NumberFormatException) {
            binding.etAmount.error = "Invalid amount"
        }
    }

    private fun calculateEndDate(period: String): String {
        val calendar = Calendar.getInstance()
        when (period.lowercase()) {
            "monthly" -> calendar.add(Calendar.MONTH, 1)
            "quarterly" -> calendar.add(Calendar.MONTH, 3)
            "yearly" -> calendar.add(Calendar.YEAR, 1)
            else -> calendar.add(Calendar.MONTH, 1)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
