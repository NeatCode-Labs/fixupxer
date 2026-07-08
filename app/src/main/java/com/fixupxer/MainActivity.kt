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

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fixupxer.databinding.ActivityMainBinding
import com.fixupxer.presentation.main.MainViewModel
import com.fixupxer.ui.BaseActivity
import com.fixupxer.ui.helpers.ResultStatusHelper
import com.fixupxer.ui.helpers.SmartFooterHelper
import com.fixupxer.ui.helpers.SnackbarHelper
import com.fixupxer.ui.helpers.UrlActionHelper
import com.fixupxer.ui.helpers.UrlDiffHelper
import com.fixupxer.utils.Constants
import com.fixupxer.utils.InputValidator
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.ui.dialogs.HistoryDialogHelper
import com.fixupxer.ui.dialogs.InstagramProxyDialogHelper
import com.fixupxer.ui.dialogs.TikTokProxyDialogHelper
import com.fixupxer.utils.PostCleanRunner
import com.fixupxer.domain.repository.UrlRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Inject
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
        
        // Handle VIEW intent if present (browser mode)
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
                            // Toast, not Snackbar: the activity finishes immediately,
                            // taking any Snackbar down with it.
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.error_processing_url),
                                Toast.LENGTH_SHORT
                            ).show()
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
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.error_processing_url),
                            Toast.LENGTH_SHORT
                        ).show()
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
    
    private fun initializeViews() {
        // Add content descriptions for accessibility (end icon description is set
        // in the layout via app:endIconContentDescription)
        binding.buttonProcess.contentDescription = getString(R.string.process_url_content_desc)
        binding.buttonShare.contentDescription = getString(R.string.share_content_desc)
        binding.buttonOpen.contentDescription = getString(R.string.open_content_desc)
        binding.buttonCopy.contentDescription = getString(R.string.copy_content_desc)
        binding.buttonHistory.contentDescription = getString(R.string.history_title)

        // Let the original URL wrap like the Share screen (up to 3 lines) instead
        // of horizontally scrolling. inputType="textUri" keeps it a single logical
        // line (no Enter key), so this only affects display wrapping.
        binding.editTextUrl.apply {
            setHorizontallyScrolling(false)
            maxLines = 3
        }

        // Set title in the TextView if it exists
        setAppTitle(binding.titleTextView)
    }
    
    private fun setupSmartFooter() {
        footerLayoutListener = SmartFooterHelper.setup(
            context = this,
            rootView = binding.root,
            scrollView = binding.mainScrollView,
            footer = binding.footerTextView
        )
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
                        val result = withTimeout(200) {
                            withContext(Dispatchers.Default) {
                                InputValidator.validate(raw)
                            }
                        }
                        if (result is InputValidator.ValidationResult.Invalid) {
                            withContext(Dispatchers.Main) {
                                binding.editTextUrl.removeTextChangedListener(urlTextWatcher)
                                binding.editTextUrl.setText("")
                                binding.editTextUrl.addTextChangedListener(urlTextWatcher)
                                viewModel.setValidationError(result.reason)
                            }
                            return@launch
                        }
                        val validated = (result as InputValidator.ValidationResult.Valid).value
                        withContext(Dispatchers.Main) {
                            viewModel.onUrlChanged(validated)
                        }
                    } catch (e: TimeoutCancellationException) {
                        withContext(Dispatchers.Main) {
                            SnackbarHelper.showShort(binding.root, getString(R.string.error_processing_url))
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
        binding.textInputLayoutUrl.setEndIconOnClickListener { pasteFromClipboard() }
        binding.buttonProcess.setOnClickListener { viewModel.processUrl() }
        binding.buttonShare.setOnClickListener {
            UrlActionHelper.shareUrl(binding.root, this, viewModel.uiState.value.actionUrl)
        }
        binding.buttonOpen.setOnClickListener {
            UrlActionHelper.openUrl(binding.root, this, viewModel.uiState.value.actionUrl)
        }
        binding.buttonCopy.setOnClickListener {
            UrlActionHelper.copyToClipboard(binding.root, this, viewModel.uiState.value.actionUrl)
        }
        binding.buttonHistory.setOnClickListener { showHistoryDialog() }
        
        // Footer click listener
        binding.footerTextView.setOnClickListener { 
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.WEBSITE_URL))
                startActivity(intent)
            } catch (e: Exception) {
                Timber.e(e, "Error opening website")
                SnackbarHelper.showShort(binding.root, getString(R.string.error_browser))
            }
        }

        binding.togglesInclude.textViewChangeProxy.setOnClickListener {
            onChangeProxyClick()
        }

        binding.togglesInclude.textViewChangeTikTokProxy.setOnClickListener {
            onChangeTikTokProxyClick()
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.togglesInclude.instagramToggleContainer.isVisible = state.isInstagramUrl
                    binding.togglesInclude.facebookToggleContainer.isVisible = state.isFacebookUrl
                    binding.togglesInclude.instagramProxyRow.isVisible = state.isInstagramUrl
                    if (state.isInstagramUrl) {
                        refreshProxyLabel()
                    }

                    binding.togglesInclude.switchInstagram.setOnCheckedChangeListener(null)
                    binding.togglesInclude.switchInstagram.isChecked = state.isInstagramConversionEnabled
                    binding.togglesInclude.switchInstagram.setOnCheckedChangeListener { _, isChecked ->
                        viewModel.onInstagramConversionToggled(isChecked)
                    }

                    binding.togglesInclude.switchFacebook.setOnCheckedChangeListener(null)
                    binding.togglesInclude.switchFacebook.isChecked = state.isInstagramConversionEnabled
                    binding.togglesInclude.switchFacebook.setOnCheckedChangeListener { _, isChecked ->
                        viewModel.onInstagramConversionToggled(isChecked)
                    }

                    binding.togglesInclude.twitterToggleContainer.isVisible = state.isTwitterUrl
                    binding.togglesInclude.switchTwitter.setOnCheckedChangeListener(null)
                    binding.togglesInclude.switchTwitter.isChecked = state.isTwitterConversionEnabled
                    binding.togglesInclude.switchTwitter.setOnCheckedChangeListener { _, isChecked ->
                        viewModel.onTwitterConversionToggled(isChecked)
                    }

                    binding.togglesInclude.tiktokToggleContainer.isVisible = state.isTikTokUrl
                    if (state.isTikTokUrl) {
                        refreshTikTokProxyLabel()
                    }
                    binding.togglesInclude.switchTikTok.setOnCheckedChangeListener(null)
                    binding.togglesInclude.switchTikTok.isChecked = state.isTikTokConversionEnabled
                    binding.togglesInclude.switchTikTok.setOnCheckedChangeListener { _, isChecked ->
                        viewModel.onTikTokConversionToggled(isChecked)
                    }

                    binding.progressIndicator.visibility =
                        if (state.isLoading) View.VISIBLE else View.INVISIBLE
                    binding.buttonProcess.isEnabled = !state.isLoading

                    // Share/Open/Copy only make sense once a result exists —
                    // same pattern as the Share screen.
                    val hasActionUrl = !state.isLoading && state.actionUrl.isNotEmpty()
                    binding.buttonShare.isEnabled = hasActionUrl
                    binding.buttonOpen.isEnabled = hasActionUrl
                    binding.buttonCopy.isEnabled = hasActionUrl

                    // Input-related errors (empty input, multiple URLs) belong on the
                    // text field; processing errors render in the result card below.
                    binding.textInputLayoutUrl.error = state.error?.takeIf {
                        state.inputUrl.isEmpty()
                    }

                    if (state.processedUrl.isNotEmpty()) {
                        binding.processedUrlInclude.textViewProcessedUrl.alpha = 1f
                        binding.processedUrlInclude.textViewProcessedUrl.text = state.processedUrl
                        ResultStatusHelper.bind(
                            this@MainActivity,
                            binding.processedUrlInclude.textViewResultStatus,
                            state.resultStatus
                        )
                    } else if (state.error != null && state.inputUrl.isNotEmpty()) {
                        binding.processedUrlInclude.textViewProcessedUrl.alpha = 1f
                        binding.processedUrlInclude.textViewProcessedUrl.text = state.error
                        ResultStatusHelper.bind(
                            this@MainActivity,
                            binding.processedUrlInclude.textViewResultStatus,
                            null
                        )
                    } else {
                        // No result yet — dimmed placeholder instead of a bare
                        // empty card so "step 2" of the flow reads as pending.
                        binding.processedUrlInclude.textViewProcessedUrl.alpha = 0.55f
                        binding.processedUrlInclude.textViewProcessedUrl.text =
                            getString(R.string.result_placeholder)
                        ResultStatusHelper.bind(
                            this@MainActivity,
                            binding.processedUrlInclude.textViewResultStatus,
                            null
                        )
                    }

                    // Strike through the removed tracking params in the input
                    // field (same visual as the Share screen). Only while the
                    // field still shows exactly the text that was processed —
                    // any edit clears the stale diff on the next state emission.
                    binding.editTextUrl.text?.let { editable ->
                        val showDiff = state.actionUrl.isNotEmpty() &&
                            state.processedInputUrl.isNotEmpty() &&
                            editable.toString().trim() == state.processedInputUrl
                        UrlDiffHelper.applyStrikesInPlace(
                            editable,
                            if (showDiff) state.actionUrl else ""
                        )
                        // Reveal the start of the URL so the struck tracking params
                        // are visible in the first lines (matches the Share screen).
                        if (showDiff && !binding.editTextUrl.hasFocus()) {
                            binding.editTextUrl.setSelection(0)
                        }
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

    private fun onChangeTikTokProxyClick() {
        // Same contract as onChangeProxyClick(), but for the TikTok proxy roster.
        TikTokProxyDialogHelper.show(this, preferencesManager) {
            refreshTikTokProxyLabel()
            viewModel.reprocessAfterProxyChange()
        }
    }

    private fun refreshProxyLabel() {
        binding.togglesInclude.textViewInstagramProxyStatus.text =
            getString(R.string.currently_using_proxy, preferencesManager.getInstagramProxy())
    }

    private fun refreshTikTokProxyLabel() {
        binding.togglesInclude.textViewTikTokProxyStatus.text =
            getString(R.string.currently_using_proxy, preferencesManager.getTikTokProxy())
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
                    // A validation run from earlier typing could otherwise land
                    // after the paste and overwrite the state set below.
                    textValidationJob?.cancel()
                    lifecycleScope.launch {
                        try {
                            withTimeout(500) {
                                val result = withContext(Dispatchers.Default) {
                                    InputValidator.validate(text)
                                }

                                if (result is InputValidator.ValidationResult.Invalid) {
                                    // Same flow as the TextWatcher rejection: clear
                                    // the field (watcher detached so this can't
                                    // race) and let the ViewModel own the error.
                                    binding.editTextUrl.removeTextChangedListener(urlTextWatcher)
                                    binding.editTextUrl.setText("")
                                    binding.editTextUrl.addTextChangedListener(urlTextWatcher)
                                    viewModel.setValidationError(result.reason)
                                    return@withTimeout
                                }

                                val validated =
                                    (result as InputValidator.ValidationResult.Valid).value
                                // Try to extract a single valid URL
                                val url = withContext(Dispatchers.Default) {
                                    UrlProcessor.findFirstValidUrl(validated)
                                }

                                if (url != null) {
                                    binding.editTextUrl.setText(url)
                                } else {
                                    SnackbarHelper.showShort(
                                        binding.root,
                                        getString(R.string.no_url_found_in_clipboard)
                                    )
                                }
                            }
                        } catch (e: TimeoutCancellationException) {
                            withContext(Dispatchers.Main) {
                                SnackbarHelper.showShort(binding.root, getString(R.string.error_processing_url))
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error during paste validation")
                            withContext(Dispatchers.Main) {
                                SnackbarHelper.showShort(binding.root, getString(R.string.error_processing_url))
                            }
                        }
                    }
                } else {
                    SnackbarHelper.showShort(binding.root, getString(R.string.no_url_found_in_clipboard))
                }
            } else {
                SnackbarHelper.showShort(binding.root, getString(R.string.no_url_found_in_clipboard))
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
            preferencesManager = preferencesManager,
            onEntrySelected = { entry ->
                binding.editTextUrl.removeTextChangedListener(urlTextWatcher)
                binding.editTextUrl.setText(entry.originalUrl)
                binding.editTextUrl.addTextChangedListener(urlTextWatcher)
                viewModel.onUrlChanged(entry.originalUrl)
            }
        )
        historyDialogHelper.showHistoryDialog()
    }
    
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
    

} 