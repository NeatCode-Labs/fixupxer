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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_rules",
    indices = [Index(value = ["phase", "sort_order"])]
)
data class CustomRuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val enabled: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    val phase: String,
    @ColumnInfo(name = "contexts_json") val contextsJson: String,
    @ColumnInfo(name = "include_scope_json") val includeScopeJson: String,
    @ColumnInfo(name = "exclude_scopes_json") val excludeScopesJson: String,
    @ColumnInfo(name = "action_json") val actionJson: String,
    @ColumnInfo(name = "stop_after_match") val stopAfterMatch: Boolean,
    @ColumnInfo(name = "test_vectors_json") val testVectorsJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(tableName = "rule_snapshots")
data class RuleSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "bundle_json") val bundleJson: String
)
