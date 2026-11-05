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


package com.fixupxer.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fixupxer.R
import com.fixupxer.databinding.ActivityShareBinding
import com.fixupxer.presentation.share.ShareViewModel
import com.fixupxer.utils.Constants
import com.fixupxer.utils.ShareAfterCleanAction
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.ui.dialogs.HistoryDialogHelper
import com.fixupxer.ui.dialogs.LinkGuardDialogHelper
import com.fixupxer.ui.dialogs.ProxyPickerDialogHelper
import com.fixupxer.ui.helpers.PlatformToggleHelper
import com.fixupxer.PreferencesManager
import com.fixupxer.ui.helpers.DominantHandLayoutHelper
import com.fixupxer.ui.helpers.ResultStatusHelper
import com.fixupxer.ui.helpers.SmartFooterHelper
import com.fixupxer.ui.helpers.SnackbarHelper
import com.fixupxer.ui.helpers.UrlActionHelper
import com.fixupxer.ui.helpers.UrlDiffHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ShareActivity : BaseActivity() {
    private lateinit var binding: ActivityShareBinding
    private val viewModel: ShareViewModel by viewModels()
    private var footerLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    
    @Inject
    lateinit var historyRepository: HistoryRepository
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("ShareActivity onCreate started")
        
        handleIntent()
        // Attach an empty transparent surface so the window participates normally
        // in focus/configuration changes while processing. No result UI is drawn.
        if (!viewModel.uiState.value.showResultScreen) {
            setContentView(android.widget.FrameLayout(this))
        }
        observeViewModel()
        observeAutomaticActions()
        Timber.d("ShareActivity onCreate completed")
    }

    /** The automatic path never inflates/draws the result screen. */
    private fun ensureResultScreen() {
        if (::binding.isInitialized) return
        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        initializeViews()
        applyDominantHandLayout()
        setupListeners()
        setupSmartFooter()
        androidx.core.view.ViewCompat.requestApplyInsets(binding.root)
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_overflow, menu)
        // ShareActivity is noHistory="true": opening Settings would kill the share
        // context, so Settings stays out of this menu (it lives on the Main screen).
        menu.findItem(R.id.action_settings)?.isVisible = false
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
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
            R.id.action_help -> {
                openHelp()
                true
            }
            R.id.action_whats_new -> {
                openWhatsNew()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(newRequest = true)
    }
    
    override fun onPause() {
        super.onPause()
        Timber.d("ShareActivity paused (changingConfiguration=$isChangingConfigurations, finishing=$isFinishing)")
        // Configuration changes (rotation, system theme switch) recreate the
        // activity — the share context must survive them. The ViewModel keeps
        // the state; processSharedText() dedupes the re-delivered intent.
        if (isChangingConfigurations) return
        // Clear state when activity loses focus (one-shot share flow)
        finish() // Destroy activity when it loses focus
        viewModel.clearState()
    }
    
    private fun initializeViews() {
        binding.buttonCopy.contentDescription = getString(R.string.copy_content_desc)
        binding.buttonShare.contentDescription = getString(R.string.share_content_desc)
        binding.buttonOpen.contentDescription = getString(R.string.open_content_desc)
        binding.buttonHistory.contentDescription = getString(R.string.history_title)
        
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
        Timber.d("ShareActivity destroyed (changingConfiguration=$isChangingConfigurations, finishing=$isFinishing)")
        footerLayoutListener?.let {
            binding.mainScrollView.parent?.let { parent ->
                (parent as? View)?.viewTreeObserver?.removeOnGlobalLayoutListener(it)
            }
        }
        footerLayoutListener = null
        super.onDestroy()
    }
    
    private fun setupListeners() {
        // Toggle listeners are now handled in observeViewModel to prevent duplicate triggers
        
        binding.buttonCopy.setOnClickListener {
            viewModel.onManualCopy()
            UrlActionHelper.copyToClipboard(binding.root, this, viewModel.uiState.value.actionUrl)
        }
        
        binding.buttonShare.setOnClickListener {
            viewModel.onManualInteraction()
            UrlActionHelper.shareUrl(binding.root, this, viewModel.uiState.value.actionUrl)
        }
        
        binding.buttonHistory.setOnClickListener {
            showHistoryDialog()
        }
        
        binding.buttonOpen.setOnClickListener {
            UrlActionHelper.openUrl(binding.root, this, viewModel.uiState.value.actionUrl)
        }

        binding.togglesInclude.textViewChangeProxy.setOnClickListener {
            onChangeProxyClick()
        }
        
        binding.footerTextView.setOnClickListener {
            openExternalPage(Constants.WEBSITE_URL)
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (isFinishing || isDestroyed) return@collect
                    if (::binding.isInitialized) binding.root.isVisible = state.showResultScreen
                    if (!state.showResultScreen) return@collect
                    ensureResultScreen()
                    // Update UI based on state; removed tracking params get struck
                    // through once the cleaned result is known.
                    binding.textViewSharedText.text = if (state.sharedText.isEmpty()) {
                        getString(R.string.no_url_found)
                    } else {
                        UrlDiffHelper.strikeRemovedParams(state.sharedText, state.actionUrl)
                    }
                    
                    val toggles = binding.togglesInclude
                    PlatformToggleHelper.bindPlatformToggle(
                        context = this@ShareActivity,
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
                    binding.buttonCopy.isEnabled = !state.isLoading && state.actionUrl.isNotEmpty()
                    binding.buttonShare.isEnabled = !state.isLoading && state.actionUrl.isNotEmpty()
                    binding.buttonOpen.isEnabled = !state.isLoading && state.actionUrl.isNotEmpty()
                    binding.processedUrlInclude.leakWarningRow.isVisible =
                        state.leakFindings.isNotEmpty()
                    binding.processedUrlInclude.autoCopyWarning.isVisible = state.autoCopyBlocked || state.autoShareNotice != null
                    binding.processedUrlInclude.autoCopyWarning.setText(
                        state.autoShareNotice ?: if (state.leakFindings.isNotEmpty()) R.string.auto_copy_shared_links_blocked
                        else R.string.auto_copy_shared_links_incomplete
                    )
                    binding.processedUrlInclude.leakWarningRow.setOnClickListener {
                        LinkGuardDialogHelper.show(
                            context = this@ShareActivity,
                            findings = state.leakFindings,
                            onRemoveParameter = viewModel::removeLeakedParameters,
                            onBack = { finish() }
                        )
                    }
                    
                    if (state.error != null) {
                        binding.processedUrlInclude.textViewProcessedUrl.text = state.error
                        ResultStatusHelper.bind(
                            this@ShareActivity,
                            binding.processedUrlInclude.textViewResultStatus,
                            null
                        )
                    } else if (state.processedUrl.isNotEmpty()) {
                        binding.processedUrlInclude.textViewProcessedUrl.text = state.processedUrl
                        ResultStatusHelper.bind(
                            this@ShareActivity,
                            binding.processedUrlInclude.textViewResultStatus,
                            state.resultStatus
                        )
                    } else {
                        binding.processedUrlInclude.textViewProcessedUrl.text = getString(R.string.processing)
                        ResultStatusHelper.bind(
                            this@ShareActivity,
                            binding.processedUrlInclude.textViewResultStatus,
                            null
                        )
                    }
                }
            }
        }
    }
    
    private fun Intent.isPlainTextSend(): Boolean {
        if (action != Intent.ACTION_SEND) return false
        val mimeType = type ?: return true
        val normalized = mimeType.substringBefore(';').trim().lowercase()
        return normalized == "text/plain" || normalized == "text/*"
    }

    private fun extractSendText(intent: Intent): String? {
        if (!intent.isPlainTextSend()) return null
        // Some apps put the shared text only into ClipData, not EXTRA_TEXT.
        return intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?: intent.clipData?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)?.coerceToText(this)?.toString()
    }

    private fun observeAutomaticActions() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collect { state ->
                    if (isFinishing || isDestroyed) return@collect
                    // One consumer fixes ordering: opening a chooser pauses this activity.
                    state.pendingAutoCopy?.let { pending ->
                        viewModel.consumeAutoCopy(pending)?.let { url ->
                            UrlActionHelper.copyToClipboard(
                                window.decorView, this@ShareActivity, url, showFeedback = state.showResultScreen,
                            )
                        }
                    }
                    state.pendingAutoShare?.let { pending ->
                        val url = viewModel.consumeAutoShare(pending) ?: return@let
                        if (!UrlActionHelper.shareUrl(window.decorView, this@ShareActivity, url, showError = false)) {
                            viewModel.onAutoShareFailed()
                        }
                    }
                }
            }
        }
    }

    private fun handleIntent(newRequest: Boolean = false) {
        val sharedText = extractSendText(intent)
        
        if (!sharedText.isNullOrEmpty()) {
            viewModel.processSharedText(
                sharedText,
                autoCopy = preferencesManager.isAutoCopySharedLinksEnabled(),
                newRequest = newRequest,
                autoShare = preferencesManager.getShareAfterCleanAction() == ShareAfterCleanAction.SHARE &&
                    !intent.getBooleanExtra(UrlActionHelper.EXTRA_SKIP_AUTO_SHARE, false),
            )
        } else {
            // Without this the UI would sit on "Processing…" forever.
            viewModel.setNoSharedText()
        }
    }
    
    private fun showHistoryDialog() {
        val historyDialogHelper = HistoryDialogHelper(
            context = this,
            lifecycleOwner = this,
            historyRepository = historyRepository,
            preferencesManager = preferencesManager,
            onEntrySelected = { entry ->
                UrlActionHelper.copyToClipboard(binding.root, this, entry.cleanedUrl)
            }
        )
        historyDialogHelper.showHistoryDialog()
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
}
