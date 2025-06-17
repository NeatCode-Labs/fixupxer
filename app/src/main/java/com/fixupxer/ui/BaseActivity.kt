package com.fixupxer.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.UrlProcessor
import com.fixupxer.utils.Constants
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Base activity with common functionality for MainActivity and ShareActivity
 */
abstract class BaseActivity : AppCompatActivity() {
    protected lateinit var preferencesManager: PreferencesManager
    protected lateinit var urlProcessor: UrlProcessor
    protected var windowInsetsListener: OnApplyWindowInsetsListener? = null
    
    // Executor for background tasks
    protected val backgroundExecutor: Executor = Executors.newSingleThreadExecutor()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize preferences manager and URL processor
        preferencesManager = PreferencesManager(this)
        urlProcessor = UrlProcessor()
        
        // Configure window insets and status bar
        setupWindowInsets()
    }
    
    private fun setupWindowInsets() {
        // Use the most modern approach to handle system insets/windows
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Set status bar icons to be dark (for our light background)
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = true // true = dark icons
            
            // Status bar color is set in the theme
        } else {
            // For older versions, use the older method but avoid deprecated APIs
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            // Use modern approach to handle status bar
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Status bar color is set in the theme
            
            // Additional flag to prevent content from being inset
            windowInsetsListener = OnApplyWindowInsetsListener { view, insets ->
                view.setPadding(0, 0, 0, 0)
                insets
            }
            ViewCompat.setOnApplyWindowInsetsListener(window.decorView, windowInsetsListener)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up insets listener to prevent memory leaks
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && windowInsetsListener != null) {
            ViewCompat.setOnApplyWindowInsetsListener(window.decorView, null)
            windowInsetsListener = null
        }
        
        // Shutdown the background executor to prevent thread leaks
        if (backgroundExecutor is java.util.concurrent.ExecutorService) {
            backgroundExecutor.shutdown()
        }
    }
    
    /**
     * Process URL in background thread
     */
    protected fun processUrlInBackground(url: String, callback: (String) -> Unit) {
        if (url.isEmpty()) {
            callback(url)
            return
        }
        
        backgroundExecutor.execute {
            try {
                val isInstagramUrl = urlProcessor.isInstagramUrl(url)
                val processedUrl = if (isInstagramUrl) {
                    // For Instagram URLs, use the Instagram conversion preference
                    urlProcessor.processUrl(
                        url,
                        true, // Always clean tracking
                        preferencesManager.isConvertInstagramEnabled() // Use Instagram conversion preference
                    )
                } else {
                    // For other URLs, use the standard process with user preferences
                    urlProcessor.processUrl(
                        url,
                        preferencesManager.isCleanTrackingEnabled(),
                        preferencesManager.isConvertTwitterEnabled()
                    )
                }
                
                // Return result on UI thread
                runOnUiThread {
                    callback(processedUrl)
                }
            } catch (e: Exception) {
                Log.e(Constants.LOG_TAG, "Error processing URL: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.error_processing_url), Toast.LENGTH_SHORT).show()
                    callback(url) // Return original URL on error
                }
            }
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
        textView.setPadding(50, 30, 50, 30)
        textView.linksClickable = true
        
        alertDialog.setView(textView)
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
            Log.e(Constants.LOG_TAG, "Error launching email app: ${e.message}")
            Toast.makeText(this, getString(R.string.error_email_app), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Show donate dialog
     */
    protected fun showDonateDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.donate_dialog_title)
            .setMessage(R.string.donate_text)
            .setPositiveButton(R.string.donate_dialog_positive) { _, _ ->
                try {
                    // Open donation link
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.DONATION_URL))
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(Constants.LOG_TAG, "Error opening donation URL: ${e.message}")
                    Toast.makeText(this, getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.donate_dialog_negative, null)
            .show()
    }
    
    /**
     * Set app title in header
     */
    protected fun setAppTitle(titleTextView: TextView?) {
        titleTextView?.let {
            it.text = getString(R.string.app_title)
            Log.d(Constants.LOG_TAG, "App title header set to: ${getString(R.string.app_title)}")
        }
    }
} 