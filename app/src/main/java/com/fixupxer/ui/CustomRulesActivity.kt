// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.fixupxer.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fixupxer.BuildConfig
import com.fixupxer.R
import com.fixupxer.databinding.ActivityCustomRulesBinding
import com.fixupxer.presentation.rules.CustomRulesViewModel
import com.fixupxer.rules.ImportMode
import com.fixupxer.ui.adapters.CustomRuleAdapter
import com.fixupxer.utils.Constants
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.io.ByteArrayOutputStream
import java.io.InputStream

@AndroidEntryPoint
class CustomRulesActivity : BaseActivity() {
    private lateinit var binding: ActivityCustomRulesBinding
    private val viewModel: CustomRulesViewModel by viewModels()
    private lateinit var adapter: CustomRuleAdapter
    private var pendingExport: String? = null

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        uri -> if (uri != null) importFrom(uri)
    }
    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingExport
        pendingExport = null
        if (uri != null && json != null) writeExport(uri, json)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomRulesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = CustomRuleAdapter(
            onEdit = { openEditor(it.id) },
            onEnabled = viewModel::setRuleEnabled,
            onReordered = { phase, ids ->
                lifecycleScope.launch { viewModel.reorder(phase, ids) }
            }
        )
        binding.recyclerRules.layoutManager = LinearLayoutManager(this)
        binding.recyclerRules.adapter = adapter
        setupDrag()
        setupActions()
        observeState()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupActions() {
        binding.switchCustomRules.setOnCheckedChangeListener { _, checked ->
            viewModel.setEnabled(checked)
        }
        binding.buttonAddRule.setOnClickListener { openEditor(null) }
        binding.buttonImport.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.custom_rules_private_warning)
                .setPositiveButton(R.string.custom_rules_import) { _, _ ->
                    openDocument.launch(arrayOf("application/json", "text/plain"))
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        binding.buttonExport.setOnClickListener {
            lifecycleScope.launch {
                pendingExport = viewModel.exportJson()
                createDocument.launch("fixupxer-rules-${BuildConfig.VERSION_NAME}.json")
            }
        }
        binding.buttonRollback.setOnClickListener {
            lifecycleScope.launch {
                val message = if (viewModel.rollback()) {
                    R.string.custom_rules_rollback_done
                } else {
                    R.string.custom_rules_rollback_empty
                }
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            }
        }
        binding.buttonTemplates.setOnClickListener {
            val labels = arrayOf(
                getString(R.string.custom_rules_template_privacy),
                getString(R.string.custom_rules_template_redirects)
            )
            val files = arrayOf("privacy_basics.json", "offline_redirects.json")
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.custom_rules_templates)
                .setItems(labels) { _, index ->
                    lifecycleScope.launch {
                        runCatching {
                            val json = assets.open("rule_templates/${files[index]}")
                                .bufferedReader()
                                .use { it.readText() }
                            viewModel.importJson(json, ImportMode.ADD_NEW)
                        }.onSuccess {
                            Snackbar.make(
                                binding.root,
                                getString(
                                    R.string.custom_rules_imported,
                                    it.added,
                                    it.updated,
                                    it.skipped
                                ),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }.onFailure(::showImportError)
                    }
                }
                .show()
        }
        binding.buttonClear.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setMessage(R.string.custom_rules_clear_confirm)
                .setPositiveButton(R.string.custom_rules_clear) { _, _ ->
                    lifecycleScope.launch { viewModel.clear() }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.rules.collectLatest { rules ->
                        adapter.submitRules(rules)
                        binding.textEmpty.isVisible = rules.isEmpty()
                        binding.recyclerRules.isVisible = rules.isNotEmpty()
                        binding.textRuleCount.text = getString(
                            R.string.custom_rules_count,
                            rules.count { it.enabled }
                        )
                    }
                }
                launch {
                    viewModel.enabled.collectLatest { enabled ->
                        if (binding.switchCustomRules.isChecked != enabled) {
                            binding.switchCustomRules.isChecked = enabled
                        }
                    }
                }
            }
        }
    }

    private fun setupDrag() {
        val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = adapter.move(
                viewHolder.bindingAdapterPosition,
                target.bindingAdapterPosition
            )

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
            override fun isLongPressDragEnabled(): Boolean = false
        })
        helper.attachToRecyclerView(binding.recyclerRules)
        adapter.attachTouchHelper(helper)
    }

    private fun openEditor(id: String?) {
        startActivity(Intent(this, RuleEditorActivity::class.java).apply {
            id?.let { putExtra(RuleEditorActivity.EXTRA_RULE_ID, it) }
        })
    }

    private fun importFrom(uri: Uri) {
        lifecycleScope.launch {
            val json = runCatching {
                contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                    require(descriptor.length < 0 || descriptor.length <= Constants.MAX_RULE_BUNDLE_BYTES)
                }
                contentResolver.openInputStream(uri)?.use { stream ->
                    String(readLimited(stream), StandardCharsets.UTF_8)
                } ?: error("Cannot open selected file")
            }.getOrElse {
                showImportError(it)
                return@launch
            }
            showImportMode(json)
        }
    }

    private suspend fun showImportMode(json: String) {
        val modeNames = arrayOf(
            getString(R.string.custom_rules_import_add),
            getString(R.string.custom_rules_import_update),
            getString(R.string.custom_rules_import_replace)
        )
        val previews = runCatching {
            ImportMode.entries.map { viewModel.previewImport(json, it) }
        }.getOrElse {
            showImportError(it)
            return
        }
        val labels = modeNames.mapIndexed { index, name ->
            val preview = previews[index]
            getString(
                R.string.custom_rules_import_preview,
                name,
                preview.added,
                preview.updated,
                preview.skipped
            )
        }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.custom_rules_import_mode)
            .setItems(labels) { _, index ->
                val mode = ImportMode.entries[index]
                lifecycleScope.launch {
                    runCatching { viewModel.importJson(json, mode) }
                        .onSuccess {
                            Snackbar.make(
                                binding.root,
                                getString(
                                    R.string.custom_rules_imported,
                                    it.added,
                                    it.updated,
                                    it.skipped
                                ),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                        .onFailure(::showImportError)
                }
            }
            .show()
    }

    private fun showImportError(error: Throwable) {
        Snackbar.make(
            binding.root,
            getString(R.string.error_rule_import, error.message.orEmpty()),
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun writeExport(uri: Uri, json: String) {
        runCatching {
            contentResolver.openOutputStream(uri, "w")?.bufferedWriter()?.use { it.write(json) }
                ?: error("Cannot open destination")
        }.onSuccess {
            Snackbar.make(binding.root, R.string.custom_rules_exported, Snackbar.LENGTH_SHORT).show()
        }.onFailure {
            Snackbar.make(
                binding.root,
                getString(R.string.error_rule_export, it.message.orEmpty()),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun readLimited(stream: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            require(output.size() <= Constants.MAX_RULE_BUNDLE_BYTES) {
                getString(R.string.error_rule_file_too_large)
            }
        }
        return output.toByteArray()
    }
}
