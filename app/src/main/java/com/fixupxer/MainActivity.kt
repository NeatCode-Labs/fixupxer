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
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.fixupxer.ui.BaseActivity
import com.fixupxer.utils.Constants
import com.google.android.material.button.MaterialButton

class MainActivity : BaseActivity() {
    private lateinit var editTextUrl: EditText
    private lateinit var buttonProcessUrl: MaterialButton
    private lateinit var textViewProcessedUrl: TextView
    private lateinit var buttonShare: MaterialButton
    private lateinit var buttonOpen: MaterialButton
    private lateinit var buttonCopy: MaterialButton
    private lateinit var textViewAbout: TextView
    private lateinit var textViewReportBug: TextView
    private lateinit var buttonDonate: MaterialButton
    private lateinit var instagramToggleContainer: LinearLayout
    private lateinit var switchInstagram: SwitchCompat
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(Constants.LOG_TAG, "MainActivity onCreate started")
        
        // Set content view
        setContentView(R.layout.activity_main)
        
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
        // Hide Instagram toggle when app resumes
        instagramToggleContainer.visibility = View.GONE
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
        
        // Initialize Instagram toggle
        instagramToggleContainer = findViewById(R.id.instagramToggleContainer)
        switchInstagram = findViewById(R.id.switchInstagram)
        
        // Set initial state from preferences
        switchInstagram.isChecked = preferencesManager.isConvertInstagramEnabled()
        
        // Add content descriptions for accessibility
        buttonProcessUrl.contentDescription = getString(R.string.process_url_content_desc)
        buttonShare.contentDescription = getString(R.string.share_content_desc)
        buttonOpen.contentDescription = getString(R.string.open_content_desc)
        buttonCopy.contentDescription = getString(R.string.copy_content_desc)
        textViewAbout.contentDescription = getString(R.string.about_content_desc)
        textViewReportBug.contentDescription = getString(R.string.report_bug_content_desc)
        buttonDonate.contentDescription = getString(R.string.donate_content_desc)
        
        // Set title in the TextView if it exists
        setAppTitle(findViewById(R.id.appTitleHeader))
    }
    
    private fun setupListeners() {
        // URL input text change listener
        editTextUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val inputText = s?.toString() ?: ""
                // Check if the input URL is an Instagram URL
                if (urlProcessor.isInstagramUrl(inputText)) {
                    // Show Instagram toggle
                    instagramToggleContainer.visibility = View.VISIBLE
                } else {
                    // Hide Instagram toggle
                    instagramToggleContainer.visibility = View.GONE
                }
            }
        })
        
        // Instagram toggle switch
        switchInstagram.setOnCheckedChangeListener { _, isChecked ->
            // Only save the preference, don't process the URL automatically
            preferencesManager.setConvertInstagramEnabled(isChecked)
            // Log the preference change
            Log.d(Constants.LOG_TAG, "Instagram conversion preference set to: $isChecked")
        }
        
        // Process URL button
        buttonProcessUrl.setOnClickListener {
            val inputUrl = editTextUrl.text.toString().trim()
            if (inputUrl.isNotEmpty()) {
                try {
                    // Process URL in background thread
                    processUrlInBackground(inputUrl) { processedUrl ->
                        textViewProcessedUrl.text = processedUrl
                    }
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
                    // Use the processed URL directly without further processing
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
            copyToClipboard()
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
    
    @SuppressLint("NewApi")
    private fun copyToClipboard() {
        val processedUrl = textViewProcessedUrl.text.toString()
        if (processedUrl.isNotEmpty() && processedUrl != getString(R.string.no_url)) {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("URL", processedUrl)
            clipboardManager.setPrimaryClip(clipData)
            
            // On Android < 10, show a toast notification
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Toast.makeText(this, getString(R.string.url_copied), Toast.LENGTH_SHORT).show()
            } else {
                // Android 10+ shows its own notification, so we don't need to show a toast
                // But we can still log for debugging
                Log.d(Constants.LOG_TAG, "URL copied to clipboard (Android 10+)")
            }
        } else {
            Toast.makeText(this, getString(R.string.no_url_to_copy), Toast.LENGTH_SHORT).show()
        }
    }
} 