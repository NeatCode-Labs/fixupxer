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

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.fixupxer.PreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import android.view.View

@AndroidEntryPoint
class ShareActivity : BaseActivity() {
    private lateinit var binding: ActivityShareBinding
    private val viewModel: ShareViewModel by viewModels()
    
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
        handleIntent()
    }
    
    override fun onPause() {
        super.onPause()
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
        parentLayout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
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
        })
    }
    
    private fun setupListeners() {
        // Toggle listeners are now handled in observeViewModel to prevent duplicate triggers
        
        binding.buttonCopy.setOnClickListener {
            copyToClipboard()
        }
        
        binding.buttonShare.setOnClickListener {
            shareProcessedUrl()
        }
        
        binding.buttonHistory.setOnClickListener {
            showHistoryDialog()
        }
        
        binding.buttonOpen.setOnClickListener {
            openProcessedUrl()
        }

        binding.textViewChangeProxy.setOnClickListener {
            onChangeProxyClick()
        }
        
        binding.footerTextView.setOnClickListener {
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
                    binding.textViewSharedText.text = state.sharedText.ifEmpty { getString(R.string.no_url_found) }
                    
                    binding.instagramToggleContainer.isVisible = state.isInstagramUrl || state.isFacebookUrl
                    // The 'Currently using: <proxy>. Change.' row is only relevant for Instagram
                    // (it has 3 proxies); Facebook has a single proxy and keeps the row hidden.
                    binding.instagramProxyRow.isVisible = state.isInstagramUrl
                    if (state.isInstagramUrl) {
                        refreshProxyLabel()
                    }
                    
                    // Temporarily remove listener to avoid triggering when setting programmatically
                    binding.switchInstagram.setOnCheckedChangeListener(null)
                    binding.switchInstagram.isChecked = state.isInstagramConversionEnabled
                    binding.switchInstagram.setOnCheckedChangeListener { _, isChecked ->
                        viewModel.onInstagramConversionToggled(isChecked)
                    }
                    
                    binding.twitterToggleContainer.isVisible = state.isTwitterUrl || state.isTikTokUrl
                    
                    // Temporarily remove listener to avoid triggering when setting programmatically
                    binding.switchTwitter.setOnCheckedChangeListener(null)
                    binding.switchTwitter.isChecked = state.isTwitterConversionEnabled
                    binding.switchTwitter.setOnCheckedChangeListener { _, isChecked ->
                        viewModel.onTwitterConversionToggled(isChecked)
                    }
                    
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
    
    private fun showHistoryDialog() {
        val historyDialogHelper = HistoryDialogHelper(
            context = this,
            lifecycleOwner = this,
            historyRepository = historyRepository,
            preferencesManager = preferencesManager
        )
        historyDialogHelper.showHistoryDialog()
    }
    
    private fun openProcessedUrl() {
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

    private fun onChangeProxyClick() {
        // ShareActivity is marked android:noHistory="true" in the manifest, which makes
        // the system call finish() automatically whenever it loses focus. Launching
        // Settings would therefore destroy the share context. Show the proxy chooser
        // inline as a dialog and re-process the shared URL so the preview updates
        // immediately — no app restart required.
        InstagramProxyDialogHelper.show(this, preferencesManager) {
            refreshProxyLabel()
            // Trigger a local re-process with the current toggle state so the
            // "Processed URL" field reflects the freshly selected proxy.
            viewModel.onInstagramConversionToggled(binding.switchInstagram.isChecked)
        }
    }

    private fun refreshProxyLabel() {
        binding.textViewInstagramProxyStatus.text =
            getString(R.string.currently_using_proxy, preferencesManager.getInstagramProxy())
    }
} 