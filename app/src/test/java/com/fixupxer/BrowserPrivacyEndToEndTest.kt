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

import android.content.Context
import androidx.room.Room
import com.fixupxer.cleaners.CleanerCatalog
import com.fixupxer.cleaners.CleanerRegistry
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.cleaners.cache.CleanerCache
import com.fixupxer.data.database.FixupXerDatabase
import com.fixupxer.data.repository.UrlRepositoryImpl
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.processing.DomainConversionService
import com.fixupxer.processing.RawUrlExtractor
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.processing.UrlProcessingOrchestrator
import com.fixupxer.rules.CustomRuleEngine
import com.fixupxer.rules.CustomRuleRepository
import com.fixupxer.rules.RuleActionExecutor
import com.fixupxer.rules.RuleBundleCodec
import com.fixupxer.rules.RuleCompiler
import com.fixupxer.rules.RuleMatcher
import com.fixupxer.rules.RuleVectorRunner
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import com.fixupxer.utils.TikTokProxyStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BrowserPrivacyEndToEndTest {

    private lateinit var context: Context
    private lateinit var database: FixupXerDatabase
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var repository: UrlRepositoryImpl

    @Before
    fun setUp() {
        ProxyRoster.reset()
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        preferencesManager = PreferencesManager(context).apply {
            setHistoryEnabled(false)
            setBrowserConvertTwitterEnabled(true)
            setBrowserConvertBlueskyEnabled(true)
            setBrowserConvertRedditEnabled(true)
            setBrowserConvertPinterestEnabled(true)
        }

        database = Room.inMemoryDatabaseBuilder(context, FixupXerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val cleanerService = CleanerService(
            CleanerRegistry().apply { registerAll(CleanerCatalog.createBuiltInCleaners()) },
            CleanerCache(),
        )
        val urlProcessor = UrlProcessor(cleanerService)
        val normalizer = UrlNormalizer()
        val compiler = RuleCompiler()
        val ruleEngine = CustomRuleEngine(
            RuleMatcher(normalizer),
            RuleActionExecutor(normalizer),
        )
        val customRuleRepository = CustomRuleRepository(
            database,
            database.customRuleDao(),
            database.ruleSnapshotDao(),
            RuleBundleCodec(),
            compiler,
            RuleVectorRunner(compiler, ruleEngine),
            preferencesManager,
        )
        val orchestrator = UrlProcessingOrchestrator(
            RawUrlExtractor(),
            normalizer,
            cleanerService,
            DomainConversionService(urlProcessor),
            ruleEngine,
            customRuleRepository,
        )
        repository = UrlRepositoryImpl(
            urlProcessor,
            preferencesManager,
            mock<HistoryRepository>(),
            orchestrator,
        )
    }

    @After
    fun tearDown() {
        database.close()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        ProxyRoster.reset()
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
    }

    @Test
    fun `browser pipeline converts all reader platforms to catalog defaults`() = runTest {
        val cases = listOf(
            BrowserCase(
                ProxyPlatform.X,
                "https://x.com/user/status/1",
                "/user/status/1",
            ),
            BrowserCase(
                ProxyPlatform.BLUESKY,
                "https://bsky.app/profile/a/post/b",
                "/profile/a/post/b",
            ),
            BrowserCase(
                ProxyPlatform.REDDIT,
                "https://www.reddit.com/r/test/comments/1/",
                "/r/test/comments/1/",
            ),
            BrowserCase(
                ProxyPlatform.PINTEREST,
                "https://www.pinterest.com/pin/1/",
                "/pin/1/",
            ),
        )

        cases.forEach { case ->
            val defaultReader = AlternativeFrontendCatalog.builtInReaders(case.platform).first()
            assertEquals(
                "https://${defaultReader.domain}${case.expectedPath}",
                repository.processUrlForBrowser(case.input).url,
            )
        }
    }

    @Test
    fun `browser pipeline honors explicitly selected reader`() = runTest {
        val selectedReader =
            AlternativeFrontendCatalog.builtInReaders(ProxyPlatform.BLUESKY)[1]
        preferencesManager.setBrowserPrivacyTargetId(ProxyPlatform.BLUESKY, selectedReader.id)

        assertEquals(
            "https://${selectedReader.domain}/profile/a/post/b",
            repository.processUrlForBrowser("https://bsky.app/profile/a/post/b").url,
        )
    }

    private data class BrowserCase(
        val platform: ProxyPlatform,
        val input: String,
        val expectedPath: String,
    )

    private companion object {
        const val PREFS_NAME = "FixupXerPrefs"
    }
}
