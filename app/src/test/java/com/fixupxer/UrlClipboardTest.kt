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

import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fixupxer.databinding.ItemHistoryBinding
import com.fixupxer.domain.model.UrlHistory
import com.fixupxer.ui.adapters.HistoryAdapter
import com.fixupxer.ui.helpers.UrlActionHelper
import com.fixupxer.utils.PostCleanRunner
import com.fixupxer.utils.UrlClipboard
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 32, 33])
class UrlClipboardTest {
    private lateinit var activity: AppCompatActivity
    private lateinit var anchor: CoordinatorLayout
    private val url = "https://example.com/private?token=kept-functional-value"

    @Before
    fun setUp() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        activity = controller.get()
        activity.setTheme(R.style.Theme_FixupXer)
        controller.setup()
        anchor = CoordinatorLayout(activity)
        activity.setContentView(anchor)
        ShadowToast.reset()
    }

    @After
    fun tearDown() = activity.finish()

    @Test
    fun `main and share copy mask preview and show app feedback only before Android 13`() {
        UrlActionHelper.copyToClipboard(anchor, activity, url)
        shadowOf(Looper.getMainLooper()).idle()

        assertSensitiveClipboard()
        assertAppSnackbarPolicy()
    }

    @Test
    fun `history copy follows the same privacy and feedback policy`() {
        val binding = ItemHistoryBinding.inflate(activity.layoutInflater, anchor, true)
        val holder = HistoryAdapter.HistoryViewHolder(binding, null, {}, anchor)
        holder.bind(UrlHistory(1, url, url, "Other", "Cleaned", 100L, "now"))

        binding.buttonCopy.performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertSensitiveClipboard()
        assertAppSnackbarPolicy()
    }

    @Test
    fun `post clean copy masks preview without duplicate Android 13 toast`() {
        val preferences: PreferencesManager = mock()
        whenever(preferences.getActionMode()).thenReturn(PreferencesManager.ACTION_MODE_PRIORITY)
        whenever(preferences.getActionPriority()).thenReturn(listOf(PreferencesManager.ACTION_CLIPBOARD))

        PostCleanRunner(activity, preferences).run(Uri.parse(url))

        assertSensitiveClipboard()
        if (Build.VERSION.SDK_INT < 33) {
            assertEquals(activity.getString(R.string.url_copied), ShadowToast.getTextOfLatestToast())
        } else {
            assertNull(ShadowToast.getTextOfLatestToast())
        }
    }

    @Test
    @Config(sdk = [33])
    fun `unavailable clipboard shows error instead of success`() {
        val unavailable = object : ContextWrapper(activity) {
            override fun getSystemService(name: String): Any? =
                if (name == Context.CLIPBOARD_SERVICE) null else super.getSystemService(name)
        }

        UrlActionHelper.copyToClipboard(anchor, unavailable, url)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(
            activity.getString(R.string.error_copying_url),
            anchor.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.text,
        )
    }

    @Test
    @Config(sdk = [21])
    fun `copy still works below clipboard extras API`() {
        UrlClipboard.copy(activity, url)

        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals(url, clipboard.primaryClip!!.getItemAt(0).text.toString())
    }

    private fun assertSensitiveClipboard() {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip!!
        assertEquals(url, clip.getItemAt(0).text.toString())
        assertTrue(clip.description.extras!!.getBoolean("android.content.extra.IS_SENSITIVE"))
    }

    private fun assertAppSnackbarPolicy() {
        val feedback = anchor.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        if (Build.VERSION.SDK_INT < 33) {
            assertEquals(activity.getString(R.string.url_copied), feedback?.text)
            assertEquals(View.VISIBLE, feedback?.visibility)
        } else {
            assertNull(feedback)
        }
    }
}
