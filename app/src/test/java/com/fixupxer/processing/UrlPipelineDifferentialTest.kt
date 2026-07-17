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

package com.fixupxer.processing

import androidx.room.Room
import com.fixupxer.PreferencesManager
import com.fixupxer.UrlProcessor
import com.fixupxer.cleaners.CleanerCatalog
import com.fixupxer.cleaners.CleanerRegistry
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.cleaners.cache.CleanerCache
import com.fixupxer.data.database.FixupXerDatabase
import com.fixupxer.rules.CustomRuleEngine
import com.fixupxer.rules.CustomRuleRepository
import com.fixupxer.rules.RuleActionExecutor
import com.fixupxer.rules.RuleBundleCodec
import com.fixupxer.rules.RuleCompiler
import com.fixupxer.rules.RuleMatcher
import com.fixupxer.rules.RuleVectorRunner
import com.fixupxer.rules.CustomUrlRule
import com.fixupxer.rules.RedirectDecodeMode
import com.fixupxer.rules.RuleAction
import com.fixupxer.rules.RuleScope
import com.fixupxer.rules.RuleTraceStatus
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.InputStreamReader

@RunWith(RobolectricTestRunner::class)
class UrlPipelineDifferentialTest {
    private lateinit var database: FixupXerDatabase
    private lateinit var orchestrator: UrlProcessingOrchestrator
    private lateinit var ruleRepository: CustomRuleRepository

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        database = Room.inMemoryDatabaseBuilder(context, FixupXerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val registry = CleanerRegistry().apply {
            registerAll(CleanerCatalog.createBuiltInCleaners())
        }
        val cleanerService = CleanerService(registry, CleanerCache())
        val urlProcessor = UrlProcessor(cleanerService)
        val normalizer = UrlNormalizer()
        val compiler = RuleCompiler()
        val codec = RuleBundleCodec()
        val preferences = PreferencesManager(context)
        val ruleEngine = CustomRuleEngine(
            RuleMatcher(normalizer),
            RuleActionExecutor(normalizer)
        )
        ruleRepository = CustomRuleRepository(
            database,
            database.customRuleDao(),
            database.ruleSnapshotDao(),
            codec,
            compiler,
            RuleVectorRunner(compiler, ruleEngine),
            preferences
        )
        orchestrator = UrlProcessingOrchestrator(
            RawUrlExtractor(),
            normalizer,
            cleanerService,
            DomainConversionService(urlProcessor),
            ruleEngine,
            ruleRepository
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `master-off pipeline matches frozen baseline`() = runTest {
        val stream = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("url-pipeline-baseline-pre-rules.json")
        )
        val fixtures = InputStreamReader(stream).use {
            Gson().fromJson(it, Array<Fixture>::class.java)
        }
        fixtures.forEach { fixture ->
            val result = orchestrator.process(
                fixture.input,
                ProcessingOptions(
                    profile = ProcessingProfile.MAIN,
                    cleanTracking = fixture.cleanTracking,
                    convertDomains = fixture.convertDomains,
                    instagramProxy = "toinstagram.com",
                    tiktokProxy = "tnktok.com",
                    customRulesEnabled = false
                )
            )
            assertEquals(fixture.id, fixture.expected, result.url)
        }
    }

    @Test
    fun `redirect reentry stops at global hop limit`() = runTest {
        ruleRepository.save(
            CustomUrlRule(
                name = "Follow next",
                includeScope = RuleScope.AllUrls,
                action = RuleAction.ExtractRedirect(
                    parameterName = "next",
                    decodeMode = RedirectDecodeMode.PERCENT_ONCE
                )
            )
        )
        var chained = "https://final.example/path"
        repeat(7) { index ->
            chained = "https://hop$index.example/?next=" +
                java.net.URLEncoder.encode(chained, java.nio.charset.StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
        }

        val result = orchestrator.process(
            chained,
            ProcessingOptions(
                profile = ProcessingProfile.MAIN,
                cleanTracking = false,
                convertDomains = false,
                instagramProxy = "toinstagram.com",
                tiktokProxy = "tnktok.com",
                customRulesEnabled = true,
                traceEnabled = true
            )
        )

        assertEquals(true, result.trace.any { it.status == RuleTraceStatus.HOP_LIMIT })
    }

    private data class Fixture(
        val id: String,
        val input: String,
        val expected: String,
        val cleanTracking: Boolean,
        val convertDomains: Boolean
    )
}
