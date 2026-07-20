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

package com.fixupxer.backup

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.room.Room
import com.fixupxer.PreferencesManager
import com.fixupxer.data.database.FixupXerDatabase
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.rules.CustomRuleEngine
import com.fixupxer.rules.CustomRuleRepository
import com.fixupxer.rules.CustomUrlRule
import com.fixupxer.rules.ImportResult
import com.fixupxer.rules.RuleAction
import com.fixupxer.rules.RuleActionExecutor
import com.fixupxer.rules.RuleBundleCodec
import com.fixupxer.rules.RuleCompiler
import com.fixupxer.rules.RuleMatcher
import com.fixupxer.rules.RuleVectorRunner
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.Constants
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LocalBackupManagerTest {
    private lateinit var context: Context
    private lateinit var database: FixupXerDatabase
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var repository: CustomRuleRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var manager: LocalBackupManager

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication().applicationContext
        context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        rollbackFile().delete()
        rollbackTempFile().delete()
        BrowserModeUtils.setBrowserAliasEnabled(context, false)
        database = Room.inMemoryDatabaseBuilder(context, FixupXerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferencesManager = PreferencesManager(context)
        val codec = RuleBundleCodec()
        historyRepository = mock()
        repository = CustomRuleRepository(
            database,
            database.customRuleDao(),
            database.ruleSnapshotDao(),
            codec,
            RuleCompiler(),
            RuleVectorRunner(
                RuleCompiler(),
                CustomRuleEngine(
                    RuleMatcher(UrlNormalizer()),
                    RuleActionExecutor(UrlNormalizer()),
                ),
            ),
            preferencesManager,
        )
        manager = LocalBackupManager(
            context,
            preferencesManager,
            repository,
            codec,
            LocalBackupCodec(codec),
            historyRepository,
        )
    }

    @After
    fun tearDown() {
        database.close()
        context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        rollbackFile().delete()
        rollbackTempFile().delete()
        BrowserModeUtils.setBrowserAliasEnabled(context, false)
    }

    @Test
    fun `restore replaces settings and refreshes proxy roster`() = runTest {
        preferencesManager.setBrowserModeEnabled(true)
        preferencesManager.setConvertRedditEnabled(true)
        preferencesManager.setConfigurationStatusWidgetEnabled(false)
        val exported = manager.exportJson()
        preferencesManager.setBrowserModeEnabled(false)
        preferencesManager.setConvertRedditEnabled(false)
        preferencesManager.setConfigurationStatusWidgetEnabled(true)

        assertTrue(manager.restore(exported).isSuccess)
        assertTrue(preferencesManager.isBrowserModeEnabled())
        assertTrue(preferencesManager.isConvertRedditEnabled())
        assertFalse(preferencesManager.isConfigurationStatusWidgetEnabled())
    }

    @Test
    fun `restore applies browser alias state verifiably`() = runTest {
        val context = RuntimeEnvironment.getApplication().applicationContext
        preferencesManager.setBrowserModeEnabled(true)
        val exported = manager.exportJson()
        preferencesManager.setBrowserModeEnabled(false)
        BrowserModeUtils.setBrowserAliasEnabled(context, false)

        assertTrue(manager.restore(exported).isSuccess)
        assertTrue(BrowserModeUtils.isBrowserAliasEnabled(context))
    }

    @Test
    fun `restore trims retained history to restored limit after state succeeds`() = runTest {
        preferencesManager.setMaxHistoryEntries(37)
        val exported = manager.exportJson()
        preferencesManager.setMaxHistoryEntries(100)

        assertTrue(manager.restore(exported).isSuccess)

        verify(historyRepository).trimHistory(37)
    }

    @Test
    fun `startup recovery restores rollback snapshot and clears marker`() = runTest {
        preferencesManager.setBrowserModeEnabled(true)
        preferencesManager.setConvertRedditEnabled(true)
        context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("max_history_entries", 20_000)
            .commit()
        repository.save(CustomUrlRule(name = "Before", action = RuleAction.RemoveAllParams))
        BrowserModeUtils.setBrowserAliasEnabled(context, true)
        val rollbackBackup = manager.exportJson()
        rollbackFile().writeText(
            JSONObject()
                .put("format", Constants.RESTORE_ROLLBACK_FORMAT)
                .put("schemaVersion", Constants.RESTORE_ROLLBACK_SCHEMA_VERSION)
                .put("backup", JSONObject(rollbackBackup))
                .put("browserAliasEnabled", true)
                .put("pendingLegacyHistoryLimit", 20_000)
                .toString()
        )

        preferencesManager.setBrowserModeEnabled(false)
        preferencesManager.setConvertRedditEnabled(false)
        preferencesManager.setMaxHistoryEntries(100)
        repository.clear()
        repository.save(CustomUrlRule(name = "After", action = RuleAction.RemoveAllParams))
        BrowserModeUtils.setBrowserAliasEnabled(context, false)

        val recovered = manager.recoverInterruptedRestore()

        assertTrue(recovered.isSuccess)
        assertTrue(recovered.getOrThrow())
        assertTrue(preferencesManager.isBrowserModeEnabled())
        assertTrue(preferencesManager.isConvertRedditEnabled())
        assertEquals(20_000, preferencesManager.getPendingLegacyHistoryLimit())
        assertEquals(listOf("Before"), repository.getRules().map { it.name })
        assertTrue(BrowserModeUtils.isBrowserAliasEnabled(context))
        verify(historyRepository).trimHistory(20_000)
        assertFalse(rollbackFile().exists())
    }

    @Test
    fun `trim failure rolls back restored settings and actual alias`() = runTest {
        preferencesManager.setBrowserModeEnabled(true)
        preferencesManager.setConvertRedditEnabled(true)
        preferencesManager.setMaxHistoryEntries(37)
        val exported = manager.exportJson()
        preferencesManager.setBrowserModeEnabled(false)
        preferencesManager.setConvertRedditEnabled(false)
        preferencesManager.setMaxHistoryEntries(100)
        BrowserModeUtils.setBrowserAliasEnabled(context, false)
        whenever(historyRepository.trimHistory(37))
            .thenThrow(IllegalStateException("trim failed"))

        assertTrue(manager.restore(exported).isFailure)

        assertFalse(preferencesManager.isBrowserModeEnabled())
        assertFalse(preferencesManager.isConvertRedditEnabled())
        assertEquals(100, preferencesManager.getMaxHistoryEntries())
        assertFalse(BrowserModeUtils.isBrowserAliasEnabled(context))
        verify(historyRepository).trimHistory(37)
    }

    @Test
    fun `alias failure restores actual previous alias instead of preference value`() = runTest {
        preferencesManager.setBrowserModeEnabled(false)
        val exported = manager.exportJson()
        preferencesManager.setBrowserModeEnabled(true)

        var aliasState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        var rejectDisable = true
        val packageManager: PackageManager = mock()
        whenever(packageManager.getComponentEnabledSetting(any<ComponentName>()))
            .thenAnswer { aliasState }
        doAnswer { invocation ->
            val requestedState = invocation.arguments[1] as Int
            if (requestedState == PackageManager.COMPONENT_ENABLED_STATE_DISABLED &&
                rejectDisable
            ) {
                rejectDisable = false
                throw IllegalStateException("disable failed")
            }
            aliasState = requestedState
            null
        }.whenever(packageManager).setComponentEnabledSetting(any(), any(), any())
        val baseContext = RuntimeEnvironment.getApplication().applicationContext
        val aliasContext = object : ContextWrapper(baseContext) {
            override fun getPackageManager(): PackageManager = packageManager
        }
        val codec = RuleBundleCodec()
        val aliasManager = LocalBackupManager(
            aliasContext,
            preferencesManager,
            repository,
            codec,
            LocalBackupCodec(codec),
            historyRepository,
        )

        assertTrue(aliasManager.restore(exported).isFailure)
        assertTrue(preferencesManager.isBrowserModeEnabled())
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, aliasState)
        verify(historyRepository, never()).trimHistory(any())
    }

    @Test
    fun `invalid backup does not mutate prefs`() = runTest {
        preferencesManager.setBrowserModeEnabled(true)
        val before = preferencesManager.exportSettingsSnapshot()
        assertTrue(manager.restore("not-json").isFailure)
        assertEquals(before, preferencesManager.exportSettingsSnapshot())
    }

    @Test
    fun `preview rejects own package routes and uncompilable rules like restore`() = runTest {
        val exported = manager.exportJson()
        val ownRoute = withRoute(exported, "example.com", "BROWSER", context.packageName)

        assertTrue(runCatching { manager.previewRestore(ownRoute) }.isFailure)
        assertTrue(runCatching { manager.previewRestore(withInvalidRule(exported)) }.isFailure)
    }

    @Test
    fun `failed settings commit immediately attempts rollback`() = runTest {
        val exported = manager.exportJson()
        val rollbackSnapshot = preferencesManager.exportSettingsSnapshot()
        val failingPreferences: PreferencesManager = mock()
        whenever(failingPreferences.exportSettingsSnapshot()).thenReturn(rollbackSnapshot)
        whenever(failingPreferences.getPendingLegacyHistoryLimit()).thenReturn(null)
        whenever(failingPreferences.replaceSettingsSnapshot(any()))
            .thenReturn(false, true)
        whenever(failingPreferences.restorePendingLegacyHistoryLimitForRollback(null))
            .thenReturn(true)
        val codec = RuleBundleCodec()
        val failingManager = LocalBackupManager(
            context,
            failingPreferences,
            repository,
            codec,
            LocalBackupCodec(codec),
            historyRepository,
        )

        assertTrue(failingManager.restore(exported).isFailure)

        verify(failingPreferences, times(2)).replaceSettingsSnapshot(any())
        verify(historyRepository, never()).trimHistory(any())
    }

    @Test
    fun `restore replaces custom rules with backup rules`() = runTest {
        repository.save(CustomUrlRule(name = "Original", action = RuleAction.RemoveAllParams))
        val exported = manager.exportJson()

        repository.clear()
        repository.save(CustomUrlRule(name = "Replacement", action = RuleAction.RemoveAllParams))

        assertTrue(manager.restore(exported).isSuccess)
        val rules = repository.getRules()
        assertEquals(listOf("Original"), rules.map { it.name })
    }

    @Test
    fun `settings roll back when rule import fails`() = runTest {
        preferencesManager.setBrowserModeEnabled(true)
        val exported = manager.exportJson()
        preferencesManager.setBrowserModeEnabled(false)
        RuntimeEnvironment.getApplication().applicationContext
            .getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("max_history_entries", 0)
            .commit()
        val before = preferencesManager.exportSettingsSnapshot()

        val context = RuntimeEnvironment.getApplication().applicationContext
        val failingRepository: CustomRuleRepository = mock()
        whenever(failingRepository.getRules()).thenReturn(emptyList())
        whenever(failingRepository.previewImport(any(), any()))
            .thenReturn(ImportResult(0, 0, 0))
        whenever(failingRepository.importBundle(any(), any()))
            .thenThrow(IllegalStateException("import failed"))
        val ruleCodec = RuleBundleCodec()
        val failingManager = LocalBackupManager(
            context,
            preferencesManager,
            failingRepository,
            ruleCodec,
            LocalBackupCodec(ruleCodec),
            historyRepository,
        )

        assertTrue(failingManager.restore(exported).isFailure)
        assertEquals(before, preferencesManager.exportSettingsSnapshot())
        assertFalse(preferencesManager.isBrowserModeEnabled())
        assertEquals(0, preferencesManager.getPendingLegacyHistoryLimit())
        verify(historyRepository, never()).trimHistory(any())
    }

    @Test
    fun `uncompilable rule in backup rejects restore before any mutation`() = runTest {
        preferencesManager.setBrowserModeEnabled(true)
        val exported = manager.exportJson()
        preferencesManager.setBrowserModeEnabled(false)
        val before = preferencesManager.exportSettingsSnapshot()

        val tampered = withInvalidRule(exported)

        assertTrue(manager.restore(tampered).isFailure)
        assertEquals(before, preferencesManager.exportSettingsSnapshot())
        assertFalse(preferencesManager.isBrowserModeEnabled())
        assertTrue(repository.getRules().isEmpty())
    }

    @Test
    fun `own package remembered route is rejected on save`() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        val saved = preferencesManager.setRememberedRoute(
            "example.com",
            RememberedRoute(RememberedRouteKind.BROWSER, context.packageName),
        )
        assertFalse(saved)
        assertEquals(0, preferencesManager.getRememberedRouteCount())
    }

    @Test
    fun `backup with own package route is hard rejected without mutation`() = runTest {
        val context = RuntimeEnvironment.getApplication().applicationContext
        val exported = manager.exportJson()
        val tampered = withRoute(exported, "example.com", "BROWSER", context.packageName)
        val before = preferencesManager.exportSettingsSnapshot()

        assertTrue(manager.restore(tampered).isFailure)
        assertEquals(before, preferencesManager.exportSettingsSnapshot())
    }

    @Test
    fun `cross-device browser route survives restore even when not installed`() = runTest {
        val exported = manager.exportJson()
        val withForeignRoute = withRoute(exported, "example.com", "BROWSER", "org.mozilla.firefox")

        assertTrue(manager.restore(withForeignRoute).isSuccess)
        assertNotNull(preferencesManager.getRememberedRoute("example.com"))
        assertEquals(
            "org.mozilla.firefox",
            preferencesManager.getRememberedRoute("example.com")?.packageName,
        )
    }

    @Test
    fun `invalid route rejects whole restore instead of filtering`() = runTest {
        val exported = manager.exportJson()
        val tampered = withRoute(exported, "example.com", "BROWSER", "bad..package")
        val before = preferencesManager.exportSettingsSnapshot()

        assertTrue(manager.restore(tampered).isFailure)
        assertEquals(before, preferencesManager.exportSettingsSnapshot())
        assertEquals(0, preferencesManager.getRememberedRouteCount())
    }

    /** Injects a rule that passes structural decode but fails compilation (bad UUID). */
    private fun withInvalidRule(backupJson: String): String {
        val root = JSONObject(backupJson)
        root.getJSONObject("customRules")
            .getJSONArray("rules")
            .put(
                JSONObject()
                    .put("id", "not-a-uuid")
                    .put("name", "Bad rule")
                    .put("phase", "post_clean")
                    .put("contexts", org.json.JSONArray(listOf("MAIN")))
                    .put("includeScope", JSONObject().put("type", "all_urls"))
                    .put("action", JSONObject().put("type", "remove_all_params")),
            )
        return root.toString(2)
    }

    private fun withRoute(
        backupJson: String,
        host: String,
        kind: String,
        packageName: String,
    ): String {
        val root = JSONObject(backupJson)
        root.getJSONObject("settings")
            .getJSONObject("rememberedRoutes")
            .put(
                host,
                JSONObject().put("kind", kind).put("packageName", packageName),
            )
        return root.toString(2)
    }

    private fun rollbackFile() =
        context.filesDir.resolve(Constants.RESTORE_ROLLBACK_FILE_NAME)

    private fun rollbackTempFile() =
        context.filesDir.resolve("${Constants.RESTORE_ROLLBACK_FILE_NAME}.tmp")
}
