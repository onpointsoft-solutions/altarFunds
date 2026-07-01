package com.sanctum.member.ui.giving

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sanctum.member.MemberApp
import com.sanctum.member.databinding.ActivityPaystackWebviewBinding
import com.sanctum.member.utils.showToast
import kotlinx.coroutines.launch

/**
 * Hosts the Paystack checkout page in an in-app WebView.
 *
 * Flow:
 *   1.  Receives the authorization_url and payment_reference via Intent extras.
 *   2.  Loads the URL in a WebView.
 *   3.  Intercepts the redirect to our callback URL (altarfunds.pythonanywhere.com/api/...).
 *   4.  Extracts the Paystack reference from the redirect URL.
 *   5.  Calls the backend verify endpoint → marks the GivingTransaction complete.
 *   6.  Returns RESULT_OK to the calling Activity.
 */
class PaystackWebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaystackWebviewBinding
    private val app by lazy { MemberApp.getInstance() }

    // ── Intent extras ─────────────────────────────────────────────────────
    companion object {
        const val EXTRA_AUTH_URL     = "auth_url"
        const val EXTRA_REFERENCE    = "payment_reference"
        const val EXTRA_TX_ID        = "transaction_id"     // our internal UUID
        const val RESULT_PAID        = "result_paid"

        /** Callback URL prefix Paystack redirects to after payment. */
        private const val CALLBACK_HOST = "altarfunds.pythonanywhere.com"

        fun newIntent(
            context:   Context,
            authUrl:   String,
            reference: String,
            txId:      String = "",
        ) = Intent(context, PaystackWebViewActivity::class.java).apply {
            putExtra(EXTRA_AUTH_URL,  authUrl)
            putExtra(EXTRA_REFERENCE, reference)
            putExtra(EXTRA_TX_ID,     txId)
        }
    }

    private var reference    = ""
    private var txId         = ""
    private var verifyDone   = false   // prevent double-verify

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaystackWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reference = intent.getStringExtra(EXTRA_REFERENCE) ?: ""
        txId      = intent.getStringExtra(EXTRA_TX_ID)     ?: ""
        val authUrl = intent.getStringExtra(EXTRA_AUTH_URL) ?: run {
            showToast("No payment URL provided")
            finish()
            return
        }

        // ── Toolbar ───────────────────────────────────────────────────────
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Complete Payment"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { confirmCancel() }

        // ── WebView setup ─────────────────────────────────────────────────
        binding.webView.apply {
            settings.javaScriptEnabled  = true
            settings.domStorageEnabled  = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort    = true

            webViewClient = PaystackWebViewClient()
            loadUrl(authUrl)
        }
    }

    override fun onBackPressed() = confirmCancel()

    override fun onSupportNavigateUp(): Boolean {
        confirmCancel()
        return true
    }

    private fun confirmCancel() {
        if (verifyDone) { finish(); return }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cancel Payment?")
            .setMessage("Are you sure you want to cancel this payment? Your transaction will remain pending.")
            .setPositiveButton("Yes, cancel") { _, _ -> finish() }
            .setNegativeButton("No, continue", null)
            .show()
    }

    // ── WebViewClient ─────────────────────────────────────────────────────

    inner class PaystackWebViewClient : WebViewClient() {

        private val progress = ProgressDialog(this@PaystackWebViewActivity).apply {
            setMessage("Loading payment page…")
            setCancelable(false)
        }

        override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
            progress.show()
            // Intercept Paystack callback redirect
            if (url.contains(CALLBACK_HOST) && !verifyDone) {
                verifyDone = true
                progress.dismiss()
                // Extract 'reference' or 'trxref' query param from the redirect URL
                val uri     = android.net.Uri.parse(url)
                val refFromUrl = uri.getQueryParameter("reference")
                    ?: uri.getQueryParameter("trxref")
                    ?: reference   // fallback to the one we sent

                verifyAndComplete(refFromUrl)
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            progress.dismiss()
        }

        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (url.contains(CALLBACK_HOST) && !verifyDone) {
                verifyDone = true
                val uri     = android.net.Uri.parse(url)
                val refFromUrl = uri.getQueryParameter("reference")
                    ?: uri.getQueryParameter("trxref")
                    ?: reference
                verifyAndComplete(refFromUrl)
                return true   // don't load the callback URL in the WebView
            }
            return false
        }

        override fun shouldOverrideUrlLoading(
            view: WebView, request: WebResourceRequest
        ): Boolean {
            val url = request.url.toString()
            if (url.contains(CALLBACK_HOST) && !verifyDone) {
                verifyDone = true
                val refFromUrl = request.url.getQueryParameter("reference")
                    ?: request.url.getQueryParameter("trxref")
                    ?: reference
                verifyAndComplete(refFromUrl)
                return true
            }
            return false
        }
    }

    // ── Verify & mark complete ─────────────────────────────────────────────

    private fun verifyAndComplete(ref: String) {
        val verifyingDialog = ProgressDialog(this).apply {
            setMessage("Verifying payment…")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch {
            try {
                val response = app.apiService.verifyPaystackPayment(ref)
                verifyingDialog.dismiss()

                if (response.isSuccessful && response.body() != null) {
                    val data   = response.body()!!
                    val status = (data.data?.get("status") as? String)?.lowercase() ?: ""

                    if (status == "success" || status == "completed") {
                        showToast("✓ Payment successful!")
                        val result = Intent().putExtra(RESULT_PAID, true)
                            .putExtra(EXTRA_REFERENCE, ref)
                            .putExtra(EXTRA_TX_ID,     txId)
                        setResult(RESULT_OK, result)
                        finish()
                    } else {
                        showToast("Payment status: $status — please try again.")
                        verifyDone = false   // allow retry
                    }
                } else {
                    showToast("Could not verify payment (${response.code()}). Try again.")
                    verifyDone = false
                }
            } catch (e: Exception) {
                verifyingDialog.dismiss()
                showToast("Network error during verification: ${e.message}")
                verifyDone = false
            }
        }
    }
}
