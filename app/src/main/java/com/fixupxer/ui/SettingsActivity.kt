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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.databinding.ActivitySettingsBinding
import com.fixupxer.databinding.DialogConversionDefaultsBinding
import com.fixupxer.ui.adapters.ActionPriorityAdapter
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.Constants
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber
import java.util.Collections
import android.widget.Toast

/**
 * Settings activity for configuring browser mode and other app preferences
 */
@AndroidEntryPoint
class SettingsActivity : BaseActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var actionPriorityAdapter: ActionPriorityAdapter
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
        
        setAppTitle(binding.titleTextView)
        
        setupViews()
        loadSettings()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupViews() {
        // Instagram embed proxy radio group
        binding.radioGroupInstagramProxy.setOnCheckedChangeListener { _, checkedId ->
            val domain = when (checkedId) {
                R.id.radioProxyAdamlikes -> Constants.ADAMLIKES_DOMAIN
                R.id.radioProxyTo -> Constants.TOINSTAGRAM_DOMAIN
                R.id.radioProxy7 -> Constants.INSTAGRAM7_DOMAIN
                else -> Constants.INSTAGRAM_DEFAULT_PROXY
            }
            preferencesManager.setInstagramProxy(domain)
            Timber.d("Instagram proxy changed to: $domain")
        }

        // Instagram proxy info icon -> show tooltip dialog with primary/backup explanation
        binding.instagramProxyInfoIcon.setOnClickListener {
            showInstagramProxyInfoDialog()
        }

        // Browser mode switch
        binding.switchBrowserMode.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBrowserModeEnabled(isChecked)
            BrowserModeUtils.setBrowserAliasEnabled(this, isChecked)
            updateBrowserModeDescription(isChecked)
            Timber.d("Browser mode enabled: $isChecked")
        }
        
        // Read this button
        binding.buttonReadThis.setOnClickListener {
            showInstructionsDialog()
        }
        
        // Action mode radio group
        binding.radioGroupActionMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioAskEveryTime -> PreferencesManager.ACTION_MODE_ASK
                R.id.radioFollowPriority -> PreferencesManager.ACTION_MODE_PRIORITY
                else -> PreferencesManager.ACTION_MODE_ASK
            }
            preferencesManager.setActionMode(mode)
            updateActionPriorityVisibility(mode)
            Timber.d("Action mode changed to: $mode")
        }
        
        // Setup action priority RecyclerView
        setupActionPriorityList()
        
        // Conversion defaults button
        binding.buttonConversionDefaults.setOnClickListener {
            showConversionDefaultsDialog()
        }
    }
    
    private fun loadSettings() {
        // Load Instagram proxy selection (default = toinstagram.com)
        when (preferencesManager.getInstagramProxy()) {
            Constants.ADAMLIKES_DOMAIN -> binding.radioProxyAdamlikes.isChecked = true
            Constants.INSTAGRAM7_DOMAIN -> binding.radioProxy7.isChecked = true
            else -> binding.radioProxyTo.isChecked = true
        }

        // Load browser mode state
        val browserModeEnabled = preferencesManager.isBrowserModeEnabled()
        binding.switchBrowserMode.isChecked = browserModeEnabled
        updateBrowserModeDescription(browserModeEnabled)
        
        // Load action mode
        val actionMode = preferencesManager.getActionMode()
        when (actionMode) {
            PreferencesManager.ACTION_MODE_ASK -> binding.radioAskEveryTime.isChecked = true
            PreferencesManager.ACTION_MODE_PRIORITY -> binding.radioFollowPriority.isChecked = true
        }
        updateActionPriorityVisibility(actionMode)
        
        // Load action priority list
        val actionPriority = preferencesManager.getActionPriority()
        actionPriorityAdapter.updateItems(actionPriority)
    }
    
    private fun setupActionPriorityList() {
        actionPriorityAdapter = ActionPriorityAdapter(
            onItemsReordered = { newOrder ->
                preferencesManager.setActionPriority(newOrder)
                Timber.d("Action priority reordered: $newOrder")
            }
        )
        
        binding.recyclerViewActionPriority.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = actionPriorityAdapter
        }
        
        // Setup drag and drop
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                return makeMovementFlags(dragFlags, 0)
            }
            
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition
                actionPriorityAdapter.moveItem(fromPosition, toPosition)
                return true
            }
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not implemented - no swipe to delete
            }
            
            override fun isLongPressDragEnabled(): Boolean = false
        })
        
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewActionPriority)
        actionPriorityAdapter.setItemTouchHelper(itemTouchHelper)
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun updateBrowserModeDescription(enabled: Boolean) {
        // You could update a description text here if needed
    }
    
    private fun updateActionPriorityVisibility(mode: String) {
        val isPriorityMode = mode == PreferencesManager.ACTION_MODE_PRIORITY
        binding.actionPrioritySection.visibility = if (isPriorityMode) View.VISIBLE else View.GONE
    }
    
    private fun showInstagramProxyInfoDialog() {
        val message = HtmlCompat.fromHtml(
            getString(R.string.instagram_proxy_info_text),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.instagram_proxy_info_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showInstructionsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_instructions, null)
        
        // Set HTML content for each content TextView in the MaterialCardViews
        val scrollView = dialogView as? ScrollView
        val mainLinearLayout = scrollView?.getChildAt(0) as? LinearLayout
        
        if (mainLinearLayout != null) {
            val contentStrings = listOf(
                R.string.instructions_section1_content,
                R.string.instructions_section2_content,
                R.string.instructions_section3_content,
                R.string.instructions_section4_content,
                R.string.instructions_section5_content
            )
            
            var contentIndex = 0
            // Iterate through each MaterialCardView
            for (i in 0 until mainLinearLayout.childCount) {
                val cardView = mainLinearLayout.getChildAt(i)
                if (cardView is com.google.android.material.card.MaterialCardView && contentIndex < contentStrings.size) {
                    // Get the LinearLayout inside the card
                    val cardLinearLayout = cardView.getChildAt(0) as? LinearLayout
                    if (cardLinearLayout != null && cardLinearLayout.childCount >= 2) {
                        // The content TextView is the second child (index 1)
                        val contentTextView = cardLinearLayout.getChildAt(1) as? TextView
                        if (contentTextView != null) {
                            val htmlText = getString(contentStrings[contentIndex])
                            contentTextView.text = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY)
                            contentIndex++
                        }
                    }
                }
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle(R.string.instructions_title)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
    
    private fun showConversionDefaultsDialog() {
        val dialogBinding = DialogConversionDefaultsBinding.inflate(layoutInflater)
        
        // Load current settings
        dialogBinding.switchBrowserTwitter.isChecked = preferencesManager.isBrowserConvertTwitterEnabled()
        dialogBinding.switchBrowserInstagram.isChecked = preferencesManager.isBrowserConvertInstagramEnabled()
        dialogBinding.switchBrowserFacebook.isChecked = preferencesManager.isBrowserConvertFacebookEnabled()
        
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.conversion_defaults_title)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
        
        // Set up button click listeners
        dialogBinding.btnSave.setOnClickListener {
            // Save the settings
            preferencesManager.setBrowserConvertTwitterEnabled(dialogBinding.switchBrowserTwitter.isChecked)
            preferencesManager.setBrowserConvertInstagramEnabled(dialogBinding.switchBrowserInstagram.isChecked)
            preferencesManager.setBrowserConvertFacebookEnabled(dialogBinding.switchBrowserFacebook.isChecked)
            
            Toast.makeText(this, "Browser conversion settings saved", Toast.LENGTH_SHORT).show()
            Timber.d("Browser conversion settings saved - Twitter: ${dialogBinding.switchBrowserTwitter.isChecked}, Instagram: ${dialogBinding.switchBrowserInstagram.isChecked}, Facebook: ${dialogBinding.switchBrowserFacebook.isChecked}")
            dialog.dismiss()
        }
        
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
} 