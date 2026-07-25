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
        
        // Edge-to-edge is now handled in BaseActivity
        
        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        initializeViews()
        applyDominantHandLayout()
        setupListeners()
        observeViewModel()
        setupSmartFooter()
        handleIntent()
        
        Timber.d("ShareActivity onCreate completed")
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
        handleIntent()
    }
    
    override fun onPause() {
        super.onPause()
        // Configuration changes (rotation, system theme switch) recreate the
        // activity — the share context must survive them. The ViewModel keeps
        // the state; processSharedText() dedupes the re-delivered intent.
        if (isChangingConfigurations) return
        // Clear state when activity loses focus (one-shot share flow)
        viewModel.clearState()
        finish() // Destroy activity when it loses focus
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
            UrlActionHelper.copyToClipboard(binding.root, this, viewModel.uiState.value.actionUrl)
        }
        
        binding.buttonShare.setOnClickListener {
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
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.WEBSITE_URL))
                startActivity(intent)
            } catch (e: Exception) {
                Timber.e(e, "Error opening website")
                SnackbarHelper.showShort(binding.root, getString(R.string.error_browser))
            }
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
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

    private fun handleIntent() {
        val sharedText = extractSendText(intent)
        
        if (!sharedText.isNullOrEmpty()) {
            viewModel.processSharedText(sharedText)
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