package com.fixupxer.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.net.toUri
import com.fixupxer.R
import com.fixupxer.utils.Constants
import com.google.android.material.button.MaterialButton

class ShareActivity : BaseActivity() {
    private lateinit var textViewSharedText: TextView
    private lateinit var textViewProcessedUrl: TextView
    private lateinit var buttonCopy: MaterialButton
    private lateinit var buttonShare: MaterialButton
    private lateinit var buttonDonate: MaterialButton
    private lateinit var backButton: ImageButton
    private lateinit var titleTextView: TextView
    private lateinit var textViewAbout: TextView
    private lateinit var textViewReportBug: TextView
    private lateinit var instagramToggleContainer: LinearLayout
    private lateinit var switchInstagram: SwitchCompat
    private var processedUrl: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(Constants.LOG_TAG, "ShareActivity onCreate started")
        
        // Set content view
        setContentView(R.layout.activity_share)
        
        initializeViews()
        setupListeners()
        
        // Get shared text
        handleIntent(intent)
        
        Log.d(Constants.LOG_TAG, "ShareActivity onCreate completed")
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        titleTextView = findViewById(R.id.titleTextView)
        
        textViewSharedText = findViewById(R.id.textViewSharedText)
        textViewProcessedUrl = findViewById(R.id.textViewProcessedUrl)
        buttonCopy = findViewById(R.id.buttonCopy)
        buttonShare = findViewById(R.id.buttonShare)
        buttonDonate = findViewById(R.id.buttonDonate)
        textViewAbout = findViewById(R.id.textViewAbout)
        textViewReportBug = findViewById(R.id.textViewReportBug)
        
        // Initialize Instagram toggle
        instagramToggleContainer = findViewById(R.id.instagramToggleContainer)
        switchInstagram = findViewById(R.id.switchInstagram)
        
        // Set initial state from preferences
        switchInstagram.isChecked = preferencesManager.isConvertInstagramEnabled()
        
        // Add content descriptions for accessibility
        buttonCopy.contentDescription = getString(R.string.copy_content_desc)
        buttonShare.contentDescription = getString(R.string.share_content_desc)
        buttonDonate.contentDescription = getString(R.string.donate_content_desc)
        textViewAbout.contentDescription = getString(R.string.about_content_desc)
        textViewReportBug.contentDescription = getString(R.string.report_bug_content_desc)
        
        // Set app title
        setAppTitle(titleTextView)
    }
    
    private fun setupListeners() {
        // Instagram toggle switch
        switchInstagram.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setConvertInstagramEnabled(isChecked)
            
            // Re-process the URL with the new setting
            val sharedText = textViewSharedText.text.toString()
            if (sharedText.isNotEmpty() && sharedText != getString(R.string.no_url_found)) {
                processSharedText(sharedText)
            }
        }
        
        buttonCopy.setOnClickListener {
            copyToClipboard()
        }
        
        buttonShare.setOnClickListener {
            shareProcessedUrl()
        }
        
        backButton.setOnClickListener {
            finish()
        }
        
        buttonDonate.setOnClickListener {
            showDonateDialog()
        }
        
        textViewAbout.setOnClickListener {
            showAboutDialog()
        }
        
        textViewReportBug.setOnClickListener {
            reportBug()
        }
    }
    
    private fun handleIntent(intent: Intent) {
        val sharedText = when {
            intent.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            else -> null
        }
        
        Log.d(Constants.LOG_TAG, "Received shared text: $sharedText")
        
        if (sharedText.isNullOrEmpty()) {
            textViewSharedText.text = getString(R.string.no_url_found)
            instagramToggleContainer.visibility = View.GONE
        } else {
            textViewSharedText.text = sharedText
            
            // Check if the shared text contains an Instagram URL
            if (urlProcessor.isInstagramUrl(sharedText)) {
                instagramToggleContainer.visibility = View.VISIBLE
            } else {
                instagramToggleContainer.visibility = View.GONE
            }
            
            // Automatically process the URL
            processSharedText(sharedText)
        }
    }
    
    private fun processSharedText(text: String) {
        if (text.isNotEmpty()) {
            try {
                Log.d(Constants.LOG_TAG, "Processing shared text: $text")
                
                // Process URL in background thread
                processUrlInBackground(text) { result ->
                    processedUrl = result
                    textViewProcessedUrl.text = processedUrl
                }
            } catch (e: Exception) {
                Log.e(Constants.LOG_TAG, "Error processing text: ${e.message}")
                Toast.makeText(this, getString(R.string.error_processing_url), Toast.LENGTH_SHORT).show()
                textViewProcessedUrl.text = text
                processedUrl = text
            }
        } else {
            textViewProcessedUrl.text = getString(R.string.no_url_found)
            processedUrl = ""
        }
    }
    
    @SuppressLint("NewApi")
    private fun copyToClipboard() {
        if (processedUrl.isNotEmpty()) {
            try {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Processed URL", processedUrl)
                clipboard.setPrimaryClip(clip)
                
                // On Android < 10, show a toast notification
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    Toast.makeText(this, getString(R.string.url_copied), Toast.LENGTH_SHORT).show()
                } else {
                    // Android 10+ shows its own notification, so we don't need to show a toast
                    Log.d(Constants.LOG_TAG, "URL copied to clipboard (Android 10+)")
                }
            } catch (e: Exception) {
                Log.e(Constants.LOG_TAG, "Error copying to clipboard: ${e.message}")
                Toast.makeText(this, getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.no_url_to_copy), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareProcessedUrl() {
        if (processedUrl.isNotEmpty()) {
            try {
                // Use the processed URL directly
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, processedUrl)
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
} 