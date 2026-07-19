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

package com.fixupxer.presentation.rules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixupxer.PreferencesManager
import com.fixupxer.domain.repository.UrlRepository
import com.fixupxer.processing.BrowserConversionPolicy
import com.fixupxer.processing.ProcessingOptions
import com.fixupxer.processing.ProcessingProfile
import com.fixupxer.processing.ProxySelections
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.processing.UrlProcessingOrchestrator
import com.fixupxer.rules.CustomRuleRepository
import com.fixupxer.rules.CustomUrlRule
import com.fixupxer.rules.ImportMode
import com.fixupxer.rules.ImportResult
import com.fixupxer.rules.RuleCompiler
import com.fixupxer.rules.RuleExampleInference
import com.fixupxer.rules.RuleExampleInferenceResult
import com.fixupxer.rules.RulePhase
import com.fixupxer.rules.RuleSnapshot
import com.fixupxer.rules.RuleVectorRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomRulesViewModel @Inject constructor(
    private val repository: CustomRuleRepository
) : ViewModel() {
    val rules: StateFlow<List<CustomUrlRule>> = repository.observeRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val enabled: StateFlow<Boolean> = repository.enabledFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.isEnabled())

    fun setEnabled(enabled: Boolean) = repository.setEnabled(enabled)

    suspend fun getRulesSnapshot(): List<CustomUrlRule> = repository.getRules()

    suspend fun setRuleEnabled(rule: CustomUrlRule, enabled: Boolean) =
        repository.save(rule.copy(enabled = enabled))

    suspend fun reorder(phase: RulePhase, ids: List<String>) = repository.reorder(phase, ids)
    suspend fun delete(id: String) = repository.delete(id)
    suspend fun clear() = repository.clear()
    suspend fun duplicate(id: String): CustomUrlRule = repository.duplicate(id)
    suspend fun exportJson(): String = repository.exportRules(repository.getRules())
    suspend fun importJson(json: String, mode: ImportMode): ImportResult =
        repository.importBundle(json, mode)
    suspend fun previewImport(json: String, mode: ImportMode): ImportResult =
        repository.previewImport(json, mode)
    suspend fun rollback(): Boolean = repository.rollbackLatest()
}

