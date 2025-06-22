package com.fixupxer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fixupxer.presentation.main.MainViewModel
import com.fixupxer.ui.BaseActivity
import com.fixupxer.utils.Constants
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
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
    private lateinit var progressIndicator: CircularProgressIndicator
    
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate started")
        
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupListeners()
        observeViewModel()
        
        Timber.d("MainActivity onCreate completed")
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.clearInput()
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
        
        // Find or create progress indicator
        progressIndicator = findViewById(R.id.progressIndicator) ?: run {
            // If not in layout, create programmatically
            CircularProgressIndicator(this).apply {
                isIndeterminate = true
                visibility = View.GONE
            }
        }
        
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
                viewModel.onUrlChanged(s?.toString() ?: "")
            }
        })
        
        // Instagram toggle switch
        switchInstagram.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onInstagramConversionToggled(isChecked)
        }
        
        // Process URL button
        buttonProcessUrl.setOnClickListener {
            viewModel.processUrl()
        }
        
        // Share button
        buttonShare.setOnClickListener {
            shareProcessedUrl()
        }
        
        // Open button
        buttonOpen.setOnClickListener {
            openProcessedUrl()
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
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update UI based on state
                    instagramToggleContainer.isVisible = state.isInstagramUrl
                    switchInstagram.isChecked = state.isInstagramConversionEnabled
                    
                    progressIndicator.isVisible = state.isLoading
                    buttonProcessUrl.isEnabled = !state.isLoading
                    
                    if (state.processedUrl.isNotEmpty()) {
                        textViewProcessedUrl.text = state.processedUrl
                    } else if (state.error != null) {
                        textViewProcessedUrl.text = getString(R.string.no_url)
                        Toast.makeText(this@MainActivity, state.error, Toast.LENGTH_SHORT).show()
                    } else {
                        textViewProcessedUrl.text = ""
                    }
                }
            }
        }
    }
    
    private fun shareProcessedUrl() {
        val processedUrl = viewModel.uiState.value.processedUrl
        if (processedUrl.isNotEmpty()) {
            try {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, processedUrl)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
            } catch (e: Exception) {
                Timber.e(e, "Error sharing URL")
                Toast.makeText(this, getString(R.string.error_sharing_url), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.no_url_to_share), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openProcessedUrl() {
        val processedUrl = viewModel.uiState.value.processedUrl
        if (processedUrl.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, processedUrl.toUri())
                startActivity(intent)
            } catch (e: Exception) {
                Timber.e(e, "Error opening URL")
                Toast.makeText(this, getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.no_url_to_open), Toast.LENGTH_SHORT).show()
        }
    }
    
    @SuppressLint("NewApi")
    private fun copyToClipboard() {
        val processedUrl = viewModel.uiState.value.processedUrl
        if (processedUrl.isNotEmpty()) {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("URL", processedUrl)
            clipboardManager.setPrimaryClip(clipData)
            
            // On Android < 10, show a toast notification
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Toast.makeText(this, getString(R.string.url_copied), Toast.LENGTH_SHORT).show()
            } else {
                // Android 10+ shows its own notification
                Timber.d("URL copied to clipboard (Android 10+)")
            }
        } else {
            Toast.makeText(this, getString(R.string.no_url_to_copy), Toast.LENGTH_SHORT).show()
        }
    }
} 