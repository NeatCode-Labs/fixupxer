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

package com.fixupxer.rules

import androidx.room.withTransaction
import com.fixupxer.PreferencesManager
import com.fixupxer.data.database.CustomRuleDao
import com.fixupxer.data.database.FixupXerDatabase
import com.fixupxer.data.database.RuleSnapshotDao
import com.fixupxer.data.database.RuleSnapshotEntity
import com.fixupxer.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class ImportMode {
    ADD_NEW,
    UPDATE_MATCHING,
    REPLACE_ALL
}

data class ImportResult(
    val added: Int,
    val updated: Int,
    val skipped: Int
)

@Singleton
class CustomRuleRepository @Inject constructor(
    private val database: FixupXerDatabase,
    private val ruleDao: CustomRuleDao,
    private val snapshotDao: RuleSnapshotDao,
    private val codec: RuleBundleCodec,
    private val compiler: RuleCompiler,
    private val preferences: PreferencesManager
) {
    private val mutex = Mutex()
    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision

    @Volatile
    private var initialized = false

    @Volatile
    private var runtimeSnapshot = RuleSnapshot.EMPTY

    fun observeRules(): Flow<List<CustomUrlRule>> =
        ruleDao.observeAll().map { entities -> entities.map(codec::fromEntity) }

    fun observeEnabledCount(): Flow<Int> = ruleDao.observeEnabledCount()

    fun enabledFlow(): Flow<Boolean> = preferences.customRulesEnabledFlow()

    fun isEnabled(): Boolean = preferences.areCustomRulesEnabled()

    fun setEnabled(enabled: Boolean) = preferences.setCustomRulesEnabled(enabled)

    suspend fun awaitSnapshot(): RuleSnapshot {
        if (initialized) return runtimeSnapshot
        return mutex.withLock {
            if (!initialized) refreshSnapshotLocked()
            runtimeSnapshot
        }
    }

    suspend fun getRules(): List<CustomUrlRule> =
        ruleDao.getAll().map(codec::fromEntity)

    suspend fun getRule(id: String): CustomUrlRule? =
        ruleDao.getById(id)?.let(codec::fromEntity)

    suspend fun save(rule: CustomUrlRule) = mutex.withLock {
        val current = ruleDao.getAll().map(codec::fromEntity)
        val existing = current.firstOrNull { it.id == rule.id }
        val moveToEnd = existing == null || existing.phase != rule.phase
        val targetOrder = if (moveToEnd) {
            current.filter { it.phase == rule.phase && it.id != rule.id }
                .maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
        } else {
            requireNotNull(existing).sortOrder
        }
        val normalized = rule.copy(
            name = rule.name.trim(),
            sortOrder = targetOrder,
            updatedAt = System.currentTimeMillis()
        )
        compiler.compile(normalized)
        database.withTransaction {
            ruleDao.upsert(codec.toEntity(normalized))
            reindexLocked(ruleDao.getAll().map(codec::fromEntity))
        }
        refreshSnapshotLocked()
    }

    suspend fun duplicate(id: String): CustomUrlRule = mutex.withLock {
        val source = requireNotNull(ruleDao.getById(id)?.let(codec::fromEntity))
        val rules = ruleDao.getAll().map(codec::fromEntity)
        val nextOrder = rules.filter { it.phase == source.phase }
            .maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
        val copy = source.copy(
            id = UUID.randomUUID().toString(),
            name = "${source.name} (copy)",
            sortOrder = nextOrder,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        compiler.compile(copy)
        database.withTransaction { ruleDao.upsert(codec.toEntity(copy)) }
        refreshSnapshotLocked()
        copy
    }

    suspend fun delete(id: String) = mutex.withLock {
        database.withTransaction {
            ruleDao.deleteById(id)
            reindexLocked(ruleDao.getAll().map(codec::fromEntity))
        }
        refreshSnapshotLocked()
    }

    suspend fun clear() = mutex.withLock {
        database.withTransaction { ruleDao.deleteAll() }
        refreshSnapshotLocked()
    }

    suspend fun reorder(phase: RulePhase, orderedIds: List<String>) = mutex.withLock {
        database.withTransaction {
            val rules = ruleDao.getAll().map(codec::fromEntity)
            val inPhase = rules.filter { it.phase == phase }
            require(inPhase.map { it.id }.toSet() == orderedIds.toSet()) {
                "Reorder list does not match the phase"
            }
            val byId = inPhase.associateBy { it.id }
            orderedIds.forEachIndexed { index, id ->
                ruleDao.upsert(codec.toEntity(requireNotNull(byId[id]).copy(sortOrder = index)))
            }
        }
        refreshSnapshotLocked()
    }

    fun exportRules(rules: List<CustomUrlRule>): String = codec.encodeBundle(rules)

    suspend fun previewImport(json: String, mode: ImportMode): ImportResult {
        require(json.toByteArray().size <= Constants.MAX_RULE_BUNDLE_BYTES) {
            "Rule bundle is too large"
        }
        val imported = codec.decodeBundle(json).rules
        compiler.compileAll(imported)
        val currentIds = ruleDao.getAll().map { it.id }.toSet()
        val matching = imported.count { it.id in currentIds }
        val newRules = imported.size - matching
        return when (mode) {
            ImportMode.ADD_NEW -> ImportResult(newRules, 0, matching)
            ImportMode.UPDATE_MATCHING -> ImportResult(newRules, matching, 0)
            ImportMode.REPLACE_ALL -> ImportResult(newRules, matching, 0)
        }
    }

    suspend fun importBundle(json: String, mode: ImportMode): ImportResult = mutex.withLock {
        require(json.toByteArray().size <= Constants.MAX_RULE_BUNDLE_BYTES) {
            "Rule bundle is too large"
        }
        val imported = codec.decodeBundle(json).rules
        compiler.compileAll(imported)
        require(imported.size <= Constants.MAX_CUSTOM_RULES) { "Too many rules" }

        var added = 0
        var updated = 0
        var skipped = 0
        database.withTransaction {
            val current = ruleDao.getAll().map(codec::fromEntity)
            createSnapshotLocked(current)
            val currentById = current.associateBy { it.id }
            val output = when (mode) {
                ImportMode.ADD_NEW -> {
                    val nextByPhase = RulePhase.entries.associateWith { phase ->
                        current.filter { it.phase == phase }.maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
                    }.toMutableMap()
                    val additions = imported.mapNotNull { rule ->
                        if (rule.id in currentById) {
                            skipped++
                            null
                        } else {
                            val order = requireNotNull(nextByPhase[rule.phase])
                            nextByPhase[rule.phase] = order + 1
                            added++
                            rule.copy(sortOrder = order)
                        }
                    }
                    current + additions
                }
                ImportMode.UPDATE_MATCHING -> {
                    val nextByPhase = RulePhase.entries.associateWith { phase ->
                        current.filter { it.phase == phase }.maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
                    }.toMutableMap()
                    val replacements = imported.associateBy { it.id }
                    val retainedOrUpdated = current.map { existing ->
                        val incoming = replacements[existing.id] ?: return@map existing
                        updated++
                        if (incoming.phase == existing.phase) {
                            incoming.copy(sortOrder = existing.sortOrder)
                        } else {
                            val order = requireNotNull(nextByPhase[incoming.phase])
                            nextByPhase[incoming.phase] = order + 1
                            incoming.copy(sortOrder = order)
                        }
                    }
                    val additions = imported.filter { it.id !in currentById }.map { incoming ->
                        val order = requireNotNull(nextByPhase[incoming.phase])
                        nextByPhase[incoming.phase] = order + 1
                        added++
                        incoming.copy(sortOrder = order)
                    }
                    retainedOrUpdated + additions
                }
                ImportMode.REPLACE_ALL -> {
                    added = imported.size
                    imported
                }
            }
            val normalized = normalizeOrders(output)
            require(normalized.size <= Constants.MAX_CUSTOM_RULES) { "Too many rules" }
            compiler.compileAll(normalized)
            ruleDao.deleteAll()
            ruleDao.upsertAll(normalized.map(codec::toEntity))
            snapshotDao.prune(Constants.MAX_RULE_SNAPSHOTS)
        }
        refreshSnapshotLocked()
        ImportResult(added, updated, skipped)
    }

    suspend fun rollbackLatest(): Boolean = mutex.withLock {
        val latest = snapshotDao.latest() ?: return@withLock false
        val rules = codec.decodeBundle(latest.bundleJson).rules
        compiler.compileAll(rules)
        database.withTransaction {
            ruleDao.deleteAll()
            ruleDao.upsertAll(normalizeOrders(rules).map(codec::toEntity))
        }
        refreshSnapshotLocked()
        true
    }

    private suspend fun refreshSnapshotLocked() {
        val rules = ruleDao.getAll().map(codec::fromEntity)
        runtimeSnapshot = RuleSnapshot(compiler.compileAll(rules), _revision.value + 1)
        _revision.value = runtimeSnapshot.revision
        initialized = true
    }

    private suspend fun createSnapshotLocked(rules: List<CustomUrlRule>) {
        val json = codec.encodeBundle(rules)
        require(json.toByteArray().size <= Constants.MAX_RULE_BUNDLE_BYTES) {
            "Current rules are too large to snapshot"
        }
        snapshotDao.insert(RuleSnapshotEntity(createdAt = System.currentTimeMillis(), bundleJson = json))
    }

    private suspend fun reindexLocked(rules: List<CustomUrlRule>) {
        ruleDao.upsertAll(normalizeOrders(rules).map(codec::toEntity))
    }

    private fun normalizeOrders(rules: List<CustomUrlRule>): List<CustomUrlRule> =
        RulePhase.entries.flatMap { phase ->
            rules.filter { it.phase == phase }
                .sortedWith(compareBy<CustomUrlRule>({ it.sortOrder }, { it.id }))
                .mapIndexed { index, rule -> rule.copy(sortOrder = index) }
        }
}
