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

import android.content.Context
import com.fixupxer.PreferencesManager
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.rules.CustomRuleRepository
import com.fixupxer.rules.ImportMode
import com.fixupxer.rules.RuleBundleCodec
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.BrowserViewGate
import com.fixupxer.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val customRuleRepository: CustomRuleRepository,
    private val ruleBundleCodec: RuleBundleCodec,
    private val backupCodec: LocalBackupCodec,
    private val historyRepository: HistoryRepository,
) {
    private val restoreMutex = Mutex()
    private val rollbackFile
        get() = context.filesDir.resolve(Constants.RESTORE_ROLLBACK_FILE_NAME)
    private val rollbackTempFile
        get() = context.filesDir.resolve("${Constants.RESTORE_ROLLBACK_FILE_NAME}.tmp")

    private data class RestoreRollbackSnapshot(
        val settings: SettingsSnapshot,
        val pendingLegacyHistoryLimit: Int?,
        val rulesJson: String,
        val aliasEnabled: Boolean,
    )

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val settings = preferencesManager.exportSettingsSnapshot()
        SettingsSnapshotValidator.validate(settings, ownPackageName = context.packageName)
        val rules = customRuleRepository.getRules()
        val rulesJson = ruleBundleCodec.encodeBundle(rules)
        backupCodec.encode(settings, rulesJson)
    }

    suspend fun previewRestore(json: String): LocalBackupPreview = withContext(Dispatchers.IO) {
        val bundle = backupCodec.decode(json)
        validateIncomingBundle(bundle)
        LocalBackupPreview(
            schemaVersion = bundle.schemaVersion,
            appVersion = bundle.appVersion,
            exportedAt = bundle.exportedAt,
            ruleCount = bundle.rules.rules.size,
            routeCount = bundle.settings.rememberedRoutes.size,
            historyEnabled = bundle.settings.historyEnabled,
            maxHistoryEntries = bundle.settings.maxHistoryEntries,
        )
    }

    suspend fun restore(json: String): Result<Unit> {
        return try {
            val bundle = withContext(Dispatchers.IO) {
                backupCodec.decode(json).also { validateIncomingBundle(it) }
            }
            currentCoroutineContext().ensureActive()
            restoreMutex.withLock {
                withContext(NonCancellable + Dispatchers.IO) {
                    BrowserViewGate.pause()
                    try {
                        applyBundle(bundle)
                    } finally {
                        BrowserViewGate.resume()
                    }
                }
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    fun hasInterruptedRestore(): Boolean = rollbackFile.exists()

    suspend fun recoverInterruptedRestore(): Result<Boolean> =
        restoreMutex.withLock {
            withContext(NonCancellable + Dispatchers.IO) {
                if (!rollbackFile.exists()) {
                    return@withContext Result.success(false)
                }
                try {
                    val rollback = readRollbackSnapshot()
                    applyRollbackSnapshotForRecovery(rollback)
                    Timber.w("Recovered settings after an interrupted local restore")
                    Result.success(true)
                } catch (error: Throwable) {
                    Timber.e(error, "Failed to recover interrupted local restore")
                    Result.failure(error)
                } finally {
                    clearRollbackSnapshotBestEffort()
                }
            }
        }

    private suspend fun validateIncomingBundle(bundle: LocalBackupBundle) {
        require(bundle.rules.rules.size <= Constants.MAX_CUSTOM_RULES) {
            "Too many custom rules in backup"
        }
        SettingsSnapshotValidator.validate(bundle.settings, ownPackageName = context.packageName)
        val incomingRulesJson = ruleBundleCodec.encodeBundle(bundle.rules.rules)
        customRuleRepository.previewImport(incomingRulesJson, ImportMode.REPLACE_ALL)
    }

    private suspend fun applyBundle(bundle: LocalBackupBundle) {
        val rollback = RestoreRollbackSnapshot(
            settings = preferencesManager.exportSettingsSnapshot(),
            pendingLegacyHistoryLimit = preferencesManager.getPendingLegacyHistoryLimit(),
            rulesJson = ruleBundleCodec.encodeBundle(customRuleRepository.getRules()),
            aliasEnabled = BrowserModeUtils.isBrowserAliasEnabled(context),
        )
        val incomingRulesJson = ruleBundleCodec.encodeBundle(bundle.rules.rules)
        writeRollbackSnapshot(rollback)

        try {
            val prefsApplied = preferencesManager.replaceSettingsSnapshot(bundle.settings)
            if (!prefsApplied) {
                throw IllegalStateException("Failed to apply settings snapshot")
            }
            customRuleRepository.importBundle(incomingRulesJson, ImportMode.REPLACE_ALL)
            check(BrowserModeUtils.setBrowserAliasEnabled(context, bundle.settings.browserEnabled)) {
                "Browser alias update failed"
            }
            check(BrowserModeUtils.isBrowserAliasEnabled(context) == bundle.settings.browserEnabled) {
                "Browser alias state verification failed"
            }
            // URL history itself is not restored. Trimming retained local entries is the
            // final post-restore operation, after every backed-up state change succeeded.
            historyRepository.trimHistory(bundle.settings.maxHistoryEntries)
            clearRollbackSnapshot()
            Timber.d(
                "Local backup restored (rules=%d, routes=%d)",
                bundle.rules.rules.size,
                bundle.settings.rememberedRoutes.size,
            )
        } catch (error: Throwable) {
            Timber.w(error, "Local backup restore failed; rolling back all mutated state")
            val settingsRestored = rollbackSettings(
                rollback.settings,
                rollback.pendingLegacyHistoryLimit,
            )
            val rulesRestored = rollbackRules(rollback.rulesJson)
            val aliasRestored = rollbackAlias(rollback.aliasEnabled)
            if (settingsRestored && rulesRestored && aliasRestored) {
                clearRollbackSnapshotBestEffort()
            }
            throw error
        }
    }

    private fun rollbackSettings(
        snapshot: SettingsSnapshot,
        pendingLegacyHistoryLimit: Int?,
    ): Boolean {
        val restored = runCatching { preferencesManager.replaceSettingsSnapshot(snapshot) }
            .getOrDefault(false)
        val legacyRestored = restored &&
            preferencesManager.restorePendingLegacyHistoryLimitForRollback(
                pendingLegacyHistoryLimit
            )
        if (!legacyRestored) {
            Timber.e("Settings rollback failed after restore error")
        }
        return legacyRestored
    }

    private suspend fun rollbackRules(rulesJson: String): Boolean =
        runCatching {
            customRuleRepository.importBundle(rulesJson, ImportMode.REPLACE_ALL)
        }.onFailure {
            Timber.e(it, "Rules rollback failed after restore error")
        }.isSuccess

    private fun rollbackAlias(enabled: Boolean): Boolean {
        val restored = BrowserModeUtils.setBrowserAliasEnabled(context, enabled) &&
            BrowserModeUtils.isBrowserAliasEnabled(context) == enabled
        if (!restored) {
            Timber.e("Browser alias rollback failed after restore error")
        }
        return restored
    }

    private suspend fun applyRollbackSnapshotForRecovery(
        rollback: RestoreRollbackSnapshot,
    ) {
        val settingsRecovered = rollbackSettings(
            rollback.settings,
            rollback.pendingLegacyHistoryLimit,
        )
        val rulesRecovered = rollbackRules(rollback.rulesJson)
        val aliasRecovered = rollbackAlias(rollback.aliasEnabled)
        check(settingsRecovered && rulesRecovered && aliasRecovered) {
            "Interrupted restore rollback could not recover all persistent state"
        }
        historyRepository.trimHistory(
            rollback.pendingLegacyHistoryLimit ?: rollback.settings.maxHistoryEntries
        )
    }

    private fun writeRollbackSnapshot(rollback: RestoreRollbackSnapshot) {
        check(!rollbackFile.exists()) { "An interrupted restore is already pending recovery" }
        rollbackTempFile.delete()

        val backupJson = backupCodec.encode(rollback.settings, rollback.rulesJson)
        val encoded = JSONObject()
            .put("format", Constants.RESTORE_ROLLBACK_FORMAT)
            .put("schemaVersion", Constants.RESTORE_ROLLBACK_SCHEMA_VERSION)
            .put("backup", JSONObject(backupJson))
            .put("browserAliasEnabled", rollback.aliasEnabled)
            .put(
                "pendingLegacyHistoryLimit",
                rollback.pendingLegacyHistoryLimit ?: JSONObject.NULL,
            )
            .toString()

        try {
            FileOutputStream(rollbackTempFile).use { output ->
                output.write(encoded.toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            check(rollbackTempFile.renameTo(rollbackFile)) {
                "Failed to atomically persist restore rollback snapshot"
            }
        } catch (error: Throwable) {
            rollbackTempFile.delete()
            throw error
        }
    }

    private fun readRollbackSnapshot(): RestoreRollbackSnapshot {
        val root = JSONObject(rollbackFile.readText(Charsets.UTF_8))
        require(root.getString("format") == Constants.RESTORE_ROLLBACK_FORMAT) {
            "Unsupported restore rollback format"
        }
        require(root.getInt("schemaVersion") == Constants.RESTORE_ROLLBACK_SCHEMA_VERSION) {
            "Unsupported restore rollback schema"
        }
        val backup = backupCodec.decode(root.getJSONObject("backup").toString())
        require(backup.rules.rules.size <= Constants.MAX_CUSTOM_RULES) {
            "Too many custom rules in restore rollback"
        }
        SettingsSnapshotValidator.validate(
            backup.settings,
            ownPackageName = context.packageName,
        )
        val pendingLegacyHistoryLimit =
            if (root.isNull("pendingLegacyHistoryLimit")) {
                null
            } else {
                root.getInt("pendingLegacyHistoryLimit").also { limit ->
                    require(
                        limit !in Constants.MIN_HISTORY_ENTRIES..Constants.MAX_HISTORY_ENTRIES
                    ) {
                        "Restore rollback legacy History limit is already supported"
                    }
                }
            }
        return RestoreRollbackSnapshot(
            settings = backup.settings,
            pendingLegacyHistoryLimit = pendingLegacyHistoryLimit,
            rulesJson = ruleBundleCodec.encodeBundle(backup.rules.rules),
            aliasEnabled = root.getBoolean("browserAliasEnabled"),
        )
    }

    private fun clearRollbackSnapshot() {
        if (rollbackFile.exists()) {
            check(rollbackFile.delete()) { "Failed to clear restore rollback snapshot" }
        }
        rollbackTempFile.delete()
    }

    private fun clearRollbackSnapshotBestEffort() {
        runCatching { clearRollbackSnapshot() }
            .onFailure { Timber.e(it, "Failed to clear restore rollback snapshot") }
    }
}
