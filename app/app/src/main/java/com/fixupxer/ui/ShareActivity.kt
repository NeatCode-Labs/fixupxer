package com.fixupxer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.UrlProcessor
import com.fixupxer.utils.Constants
import com.google.android.material.button.MaterialButton

class ShareActivity : AppCompatActivity() {
    private lateinit var textViewSharedText: TextView
    private lateinit var textViewProcessedUrl: TextView
    private lateinit var buttonCopy: MaterialButton
    private lateinit var buttonShare: MaterialButton
    private lateinit var buttonDonate: MaterialButton
    private lateinit var backButton: ImageButton
    private lateinit var titleTextView: TextView
    private lateinit var textViewAbout: TextView
    private lateinit var textViewReportBug: TextView
    private lateinit var preferencesManager: PreferencesManager
    private var processedUrl: String = ""
    private lateinit var urlProcessor: UrlProcessor
    private var windowInsetsListener: OnApplyWindowInsetsListener? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(Constants.LOG_TAG, "ShareActivity onCreate started")
        
        // Set content view first before any window manipulation
        setContentView(R.layout.activity_share)
        
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
        
        preferencesManager = PreferencesManager(this)
        urlProcessor = UrlProcessor()
        
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
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up insets listener to prevent memory leaks
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && windowInsetsListener != null) {
            ViewCompat.setOnApplyWindowInsetsListener(window.decorView, null)
            windowInsetsListener = null
        }
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
        
        // Add content descriptions for accessibility
        buttonCopy.contentDescription = getString(R.string.copy_content_desc)
        buttonShare.contentDescription = getString(R.string.share_content_desc)
        buttonDonate.contentDescription = getString(R.string.donate_content_desc)
        textViewAbout.contentDescription = getString(R.string.about_content_desc)
        textViewReportBug.contentDescription = getString(R.string.report_bug_content_desc)
    }
    
    private fun setupListeners() {
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
        } else {
            textViewSharedText.text = sharedText
            // Automatically process the URL
            processSharedText(sharedText)
        }
    }
    
    private fun processSharedText(text: String) {
        if (text.isNotEmpty()) {
            try {
                Log.d(Constants.LOG_TAG, "Processing shared text: $text")
                
                // Handle Instagram URLs specially - we always want to convert them
                if (text.contains(Constants.INSTAGRAM_DOMAIN, ignoreCase = true)) {
                    Log.d(Constants.LOG_TAG, "Instagram URL detected, processing for sharing")
                    processedUrl = urlProcessor.processUrlForSharing(text)
                    Log.d(Constants.LOG_TAG, "Instagram URL processed for sharing: $processedUrl")
                } else {
                    // For other URLs, process normally
                    processedUrl = urlProcessor.processUrl(
                        text,
                        true, // Always clean tracking
                        preferencesManager.isConvertTwitterEnabled()
                    )
                    Log.d(Constants.LOG_TAG, "Regular URL processed: $processedUrl")
                }
                
                textViewProcessedUrl.text = processedUrl
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
    
    private fun copyToClipboard() {
        if (processedUrl.isNotEmpty()) {
            try {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Processed URL", processedUrl)
                clipboard.setPrimaryClip(clip)
                
                // Notify user that text has been copied
                Toast.makeText(this, getString(R.string.url_copied), Toast.LENGTH_SHORT).show()
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
                // Check if URL already contains kkinstagram
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
                
                // Log for debugging
                Log.d(Constants.LOG_TAG, "Final URL for sharing: $urlForSharing")
                
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