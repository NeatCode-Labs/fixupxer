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

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.data.database.FixupXerDatabase
import com.fixupxer.data.database.MIGRATION_1_2
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomRuleMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FixupXerDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2PreservesHistoryAndCreatesRuleTables() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL(
                """
                INSERT INTO url_history
                (id, originalUrl, cleanedUrl, platform, conversionType, timestamp)
                VALUES (1, 'https://example.com/?utm_source=x',
                'https://example.com/', 'Other', 'Tracking removed', 123)
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)
        migrated.query("SELECT COUNT(*) FROM url_history").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM custom_rules").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM rule_snapshots").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
        migrated.close()
    }

    private companion object {
        const val DB_NAME = "custom-rule-migration-test"
    }
}