@HiltViewModel
class RuleEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CustomRuleRepository,
    private val compiler: RuleCompiler,
    private val vectorRunner: RuleVectorRunner,
    private val exampleInference: RuleExampleInference,
    private val orchestrator: UrlProcessingOrchestrator,
    private val preferences: PreferencesManager,
    private val urlRepository: UrlRepository
) : ViewModel() {
    private val _rule = MutableStateFlow<CustomUrlRule?>(null)
    val rule: StateFlow<CustomUrlRule?> = _rule

    init {
        val id: String? = savedStateHandle["ruleId"]
        if (!id.isNullOrBlank()) {
            viewModelScope.launch { _rule.value = repository.getRule(id) }
        }
    }

    suspend fun save(rule: CustomUrlRule) {
        compiler.compile(rule)
        repository.save(rule)
        _rule.value = rule
    }

    suspend fun duplicate(id: String): CustomUrlRule = repository.duplicate(id)
    suspend fun delete(id: String) = repository.delete(id)
    fun runVectors(rule: CustomUrlRule) = vectorRunner.run(rule)
    fun inferExample(before: String, desired: String): RuleExampleInferenceResult =
        exampleInference.infer(before, desired)

    suspend fun isExampleRedundant(before: String, desired: String): Boolean =
        processExistingRules(before).url == desired

    suspend fun preview(rule: CustomUrlRule, url: String, profile: ProcessingProfile) =
        repository.getRules().let { stored ->
            val previewRules = if (stored.any { it.id == rule.id }) {
                stored.map { if (it.id == rule.id) rule else it }
            } else {
                stored + rule.copy(
                    sortOrder = stored.filter { it.phase == rule.phase }
                        .maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
                )
            }
            val snapshot = RuleSnapshot(compiler.compileAll(previewRules), repository.revision.value)
            // Test Lab intentionally ignores the master toggle: the user is
            // inspecting a draft and expects it to run.
            processWithSnapshot(url, profile, snapshot, customRulesEnabled = true)
        }

    private suspend fun processExistingRules(url: String) =
        repository.getRules().let { stored ->
            val snapshot = RuleSnapshot(compiler.compileAll(stored), repository.revision.value)
            // Redundancy check must mirror the real MAIN pipeline, including
            // the custom-rules master toggle.
            processWithSnapshot(
                url,
                ProcessingProfile.MAIN,
                snapshot,
                customRulesEnabled = preferences.areCustomRulesEnabled()
            )
        }

    private suspend fun processWithSnapshot(
        url: String,
        profile: ProcessingProfile,
        snapshot: RuleSnapshot,
        customRulesEnabled: Boolean
    ) = orchestrator.process(
        rawInput = url,
        options = ProcessingOptions(
            profile = profile,
            cleanTracking = urlRepository.isInstagramUrl(url) || preferences.isCleanTrackingEnabled(),
            convertDomains = shouldConvertDomains(url, profile),
            proxySelections = when (profile) {
                ProcessingProfile.BROWSER ->
                    ProxySelections(preferences.resolveBrowserPrivacySelections())
                ProcessingProfile.MAIN, ProcessingProfile.SHARE ->
                    ProxySelections(
                        ProxyPlatform.entries.associateWith { preferences.getSelectedProxyDomain(it) },
                    )
            },
            customRulesEnabled = customRulesEnabled,
            persistHistory = false,
            useCache = false,
            traceEnabled = true
        ),
        snapshotOverride = snapshot
    )

    private fun shouldConvertDomains(url: String, profile: ProcessingProfile): Boolean {
        val isInstagram = urlRepository.isInstagramUrl(url)
        val isFacebook = urlRepository.isFacebookUrl(url)
        val isTwitter = urlRepository.isTwitterUrl(url)
        val isTikTok = urlRepository.isTikTokUrl(url)
        val isBluesky = urlRepository.isBlueskyUrl(url)
        val isReddit = urlRepository.isRedditUrl(url)
        val isYouTube = urlRepository.isYouTubeUrl(url)
        val isPinterest = urlRepository.isPinterestUrl(url)
        val isThreads = urlRepository.isThreadsUrl(url)
        val browserPlatform = when {
            isInstagram -> ProxyPlatform.INSTAGRAM
            isFacebook -> ProxyPlatform.FACEBOOK
            isTwitter -> ProxyPlatform.X
            isTikTok -> ProxyPlatform.TIKTOK
            isBluesky -> ProxyPlatform.BLUESKY
            isReddit -> ProxyPlatform.REDDIT
            isYouTube -> ProxyPlatform.YOUTUBE
            isPinterest -> ProxyPlatform.PINTEREST
            isThreads -> ProxyPlatform.THREADS
            else -> null
        }
        return when (profile) {
            ProcessingProfile.MAIN, ProcessingProfile.SHARE -> when {
                isInstagram -> preferences.isConvertInstagramEnabled()
                isFacebook -> preferences.isConvertFacebookEnabled()
                isTikTok -> preferences.isConvertTikTokEnabled()
                isBluesky -> preferences.isConvertBlueskyEnabled()
                isReddit -> preferences.isConvertRedditEnabled()
                isYouTube -> preferences.isConvertYoutubeEnabled()
                isPinterest -> preferences.isConvertPinterestEnabled()
                isThreads -> preferences.isConvertThreadsEnabled()
                else -> preferences.isConvertTwitterEnabled()
            }
            ProcessingProfile.BROWSER -> BrowserConversionPolicy.shouldConvert(
                platform = browserPlatform,
                toggleEnabled = browserPlatform?.let {
                    preferences.isBrowserPrivacyConversionEnabled(it)
                } == true,
                hasActiveTarget = browserPlatform?.let {
                    preferences.resolveBrowserPrivacyTarget(it)
                } != null,
            )
        }
    }
}
