// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026 NeatCode Labs
 *
 * This program is free software under the GNU General Public License,
 * version 3 or (at your option) any later version.
 */

package com.fixupxer

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fixupxer.cleaners.CleanerCatalog
import com.fixupxer.cleaners.CleanerRegistry
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.cleaners.UrlCleaner
import com.fixupxer.cleaners.cache.CachedCleanResult
import com.fixupxer.cleaners.cache.CleanerCache
import com.fixupxer.data.database.FixupXerDatabase
import com.fixupxer.processing.BrowserConversionMode
import com.fixupxer.processing.BrowserFrontendPreference
import com.fixupxer.processing.BrowserFrontendSnapshot
import com.fixupxer.processing.DomainConversionService
import com.fixupxer.processing.PipelineStatus
import com.fixupxer.processing.ProcessingOptions
import com.fixupxer.processing.ProcessingProfile
import com.fixupxer.processing.ProxySelections
import com.fixupxer.processing.RawUrlExtractor
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.processing.UrlProcessingOrchestrator
import com.fixupxer.rules.CustomRuleEngine
import com.fixupxer.rules.CustomRuleRepository
import com.fixupxer.rules.CustomUrlRule
import com.fixupxer.rules.RedirectDecodeMode
import com.fixupxer.rules.RuleAction
import com.fixupxer.rules.RuleActionExecutor
import com.fixupxer.rules.RuleBundleCodec
import com.fixupxer.rules.RuleCompiler
import com.fixupxer.rules.RuleMatcher
import com.fixupxer.rules.RulePhase
import com.fixupxer.rules.RuleScope
import com.fixupxer.rules.RuleTraceStatus
import com.fixupxer.rules.RuleVectorRunner
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.BrowserViewGate
import com.fixupxer.utils.FrontendRole
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class BrowserPipelineContractDeviceTest {
    private lateinit var context: IsolatedPreferencesContext
    private lateinit var database: FixupXerDatabase
    private lateinit var rules: CustomRuleRepository
    private lateinit var orchestrator: UrlProcessingOrchestrator
    private lateinit var originalCustoms: Map<ProxyPlatform, List<String>>
    private lateinit var originalDisabled: Map<ProxyPlatform, Set<String>>

    @Before
    fun setUp() {
        originalCustoms = ProxyPlatform.entries.associateWith {
            ProxyRoster.getCustomProxies(it).toList()
        }
        originalDisabled = ProxyPlatform.entries.associateWith {
            ProxyRoster.getDisabledBuiltIns(it).toSet()
        }
        ProxyRoster.reset()
        BrowserViewGate.resetForTests()

        context = IsolatedPreferencesContext(
            ApplicationProvider.getApplicationContext(),
            "BrowserPipelineContractDeviceTest-${UUID.randomUUID()}",
        )
        assertTrue(context.preferences.edit().clear().commit())
        val preferences = PreferencesManager(context).apply {
            setCustomRulesEnabled(true)
            setHistoryEnabled(false)
        }
        database = Room.inMemoryDatabaseBuilder(context, FixupXerDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val registry = CleanerRegistry().apply {
            registerAll(CleanerCatalog.createBuiltInCleaners())
        }
        val cleanerService = CleanerService(registry, CleanerCache())
        val normalizer = UrlNormalizer()
        val compiler = RuleCompiler()
        val engine = CustomRuleEngine(
            RuleMatcher(normalizer),
            RuleActionExecutor(normalizer),
        )
        rules = CustomRuleRepository(
            database,
            database.customRuleDao(),
            database.ruleSnapshotDao(),
            RuleBundleCodec(),
            compiler,
            RuleVectorRunner(compiler, engine),
            preferences,
        )
        orchestrator = UrlProcessingOrchestrator(
            RawUrlExtractor(),
            normalizer,
            cleanerService,
            DomainConversionService(UrlProcessor(cleanerService)),
            engine,
            rules,
        )
    }

    @After
    fun tearDown() {
        database.close()
        context.preferences.edit().clear().commit()
        ProxyRoster.reset()
        ProxyPlatform.entries.forEach { platform ->
            ProxyRoster.setCustomProxies(platform, originalCustoms.getValue(platform))
            ProxyRoster.setDisabledBuiltIns(platform, originalDisabled.getValue(platform))
        }
        BrowserViewGate.resetForTests()
    }

    @Test
    fun postCleanPlatformChangeUsesDestinationBrowserPreference() = runTest {
        val target = embedTarget(ProxyPlatform.TIKTOK)
        rules.save(
            CustomUrlRule(
                name = "Change platform after cleaning",
                phase = RulePhase.POST_CLEAN,
                includeScope = RuleScope.ExactHost("example.org"),
                action = RuleAction.TemplateRewrite(
                    "https://vm.tiktok.com/Z123/?utm_source=rule&keep=1",
                ),
            ),
        )

        val result = orchestrator.process(
            "https://example.org/start?utm_source=input",
            browserOptions(ProxyPlatform.TIKTOK, BrowserConversionMode.EMBED, target.id),
        )

        // POST_CLEAN deliberately runs after the cleaner; a template rewrite is not a redirect.
        assertEquals("https://vm.${target.domain}/Z123/?utm_source=rule&keep=1", result.url)
        assertEquals(PipelineStatus.COMPLETE, result.status)
        assertEquals("vm.tiktok.com", result.routingHost)
    }

    @Test
    fun redirectReentryCleansAndConvertsTheRedirectDestination() = runTest {
        val target = embedTarget(ProxyPlatform.TIKTOK)
        rules.save(
            CustomUrlRule(
                name = "Extract Browser destination",
                phase = RulePhase.PRE_CLEAN,
                includeScope = RuleScope.ExactHost("redirect.example"),
                action = RuleAction.ExtractRedirect(
                    parameterName = "next",
                    decodeMode = RedirectDecodeMode.PERCENT_ONCE,
                ),
            ),
        )
        val destination = "https://vt.tiktok.com/Z456/?utm_source=redirect&keep=1"
        val input = "https://redirect.example/?next=" + URLEncoder.encode(destination, "UTF-8")

        val result = orchestrator.process(
            input,
            browserOptions(ProxyPlatform.TIKTOK, BrowserConversionMode.EMBED, target.id),
        )

        assertEquals("https://vt.${target.domain}/Z456/?keep=1", result.url)
        assertEquals(PipelineStatus.COMPLETE, result.status)
        assertEquals("vt.tiktok.com", result.routingHost)
        assertTrue(result.trace.any { it.ruleName == "Extract Browser destination" })
    }

    @Test
    fun phasesRunAroundCleaningAndFrontendConversionInContractOrder() = runTest {
        val target = embedTarget(ProxyPlatform.X)
        rules.save(rule("PRE", RulePhase.PRE_CLEAN, "pre", RuleScope.ExactHost("x.com")))
        rules.save(rule("POST_CLEAN", RulePhase.POST_CLEAN, "after_clean", RuleScope.ExactHost("x.com")))
        rules.save(
            rule(
                "POST_CONVERSION",
                RulePhase.POST_CONVERSION,
                "after_conversion",
                RuleScope.ExactHost(target.domain),
            ),
        )

        val result = orchestrator.process(
            "https://x.com/alice/status/1?pre=1&utm_source=tracking&after_clean=1&after_conversion=1",
            browserOptions(ProxyPlatform.X, BrowserConversionMode.EMBED, target.id),
        )

        assertEquals(
            "https://${target.domain}${target.pathPrefix.orEmpty()}/alice/status/1",
            result.url,
        )
        assertEquals(
            listOf(RulePhase.PRE_CLEAN, RulePhase.POST_CLEAN, RulePhase.POST_CONVERSION),
            result.trace.filter { it.status == RuleTraceStatus.APPLIED }.map { it.phase },
        )
        assertTrue(result.builtinChanged)
        assertTrue(result.domainConverted)
        assertTrue(result.customRuleChanged)
    }

    @Test
    fun terminalFailuresRemainDistinctOnAndroidRuntime() = runTest {
        rules.save(
            CustomUrlRule(
                name = "Invalid output",
                phase = RulePhase.POST_CONVERSION,
                includeScope = RuleScope.ExactHost("invalid.example"),
                action = RuleAction.ExtractRedirect("missing"),
            ),
        )
        val invalid = orchestrator.process(
            "https://invalid.example/start?keep=1",
            browserOptions(),
        )
        assertEquals(PipelineStatus.INVALID_RESULT, invalid.status)
        assertEquals("https://invalid.example/start?keep=1", invalid.url)

        rules.clear()
        rules.save(
            CustomUrlRule(
                name = "Cycle out",
                phase = RulePhase.PRE_CLEAN,
                includeScope = RuleScope.ExactHost("cycle.example"),
                action = RuleAction.TemplateRewrite(
                    "https://cycle-wrapper.example/?next=https%3A%2F%2Fcycle.example%2Fstart",
                ),
            ),
        )
        rules.save(
            CustomUrlRule(
                name = "Cycle back",
                phase = RulePhase.PRE_CLEAN,
                includeScope = RuleScope.ExactHost("cycle-wrapper.example"),
                action = RuleAction.ExtractRedirect("next"),
            ),
        )
        val cycle = orchestrator.process("https://cycle.example/start", browserOptions())
        assertEquals(PipelineStatus.CYCLE, cycle.status)
        assertTrue(cycle.trace.any { it.status == RuleTraceStatus.CYCLE })

        rules.clear()
        rules.save(
            CustomUrlRule(
                name = "Follow chain",
                phase = RulePhase.PRE_CLEAN,
                includeScope = RuleScope.AllUrls,
                action = RuleAction.ExtractRedirect("next"),
            ),
        )
        var chained = "https://final.example/path"
        repeat(8) { index ->
            chained = "https://hop$index.example/?next=" + URLEncoder.encode(chained, "UTF-8")
        }
        val limited = orchestrator.process(chained, browserOptions())
        assertEquals(PipelineStatus.HOP_LIMIT, limited.status)
        assertTrue(limited.trace.any { it.status == RuleTraceStatus.HOP_LIMIT })
    }

    @Test
    fun cacheNamespaceAndGenerationRejectStaleReuse() {
        val applications = AtomicInteger(0)
        val registry = CleanerRegistry().apply {
            register(
                object : UrlCleaner {
                    override val id = "device-cache-probe"
                    override fun matches(url: String): Boolean = true
                    override fun clean(url: String): String {
                        applications.incrementAndGet()
                        return url
                    }
                },
            )
        }
        val cache = CleanerCache()
        val service = CleanerService(registry, cache)
        val input = "https://cache.example/path"

        service.deepCleanWithDetails(input, maxPasses = 1)
        service.deepCleanWithDetails(input, maxPasses = 1)
        assertEquals(1, applications.get())
        service.deepCleanWithDetails(input, maxPasses = 2)
        assertEquals(2, applications.get())
        ProxyRoster.setDisabledBuiltIns(ProxyPlatform.X, setOf("x_xcancel"))
        service.deepCleanWithDetails(input, maxPasses = 2)
        assertEquals(3, applications.get())

        val pendingCache = CleanerCache()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val pending = executor.submit<CachedCleanResult> {
                pendingCache.getOrCompute(input, "old-generation") {
                    started.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                    CachedCleanResult("stale", emptyList(), 1)
                }
            }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            pendingCache.clear()
            release.countDown()
            assertEquals("stale", pending.get(5, TimeUnit.SECONDS).cleanedUrl)
            assertEquals(0, pendingCache.getStats().size)
            val fresh = pendingCache.getOrCompute(input, "old-generation") {
                CachedCleanResult("fresh", emptyList(), 1)
            }
            assertEquals("fresh", fresh.cleanedUrl)
            assertEquals(1, pendingCache.getStats().size)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    private fun browserOptions(
        platform: ProxyPlatform? = null,
        mode: BrowserConversionMode = BrowserConversionMode.CLEAN_ONLY,
        targetId: String? = null,
    ): ProcessingOptions {
        val preferences = ProxyPlatform.entries.associateWith {
            BrowserFrontendPreference.CLEAN_ONLY
        }.toMutableMap()
        if (platform != null) preferences[platform] = BrowserFrontendPreference(mode, targetId)
        return ProcessingOptions(
            profile = ProcessingProfile.BROWSER,
            cleanTracking = true,
            convertDomains = false,
            proxySelections = ProxySelections.EMPTY,
            customRulesEnabled = true,
            persistHistory = false,
            useCache = true,
            traceEnabled = true,
            browserFrontends = BrowserFrontendSnapshot(preferences),
        )
    }

    private fun embedTarget(platform: ProxyPlatform) =
        AlternativeFrontendCatalog.builtIn(platform).first { it.role == FrontendRole.EMBED }

    private fun rule(
        name: String,
        phase: RulePhase,
        parameter: String,
        scope: RuleScope,
    ) = CustomUrlRule(
        name = name,
        phase = phase,
        includeScope = scope,
        action = RuleAction.RemoveParams(listOf(parameter)),
    )

    private class IsolatedPreferencesContext(
        base: Context,
        private val prefix: String,
    ) : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this

        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            baseContext.getSharedPreferences("$prefix-$name", mode)

        val preferences: SharedPreferences
            get() = getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
    }
}
