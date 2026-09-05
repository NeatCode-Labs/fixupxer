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

import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.fixupxer.domain.model.UrlHistory
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.ui.adapters.HistoryAdapter
import com.fixupxer.ui.dialogs.HistoryDialogHelper
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HistoryDialogHelperTest {

    private lateinit var activity: AppCompatActivity

    @Before
    fun setUp() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        activity = controller.get()
        activity.setTheme(R.style.Theme_FixupXer)
        controller.setup()
    }

    @After
    fun tearDown() {
        ShadowDialog.getLatestDialog()?.dismiss()
        activity.finish()
    }

    @Test
    fun `recording off keeps retained entries visible and passive row tap keeps sheet open`() {
        val preferences: PreferencesManager = mock()
        whenever(preferences.isHistoryEnabled()).thenReturn(false)
        val repository: HistoryRepository = mock()
        whenever(repository.getAllHistory()).thenReturn(
            flowOf(
                listOf(
                    UrlHistory(
                        id = 1,
                        originalUrl = "https://example.com/?utm_source=test",
                        cleanedUrl = "https://example.com/",
                        platform = "Other",
                        conversionType = "Tracking removed",
                        timestamp = 1,
                        timeAgo = "now",
                    )
                )
            )
        )
        var settingsChanged = 0
        var dismissed = 0

        HistoryDialogHelper(
            context = activity,
            lifecycleOwner = activity,
            historyRepository = repository,
            preferencesManager = preferences,
            onSettingsChanged = { settingsChanged++ },
            onDismiss = { dismissed++ },
        ).showHistoryDialog()
        shadowOf(Looper.getMainLooper()).idle()

        val dialog = ShadowDialog.getLatestDialog()
        val recycler = dialog.findViewById<RecyclerView>(R.id.recyclerViewHistory)!!
        assertEquals(View.VISIBLE, dialog.findViewById<View>(R.id.recordingOffBanner)?.visibility)
        assertEquals(View.VISIBLE, recycler.visibility)
        assertEquals(1, recycler.adapter?.itemCount)

        val adapter = recycler.adapter as HistoryAdapter
        val holder = adapter.onCreateViewHolder(recycler, 0)
        adapter.onBindViewHolder(holder, 0)
        assertFalse(holder.itemView.isClickable)
        holder.itemView.performClick()
        assertTrue(dialog.isShowing)

        dialog.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(
            R.id.switchHistoryEnabled
        )!!.isChecked = true
        verify(preferences).setHistoryEnabled(true)
        assertEquals(1, settingsChanged)

        dialog.dismiss()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, dismissed)
    }

    @Test
    fun `pending legacy limit offers supported value with migration explanation`() {
        val preferences: PreferencesManager = mock()
        whenever(preferences.isHistoryEnabled()).thenReturn(true)
        whenever(preferences.isHistoryLimitMigrationPending()).thenReturn(true)
        whenever(preferences.getSupportedHistoryLimit()).thenReturn(10_000)
        val repository: HistoryRepository = mock()
        whenever(repository.getAllHistory()).thenReturn(flowOf(emptyList()))

        HistoryDialogHelper(
            context = activity,
            lifecycleOwner = activity,
            historyRepository = repository,
            preferencesManager = preferences,
        ).showHistoryDialog()
        shadowOf(Looper.getMainLooper()).idle()

        ShadowDialog.getLatestDialog().findViewById<View>(R.id.btnMaxEntries)!!.performClick()
        val limitDialog = ShadowDialog.getLatestDialog()

        assertEquals(
            "10000",
            limitDialog.findViewById<android.widget.EditText>(R.id.editTextMaxEntries)?.text.toString(),
        )
        assertEquals(
            activity.getString(R.string.history_limit_legacy_migration, 10_000),
            limitDialog.findViewById<com.google.android.material.textfield.TextInputLayout>(
                R.id.textInputMaxEntries
            )?.helperText,
        )
    }

    @Test
    fun `history limit persists only after trim succeeds`() = runTest {
        val preferences: PreferencesManager = mock()
        val repository: HistoryRepository = mock()
        var settingsChanged = 0
        val helper = HistoryDialogHelper(
            context = activity,
            lifecycleOwner = activity,
            historyRepository = repository,
            preferencesManager = preferences,
            onSettingsChanged = { settingsChanged++ },
        )

        assertTrue(helper.commitHistoryLimit(42))

        inOrder(repository, preferences) {
            verify(repository).trimHistory(42)
            verify(preferences).setMaxHistoryEntries(42)
        }
        assertEquals(1, settingsChanged)
    }

    @Test
    fun `trim failure keeps old history limit and skips callback`() = runTest {
        val preferences: PreferencesManager = mock()
        val repository: HistoryRepository = mock()
        whenever(repository.trimHistory(42)).thenThrow(IllegalStateException("db failed"))
        var settingsChanged = 0
        val helper = HistoryDialogHelper(
            context = activity,
            lifecycleOwner = activity,
            historyRepository = repository,
            preferencesManager = preferences,
            onSettingsChanged = { settingsChanged++ },
        )

        assertFalse(helper.commitHistoryLimit(42))

        verify(preferences, never()).setMaxHistoryEntries(42)
        assertEquals(0, settingsChanged)
    }

    @Test
    fun `failed deletion reports error without offering undo`() = runTest {
        val repository: HistoryRepository = mock()
        whenever(repository.deleteHistory(historyEntry.id)).thenThrow(IllegalStateException("db failed"))
        val dialog = showHistoryWithEntry(repository)

        clickDelete(dialog)

        assertEquals(
            activity.getString(R.string.history_delete_failed),
            dialog.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.text,
        )
        assertFalse(
            dialog.findViewById<View>(com.google.android.material.R.id.snackbar_action)?.visibility == View.VISIBLE
        )
        verify(repository, never()).restoreHistory(any())
    }

    @Test
    fun `undo restores original entry snapshot`() = runTest {
        val repository: HistoryRepository = mock()
        val dialog = showHistoryWithEntry(repository)

        clickDelete(dialog)
        assertEquals(
            activity.getString(R.string.history_entry_deleted),
            dialog.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.text,
        )
        clickUndo(dialog)

        inOrder(repository) {
            verify(repository).deleteHistory(historyEntry.id)
            verify(repository).restoreHistory(historyEntry)
        }
        verify(repository, never()).insertHistory(any(), any(), any(), any())
    }

    @Test
    fun `failed undo shows error and allows restoring the same snapshot again`() = runTest {
        val repository: HistoryRepository = mock()
        whenever(repository.restoreHistory(historyEntry))
            .thenThrow(IllegalStateException("db failed"))
            .thenReturn(Unit)
        val dialog = showHistoryWithEntry(repository)

        clickDelete(dialog)
        clickUndo(dialog)

        assertEquals(
            activity.getString(R.string.history_restore_failed),
            dialog.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.text,
        )
        clickUndo(dialog)
        verify(repository, org.mockito.kotlin.times(2)).restoreHistory(historyEntry)
    }

    @Test
    fun `clear failure keeps confirmation open and retry enabled`() = runTest {
        val repository: HistoryRepository = mock()
        whenever(repository.deleteAllHistory()).thenThrow(IllegalStateException("db failed"))
        val historyDialog = showHistoryWithEntry(repository)
        historyDialog.findViewById<View>(R.id.btnClearAll)!!.performClick()
        val confirmDialog = ShadowDialog.getLatestDialog()

        confirmDialog.findViewById<View>(R.id.buttonClearAll)!!.performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(confirmDialog.isShowing)
        assertTrue(confirmDialog.findViewById<View>(R.id.buttonClearAll)!!.isEnabled)
        assertEquals(
            activity.getString(R.string.history_clear_failed),
            confirmDialog.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.text,
        )
    }

    private val historyEntry = UrlHistory(
        id = 42,
        originalUrl = "https://example.com/old?utm_source=test",
        cleanedUrl = "https://example.com/old",
        platform = "Other",
        conversionType = "Tracking removed",
        timestamp = 1_000,
        timeAgo = "a while ago",
    )

    private fun showHistoryWithEntry(repository: HistoryRepository): android.app.Dialog {
        whenever(repository.getAllHistory()).thenReturn(flowOf(listOf(historyEntry)))
        HistoryDialogHelper(activity, activity, repository, mock()).showHistoryDialog()
        shadowOf(Looper.getMainLooper()).idle()
        return ShadowDialog.getLatestDialog()
    }

    private fun clickDelete(dialog: android.app.Dialog) {
        val recycler = dialog.findViewById<RecyclerView>(R.id.recyclerViewHistory)!!
        val adapter = recycler.adapter as HistoryAdapter
        val holder = adapter.onCreateViewHolder(recycler, 0)
        adapter.onBindViewHolder(holder, 0)
        holder.itemView.findViewById<View>(R.id.buttonDelete)!!.performClick()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
    }

    private fun clickUndo(dialog: android.app.Dialog) {
        dialog.findViewById<View>(com.google.android.material.R.id.snackbar_action)!!.performClick()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
    }
}
