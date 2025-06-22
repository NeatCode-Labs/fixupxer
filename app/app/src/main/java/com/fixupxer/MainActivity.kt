package com.fixupxer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.fixupxer.utils.Constants
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private lateinit var editTextUrl: EditText
    private lateinit var buttonProcessUrl: MaterialButton
    private lateinit var textViewProcessedUrl: TextView
    private lateinit var buttonShare: MaterialButton
    private lateinit var buttonOpen: MaterialButton
    private lateinit var buttonCopy: MaterialButton
    private lateinit var textViewAbout: TextView
    private lateinit var textViewReportBug: TextView
    private lateinit var buttonDonate: MaterialButton
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var urlProcessor: UrlProcessor
    private var windowInsetsListener: OnApplyWindowInsetsListener? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(Constants.LOG_TAG, "MainActivity onCreate started")
        
        // Set content view first before any window manipulation
        setContentView(R.layout.activity_main)
        
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
        
        // Initialize preferences manager and URL processor
        preferencesManager = PreferencesManager(this)
        urlProcessor = UrlProcessor()
        
        // Set default preferences to always on
        preferencesManager.setCleanTrackingEnabled(true)
        preferencesManager.setConvertTwitterEnabled(true)
        
        // Initialize UI components
        initializeViews()
        setupListeners()
        
        Log.d(Constants.LOG_TAG, "MainActivity onCreate completed")
    }
    
    override fun onResume() {
        super.onResume()
        // Clear URL input field and processed URL text when app resumes
        editTextUrl.setText("")
        textViewProcessedUrl.text = ""
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up insets listener to prevent memory leaks
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && windowInsetsListener != null) {
            ViewCompat.setOnApplyWindowInsetsListener(window.decorView, null)
            windowInsetsListener = null
        }
    }
    
    private fun initializeViews() {
        // Find views
        editTextUrl = findViewById(R.id.editTextUrl)
        buttonProcessUrl = findViewById(R.id.buttonProcessUrl)
        textViewProcessedUrl = findViewById(R.id.textViewProcessedUrl)
        buttonShare = findViewById(R.id.buttonShare)
        buttonOpen = findViewById(R.id.buttonOpen)
        buttonCopy = findViewById(R.id.buttonCopy)
        textViewAbout = findViewById(R.id.textViewAbout)
        textViewReportBug = findViewById(R.id.textViewReportBug)
        buttonDonate = findViewById(R.id.buttonDonate)
        
        // Add content descriptions for accessibility
        buttonProcessUrl.contentDescription = getString(R.string.process_url_content_desc)
        buttonShare.contentDescription = getString(R.string.share_content_desc)
        buttonOpen.contentDescription = getString(R.string.open_content_desc)
        buttonCopy.contentDescription = getString(R.string.copy_content_desc)
        textViewAbout.contentDescription = getString(R.string.about_content_desc)
        textViewReportBug.contentDescription = getString(R.string.report_bug_content_desc)
        buttonDonate.contentDescription = getString(R.string.donate_content_desc)
        
        // Set title in the TextView if it exists
        findViewById<TextView>(R.id.appTitleHeader)?.let {
            it.text = getString(R.string.app_title)
            Log.d(Constants.LOG_TAG, "App title header set to: ${getString(R.string.app_title)}")
        }
    }
    
    private fun setupListeners() {
        // Process URL button
        buttonProcessUrl.setOnClickListener {
            val inputUrl = editTextUrl.text.toString().trim()
            if (inputUrl.isNotEmpty()) {
                try {
                    val processedUrl = processUrl(inputUrl)
                    textViewProcessedUrl.text = processedUrl
                } catch (e: Exception) {
                    Log.e(Constants.LOG_TAG, "Error processing URL: ${e.message}")
                    Toast.makeText(this, getString(R.string.error_processing_url), Toast.LENGTH_SHORT).show()
                    textViewProcessedUrl.text = inputUrl
                }
            } else {
                textViewProcessedUrl.text = getString(R.string.no_url)
            }
        }
        
        // Share button
        buttonShare.setOnClickListener {
            val processedUrl = textViewProcessedUrl.text.toString()
            if (processedUrl.isNotEmpty() && processedUrl != getString(R.string.no_url)) {
                try {
                    // Check if URL already contains kkinstagram before processing
                    val urlForSharing = if (processedUrl.contains(Constants.KKINSTAGRAM_DOMAIN, ignoreCase = true)) {
                        // Already converted, don't process again
                        Log.d(Constants.LOG_TAG, "URL already contains kkinstagram, no need to convert: $processedUrl")
                        processedUrl
                    } else {
                    // Use processUrlForSharing to convert URLs for better embedding
                        val converted = urlProcessor.processUrlForSharing(processedUrl)
                        Log.d(Constants.LOG_TAG, "URL converted for sharing: $converted")
                        converted
                    }
                    
                    Log.d(Constants.LOG_TAG, "Final sharing URL: $urlForSharing")
                    
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, urlForSharing)
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
                } catch (e: Exception) {
                    Log.e(Constants.LOG_TAG, "Error sharing URL: ${e.message}")
                    Toast.makeText(this, getString(R.string.error_sharing_url), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.no_url_to_share), Toast.LENGTH_SHORT).show()
            }
        }
        
        // Open button
        buttonOpen.setOnClickListener {
            val processedUrl = textViewProcessedUrl.text.toString()
            if (processedUrl.isNotEmpty() && processedUrl != getString(R.string.no_url)) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, processedUrl.toUri())
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(Constants.LOG_TAG, "Error opening URL: ${e.message}")
                    Toast.makeText(this, getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.no_url_to_open), Toast.LENGTH_SHORT).show()
            }
        }
        
        // Copy button
        buttonCopy.setOnClickListener {
            val processedUrl = textViewProcessedUrl.text.toString()
            if (processedUrl.isNotEmpty() && processedUrl != getString(R.string.no_url)) {
                val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("URL", processedUrl)
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(this, getString(R.string.url_copied), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.no_url_to_copy), Toast.LENGTH_SHORT).show()
            }
        }
        
        // About text link
        textViewAbout.setOnClickListener {
            showAboutDialog()
        }
        
        // Report Bug text link
        textViewReportBug.setOnClickListener {
            reportBug()
        }
        
        // Donate button
        buttonDonate.setOnClickListener {
            showDonateDialog()
        }
    }
    
    private fun processUrl(url: String): String {
        Log.d(Constants.LOG_TAG, "Processing URL: $url")
        
        // First check if it's an Instagram URL to give it special handling
        if (url.contains(Constants.INSTAGRAM_DOMAIN, ignoreCase = true)) {
            Log.d(Constants.LOG_TAG, "Instagram URL detected in main activity")
            
            // For Instagram URLs, we directly use the processUrlForSharing method
            // This ensures all Instagram URLs are properly converted to kkinstagram
            val processed = urlProcessor.processUrlForSharing(url)
            Log.d(Constants.LOG_TAG, "Instagram URL processed: $processed")
            return processed
        }
        
        // For other URLs, we use the standard process with user preferences
        val processed = urlProcessor.processUrl(
            url,
            preferencesManager.isCleanTrackingEnabled(),
            preferencesManager.isConvertTwitterEnabled()
        )
        
        Log.d(Constants.LOG_TAG, "Regular URL processed: $processed")
        return processed
    }
    
    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.about_dialog_title)
            .setMessage(R.string.about_text)
            .setPositiveButton(R.string.about_dialog_positive, null)
            .show()
    }
    
    private fun reportBug() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:${getString(R.string.bug_report_email)}".toUri()
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.bug_report_subject))
            }
            
            startActivity(Intent.createChooser(intent, getString(R.string.send_email)))
        } catch (e: Exception) {
            Log.e(Constants.LOG_TAG, "Error launching email app: ${e.message}")
            Toast.makeText(this, getString(R.string.error_email_app), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showDonateDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.donate_dialog_title)
            .setMessage(R.string.donate_text)
            .setPositiveButton(R.string.donate_dialog_positive) { _, _ ->
                try {
                    // Open donation link
                    val intent = Intent(Intent.ACTION_VIEW, Constants.DONATION_URL.toUri())
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(Constants.LOG_TAG, "Error opening donation URL: ${e.message}")
                    Toast.makeText(this, getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.donate_dialog_negative, null)
            .show()
    }
} 