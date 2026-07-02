// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2025  NeatCode Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.ui.dialogs.HistoryDialogHelper
import com.fixupxer.ui.dialogs.InstagramProxyDialogHelper
import com.fixupxer.utils.PostCleanRunner
import com.fixupxer.domain.repository.UrlRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Inject
import androidx.core.text.HtmlCompat
import android.widget.ScrollView
import com.fixupxer.ui.SettingsActivity

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var urlTextWatcher: TextWatcher
    private var footerLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var textValidationJob: kotlinx.coroutines.Job? = null
    
    @Inject
    lateinit var historyRepository: HistoryRepository
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    @Inject
    lateinit var urlRepository: UrlRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate started")
        
        // Edge-to-edge is now handled in BaseActivity
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        initializeViews()
        setupListeners()
        observeViewModel()
        setupSmartFooter()
        
        // Handle VIEW intent if present
        handleViewIntentIfPresent(intent)
        
        Timber.d("MainActivity onCreate completed")
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleViewIntentIfPresent(intent)
    }
    
    private fun handleViewIntentIfPresent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data
            val scheme = uri?.scheme
            
            Timber.d("handleViewIntentIfPresent: URI=$uri, scheme=$scheme")
            
            // Only handle http and https schemes
            if (scheme == "http" || scheme == "https") {
                lifecycleScope.launch {
                    try {
                        val originalUrl = uri.toString()
                        // Gate only: the validator's *output* is URL-decoded, and
                        // UrlProcessor decodes again, so we must pass the ORIGINAL
                        // URI string onward to avoid double-decoding %-encoded URLs.
                        val validated = withContext(Dispatchers.Default) {
                            InputValidator.validateAndSanitizeInput(originalUrl)
                        }
                        if (validated == null) {
                            Timber.w("VIEW intent URL rejected by validator")
                            Toast.makeText(this@MainActivity, getString(R.string.error_processing_url), Toast.LENGTH_SHORT).show()
                            finish()
                            return@launch
                        }
                        
                        // Clean the URL using browser-specific preferences
                        val result = urlRepository.processUrlForBrowser(originalUrl)
                        val cleanedUri = Uri.parse(result.url)
                        
                        Timber.d("URL cleaned: $uri -> $cleanedUri")
                        
                        // Run post-clean action
                        val postCleanRunner = PostCleanRunner(this@MainActivity, preferencesManager)
                        postCleanRunner.run(cleanedUri) {
                            // Finish only after the user has made a choice in ask mode.
                            finish()
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to handle VIEW intent")
                        Toast.makeText(this@MainActivity, getString(R.string.error_processing_url), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_overflow, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                openSettings()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            R.id.action_report_bug -> {
                reportBug()
                true
            }
            R.id.action_disclaimer -> {
                showDisclaimerDialog()
                true
            }
            R.id.action_donate -> {
                showDonateDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
        binding.buttonHistory.contentDescription = getString(R.string.history_title)
        
        // Set title in the TextView if it exists
        setAppTitle(binding.titleTextView)
    }
    
    private fun setupSmartFooter() {
        // Get the parent constraint layout properly
        val scrollView = binding.mainScrollView
        val parentLayout = scrollView.parent as? ConstraintLayout ?: return
        val footer = binding.footerTextView
        
        // Check if we're running tests
        val isRunningTest = try {
            packageManager.getPackageInfo("com.fixupxer.debug.test", 0)
            true
        } catch (e: Exception) {
            false
        }
        
        if (isRunningTest) {
            Timber.d("Running in test mode, using simplified footer setup")
            // For tests, just position the footer at the bottom without monitoring
            val scrollViewParams = scrollView.layoutParams as ConstraintLayout.LayoutParams
            scrollViewParams.bottomToTop = footer.id
            scrollViewParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            scrollViewParams.bottomMargin = 0
            scrollView.layoutParams = scrollViewParams
            return
        }
        
        // Set up a global layout listener to check available space
        // (reference kept so it can be removed in onDestroy)
        footerLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Get the actual height of the parent layout (excluding system bars)
                val parentHeight = parentLayout.height
                val appBarHeight = binding.root.findViewById<View>(R.id.appBarLayout)?.height ?: 0
                val availableHeight = parentHeight - appBarHeight
                
                // Check if we have very limited space (small screen)
                val isSmallScreen = availableHeight < resources.getDimensionPixelSize(R.dimen.min_content_height)
                
                val scrollViewParams = scrollView.layoutParams as ConstraintLayout.LayoutParams
                
                if (isSmallScreen || availableHeight < 600) {
                    // Small screen: Make footer part of scrollable content
                    
                    // Update ScrollView to fill entire parent
                    scrollViewParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    scrollViewParams.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                    scrollViewParams.bottomMargin = 0
                    
                    // Move footer inside the scrollable content by removing it from ConstraintLayout
                    if (footer.parent == parentLayout) {
                        parentLayout.removeView(footer)
                        val scrollContent = scrollView.getChildAt(0) as? LinearLayout
                        
                        // Create new LinearLayout.LayoutParams for the footer
                        val linearParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        linearParams.topMargin = resources.getDimensionPixelSize(R.dimen.margin_medium)
                        footer.layoutParams = linearParams
                        
                        scrollContent?.addView(footer)
                    }
                } else {
                    // Large screen: Keep footer anchored at bottom
                    
                    // Move footer back to ConstraintLayout if it was in scroll content
                    if (footer.parent != parentLayout) {
                        (footer.parent as? ViewGroup)?.removeView(footer)
                        
                        // Create new ConstraintLayout.LayoutParams for the footer
                        val constraintParams = ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.MATCH_PARENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                        )
                        constraintParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                        constraintParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        constraintParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        footer.layoutParams = constraintParams
                        
                        parentLayout.addView(footer)
                    }
                    
                    // Update ScrollView to stop above footer
                    scrollViewParams.bottomToTop = footer.id
                    scrollViewParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                    scrollViewParams.bottomMargin = 0
                }
                
                scrollView.layoutParams = scrollViewParams
            }
        }
        parentLayout.viewTreeObserver.addOnGlobalLayoutListener(footerLayoutListener)
    }
    
    override fun onDestroy() {
        footerLayoutListener?.let {
            binding.mainScrollView.parent?.let { parent ->
                (parent as? View)?.viewTreeObserver?.removeOnGlobalLayoutListener(it)
            }
        }
        footerLayoutListener = null
        super.onDestroy()
    }
    
    private fun setupListeners() {
        Timber.d("Setting up listeners")
        
        // URL input text change listener
        urlTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val raw = s?.toString() ?: ""
                // Cancel any in-flight validation so rapid typing can't deliver
                // out-of-order results to the ViewModel.
                textValidationJob?.cancel()
                textValidationJob = lifecycleScope.launch {
                    try {
                        val validated = withTimeout(200) {
                            withContext(Dispatchers.Default) {
                                InputValidator.validateAndSanitizeInput(raw)
                            }
                        }
                        if (validated == null) {
                            withContext(Dispatchers.Main) {
                                binding.editTextUrl.removeTextChangedListener(urlTextWatcher)
                                binding.editTextUrl.setText("")
                                binding.editTextUrl.addTextChangedListener(urlTextWatcher)
                                viewModel.setMultipleUrlsError()
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
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e // superseded by a newer text change — don't log as error
                    } catch (e: Exception) {
                        Timber.e(e, "Error during text validation")
                    }
                }
            }
        }
        binding.editTextUrl.addTextChangedListener(urlTextWatcher)
        
        // Button listeners
        binding.buttonPaste.setOnClickListener { pasteFromClipboard() }
        binding.buttonProcess.setOnClickListener { viewModel.processUrl() }
        binding.buttonShare.setOnClickListener { shareProcessedUrl() }
        binding.buttonOpen.setOnClickListener { openProcessedUrl() }
        binding.buttonCopy.setOnClickListener { copyToClipboard() }
        binding.buttonHistory.setOnClickListener { showHistoryDialog() }
        
        // Footer click listener
        binding.footerTextView.setOnClickListener { 
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.WEBSITE_URL))
                startActivity(intent)
            } catch (e: Exception) {
                Timber.e(e, "Error opening website")
                Toast.makeText(this, getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
            }
        }
        
        // Instagram toggle listener
        binding.switchInstagram.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onInstagramConversionToggled(isChecked)
        }
        
        // Twitter toggle switch
        binding.switchTwitter.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onTwitterConversionToggled(isChecked)
        }

        binding.textViewChangeProxy.setOnClickListener {
            onChangeProxyClick()
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update UI based on state
                    binding.instagramToggleContainer.isVisible = state.isInstagramUrl || state.isFacebookUrl
                    // The 'Currently using: <proxy>. Change.' row applies only to Instagram
                    binding.instagramProxyRow.isVisible = state.isInstagramUrl
                    if (state.isInstagramUrl) {
                        refreshProxyLabel()
                    }
                    binding.switchInstagram.isChecked = state.isInstagramConversionEnabled
                    
                    binding.twitterToggleContainer.isVisible = state.isTwitterUrl
                    binding.switchTwitter.isChecked = state.isTwitterConversionEnabled
                    
                    binding.progressIndicator.isVisible = state.isLoading
                    binding.buttonProcess.isEnabled = !state.isLoading
                    
                    if (state.processedUrl.isNotEmpty()) {
                        binding.textViewProcessedUrl.text = state.processedUrl
                    } else if (state.error != null) {
                        binding.textViewProcessedUrl.text = state.error
                    } else {
                        binding.textViewProcessedUrl.text = ""
                    }
                }
            }
        }
    }

    private fun onChangeProxyClick() {
        // Settings no longer hosts a proxy chooser (v1.5.1+); the dialog is
        // now the single source of truth for picking the Instagram proxy on
        // both Main and Share screens. After the user picks a proxy:
        //   1. refresh the "Active: <proxy>." label,
        //   2. re-process *only if* a Processed URL already exists for an
        //      Instagram input (i.e. the user has already tapped Process once).
        // The first-time processing flow still belongs to the Process button —
        // pasting a fresh link does not auto-process.
        InstagramProxyDialogHelper.show(this, preferencesManager) {
            refreshProxyLabel()
            viewModel.reprocessAfterProxyChange()
        }
    }

    private fun refreshProxyLabel() {
        binding.textViewInstagramProxyStatus.text =
            getString(R.string.currently_using_proxy, preferencesManager.getInstagramProxy())
    }
    
    private fun shareProcessedUrl() {
        // Action buttons operate on actionUrl (always a real URL), not the display
        // text — which may be the "Nothing to do!" message.
        val actionUrl = viewModel.uiState.value.actionUrl
        if (actionUrl.isNotEmpty()) {
            try {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, actionUrl)
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
        val actionUrl = viewModel.uiState.value.actionUrl
        if (actionUrl.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, actionUrl.toUri())
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
        val actionUrl = viewModel.uiState.value.actionUrl
        if (actionUrl.isNotEmpty()) {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboardManager == null) {
                Timber.e("ClipboardManager not available")
                Toast.makeText(this, getString(R.string.error_processing_url), Toast.LENGTH_SHORT).show()
                return
            }
            val clipData = ClipData.newPlainText(getString(R.string.clipboard_label_url), actionUrl)
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
                                            binding.textViewProcessedUrl.text = getString(R.string.error_multiple_urls)
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
    
    private fun showHistoryDialog() {
        val historyDialogHelper = HistoryDialogHelper(
            context = this,
            lifecycleOwner = this,
            historyRepository = historyRepository,
            preferencesManager = preferencesManager
        )
        historyDialogHelper.showHistoryDialog()
    }
    
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
    

} 