package com.fixupxer.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.fixupxer.R
import com.fixupxer.utils.Constants
import timber.log.Timber

/**
 * Base activity with common functionality for all activities
 */
abstract class BaseActivity : AppCompatActivity() {
    protected var windowInsetsListener: OnApplyWindowInsetsListener? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure window insets and status bar
        setupWindowInsets()
    }
    
    private fun setupWindowInsets() {
        // Enable edge-to-edge display
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
        // Set status bar appearance
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true // dark icons on light background
        
        // Apply window insets dynamically
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView.rootView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Find the AppBarLayout and apply top padding dynamically
            val appBarLayout = view.findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
            if (appBarLayout != null) {
                // Use the actual status bar height instead of fixed padding
                appBarLayout.setPadding(0, insets.top, 0, 0)
            
                // Find the title/header view inside AppBarLayout and adjust its padding
                val titleView = appBarLayout.findViewById<TextView>(R.id.titleTextView) 
                    ?: appBarLayout.findViewById<FrameLayout>(R.id.titleFrameLayout)
                    
                titleView?.setPadding(
                    titleView.paddingLeft,
                    resources.getDimensionPixelSize(R.dimen.title_padding_top),
                    titleView.paddingRight,
                    titleView.paddingBottom
                )
            }
            
            // Apply bottom padding to the main content if needed
            val scrollView = view.findViewById<androidx.core.widget.NestedScrollView>(R.id.mainScrollView)
            scrollView?.setPadding(
                scrollView.paddingLeft,
                scrollView.paddingTop,
                scrollView.paddingRight,
                insets.bottom
            )
            
            WindowInsetsCompat.CONSUMED
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up insets listener to prevent memory leaks
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && windowInsetsListener != null) {
            ViewCompat.setOnApplyWindowInsetsListener(window.decorView, null)
            windowInsetsListener = null
        }
    }
    
    /**
     * Show about dialog
     */
    protected fun showAboutDialog() {
        val message = getString(R.string.about_text)
        val spannedMessage = android.text.SpannableString(message)
        
        // Find website URL and make it clickable
        val websiteIndex = message.indexOf(Constants.WEBSITE_URL)
        if (websiteIndex != -1) {
            spannedMessage.setSpan(
                android.text.style.URLSpan(Constants.WEBSITE_URL),
                websiteIndex,
                websiteIndex + Constants.WEBSITE_URL.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        // Find GitHub URL and make it clickable
        val githubIndex = message.indexOf(Constants.GITHUB_URL)
        if (githubIndex != -1) {
            spannedMessage.setSpan(
                android.text.style.URLSpan(Constants.GITHUB_URL),
                githubIndex,
                githubIndex + Constants.GITHUB_URL.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.about_dialog_title)
            .setPositiveButton(R.string.about_dialog_positive, null)
            .create()
            
        // Use a TextView to make links clickable
        val textView = TextView(this)
        textView.text = spannedMessage
        textView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.dialog_text_padding_horizontal)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.dialog_text_padding_vertical)
        textView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        textView.linksClickable = true
        
        alertDialog.setView(textView)
        alertDialog.show()
    }
    
    /**
     * Show disclaimer dialog
     */
    protected fun showDisclaimerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_disclaimer, null)
        
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.disclaimer_dialog_title)
            .setView(dialogView)
            .setCancelable(false) // User must scroll and click button
            .create()
        
        // Find views
        val scrollView = dialogView.findViewById<android.widget.ScrollView>(R.id.scrollViewDisclaimer)
        val textViewContent = dialogView.findViewById<TextView>(R.id.textViewDisclaimerContent)
        val buttonAgreeAndClose = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonAgreeAndClose)
        
        // Set HTML content
        textViewContent.text = HtmlCompat.fromHtml(getString(R.string.disclaimer_text), HtmlCompat.FROM_HTML_MODE_LEGACY)
        
        // Set up scroll listener to show button when scrolled to bottom
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val view = scrollView.getChildAt(scrollView.childCount - 1)
            val diff = (view.bottom - (scrollView.height + scrollView.scrollY))
            
            // If diff is zero or less, we've reached the bottom
            if (diff <= 0) {
                buttonAgreeAndClose.visibility = android.view.View.VISIBLE
            }
        }
        
        // Also check immediately in case content is already fully visible
        scrollView.post {
            val view = scrollView.getChildAt(0)
            if (view.height <= scrollView.height) {
                // Content fits without scrolling
                buttonAgreeAndClose.visibility = android.view.View.VISIBLE
            }
        }
        
        // Set up button click
        buttonAgreeAndClose.setOnClickListener {
            alertDialog.dismiss()
        }
        
        alertDialog.show()
    }
    
    /**
     * Report bug via email
     */
    protected fun reportBug() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${getString(R.string.bug_report_email)}")
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.bug_report_subject))
            }
            
            startActivity(Intent.createChooser(intent, getString(R.string.send_email)))
        } catch (e: Exception) {
            Timber.e(e, "Error launching email app")
            Toast.makeText(this, getString(R.string.error_email_app), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Show donate dialog
     */
    protected fun showDonateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_donate, null)
        
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // Find views in the custom layout
        val buttonDonate = dialogView.findViewById<android.widget.Button>(R.id.buttonDonate)
        val buttonMaybeLater = dialogView.findViewById<android.widget.Button>(R.id.buttonMaybeLater)
        val textViewMonero = dialogView.findViewById<TextView>(R.id.textViewMonero)
        
        // Set up donate button click
        buttonDonate.setOnClickListener {
                try {
                    // Open donation link
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.DONATION_URL))
                    startActivity(intent)
                alertDialog.dismiss()
                } catch (e: Exception) {
                    Timber.e(e, "Error opening donation URL")
                    Toast.makeText(this, getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
                }
            }
        
        // Set up maybe later button click
        buttonMaybeLater.setOnClickListener {
            alertDialog.dismiss()
        }
        
        // Set up Monero text click to copy address
        textViewMonero.setOnClickListener {
            val clipboardManager = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            if (clipboardManager != null) {
                val clipData = android.content.ClipData.newPlainText(getString(R.string.clipboard_label_monero_address), Constants.MONERO_ADDRESS)
                clipboardManager.setPrimaryClip(clipData)
                // Only show toast on Android < 12 (API 31) to avoid duplicate with system notification
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    Toast.makeText(this, getString(R.string.monero_address_copied), Toast.LENGTH_SHORT).show()
                } else {
                    // Android 12+ shows its own clipboard notification
                    Timber.d("Monero address copied to clipboard (Android 12+)")
                }
            } else {
                Timber.e("ClipboardManager not available")
                Toast.makeText(this, getString(R.string.error_processing_url), Toast.LENGTH_SHORT).show()
            }
        }
        
        alertDialog.show()
    }
    
    /**
     * Set app title in header
     */
    protected fun setAppTitle(titleTextView: TextView?) {
        titleTextView?.let {
            it.text = getString(R.string.app_title)
            Timber.d("App title header set to: ${getString(R.string.app_title)}")
        }
    }
} 