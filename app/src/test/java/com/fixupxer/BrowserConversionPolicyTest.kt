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

package com.fixupxer

import androidx.lifecycle.SavedStateHandle
import com.fixupxer.domain.repository.UrlRepository
import com.fixupxer.presentation.rules.RuleEditorViewModel
import com.fixupxer.processing.BrowserConversionPolicy
import com.fixupxer.processing.PipelineProcessingResult
import com.fixupxer.processing.ProcessingOptions
import com.fixupxer.processing.ProcessingProfile
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.processing.UrlProcessingOrchestrator
import com.fixupxer.rules.CustomRuleRepository
import com.fixupxer.rules.CustomUrlRule
import com.fixupxer.rules.RuleAction
import com.fixupxer.rules.RuleCompiler
import com.fixupxer.rules.RuleExampleInference
import com.fixupxer.rules.RuleVectorRunner
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.ProxyPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BrowserConversionPolicyTest {

    @Test
    fun `policy matrix requires reader platform enabled toggle and active target`() {
        ProxyPlatform.entries.forEach { platform ->
            listOf(false, true).forEach { toggleEnabled ->
                listOf(false, true).forEach { hasActiveTarget ->
                    val expected =
                        platform in AlternativeFrontendCatalog.privacyCapablePlatforms() &&
                            toggleEnabled &&
                            hasActiveTarget
                    assertEquals(
                        "$platform toggle=$toggleEnabled target=$hasActiveTarget",
                        expected,
                        BrowserConversionPolicy.shouldConvert(
                            platform,
                            toggleEnabled,
                            hasActiveTarget,
                        ),
                    )
                }
            }
        }
        assertFalse(BrowserConversionPolicy.shouldConvert(null, true, true))
    }

    @Test
    fun `test lab browser preview delegates conversion decision to shared policy`() = runBlocking {
        val repository: CustomRuleRepository = mock()
        val orchestrator: UrlProcessingOrchestrator = mock()
        val preferences: PreferencesManager = mock()
        val urlRepository: UrlRepository = mock()
        val target = AlternativeFrontendCatalog.builtInReaders(ProxyPlatform.X).first()
        val rule = CustomUrlRule(name = "Preview", action = RuleAction.RemoveAllParams)

        whenever(repository.getRules()).thenReturn(emptyList())
        whenever(repository.revision).thenReturn(MutableStateFlow(0L))
        whenever(urlRepository.isTwitterUrl(URL)).thenReturn(true)
        whenever(preferences.isBrowserPrivacyConversionEnabled(ProxyPlatform.X)).thenReturn(true)
        whenever(preferences.resolveBrowserPrivacyTarget(ProxyPlatform.X)).thenReturn(target)
        whenever(preferences.resolveBrowserPrivacySelections()).thenReturn(
            mapOf(ProxyPlatform.X to target.domain),
        )
        whenever(orchestrator.process(any(), any(), any())).thenReturn(
            PipelineProcessingResult(
                originalUrl = URL,
                url = URL,
                wasAlreadyClean = true,
                builtinChanged = false,
                domainConverted = false,
                customRuleChanged = false,
                rulesRevision = 0,
                trace = emptyList(),
                operations = emptyList(),
            ),
        )

        val viewModel = RuleEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository,
            compiler = RuleCompiler(),
            vectorRunner = mock<RuleVectorRunner>(),
            exampleInference = mock<RuleExampleInference>(),
            orchestrator = orchestrator,
            preferences = preferences,
            urlRepository = urlRepository,
        )
        viewModel.preview(rule, URL, ProcessingProfile.BROWSER)

        val options = argumentCaptor<ProcessingOptions>()
        verify(orchestrator).process(eq(URL), options.capture(), any())
        assertTrue(options.firstValue.convertDomains)
    }

    private companion object {
        const val URL = "https://x.com/user/status/1"
    }
}
