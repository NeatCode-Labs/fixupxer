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
            RuleCompiler(),
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

    private fun rule(name: String) = CustomUrlRule(
        name = name,
        action = RuleAction.RemoveAllParams
    )
}
