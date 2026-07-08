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
import android.os.Build
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
import com.fixupxer.ui.dialogs.InstagramProxyDialogHelper
import com.fixupxer.ui.dialogs.TikTokProxyDialogHelper
import com.fixupxer.PreferencesManager
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

        binding.togglesInclude.textViewChangeTikTokProxy.setOnClickListener {
            onChangeTikTokProxyClick()
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
                    binding.buttonCopy.isEnabled = !state.isLoading && state.actionUrl.isNotEmpty()
                    binding.buttonShare.isEnabled = !state.isLoading && state.actionUrl.isNotEmpty()
                    binding.buttonOpen.isEnabled = !state.isLoading && state.actionUrl.isNotEmpty()
                    
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
    
    private fun handleIntent() {
        val sharedText = when {
            intent.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                // Some apps put the shared text only into ClipData, not EXTRA_TEXT.
                intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: intent.clipData?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)?.coerceToText(this)?.toString()
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
        // ShareActivity is marked android:noHistory="true" in the manifest, which makes
        // the system call finish() automatically whenever it loses focus. Launching
        // Settings would therefore destroy the share context. Show the proxy chooser
        // inline as a dialog and re-process the shared URL so the preview updates
        // immediately — no app restart required.
        InstagramProxyDialogHelper.show(this, preferencesManager) {
            refreshProxyLabel()
            // Trigger a local re-process so the "Processed URL" field reflects
            // the freshly selected proxy (the toggle value itself is unchanged).
            viewModel.reprocessAfterProxyChange()
        }
    }

    private fun onChangeTikTokProxyClick() {
        // Same inline-dialog contract as onChangeProxyClick(), but for the TikTok roster.
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
} 