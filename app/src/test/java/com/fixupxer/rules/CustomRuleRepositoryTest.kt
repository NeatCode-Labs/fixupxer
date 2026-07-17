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

import androidx.room.Room
import com.fixupxer.PreferencesManager
import com.fixupxer.data.database.FixupXerDatabase
import com.fixupxer.processing.UrlNormalizer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CustomRuleRepositoryTest {
    private lateinit var database: FixupXerDatabase
    private lateinit var repository: CustomRuleRepository
    private val codec = RuleBundleCodec()
    private val compiler = RuleCompiler()

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        database = Room.inMemoryDatabaseBuilder(context, FixupXerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CustomRuleRepository(
            database,
            database.customRuleDao(),
            database.ruleSnapshotDao(),
            codec,
            compiler,
            RuleVectorRunner(
                compiler,
                CustomRuleEngine(
                    RuleMatcher(UrlNormalizer()),
                    RuleActionExecutor(UrlNormalizer())
                )
            ),
            PreferencesManager(context)
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `failed import is atomic`() = runTest {
        val original = rule("Original")
        repository.save(original)
        val invalid = codec.encodeBundle(
            listOf(rule("Invalid").copy(includeScope = RuleScope.DomainAndSubdomains("127.0.0.1")))
        )

        assertTrue(runCatching { repository.importBundle(invalid, ImportMode.REPLACE_ALL) }.isFailure)
        assertEquals(listOf(original.id), repository.getRules().map { it.id })
    }

    @Test
    fun `import creates rollback snapshot and restores prior rules`() = runTest {
        val original = rule("Original")
        val imported = rule("Imported")
        repository.save(original)

        repository.importBundle(codec.encodeBundle(listOf(imported)), ImportMode.REPLACE_ALL)
        assertEquals(listOf(imported.id), repository.getRules().map { it.id })
        assertTrue(repository.rollbackLatest())
        assertEquals(listOf(original.id), repository.getRules().map { it.id })
    }

    @Test
    fun `empty room with restored master switch is safe`() = runTest {
        repository.setEnabled(true)

        val snapshot = repository.awaitSnapshot()

        assertTrue(repository.isEnabled())
        assertTrue(snapshot.rules.isEmpty())
        assertFalse(snapshot.revision == 0L)
    }

    @Test
    fun `save blocks enabling rule with failing vectors`() = runTest {
        val disabled = rule("Draft").copy(
            enabled = false,
            testVectors = listOf(
                RuleTestVector("https://example.com/?x=1", "https://other.example/")
            )
        )
        repository.save(disabled)

        val failure = runCatching { repository.save(disabled.copy(enabled = true)) }.exceptionOrNull()

        assertTrue(failure is RuleActivationBlockedException)
        assertFalse(requireNotNull(repository.getRule(disabled.id)).enabled)
    }

    @Test
    fun `save allows enabled rule with zero vectors`() = runTest {
        val rule = rule("No vectors")

        repository.save(rule)

        assertTrue(requireNotNull(repository.getRule(rule.id)).enabled)
    }

    @Test
    fun `save allows enabled rule when all vectors pass`() = runTest {
        val rule = rule("Passing vectors").copy(
            testVectors = listOf(
                RuleTestVector("https://example.com/?x=1", "https://example.com/")
            )
        )

        repository.save(rule)

        assertTrue(requireNotNull(repository.getRule(rule.id)).enabled)
    }

    @Test
    fun `import preview reports failures and imported failing rules are disabled`() = runTest {
        val imported = rule("Broken import").copy(
            testVectors = listOf(
                RuleTestVector("https://example.com/?x=1", "https://other.example/")
            )
        )
        val json = codec.encodeBundle(listOf(imported))

        val preview = repository.previewImport(json, ImportMode.ADD_NEW)
        val result = repository.importBundle(json, ImportMode.ADD_NEW)

        assertEquals(1, preview.vectorFailures.size)
        assertEquals(imported.id, preview.vectorFailures.single().ruleId)
        assertEquals(1, preview.vectorFailures.single().failingVectorCount)
        assertEquals(preview.vectorFailures, result.vectorFailures)
        assertFalse(requireNotNull(repository.getRule(imported.id)).enabled)
    }

    private fun rule(name: String) = CustomUrlRule(
        name = name,
        action = RuleAction.RemoveAllParams
    )
}
