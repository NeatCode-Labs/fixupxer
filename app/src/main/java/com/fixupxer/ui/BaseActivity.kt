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

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fixupxer.R
import com.fixupxer.utils.Constants
import timber.log.Timber
import com.fixupxer.BuildConfig
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/**
 * Base activity with common functionality for all activities
 */
abstract class BaseActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display with Android 15 compliant approach
        setupEdgeToEdge()
        
        // Configure window insets for proper padding
        setupWindowInsets()
    }
    
    private fun setupEdgeToEdge() {
        // For Android 15 (SDK 35+) edge-to-edge is enforced by default
        // For earlier versions, we need to enable it manually

        // Below API 26 the navigation bar buttons are always white and can't be
        // tinted, so a fully transparent bar leaves them invisible on light
        // backgrounds — keep the translucent scrim there (same values as the
        // enableEdgeToEdge() defaults). From API 26 on, icons adapt to the
        // theme, so full transparency is safe. Status bar icons adapt from
        // API 23; on 21-22 enableEdgeToEdge falls back to the system's own
        // translucent scrim, so TRANSPARENT is safe to request unconditionally.
        val navigationBarStyle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.auto(
                Color.argb(0xe6, 0xFF, 0xFF, 0xFF),
                Color.argb(0x80, 0x1b, 0x1b, 0x1b)
            )
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = navigationBarStyle
        )

        // Additional configuration for API 29+ to disable contrast enforcement
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }
    
    private fun setupWindowInsets() {
        // Apply window insets dynamically to handle system bars
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView.rootView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Find the AppBarLayout and apply top padding dynamically
            val appBarLayout = view.findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
            if (appBarLayout != null) {
                // Use the actual status bar height instead of fixed padding
                appBarLayout.setPadding(0, insets.top, 0, 0)
            }
            
            // Apply bottom padding to the main content if needed. Screens with the
            // pinned History FAB get extra clearance so content can scroll above it.
            val scrollView = view.findViewById<androidx.core.widget.NestedScrollView>(R.id.mainScrollView)
            if (scrollView != null) {
                val fabClearance = if (view.findViewById<View>(R.id.buttonHistory) != null) {
                    resources.getDimensionPixelSize(R.dimen.scroll_bottom_padding)
                } else {
                    0
                }
                scrollView.setPadding(
                    scrollView.paddingLeft,
                    scrollView.paddingTop,
                    scrollView.paddingRight,
                    insets.bottom + fabClearance
                )
            }

            // Keep the Custom Rules add button clear of gesture and three-button
            // navigation bars. The list is constrained above the button.
            val addRuleButton = view.findViewById<View>(R.id.buttonAddRule)
            val addRuleLayoutParams =
                addRuleButton?.layoutParams as? android.view.ViewGroup.MarginLayoutParams
            if (addRuleButton != null && addRuleLayoutParams != null) {
                addRuleLayoutParams.bottomMargin = insets.bottom +
                    resources.getDimensionPixelSize(R.dimen.padding_large)
                addRuleButton.layoutParams = addRuleLayoutParams
            }
            
            // Apply symmetrical padding to footer to match title spacing
            val footer = view.findViewById<TextView>(R.id.footerTextView)
            if (footer != null) {
                val titleInternalPadding = resources.getDimensionPixelSize(R.dimen.padding_medium)
                footer.setPadding(
                    footer.paddingLeft,
                    footer.paddingTop,
                    footer.paddingRight,
                    insets.bottom + titleInternalPadding
                )
            }
            
            WindowInsetsCompat.CONSUMED
        }
    }
    
    /**
     * Show about dialog
     */
    protected fun showAboutDialog() {
        val message = getString(R.string.about_text, BuildConfig.VERSION_NAME)
        val spannedMessage = android.text.SpannableString(message)
        
        // Find website URL and make it clickable
        val websiteIndex = message.indexOf(Constants.WEBSITE_URL)
        if (websiteIndex != -1) {
            spannedMessage.setSpan(
                android.text.style.URLSpan(Constants.WEBSITE_URL),
                websiteIndex,
                websiteIndex + Constants.WEBSITE_URL.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        // Find GitHub URL and make it clickable
        val githubIndex = message.indexOf(Constants.GITHUB_URL)
        if (githubIndex != -1) {
            spannedMessage.setSpan(
                android.text.style.URLSpan(Constants.GITHUB_URL),
                githubIndex,
                githubIndex + Constants.GITHUB_URL.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        val alertDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about_dialog_title)
            .setPositiveButton(R.string.about_dialog_positive, null)
            .create()
            
        // Use a TextView to make links clickable
        val textView = TextView(this)
        textView.text = spannedMessage
        textView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.dialog_text_padding_horizontal)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.dialog_text_padding_vertical)
        textView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        textView.linksClickable = true
        
        alertDialog.setView(textView)
        alertDialog.show()
    }
    
    /**
     * Show disclaimer dialog (reachable from the overflow menu).
     */
    protected fun showDisclaimerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_disclaimer, null)
        
        val alertDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.disclaimer_dialog_title)
            .setView(dialogView)
            .setCancelable(false) // User must scroll and click button
            .create()
        
        // Find views
        val scrollView = dialogView.findViewById<android.widget.ScrollView>(R.id.scrollViewDisclaimer)
        val textViewContent = dialogView.findViewById<TextView>(R.id.textViewDisclaimerContent)
        val buttonAgreeAndClose = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonAgreeAndClose)
        
        // Set HTML content
        textViewContent.text = HtmlCompat.fromHtml(getString(R.string.disclaimer_text), HtmlCompat.FROM_HTML_MODE_LEGACY)
        
        // Set up scroll listener to show button when scrolled to bottom
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val view = scrollView.getChildAt(scrollView.childCount - 1)
            val diff = (view.bottom - (scrollView.height + scrollView.scrollY))
            
            // If diff is zero or less, we've reached the bottom
            if (diff <= 0) {
                buttonAgreeAndClose.visibility = android.view.View.VISIBLE
            }
        }
        
        // Also check immediately in case content is already fully visible
        scrollView.post {
            val view = scrollView.getChildAt(0)
            if (view.height <= scrollView.height) {
                // Content fits without scrolling
                buttonAgreeAndClose.visibility = android.view.View.VISIBLE
            }
        }
        
        // Set up button click
        buttonAgreeAndClose.setOnClickListener {
            alertDialog.dismiss()
        }
        
        alertDialog.show()
    }
    
    /**
     * Report bug via email
     */
    protected fun reportBug() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${Constants.BUG_REPORT_EMAIL}")
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.bug_report_subject))
            }
            
            startActivity(Intent.createChooser(intent, getString(R.string.send_email)))
        } catch (e: Exception) {
            Timber.e(e, "Error launching email app")
            Toast.makeText(this, getString(R.string.error_email_app), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Open the GitHub release notes in an external browser.
     */
    protected fun openWhatsNew() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.RELEASE_NOTES_URL)))
        } catch (e: Exception) {
            Timber.e(e, "Error opening release notes URL")
            Toast.makeText(this, getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Show donate dialog
     */
    protected fun showDonateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_donate, null)
        
        val alertDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        
        // Find views in the custom layout
        val buttonDonate = dialogView.findViewById<android.widget.Button>(R.id.buttonDonate)
        val buttonMaybeLater = dialogView.findViewById<android.widget.Button>(R.id.buttonMaybeLater)
        val textViewMonero = dialogView.findViewById<TextView>(R.id.textViewMonero)
        
        // Set up donate button click
        buttonDonate.setOnClickListener {
                try {
                    // Open donation link
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.DONATION_URL))
                    startActivity(intent)
                alertDialog.dismiss()
                } catch (e: Exception) {
                    Timber.e(e, "Error opening donation URL")
                    Toast.makeText(this, getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
                }
            }
        
        // Set up maybe later button click
        buttonMaybeLater.setOnClickListener {
            alertDialog.dismiss()
        }
        
        // Set up Monero text click to copy address
        textViewMonero.setOnClickListener {
            val clipboardManager = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            if (clipboardManager != null) {
                val clipData = android.content.ClipData.newPlainText(getString(R.string.clipboard_label_monero_address), Constants.MONERO_ADDRESS)
                clipboardManager.setPrimaryClip(clipData)
                // Only show toast on Android < 12 (API 31) to avoid duplicate with system notification
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    Toast.makeText(this, getString(R.string.monero_address_copied), Toast.LENGTH_SHORT).show()
                } else {
                    // Android 12+ shows its own clipboard notification
                    Timber.d("Monero address copied to clipboard (Android 12+)")
                }
            } else {
                Timber.e("ClipboardManager not available")
                Toast.makeText(this, getString(R.string.error_processing_url), Toast.LENGTH_SHORT).show()
            }
        }
        
        alertDialog.show()
    }
    
    /**
     * Set app title in header
     */
    protected fun setAppTitle(titleTextView: TextView?) {
        titleTextView?.let {
            it.text = getString(R.string.app_title)
            Timber.d("App title header set to: ${getString(R.string.app_title)}")
        }
    }
} 