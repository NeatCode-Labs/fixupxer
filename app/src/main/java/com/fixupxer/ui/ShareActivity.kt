package com.fixupxer.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fixupxer.R
import com.fixupxer.databinding.ActivityShareBinding
import com.fixupxer.presentation.share.ShareViewModel
import com.fixupxer.utils.Constants
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ShareActivity : BaseActivity() {
    private lateinit var binding: ActivityShareBinding
    private val viewModel: ShareViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("ShareActivity onCreate started")
        
        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeViews()
        setupListeners()
        observeViewModel()
        handleIntent()
        
        Timber.d("ShareActivity onCreate completed")
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent()
    }
    
    override fun onPause() {
        super.onPause()
        // Clear state when activity loses focus
        viewModel.clearState()
        finish() // Destroy activity when it loses focus
    }
    
    private fun initializeViews() {
        // Add content descriptions for accessibility
        binding.buttonCopy.contentDescription = getString(R.string.copy_content_desc)
        binding.buttonShare.contentDescription = getString(R.string.share_content_desc)
        binding.textViewAbout.contentDescription = getString(R.string.about_content_desc)
        binding.textViewDisclaimer.contentDescription = getString(R.string.disclaimer_content_desc)
        binding.textViewReportBug.contentDescription = getString(R.string.report_bug_content_desc)
        binding.buttonDonate.contentDescription = getString(R.string.donate_content_desc)
        binding.backButton.contentDescription = getString(R.string.back)
        binding.buttonOpen.contentDescription = getString(R.string.open_content_desc)
        
        // Set title
        binding.titleTextView.text = getString(R.string.app_title)
    }
    
    private fun setupListeners() {
        // Instagram toggle switch
        binding.switchInstagram.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onInstagramConversionToggled(isChecked)
        }
        
        // Twitter toggle switch
        binding.switchTwitter.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onTwitterConversionToggled(isChecked)
        }
        
        binding.buttonCopy.setOnClickListener {
            copyToClipboard()
        }
        
        binding.buttonShare.setOnClickListener {
            shareProcessedUrl()
        }
        
        binding.backButton.setOnClickListener {
            finish()
        }
        
        binding.buttonDonate.setOnClickListener {
            showDonateDialog()
        }
        
        binding.textViewAbout.setOnClickListener {
            showAboutDialog()
        }
        
        binding.textViewDisclaimer.setOnClickListener {
            showDisclaimerDialog()
        }
        
        binding.textViewReportBug.setOnClickListener {
            reportBug()
        }
        
        binding.textViewFooter.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.WEBSITE_URL))
                startActivity(intent)
            } catch (e: Exception) {
                Timber.e(e, "Error opening website")
                Toast.makeText(this, getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.buttonOpen.setOnClickListener {
            val processedUrl = viewModel.uiState.value.processedUrl
            if (processedUrl.isNotEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(processedUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Error opening URL")
                    Toast.makeText(this, getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.no_url_to_open), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update UI based on state
                    binding.textViewSharedText.text = state.sharedText.ifEmpty { getString(R.string.no_url_found) }
                    
                    binding.instagramToggleContainer.isVisible = state.isInstagramUrl
                    binding.switchInstagram.isChecked = state.isInstagramConversionEnabled
                    
                    binding.twitterToggleContainer.isVisible = state.isTwitterUrl
                    binding.switchTwitter.isChecked = state.isTwitterConversionEnabled
                    
                    binding.progressIndicator.isVisible = state.isLoading
                    binding.buttonCopy.isEnabled = !state.isLoading && state.processedUrl.isNotEmpty()
                    binding.buttonShare.isEnabled = !state.isLoading && state.processedUrl.isNotEmpty()
                    
                    if (state.error != null) {
                        // Show the error directly in the processed-url field (no Toast)
                        binding.textViewProcessedUrl.text = state.error
                    } else if (state.processedUrl.isNotEmpty()) {
                        binding.textViewProcessedUrl.text = state.processedUrl
                    } else {
                        binding.textViewProcessedUrl.text = getString(R.string.processing)
                    }
                }
            }
        }
    }
    
    private fun handleIntent() {
        val sharedText = when {
            intent.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            intent.action == Intent.ACTION_PROCESS_TEXT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                } else {
                    null
                }
            }
            else -> null
        }
        
        if (!sharedText.isNullOrEmpty()) {
            viewModel.processSharedText(sharedText)
        }
    }
    
    @SuppressLint("NewApi")
    private fun copyToClipboard() {
        val processedUrl = viewModel.uiState.value.processedUrl
        if (processedUrl.isNotEmpty()) {
            try {
                val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                if (clipboardManager == null) {
                    Timber.e("ClipboardManager not available")
                    Toast.makeText(this, getString(R.string.error_processing_url), Toast.LENGTH_SHORT).show()
                    return
                }
                val clip = ClipData.newPlainText(getString(R.string.clipboard_label_processed_url), processedUrl)
                clipboardManager.setPrimaryClip(clip)
                
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