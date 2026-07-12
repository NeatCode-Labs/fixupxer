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

package com.fixupxer.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `custom_rules` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `enabled` INTEGER NOT NULL,
                `sort_order` INTEGER NOT NULL,
                `phase` TEXT NOT NULL,
                `contexts_json` TEXT NOT NULL,
                `include_scope_json` TEXT NOT NULL,
                `exclude_scopes_json` TEXT NOT NULL,
                `action_json` TEXT NOT NULL,
                `stop_after_match` INTEGER NOT NULL,
                `test_vectors_json` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_custom_rules_phase_sort_order` " +
                "ON `custom_rules` (`phase`, `sort_order`)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `rule_snapshots` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `bundle_json` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}
