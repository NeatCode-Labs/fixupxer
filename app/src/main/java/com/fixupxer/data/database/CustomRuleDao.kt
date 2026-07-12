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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRuleDao {
    @Query("SELECT * FROM custom_rules ORDER BY phase ASC, sort_order ASC, id ASC")
    fun observeAll(): Flow<List<CustomRuleEntity>>

    @Query("SELECT * FROM custom_rules ORDER BY phase ASC, sort_order ASC, id ASC")
    suspend fun getAll(): List<CustomRuleEntity>

    @Query("SELECT * FROM custom_rules WHERE id = :id")
    suspend fun getById(id: String): CustomRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: CustomRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<CustomRuleEntity>)

    @Delete
    suspend fun delete(rule: CustomRuleEntity)

    @Query("DELETE FROM custom_rules WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM custom_rules")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM custom_rules WHERE enabled = 1")
    fun observeEnabledCount(): Flow<Int>
}

@Dao
interface RuleSnapshotDao {
    @Insert
    suspend fun insert(snapshot: RuleSnapshotEntity): Long

    @Query("SELECT * FROM rule_snapshots ORDER BY created_at DESC, id DESC")
    suspend fun getAll(): List<RuleSnapshotEntity>

    @Query("SELECT * FROM rule_snapshots ORDER BY created_at DESC, id DESC LIMIT 1")
    suspend fun latest(): RuleSnapshotEntity?

    @Query(
        "DELETE FROM rule_snapshots WHERE id NOT IN " +
            "(SELECT id FROM rule_snapshots ORDER BY created_at DESC, id DESC LIMIT :keep)"
    )
    suspend fun prune(keep: Int)
}
