// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026 NeatCode Labs
 *
 * This program is free software under the GNU General Public License,
 * version 3 or (at your option) any later version.
 */

package com.fixupxer.backup

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fixupxer.PreferencesManager
import com.fixupxer.data.database.FixupXerDatabase
import com.fixupxer.data.repository.HistoryRepositoryImpl
import com.fixupxer.processing.BrowserConversionMode
import com.fixupxer.processing.BrowserFrontendPreference
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.rules.CustomRuleEngine
import com.fixupxer.rules.CustomRuleRepository
import com.fixupxer.rules.CustomUrlRule
import com.fixupxer.rules.RuleAction
import com.fixupxer.rules.RuleActionExecutor
import com.fixupxer.rules.RuleBundleCodec
import com.fixupxer.rules.RuleCompiler
import com.fixupxer.rules.RuleMatcher
import com.fixupxer.rules.RuleVectorRunner
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.BrowserViewGate
import com.fixupxer.utils.Constants
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class BrowserBackupRecoveryDeviceTest {
    private lateinit var context: IsolatedRestoreContext
    private lateinit var database: FixupXerDatabase
    private lateinit var preferences: PreferencesManager
    private lateinit var rules: CustomRuleRepository
    private lateinit var manager: LocalBackupManager
    private lateinit var originalCustoms: Map<ProxyPlatform, List<String>>
    private lateinit var originalDisabled: Map<ProxyPlatform, Set<String>>
    private var originalAliasEnabled = false

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

        val base = ApplicationProvider.getApplicationContext<Context>()
        originalAliasEnabled = BrowserModeUtils.isBrowserAliasEnabled(base)
        context = IsolatedRestoreContext(
            base,
            "BrowserBackupRecoveryDeviceTest-${UUID.randomUUID()}",
        )
        assertTrue(context.filesDir.mkdirs() || context.filesDir.isDirectory)
        assertTrue(context.preferences.edit().clear().commit())

        preferences = PreferencesManager(context).apply {
            setBrowserModeEnabled(originalAliasEnabled)
            setHistoryEnabled(false)
        }
        database = Room.inMemoryDatabaseBuilder(context, FixupXerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val ruleCodec = RuleBundleCodec()
        val compiler = RuleCompiler()
        val engine = CustomRuleEngine(
            RuleMatcher(UrlNormalizer()),
            RuleActionExecutor(UrlNormalizer()),
        )
        rules = CustomRuleRepository(
            database,
            database.customRuleDao(),
            database.ruleSnapshotDao(),
            ruleCodec,
            compiler,
            RuleVectorRunner(compiler, engine),
            preferences,
        )
        manager = LocalBackupManager(
            context,
            preferences,
            rules,
            ruleCodec,
            LocalBackupCodec(ruleCodec),
            HistoryRepositoryImpl(database.urlHistoryDao()),
        )
    }

    @After
    fun tearDown() {
        BrowserModeUtils.setBrowserAliasEnabled(context, originalAliasEnabled)
        BrowserViewGate.resetForTests()
        database.close()
        context.preferences.edit().clear().commit()
        context.filesDir.deleteRecursively()
        ProxyRoster.reset()
        ProxyPlatform.entries.forEach { platform ->
            ProxyRoster.setCustomProxies(platform, originalCustoms.getValue(platform))
            ProxyRoster.setDisabledBuiltIns(platform, originalDisabled.getValue(platform))
        }
        BrowserViewGate.resetForTests()
    }

    @Test
    fun successfulRestoreReplacesSettingsAndRulesThenClearsJournal() = runTest {
        preferences.setConvertRedditEnabled(true)
        preferences.setConfigurationStatusWidgetEnabled(false)
        rules.save(CustomUrlRule(name = "Backed up", action = RuleAction.RemoveAllParams))
        val backup = manager.exportJson()

        preferences.setConvertRedditEnabled(false)
        preferences.setConfigurationStatusWidgetEnabled(true)
        rules.clear()
        rules.save(CustomUrlRule(name = "Replacement", action = RuleAction.RemoveAllParams))

        assertTrue(manager.restore(backup).isSuccess)
        assertTrue(preferences.isConvertRedditEnabled())
        assertFalse(preferences.isConfigurationStatusWidgetEnabled())
        assertEquals(listOf("Backed up"), rules.getRules().map { it.name })
        assertFalse(manager.hasInterruptedRestore())
        assertFalse(rollbackTempFile().exists())
    }

    @Test
    fun failedSettingsCommitRollsBackAndDoesNotReportSuccess() = runTest {
        preferences.setConvertRedditEnabled(true)
        preferences.setConfigurationStatusWidgetEnabled(false)
        rules.save(CustomUrlRule(name = "Incoming", action = RuleAction.RemoveAllParams))
        val incoming = manager.exportJson()

        preferences.setConvertRedditEnabled(false)
        preferences.setConfigurationStatusWidgetEnabled(true)
        rules.clear()
        rules.save(CustomUrlRule(name = "Current", action = RuleAction.RemoveAllParams))
        val before = preferences.exportSettingsSnapshot()

        context.failNextCommits(1)
        val restored = manager.restore(incoming)

        assertTrue(restored.isFailure)
        assertEquals(before, preferences.exportSettingsSnapshot())
        assertEquals(listOf("Current"), rules.getRules().map { it.name })
        assertFalse(manager.hasInterruptedRestore())
        assertNotNull(BrowserViewGate.begin(preferenceEnabled = true, aliasEnabled = true))
    }

    @Test
    fun schemaOneJournalRecoveryMigratesDormantReaderAndRestoresState() = runTest {
        preferences.setConvertRedditEnabled(true)
        rules.save(CustomUrlRule(name = "Before interruption", action = RuleAction.RemoveAllParams))
        val v1Backup = asSchemaOneBackup(manager.exportJson())
        writeRollbackJournal(v1Backup, pendingLegacyHistoryLimit = 20_000)

        preferences.setConvertRedditEnabled(false)
        preferences.setMaxHistoryEntries(100)
        rules.clear()
        rules.save(CustomUrlRule(name = "After interruption", action = RuleAction.RemoveAllParams))

        val recovered = manager.recoverInterruptedRestore()

        assertTrue(recovered.isSuccess)
        assertTrue(recovered.getOrThrow())
        assertTrue(preferences.isConvertRedditEnabled())
        assertEquals(20_000, preferences.getPendingLegacyHistoryLimit())
        assertEquals(listOf("Before interruption"), rules.getRules().map { it.name })
        assertEquals(
            BrowserFrontendPreference(BrowserConversionMode.CLEAN_ONLY, "x_xcancel"),
            preferences.getBrowserFrontendPreferences()[ProxyPlatform.X],
        )
        assertEquals(
            BrowserFrontendPreference(BrowserConversionMode.READER, "bs_skylib_coffee"),
            preferences.getBrowserFrontendPreferences()[ProxyPlatform.BLUESKY],
        )
        assertNull(preferences.getBrowserFrontendPreferences()[ProxyPlatform.YOUTUBE]?.targetId)
        assertFalse(manager.hasInterruptedRestore())
        assertNotNull(BrowserViewGate.begin(preferenceEnabled = true, aliasEnabled = true))
    }

    @Test
    fun failedJournalRecoveryRetainsJournalAndBlocksNewRestore() = runTest {
        val otherwiseValidBackup = manager.exportJson()
        rollbackFile().writeText("not-json")

        assertTrue(manager.recoverInterruptedRestore().isFailure)
        assertTrue(manager.hasInterruptedRestore())
        assertNull(BrowserViewGate.begin(preferenceEnabled = true, aliasEnabled = true))
        assertTrue(manager.restore(otherwiseValidBackup).isFailure)
        assertTrue(manager.hasInterruptedRestore())
    }

    private fun asSchemaOneBackup(schemaTwoJson: String): String {
        val root = JSONObject(schemaTwoJson).put("schemaVersion", 1)
        val settings = root.getJSONObject("settings")
        settings.remove("browserFrontends")
        settings.put(
            "browserPrivacyTargets",
            JSONObject().apply {
                ProxyPlatform.entries.forEach { put(it.name.lowercase(), JSONObject.NULL) }
                put(ProxyPlatform.X.name.lowercase(), "x_xcancel")
                put(ProxyPlatform.BLUESKY.name.lowercase(), "bs_skylib_coffee")
            },
        )
        settings
            .put("browserConvertTwitter", false)
            .put("browserConvertBluesky", true)
            .put("browserConvertReddit", false)
            .put("browserConvertPinterest", false)
            .put("browserConvertInstagram", true)
            .put("browserConvertTikTok", true)
            .put("browserConvertFacebook", true)
            .put("browserConvertYoutube", true)
            .put("browserConvertThreads", true)
        return root.toString()
    }

    private fun writeRollbackJournal(backupJson: String, pendingLegacyHistoryLimit: Int?) {
        rollbackFile().writeText(
            JSONObject()
                .put("format", Constants.RESTORE_ROLLBACK_FORMAT)
                .put("schemaVersion", Constants.RESTORE_ROLLBACK_SCHEMA_VERSION)
                .put("backup", JSONObject(backupJson))
                .put("browserAliasEnabled", originalAliasEnabled)
                .put(
                    "pendingLegacyHistoryLimit",
                    pendingLegacyHistoryLimit ?: JSONObject.NULL,
                )
                .toString(),
        )
    }

    private fun rollbackFile() = context.filesDir.resolve(Constants.RESTORE_ROLLBACK_FILE_NAME)

    private fun rollbackTempFile() =
        context.filesDir.resolve("${Constants.RESTORE_ROLLBACK_FILE_NAME}.tmp")

    private class IsolatedRestoreContext(
        base: Context,
        prefix: String,
    ) : ContextWrapper(base) {
        private val commitFailures = AtomicInteger(0)
        private val isolatedFiles = File(base.cacheDir, prefix)
        private val delegatePreferences = base.getSharedPreferences(prefix, Context.MODE_PRIVATE)
        private val wrappedPreferences = FailingCommitPreferences(
            delegatePreferences,
            ::consumeCommitFailure,
        )

        override fun getApplicationContext(): Context = this

        override fun getFilesDir(): File = isolatedFiles

        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            wrappedPreferences

        val preferences: SharedPreferences
            get() = wrappedPreferences

        fun failNextCommits(count: Int) {
            require(count >= 0)
            commitFailures.set(count)
        }

        private fun consumeCommitFailure(): Boolean {
            while (true) {
                val remaining = commitFailures.get()
                if (remaining == 0) return false
                if (commitFailures.compareAndSet(remaining, remaining - 1)) return true
            }
        }
    }

    private class FailingCommitPreferences(
        private val delegate: SharedPreferences,
        private val shouldFailCommit: () -> Boolean,
    ) : SharedPreferences by delegate {
        override fun edit(): SharedPreferences.Editor = FailingCommitEditor(
            delegate.edit(),
            shouldFailCommit,
        )
    }

    private class FailingCommitEditor(
        private val delegate: SharedPreferences.Editor,
        private val shouldFailCommit: () -> Boolean,
    ) : SharedPreferences.Editor by delegate {
        override fun commit(): Boolean = if (shouldFailCommit()) false else delegate.commit()
    }
}
