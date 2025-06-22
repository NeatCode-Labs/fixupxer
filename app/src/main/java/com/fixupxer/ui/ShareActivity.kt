package com.fixupxer.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.SwitchCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fixupxer.R
import com.fixupxer.presentation.share.ShareViewModel
import com.fixupxer.utils.Constants
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
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
    private lateinit var progressIndicator: CircularProgressIndicator
    
    private val viewModel: ShareViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("ShareActivity onCreate started")
        
        setContentView(R.layout.activity_share)
        
        initializeViews()
        setupListeners()
        observeViewModel()
        
        // Handle the initial intent
        handleIntent(intent)
        
        Timber.d("ShareActivity onCreate completed")
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
        
        // Find or create progress indicator
        progressIndicator = findViewById(R.id.progressIndicator) ?: run {
            CircularProgressIndicator(this).apply {
                isIndeterminate = true
                visibility = View.GONE
            }
        }
        
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
            viewModel.onInstagramConversionToggled(isChecked)
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
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update UI based on state
                    textViewSharedText.text = state.sharedText.ifEmpty { getString(R.string.no_url_found) }
                    
                    instagramToggleContainer.isVisible = state.isInstagramUrl
                    switchInstagram.isChecked = state.isInstagramConversionEnabled
                    
                    progressIndicator.isVisible = state.isLoading
                    buttonCopy.isEnabled = !state.isLoading && state.processedUrl.isNotEmpty()
                    buttonShare.isEnabled = !state.isLoading && state.processedUrl.isNotEmpty()
                    
                    if (state.processedUrl.isNotEmpty()) {
                        textViewProcessedUrl.text = state.processedUrl
                    } else if (state.error != null) {
                        textViewProcessedUrl.text = getString(R.string.no_url_found)
                        if (state.sharedText.isNotEmpty()) {
                            Toast.makeText(this@ShareActivity, state.error, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        textViewProcessedUrl.text = getString(R.string.processing)
                    }
                }
            }
        }
    }
    
    private fun handleIntent(intent: Intent) {
        val sharedText = when {
            intent.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            intent.action == Intent.ACTION_PROCESS_TEXT -> {
                intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
            }
            else -> null
        }
        
        Timber.d("Received shared text: $sharedText")
        viewModel.handleSharedText(sharedText)
    }
    
    @SuppressLint("NewApi")
    private fun copyToClipboard() {
        val processedUrl = viewModel.uiState.value.processedUrl
        if (processedUrl.isNotEmpty()) {
            try {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Processed URL", processedUrl)
                clipboard.setPrimaryClip(clip)
                
                // On Android < 10, show a toast notification
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    Toast.makeText(this, getString(R.string.url_copied), Toast.LENGTH_SHORT).show()
                } else {
                    // Android 10+ shows its own notification
                    Timber.d("URL copied to clipboard (Android 10+)")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error copying to clipboard")
                Toast.makeText(this, getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.no_url_to_copy), Toast.LENGTH_SHORT).show()
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
} 