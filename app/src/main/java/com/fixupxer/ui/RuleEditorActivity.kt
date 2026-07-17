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
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fixupxer.R
import com.fixupxer.databinding.ActivityRuleEditorBinding
import com.fixupxer.databinding.DialogRuleTestVectorBinding
import com.fixupxer.databinding.ItemRuleTestVectorBinding
import com.fixupxer.presentation.rules.RuleEditorViewModel
import com.fixupxer.processing.ProcessingProfile
import com.fixupxer.rules.CustomUrlRule
import com.fixupxer.rules.HostMatchMode
import com.fixupxer.rules.HostScopeEntry
import com.fixupxer.rules.RedirectDecodeMode
import com.fixupxer.rules.ReplaceMode
import com.fixupxer.rules.RuleAction
import com.fixupxer.rules.RuleActivationBlockedException
import com.fixupxer.rules.RuleExampleInferenceRejectionReason
import com.fixupxer.rules.RuleExampleInferenceResult
import com.fixupxer.rules.RulePhase
import com.fixupxer.rules.RuleScope
import com.fixupxer.rules.RuleTestVector
import com.fixupxer.rules.RuleVectorRunResult
import com.fixupxer.utils.Constants
import com.fixupxer.utils.InputValidator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RuleEditorActivity : BaseActivity() {
    companion object {
        const val EXTRA_RULE_ID = "ruleId"
        private const val STATE_VECTOR_INPUTS = "state_vector_inputs"
        private const val STATE_VECTOR_EXPECTED = "state_vector_expected"
        private const val STATE_TEACH_EXPANDED = "state_teach_expanded"
    }

    private lateinit var binding: ActivityRuleEditorBinding
    private val viewModel: RuleEditorViewModel by viewModels()
    private var original: CustomUrlRule? = null
    private var saved = false
    private lateinit var phaseLabels: List<String>
    private lateinit var scopeLabels: List<String>
    private lateinit var actionLabels: List<String>
    private lateinit var decodeModeLabels: List<String>
    private lateinit var profileLabels: List<String>
    private val testVectors = mutableListOf<RuleTestVector>()
    private var vectorRunResult: RuleVectorRunResult? = null
    private var vectorsRestoredFromState = false

    private val isCreatingRule: Boolean
        get() = intent.getStringExtra(EXTRA_RULE_ID) == null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRuleEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(
            if (isCreatingRule) {
                R.string.custom_rule_create_title
            } else {
                R.string.custom_rule_editor_title
            }
        )

        restoreVectors(savedInstanceState)
        setupSpinners()
        setupActions()
        binding.cardTeachExample.isVisible = isCreatingRule
        binding.teachExampleFields.isVisible =
            savedInstanceState?.getBoolean(STATE_TEACH_EXPANDED) ?: false
        renderVectors()
        observeRule()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = confirmDiscard()
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(
            STATE_VECTOR_INPUTS,
            ArrayList(testVectors.map { it.input })
        )
        outState.putStringArrayList(
            STATE_VECTOR_EXPECTED,
            ArrayList(testVectors.map { it.expected })
        )
        outState.putBoolean(STATE_TEACH_EXPANDED, binding.teachExampleFields.isVisible)
    }

    private fun restoreVectors(savedInstanceState: Bundle?) {
        val inputs = savedInstanceState?.getStringArrayList(STATE_VECTOR_INPUTS) ?: return
        val expected = savedInstanceState.getStringArrayList(STATE_VECTOR_EXPECTED) ?: return
        if (inputs.size != expected.size) return
        testVectors.clear()
        inputs.zip(expected).mapTo(testVectors) { (input, exp) -> RuleTestVector(input, exp) }
        vectorsRestoredFromState = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            confirmDiscard()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupSpinners() {
        phaseLabels = labels(
            R.string.custom_rule_phase_pre,
            R.string.custom_rule_phase_post,
            R.string.custom_rule_phase_final
        )
        scopeLabels = labels(
            R.string.custom_rule_scope_all,
            R.string.custom_rule_scope_exact,
            R.string.custom_rule_scope_domain,
            R.string.custom_rule_scope_list,
            R.string.custom_rule_scope_regex
        )
        actionLabels = labels(
            R.string.custom_rule_action_remove_all,
            R.string.custom_rule_action_remove_named,
            R.string.custom_rule_action_keep,
            R.string.custom_rule_action_regex,
            R.string.custom_rule_action_redirect,
            R.string.custom_rule_action_template
        )
        decodeModeLabels = RedirectDecodeMode.entries.map { it.name.replace('_', ' ') }
        profileLabels = ProcessingProfile.entries.map { it.name }

        setupDropdown(binding.spinnerPhase, phaseLabels, onSelected = ::invalidateVectorResults)
        setupDropdown(binding.spinnerScope, scopeLabels) {
            updateConditionalFields()
            invalidateVectorResults()
        }
        setupDropdown(binding.spinnerAction, actionLabels) {
            updateConditionalFields()
            invalidateVectorResults()
        }
        setupDropdown(binding.spinnerDecodeMode, decodeModeLabels, onSelected = ::invalidateVectorResults)
        setupDropdown(binding.spinnerTestProfile, profileLabels)
        updateConditionalFields()

        // Rule-definition edits make a previous Run-all verdict stale.
        listOf(
            binding.editScopeValue,
            binding.editExcludes,
            binding.editActionValue,
            binding.editReplacement
        ).forEach { field -> field.doAfterTextChanged { invalidateVectorResults() } }
        listOf(
            binding.checkMain,
            binding.checkShare,
            binding.checkBrowser,
            binding.checkIgnoreCase,
            binding.checkReplaceAll,
            binding.checkStop
        ).forEach { box -> box.setOnCheckedChangeListener { _, _ -> invalidateVectorResults() } }
    }

    private fun invalidateVectorResults() {
        if (vectorRunResult != null) {
            vectorRunResult = null
            renderVectors()
        }
    }

    private fun setupActions() {
        binding.buttonTeachToggle.setOnClickListener {
            binding.teachExampleFields.isVisible = !binding.teachExampleFields.isVisible
        }
        binding.buttonInferExample.setOnClickListener {
            inferRuleFromExample()
        }
        binding.buttonSave.setOnClickListener {
            val draft = runCatching(::buildRule).getOrElse {
                showValidationError(it)
                return@setOnClickListener
            }
            val vectorResult = runCatching { viewModel.runVectors(draft) }.getOrElse {
                showValidationError(it)
                return@setOnClickListener
            }
            if (draft.enabled && !vectorResult.allPassed) {
                vectorRunResult = vectorResult
                renderVectors()
                showActivationBlocked(draft, vectorResult.failingCount)
            } else {
                saveRule(draft)
            }
        }
        binding.buttonAddTestVector.setOnClickListener { showAddVectorDialog() }
        binding.buttonRunAllTestVectors.setOnClickListener {
            val draft = runCatching(::buildRule).getOrElse {
                showValidationError(it)
                return@setOnClickListener
            }
            runCatching { viewModel.runVectors(draft) }
                .onSuccess {
                    vectorRunResult = it
                    renderVectors()
                }
                .onFailure(::showValidationError)
        }
        binding.buttonRunTest.setOnClickListener {
            val url = binding.editTestUrl.text?.toString().orEmpty()
            val draft = runCatching(::buildRule).getOrElse {
                showValidationError(it)
                return@setOnClickListener
            }
            lifecycleScope.launch {
                runCatching {
                    viewModel.preview(
                        draft,
                        url,
                        ProcessingProfile.entries[
                            selectedIndex(binding.spinnerTestProfile, profileLabels)
                        ]
                    )
                }.onSuccess { result ->
                    val trace = result.trace.joinToString("\n") {
                        "${it.phase.name}: ${it.ruleName} — ${it.status.name}"
                    }
                    binding.textTestResult.text = buildString {
                        append(getString(R.string.custom_rule_test_result, result.url))
                        append("\n\n")
                        append(getString(R.string.custom_rule_test_trace, trace))
                    }
                    binding.textTestResult.isVisible = true
                }.onFailure(::showValidationError)
            }
        }
        binding.buttonDelete.setOnClickListener {
            val rule = original ?: return@setOnClickListener
            MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.custom_rule_delete_confirm, rule.name))
                .setPositiveButton(R.string.custom_rule_delete) { _, _ ->
                    lifecycleScope.launch {
                        viewModel.delete(rule.id)
                        saved = true
                        finish()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        binding.buttonDuplicate.setOnClickListener {
            val rule = original ?: return@setOnClickListener
            lifecycleScope.launch {
                val duplicate = viewModel.duplicate(rule.id)
                saved = true
                startActivity(Intent(this@RuleEditorActivity, RuleEditorActivity::class.java).apply {
                    putExtra(EXTRA_RULE_ID, duplicate.id)
                })
                finish()
            }
        }
    }

    private fun inferRuleFromExample() {
        lifecycleScope.launch {
            binding.layoutExampleBefore.error = null
            binding.layoutExampleDesired.error = null
            val before = InputValidator.validateAndSanitizeInput(
                binding.editExampleBefore.text?.toString().orEmpty()
            )
            val desired = InputValidator.validateAndSanitizeInput(
                binding.editExampleDesired.text?.toString().orEmpty()
            )
            var valid = true
            if (before == null) {
                binding.layoutExampleBefore.error =
                    getString(R.string.custom_rule_example_invalid_before)
                valid = false
            }
            if (desired == null) {
                binding.layoutExampleDesired.error =
                    getString(R.string.custom_rule_example_invalid_desired)
                valid = false
            }
            if (!valid) return@launch

            when (val result = viewModel.inferExample(requireNotNull(before), requireNotNull(desired))) {
                is RuleExampleInferenceResult.Inferred -> {
                    val inferred = result.draft.copy(
                        name = getString(
                            R.string.custom_rule_example_name,
                            (result.draft.includeScope as RuleScope.ExactHost).host
                        )
                    )
                    if (testVectors.size >= Constants.MAX_TEST_VECTORS_PER_RULE &&
                        inferred.testVectors.single() !in testVectors
                    ) {
                        Snackbar.make(
                            binding.root,
                            R.string.custom_rule_vector_limit_reached,
                            Snackbar.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    val redundant = runCatching {
                        viewModel.isExampleRedundant(requireNotNull(before), requireNotNull(desired))
                    }.getOrElse {
                        showValidationError(it)
                        return@launch
                    }
                    if (redundant) {
                        Snackbar.make(
                            binding.root,
                            R.string.custom_rule_example_redundant,
                            Snackbar.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    populateInferredDraft(inferred)
                    Snackbar.make(
                        binding.root,
                        R.string.custom_rule_example_inferred,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                is RuleExampleInferenceResult.Rejected -> {
                    Snackbar.make(
                        binding.root,
                        exampleRejectionMessage(result.reason),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun populateInferredDraft(rule: CustomUrlRule) {
        populateFields(rule)
        rule.testVectors.forEach { vector ->
            if (vector !in testVectors) testVectors += vector
        }
        vectorRunResult = null
        renderVectors()
        updateConditionalFields()
    }

    private fun saveRule(draft: CustomUrlRule) {
        lifecycleScope.launch {
            runCatching { viewModel.save(draft) }
                .onSuccess {
                    saved = true
                    Snackbar.make(binding.root, R.string.custom_rule_saved, Snackbar.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure { error ->
                    if (error is RuleActivationBlockedException) {
                        showActivationBlocked(draft, error.failingVectorCount)
                    } else {
                        showValidationError(error)
                    }
                }
        }
    }

    private fun showActivationBlocked(draft: CustomUrlRule, failingCount: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.custom_rule_activation_blocked_title)
            .setMessage(getString(R.string.custom_rule_activation_blocked_message, failingCount))
            .setPositiveButton(R.string.custom_rule_save_disabled_draft) { _, _ ->
                saveRule(draft.copy(enabled = false))
            }
            .setNegativeButton(R.string.custom_rule_fix_now, null)
            .show()
    }

    private fun showAddVectorDialog() {
        val dialogBinding = DialogRuleTestVectorBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.custom_rule_vector_add)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.add, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                lifecycleScope.launch {
                    dialogBinding.layoutVectorInput.error = null
                    dialogBinding.layoutVectorExpected.error = null
                    val input = InputValidator.validateAndSanitizeInput(
                        dialogBinding.editVectorInput.text?.toString().orEmpty()
                    )
                    val expected = InputValidator.validateAndSanitizeInput(
                        dialogBinding.editVectorExpected.text?.toString().orEmpty()
                    )
                    var valid = true
                    if (input == null) {
                        dialogBinding.layoutVectorInput.error =
                            getString(R.string.custom_rule_vector_invalid_input)
                        valid = false
                    }
                    if (expected == null) {
                        dialogBinding.layoutVectorExpected.error =
                            getString(R.string.custom_rule_vector_invalid_expected)
                        valid = false
                    }
                    if (valid) {
                        testVectors += RuleTestVector(requireNotNull(input), requireNotNull(expected))
                        vectorRunResult = null
                        renderVectors()
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun renderVectors() {
        binding.vectorList.removeAllViews()
        testVectors.forEachIndexed { index, vector ->
            val row = ItemRuleTestVectorBinding.inflate(layoutInflater, binding.vectorList, false)
            val result = vectorRunResult?.results?.getOrNull(index)
            row.textVectorPair.text = getString(
                R.string.custom_rule_vector_pair,
                vector.input,
                vector.expected
            )
            row.imageVectorStatus.isVisible = result != null
            if (result != null) {
                row.imageVectorStatus.setImageResource(
                    if (result.passed) R.drawable.ic_check else R.drawable.ic_error
                )
            }
            row.buttonDeleteTestVector.setOnClickListener {
                testVectors.removeAt(index)
                vectorRunResult = null
                renderVectors()
            }
            binding.vectorList.addView(row.root)
        }

        val vectorCount = testVectors.size
        val limitReached = vectorCount >= Constants.MAX_TEST_VECTORS_PER_RULE
        binding.buttonAddTestVector.isEnabled = !limitReached
        binding.buttonRunAllTestVectors.isEnabled = vectorCount > 0
        binding.textVectorLimit.text = if (limitReached) {
            getString(R.string.custom_rule_vector_limit_reached)
        } else {
            getString(
                R.string.custom_rule_vector_count,
                vectorCount,
                Constants.MAX_TEST_VECTORS_PER_RULE
            )
        }
        binding.textVectorSummary.isVisible = vectorRunResult != null
        vectorRunResult?.let {
            binding.textVectorSummary.text = getString(
                R.string.custom_rule_vector_summary,
                it.passedCount,
                it.results.size
            )
        }
    }

    private fun observeRule() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rule.collect { rule ->
                    if (rule != null && original == null) {
                        original = rule
                        populate(rule)
                        binding.buttonDelete.isVisible = true
                        binding.buttonDuplicate.isVisible = true
                    }
                }
            }
        }
    }

    private fun populate(rule: CustomUrlRule) {
        populateFields(rule)
        if (!vectorsRestoredFromState) {
            testVectors.clear()
            testVectors.addAll(rule.testVectors)
            vectorRunResult = runCatching { viewModel.runVectors(rule) }.getOrNull()
        }
        renderVectors()
        updateConditionalFields()
    }

    private fun populateFields(rule: CustomUrlRule) {
        binding.editName.setText(rule.name)
        binding.switchEnabled.isChecked = rule.enabled
        setDropdownSelection(binding.spinnerPhase, phaseLabels, rule.phase.ordinal)
        binding.checkMain.isChecked = ProcessingProfile.MAIN in rule.contexts
        binding.checkShare.isChecked = ProcessingProfile.SHARE in rule.contexts
        binding.checkBrowser.isChecked = ProcessingProfile.BROWSER in rule.contexts
        setDropdownSelection(binding.spinnerScope, scopeLabels, scopeIndex(rule.includeScope))
        binding.editScopeValue.setText(scopeValue(rule.includeScope))
        binding.editExcludes.setText(rule.excludeScopes.joinToString("\n", transform = ::scopeLine))
        setDropdownSelection(binding.spinnerAction, actionLabels, actionIndex(rule.action))
        populateAction(rule.action)
        binding.checkStop.isChecked = rule.stopAfterMatch
    }

    private fun populateAction(action: RuleAction) {
        when (action) {
            RuleAction.RemoveAllParams -> Unit
            is RuleAction.RemoveParams -> {
                binding.editActionValue.setText(action.names.joinToString("\n"))
                binding.checkIgnoreCase.isChecked = action.ignoreCase
            }
            is RuleAction.KeepOnlyParams -> {
                binding.editActionValue.setText(action.names.joinToString("\n"))
                binding.checkIgnoreCase.isChecked = action.ignoreCase
            }
            is RuleAction.RegexReplace -> {
                binding.editActionValue.setText(action.pattern)
                binding.editReplacement.setText(action.replacement)
                binding.checkIgnoreCase.isChecked = action.ignoreCase
                binding.checkReplaceAll.isChecked = action.mode == ReplaceMode.ALL
            }
            is RuleAction.ExtractRedirect -> {
                binding.editActionValue.setText(action.parameterName)
                binding.checkIgnoreCase.isChecked = action.ignoreCase
                setDropdownSelection(
                    binding.spinnerDecodeMode,
                    decodeModeLabels,
                    action.decodeMode.ordinal
                )
            }
            is RuleAction.TemplateRewrite -> binding.editActionValue.setText(action.template)
        }
    }

    private fun buildRule(): CustomUrlRule {
        val previous = original
        val contexts = buildSet {
            if (binding.checkMain.isChecked) add(ProcessingProfile.MAIN)
            if (binding.checkShare.isChecked) add(ProcessingProfile.SHARE)
            if (binding.checkBrowser.isChecked) add(ProcessingProfile.BROWSER)
        }
        val include = parseScope(
            selectedIndex(binding.spinnerScope, scopeLabels),
            binding.editScopeValue.text?.toString().orEmpty()
        )
        val excludes = binding.editExcludes.text?.toString().orEmpty()
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(::parseExclude)
            .toList()
        val action = parseAction()
        return CustomUrlRule(
            id = previous?.id ?: java.util.UUID.randomUUID().toString(),
            name = binding.editName.text?.toString().orEmpty(),
            enabled = binding.switchEnabled.isChecked,
            sortOrder = previous?.sortOrder ?: Int.MAX_VALUE,
            phase = RulePhase.entries[selectedIndex(binding.spinnerPhase, phaseLabels)],
            contexts = contexts,
            includeScope = include,
            excludeScopes = excludes,
            action = action,
            stopAfterMatch = binding.checkStop.isChecked,
            testVectors = testVectors.toList(),
            createdAt = previous?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun parseAction(): RuleAction {
        val value = binding.editActionValue.text?.toString().orEmpty()
        val ignoreCase = binding.checkIgnoreCase.isChecked
        return when (selectedIndex(binding.spinnerAction, actionLabels)) {
            0 -> RuleAction.RemoveAllParams
            1 -> RuleAction.RemoveParams(parseLines(value), ignoreCase)
            2 -> RuleAction.KeepOnlyParams(parseLines(value), ignoreCase)
            3 -> RuleAction.RegexReplace(
                pattern = value,
                replacement = binding.editReplacement.text?.toString().orEmpty(),
                mode = if (binding.checkReplaceAll.isChecked) ReplaceMode.ALL else ReplaceMode.FIRST,
                ignoreCase = ignoreCase
            )
            4 -> RuleAction.ExtractRedirect(
                parameterName = value.trim(),
                ignoreCase = ignoreCase,
                decodeMode = RedirectDecodeMode.entries[
                    selectedIndex(binding.spinnerDecodeMode, decodeModeLabels)
                ]
            )
            else -> RuleAction.TemplateRewrite(value)
        }
    }

    private fun parseScope(index: Int, value: String): RuleScope = when (index) {
        0 -> RuleScope.AllUrls
        1 -> RuleScope.ExactHost(value.trim())
        2 -> RuleScope.DomainAndSubdomains(value.trim())
        3 -> RuleScope.HostList(
            value.lineSequence()
                .flatMap { it.split(',').asSequence() }
                .map(String::trim)
                .filter(String::isNotEmpty)
                .map {
                    if (it.startsWith("=")) {
                        HostScopeEntry(it.removePrefix("="), HostMatchMode.EXACT)
                    } else {
                        HostScopeEntry(it, HostMatchMode.DOMAIN_AND_SUBDOMAINS)
                    }
                }
                .toList()
        )
        else -> RuleScope.UrlRegex(value, binding.checkIgnoreCase.isChecked)
    }

    private fun parseExclude(value: String): RuleScope = when {
        value.startsWith("regex:", ignoreCase = true) ->
            RuleScope.UrlRegex(value.substringAfter(':'), binding.checkIgnoreCase.isChecked)
        value.startsWith("=") -> RuleScope.ExactHost(value.removePrefix("="))
        else -> RuleScope.DomainAndSubdomains(value)
    }

    private fun updateConditionalFields() {
        val scope = selectedIndex(binding.spinnerScope, scopeLabels)
        binding.layoutScopeValue.isVisible = scope != 0
        binding.layoutScopeValue.hint = getString(
            when (scope) {
                1, 2 -> R.string.custom_rule_scope_host_value
                3 -> R.string.custom_rule_scope_hosts_value
                else -> R.string.custom_rule_scope_regex_value
            }
        )
        val action = selectedIndex(binding.spinnerAction, actionLabels)
        binding.layoutActionValue.isVisible = action != 0
        binding.layoutActionValue.hint = getString(
            when (action) {
                1, 2 -> R.string.custom_rule_action_parameters_value
                3 -> R.string.custom_rule_action_regex_value
                4 -> R.string.custom_rule_action_redirect_value
                else -> R.string.custom_rule_action_template_value
            }
        )
        binding.layoutReplacement.isVisible = action == 3
        binding.checkReplaceAll.isVisible = action == 3
        binding.layoutDecodeMode.isVisible = action == 4
        binding.checkIgnoreCase.isVisible = action in 1..4 ||
            scope == 4
    }

    private fun confirmDiscard() {
        if (saved || !hasUnsavedChanges()) {
            finish()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.custom_rule_unsaved_changes)
            .setPositiveButton(R.string.discard) { _, _ -> finish() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun hasUnsavedChanges(): Boolean {
        val initial = original ?: return !binding.editName.text.isNullOrBlank() || testVectors.isNotEmpty()
        return runCatching {
            buildRule().copy(updatedAt = initial.updatedAt) != initial
        }.getOrDefault(true)
    }

    private fun showValidationError(error: Throwable) {
        Snackbar.make(
            binding.root,
            getString(R.string.custom_rule_validation_error, error.message.orEmpty()),
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun exampleRejectionMessage(reason: RuleExampleInferenceRejectionReason): Int = when (reason) {
        RuleExampleInferenceRejectionReason.INVALID_URL ->
            R.string.custom_rule_example_reject_invalid_url
        RuleExampleInferenceRejectionReason.IDENTICAL_URLS ->
            R.string.custom_rule_example_reject_identical
        RuleExampleInferenceRejectionReason.DIFFERENT_SCHEME ->
            R.string.custom_rule_example_reject_different_scheme
        RuleExampleInferenceRejectionReason.DIFFERENT_HOST ->
            R.string.custom_rule_example_reject_different_host
        RuleExampleInferenceRejectionReason.DIFFERENT_PORT ->
            R.string.custom_rule_example_reject_different_port
        RuleExampleInferenceRejectionReason.DIFFERENT_PATH ->
            R.string.custom_rule_example_reject_different_path
        RuleExampleInferenceRejectionReason.DIFFERENT_FRAGMENT ->
            R.string.custom_rule_example_reject_different_fragment
        RuleExampleInferenceRejectionReason.AMBIGUOUS_DUPLICATES ->
            R.string.custom_rule_example_reject_ambiguous_duplicates
        RuleExampleInferenceRejectionReason.CHANGED_VALUES ->
            R.string.custom_rule_example_reject_changed_values
        RuleExampleInferenceRejectionReason.REORDERED_TOKENS ->
            R.string.custom_rule_example_reject_reordered
        RuleExampleInferenceRejectionReason.MULTIPLE_REDIRECT_CANDIDATES ->
            R.string.custom_rule_example_reject_multiple_redirects
        RuleExampleInferenceRejectionReason.INVALID_REDIRECT_OUTPUT ->
            R.string.custom_rule_example_reject_invalid_redirect
        RuleExampleInferenceRejectionReason.NO_SAFE_INFERENCE ->
            R.string.custom_rule_example_reject_no_safe
    }

    private fun labels(vararg ids: Int): List<String> = ids.map(::getString)

    private fun setupDropdown(
        dropdown: MaterialAutoCompleteTextView,
        values: List<String>,
        selectedIndex: Int = 0,
        onSelected: (() -> Unit)? = null
    ) {
        dropdown.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, values)
        )
        setDropdownSelection(dropdown, values, selectedIndex)
        dropdown.setOnItemClickListener { _, _, _, _ -> onSelected?.invoke() }
    }

    private fun selectedIndex(
        dropdown: MaterialAutoCompleteTextView,
        values: List<String>
    ): Int = values.indexOf(dropdown.text.toString()).coerceAtLeast(0)

    private fun setDropdownSelection(
        dropdown: MaterialAutoCompleteTextView,
        values: List<String>,
        index: Int
    ) {
        dropdown.setText(values[index], false)
    }

    private fun parseLines(value: String): List<String> =
        value.lineSequence()
            .flatMap { it.split(',').asSequence() }
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()

    private fun scopeIndex(scope: RuleScope): Int = when (scope) {
        RuleScope.AllUrls -> 0
        is RuleScope.ExactHost -> 1
        is RuleScope.DomainAndSubdomains -> 2
        is RuleScope.HostList -> 3
        is RuleScope.UrlRegex -> 4
    }

    private fun scopeValue(scope: RuleScope): String = when (scope) {
        RuleScope.AllUrls -> ""
        is RuleScope.ExactHost -> scope.host
        is RuleScope.DomainAndSubdomains -> scope.host
        is RuleScope.HostList -> scope.entries.joinToString("\n") {
            if (it.mode == HostMatchMode.EXACT) "=${it.host}" else it.host
        }
        is RuleScope.UrlRegex -> scope.pattern
    }

    private fun scopeLine(scope: RuleScope): String = when (scope) {
        RuleScope.AllUrls -> ""
        is RuleScope.ExactHost -> "=${scope.host}"
        is RuleScope.DomainAndSubdomains -> scope.host
        is RuleScope.HostList -> scope.entries.joinToString(",") { it.host }
        is RuleScope.UrlRegex -> "regex:${scope.pattern}"
    }

    private fun actionIndex(action: RuleAction): Int = when (action) {
        RuleAction.RemoveAllParams -> 0
        is RuleAction.RemoveParams -> 1
        is RuleAction.KeepOnlyParams -> 2
        is RuleAction.RegexReplace -> 3
        is RuleAction.ExtractRedirect -> 4
        is RuleAction.TemplateRewrite -> 5
    }

}
