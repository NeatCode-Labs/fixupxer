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
import com.fixupxer.databinding.ActivityMainBinding
import com.fixupxer.presentation.main.MainViewModel
import com.fixupxer.ui.BaseActivity
import com.fixupxer.utils.Constants
import com.fixupxer.utils.InputValidator
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var urlTextWatcher: TextWatcher
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate started")
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeViews()
        setupListeners()
        observeViewModel()
        
        Timber.d("MainActivity onCreate completed")
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.clearInput()
    }
    
    override fun onPause() {
        super.onPause()
        // Clear input when app loses focus
        viewModel.clearInput()
        binding.editTextUrl.setText("")
    }
    
    private fun initializeViews() {
        // Add content descriptions for accessibility
        binding.buttonPaste.contentDescription = getString(R.string.paste_content_desc)
        binding.buttonProcess.contentDescription = getString(R.string.process_url_content_desc)
        binding.buttonShare.contentDescription = getString(R.string.share_content_desc)
        binding.buttonOpen.contentDescription = getString(R.string.open_content_desc)
        binding.buttonCopy.contentDescription = getString(R.string.copy_content_desc)
        binding.textViewAbout.contentDescription = getString(R.string.about_content_desc)
        binding.textViewDisclaimer.contentDescription = getString(R.string.disclaimer_content_desc)
        binding.textViewReportBug.contentDescription = getString(R.string.report_bug_content_desc)
        binding.buttonDonate.contentDescription = getString(R.string.donate_content_desc)
        
        // Set title in the TextView if it exists
        setAppTitle(binding.titleTextView)
    }
    
    private fun setupListeners() {
        // URL input text change listener
        urlTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val raw = s?.toString() ?: ""
                lifecycleScope.launch {
                    try {
                        val validated = withTimeout(200) {
                            withContext(Dispatchers.Default) {
                                InputValidator.validateAndSanitizeInput(raw)
                            }
                        }
                        if (validated == null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, getString(R.string.error_multiple_urls), Toast.LENGTH_SHORT).show()
                                binding.editTextUrl.removeTextChangedListener(urlTextWatcher)
                                binding.editTextUrl.setText("")
                                binding.editTextUrl.addTextChangedListener(urlTextWatcher)
                                viewModel.clearInput()
                            }
                            return@launch
                        }
                        withContext(Dispatchers.Main) {
                            viewModel.onUrlChanged(validated)
                        }
                    } catch (e: TimeoutCancellationException) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, getString(R.string.error_processing_url), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error during text validation")
                    }
                }
            }
        }
        binding.editTextUrl.addTextChangedListener(urlTextWatcher)
        
        // Instagram toggle switch
        binding.switchInstagram.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onInstagramConversionToggled(isChecked)
        }
        
        // Twitter toggle switch
        binding.switchTwitter.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onTwitterConversionToggled(isChecked)
        }
        

        
        // Paste button
        binding.buttonPaste.setOnClickListener {
            pasteFromClipboard()
        }
        
        // Process URL button
        binding.buttonProcess.setOnClickListener {
            viewModel.processUrl()
        }
        
        // Share button
        binding.buttonShare.setOnClickListener {
            shareProcessedUrl()
        }
        
        // Open button
        binding.buttonOpen.setOnClickListener {
            openProcessedUrl()
        }
        
        // Copy button
        binding.buttonCopy.setOnClickListener {
            copyToClipboard()
        }
        
        // About text link
        binding.textViewAbout.setOnClickListener {
            showAboutDialog()
        }
        
        // Disclaimer text link
        binding.textViewDisclaimer.setOnClickListener {
            showDisclaimerDialog()
        }
        
        // Report Bug text link
        binding.textViewReportBug.setOnClickListener {
            reportBug()
        }
        
        // Donate button
        binding.buttonDonate.setOnClickListener {
            showDonateDialog()
        }
        
        // Footer text link
        binding.textViewFooter.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.WEBSITE_URL))
                startActivity(intent)
            } catch (e: Exception) {
                Timber.e(e, "Error opening website")
                Toast.makeText(this, getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update UI based on state
                    binding.instagramToggleContainer.isVisible = state.isInstagramUrl
                    binding.switchInstagram.isChecked = state.isInstagramConversionEnabled
                    
                    binding.twitterToggleContainer.isVisible = state.isTwitterUrl
                    binding.switchTwitter.isChecked = state.isTwitterConversionEnabled
                    

                    
                    binding.progressIndicator.isVisible = state.isLoading
                    binding.buttonProcess.isEnabled = !state.isLoading
                    
                    if (state.processedUrl.isNotEmpty()) {
                        binding.textViewProcessedUrl.text = state.processedUrl
                    } else if (state.error != null) {
                        binding.textViewProcessedUrl.text = getString(R.string.no_url)
                        // Only show toast when explicitly requested (after processing)
                        if (state.showErrorToast) {
                        Toast.makeText(this@MainActivity, state.error, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        binding.textViewProcessedUrl.text = ""
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
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboardManager == null) {
                Timber.e("ClipboardManager not available")
                Toast.makeText(this, getString(R.string.error_processing_url), Toast.LENGTH_SHORT).show()
                return
            }
            val clipData = ClipData.newPlainText(getString(R.string.clipboard_label_url), processedUrl)
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
    
    private fun pasteFromClipboard() {
        try {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboardManager == null) {
                Timber.e("ClipboardManager not available")
                return
            }
            val clipData = clipboardManager.primaryClip
            
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                if (!text.isNullOrEmpty()) {
                    lifecycleScope.launch {
                        try {
                            withTimeout(500) {
                                withContext(Dispatchers.Default) {
                                    val validated = InputValidator.validateAndSanitizeInput(text)
                                    
                                    if (validated == null) {
                                        withContext(Dispatchers.Main) {
                                            binding.editTextUrl.setText("")
                                            Toast.makeText(this@MainActivity, getString(R.string.error_multiple_urls), Toast.LENGTH_SHORT).show()
                                        }
                                        return@withContext
                                    }
                                    
                                    // Try to extract a single valid URL
                                    val url = UrlProcessor.findFirstValidUrl(validated)
                                    
                                    withContext(Dispatchers.Main) {
                                        if (url != null) {
                                            binding.editTextUrl.setText(url)
                                        } else {
                                            Toast.makeText(this@MainActivity, getString(R.string.no_url_found_in_clipboard), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        } catch (e: TimeoutCancellationException) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, getString(R.string.error_processing_url), Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error during paste validation")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, getString(R.string.error_processing_url), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, getString(R.string.no_url_found_in_clipboard), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.no_url_found_in_clipboard), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error pasting from clipboard")
            // Don't show error toast - avoid confusion with system notifications
        }
    }
} 