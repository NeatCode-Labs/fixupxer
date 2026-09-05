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
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fixupxer.databinding.ActivityMainBinding
import com.fixupxer.presentation.main.BrowserViewProcessingResult
import com.fixupxer.presentation.main.MainViewModel
import com.fixupxer.ui.BaseActivity
import com.fixupxer.ui.helpers.DominantHandLayoutHelper
import com.fixupxer.ui.helpers.ResultStatusHelper
import com.fixupxer.ui.helpers.SmartFooterHelper
import com.fixupxer.ui.helpers.SnackbarHelper
import com.fixupxer.ui.helpers.UrlActionHelper
import com.fixupxer.ui.helpers.UrlDiffHelper
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.BrowserViewGate
import com.fixupxer.utils.BrowserViewHandoffPolicy
import com.fixupxer.utils.Constants
import com.fixupxer.utils.InputValidator
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.ui.dialogs.HistoryDialogHelper
import com.fixupxer.ui.dialogs.LinkGuardDialogHelper
import com.fixupxer.ui.dialogs.ProxyPickerDialogHelper
import com.fixupxer.ui.helpers.PlatformToggleHelper
import com.fixupxer.utils.PostCleanRunner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
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
    private var inputDraftBlocked = false
    private var viewIntentJob: Job? = null
    private var activePostCleanRunner: PostCleanRunner? = null
    
    @Inject
    lateinit var historyRepository: HistoryRepository
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate started")
        
        // Edge-to-edge is now handled in BaseActivity
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        initializeViews()
        applyDominantHandLayout()
        setupListeners()
        observeViewModel()
        setupSmartFooter()
        
        if (intent?.action != Intent.ACTION_VIEW) {
            viewModel.clearCompletedViewTransaction()
        }

        // Handle VIEW intent if present (browser mode)
        handleViewIntentIfPresent(intent)

        Timber.d("MainActivity onCreate completed")
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            applyDominantHandLayout()
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.cancelInflightViewProcessing()
        viewModel.clearCompletedViewTransaction()
        handleViewIntentIfPresent(intent)
    }
    
    private fun handleViewIntentIfPresent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            viewIntentJob?.cancel()
            activePostCleanRunner?.dismissActiveDialog()
            activePostCleanRunner = null

            val uri = intent.data
            val scheme = uri?.scheme
            
            Timber.d("VIEW intent received (host=${uri?.host ?: "unknown"}, scheme=$scheme)")
            
            // Only handle http and https schemes
            if (scheme == "http" || scheme == "https") {
                val originalUrl = uri.toString()
                val preferenceEnabled = preferencesManager.isBrowserModeEnabled()
                val aliasEnabled = BrowserModeUtils.isBrowserAliasEnabled(this)
                val gateSnapshot = BrowserViewGate.begin(preferenceEnabled, aliasEnabled)
                if (gateSnapshot == null) {
                    Timber.w(
                        "Skipping VIEW processing because Browser mode state is inconsistent " +
                            "(preference=$preferenceEnabled, alias=$aliasEnabled)"
                    )
                    handoffOriginalUrl(originalUrl)
                    return
                }

                val completedTransaction = viewModel.getCompletedViewTransaction(originalUrl)

                viewIntentJob = lifecycleScope.launch {
                    try {
                        if (!isBrowserViewGateValid(gateSnapshot)) {
                            Timber.w("Browser VIEW gate changed before URL processing")
                            handoffOriginalUrl(originalUrl)
                            return@launch
                        }

                        if (completedTransaction != null) {
                            Timber.d("Replaying completed VIEW transaction after recreation")
                            dispatchBrowserPostClean(
                                processedUrl = completedTransaction.processedUrl,
                                routingHost = completedTransaction.routingHost,
                            )
                            return@launch
                        }

                        val processingResult = viewModel.browserViewResult(originalUrl).await()
                        if (processingResult is BrowserViewProcessingResult.ValidationRejected) {
                            Timber.w("VIEW intent URL rejected by validator")
                            handoffOriginalUrl(originalUrl)
                            return@launch
                        }

                        val result = (processingResult as BrowserViewProcessingResult.Success).result
                        val cleanedUri = Uri.parse(result.url)
                        
                        Timber.d(
                            "VIEW URL processed (host=${cleanedUri.host ?: "unknown"}, " +
                                "length=${result.url.length})"
                        )

                        if (!isBrowserViewGateValid(gateSnapshot)) {
                            Timber.w("Browser VIEW gate changed before after-clean dispatch")
                            handoffOriginalUrl(originalUrl)
                            return@launch
                        }

                        dispatchBrowserPostClean(
                            processedUrl = result.url,
                            routingHost = result.routingHost,
                        )
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to handle VIEW intent")
                        handoffOriginalUrl(originalUrl)
                    }
                }
            }
        }
    }

    private fun dispatchBrowserPostClean(processedUrl: String, routingHost: String?) {
        val postCleanRunner = PostCleanRunner(this, preferencesManager)
        activePostCleanRunner = postCleanRunner
        postCleanRunner.run(Uri.parse(processedUrl), routingHost) {
            // Finish only after the user has made a choice in ask mode. A stale
            // runner's late callback must not clear a newer intent's transaction.
            if (activePostCleanRunner === postCleanRunner) {
                viewModel.clearCompletedViewTransaction()
                activePostCleanRunner = null
                finish()
            }
        }
    }

    private fun isBrowserViewGateValid(
        snapshot: com.fixupxer.utils.BrowserViewGateSnapshot,
    ): Boolean = BrowserViewGate.isValid(
        snapshot = snapshot,
        preferenceEnabled = preferencesManager.isBrowserModeEnabled(),
        aliasEnabled = BrowserModeUtils.isBrowserAliasEnabled(this),
    )

    private fun handoffOriginalUrl(originalUrl: String) {
        viewModel.clearCompletedViewTransaction()
        textValidationJob?.cancel()
        inputDraftBlocked = false
        binding.editTextUrl.removeTextChangedListener(urlTextWatcher)
        binding.editTextUrl.setText(originalUrl)
        binding.editTextUrl.addTextChangedListener(urlTextWatcher)
        viewModel.showOriginalForManualFallback(originalUrl)
        updateProcessButtonState()

        val opened = UrlActionHelper.openUrlInExternalBrowser(
            binding.root,
            this,
            originalUrl,
        )
        if (BrowserViewHandoffPolicy.shouldFinish(opened)) {
            finish()
        } else {
            Timber.w("External browser handoff failed; keeping original URL available")
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
            R.id.action_whats_new -> {
                openWhatsNew()
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
            footer = binding.footerTextView,
            historyAnchor = binding.buttonHistory,
        )
    }

    private fun applyDominantHandLayout() {
        DominantHandLayoutHelper.apply(
            actionRow = binding.actionRow,
            openButton = binding.buttonOpen,
            copyButton = binding.buttonCopy,
            shareButton = binding.buttonShare,
            historyButton = binding.buttonHistory,
            hand = preferencesManager.getDominantHand(),
            actionGapPx = resources.getDimensionPixelSize(R.dimen.margin_small),
            historyEdgeMarginPx = resources.getDimensionPixelSize(R.dimen.margin_medium),
        )
    }
    
    override fun onDestroy() {
        viewIntentJob?.cancel()
        viewIntentJob = null
        activePostCleanRunner?.dismissActiveDialog()
        activePostCleanRunner = null
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
                inputDraftBlocked = true
                binding.buttonProcess.isEnabled = false
                textValidationJob = lifecycleScope.launch {
                    try {
                        val result = withTimeout(200) {
                            withContext(Dispatchers.Default) {
                                InputValidator.validate(raw)
                            }
                        }
                        if (result is InputValidator.ValidationResult.Invalid) {
                            withContext(Dispatchers.Main) {
                                // A draft such as "%" or "%2" must remain editable
                                // until the user completes its percent escape.
                                // Multiple-URL pastes retain their existing clear UX.
                                inputDraftBlocked = result.reason == InputValidator.InvalidReason.OTHER
                                if (!inputDraftBlocked) {
                                    binding.editTextUrl.removeTextChangedListener(urlTextWatcher)
                                    binding.editTextUrl.setText("")
                                    binding.editTextUrl.addTextChangedListener(urlTextWatcher)
                                }
                                viewModel.setValidationError(result.reason)
                                updateProcessButtonState()
                            }
                            return@launch
                        }
                        val validated = (result as InputValidator.ValidationResult.Valid).value
                        withContext(Dispatchers.Main) {
                            inputDraftBlocked = false
                            viewModel.onUrlChanged(validated)
                            updateProcessButtonState()
                        }
                    } catch (e: TimeoutCancellationException) {
                        withContext(Dispatchers.Main) {
                            viewModel.setValidationError(InputValidator.InvalidReason.OTHER)
                            updateProcessButtonState()
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e // superseded by a newer text change — don't log as error
                    } catch (e: Exception) {
                        Timber.e(e, "Error during text validation")
                        viewModel.setValidationError(InputValidator.InvalidReason.OTHER)
                        updateProcessButtonState()
                    }
                }
            }
        }
        binding.editTextUrl.addTextChangedListener(urlTextWatcher)
        
        // Button listeners
        binding.textInputLayoutUrl.setEndIconOnClickListener { pasteFromClipboard() }
        binding.buttonProcess.setOnClickListener {
            if (!inputDraftBlocked) viewModel.processUrl()
        }
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
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val toggles = binding.togglesInclude
                    PlatformToggleHelper.bindPlatformToggle(
                        context = this@MainActivity,
                        container = toggles.platformToggleContainer,
                        monogram = toggles.platformMonogram,
                        title = toggles.platformTitle,
                        proxyRow = toggles.platformProxyRow,
                        proxyStatus = toggles.textViewPlatformProxyStatus,
                        changeProxy = toggles.textViewChangeProxy,
                        platformSwitch = toggles.switchPlatform,
                        platform = state.detectedPlatform,
                        preferencesManager = preferencesManager,
                        conversionEnabled = PlatformToggleHelper.isConversionEnabled(
                            platform = state.detectedPlatform,
                            isInstagramConversionEnabled = state.isInstagramConversionEnabled,
                            isTwitterConversionEnabled = state.isTwitterConversionEnabled,
                            isFacebookConversionEnabled = state.isFacebookConversionEnabled,
                            isTikTokConversionEnabled = state.isTikTokConversionEnabled,
                            isBlueskyConversionEnabled = state.isBlueskyConversionEnabled,
                            isRedditConversionEnabled = state.isRedditConversionEnabled,
                            isYoutubeConversionEnabled = state.isYoutubeConversionEnabled,
                            isPinterestConversionEnabled = state.isPinterestConversionEnabled,
                            isThreadsConversionEnabled = state.isThreadsConversionEnabled,
                        ),
                        proxySelectionRevision = state.proxySelectionRevision,
                        onToggle = { enabled ->
                            state.detectedPlatform?.let { platform ->
                                viewModel.onPlatformConversionToggled(platform, enabled)
                            }
                        },
                        onChangeProxy = { onChangeProxyClick() },
                    )

                    binding.progressIndicator.visibility =
                        if (state.isLoading) View.VISIBLE else View.INVISIBLE
                    binding.buttonProcess.isEnabled = !state.isLoading && !inputDraftBlocked

                    // Share/Open/Copy only make sense once a result exists —
                    // same pattern as the Share screen.
                    val hasActionUrl = !state.isLoading && state.actionUrl.isNotEmpty()
                    binding.buttonShare.isEnabled = hasActionUrl
                    binding.buttonOpen.isEnabled = hasActionUrl
                    binding.buttonCopy.isEnabled = hasActionUrl
                    binding.processedUrlInclude.leakWarningRow.isVisible =
                        state.leakFindings.isNotEmpty()
                    binding.processedUrlInclude.leakWarningRow.setOnClickListener {
                        LinkGuardDialogHelper.show(
                            context = this@MainActivity,
                            findings = state.leakFindings,
                            onRemoveParameter = viewModel::removeLeakedParameters,
                            onBack = viewModel::clearInput
                        )
                    }

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

    private fun updateProcessButtonState() {
        binding.buttonProcess.isEnabled = !viewModel.uiState.value.isLoading && !inputDraftBlocked
    }

    private fun onChangeProxyClick() {
        val platform = viewModel.uiState.value.detectedPlatform ?: return
        ProxyPickerDialogHelper.show(
            context = this,
            layoutInflater = layoutInflater,
            platform = platform,
            preferencesManager = preferencesManager,
        ) {
            viewModel.notifyProxySelectionChanged()
            viewModel.reprocessAfterProxyChange()
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
                                    // Explicit paste is final input, so malformed
                                    // clipboard values still use strict rejection.
                                    binding.editTextUrl.removeTextChangedListener(urlTextWatcher)
                                    binding.editTextUrl.setText("")
                                    binding.editTextUrl.addTextChangedListener(urlTextWatcher)
                                    inputDraftBlocked = false
                                    viewModel.setValidationError(result.reason)
                                    updateProcessButtonState()
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
                // Run the same validation as editing, including replacing a blocked draft.
                binding.editTextUrl.setText(entry.originalUrl)
            }
        )
        historyDialogHelper.showHistoryDialog()
    }
    
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
    

}
