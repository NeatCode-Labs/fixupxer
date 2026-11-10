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
import org.junit.Assert.assertFalse
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
    private val youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val xUrl = "https://x.com/person/status/123"

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
    @Config(sdk = [24, 29, 32, 33])
    fun `main and share copy mark only final URLs with Link Guard findings`() {
        assertTrue(UrlActionHelper.copyToClipboard(anchor, activity, url))
        assertClipboard(url, sensitive = true)

        // Copying an ordinary URL after a sensitive one must replace the old
        // ClipData metadata, not carry the previous sensitive marker forward.
        listOf(youtubeUrl, xUrl).forEach { ordinaryUrl ->
            assertTrue(UrlActionHelper.copyToClipboard(anchor, activity, ordinaryUrl))
            assertClipboard(ordinaryUrl, sensitive = false)
        }

        shadowOf(Looper.getMainLooper()).idle()
        assertAppSnackbarPolicy()
    }

    @Test
    fun `history copy follows final URL sensitivity policy`() {
        val binding = ItemHistoryBinding.inflate(activity.layoutInflater, anchor, true)
        val holder = HistoryAdapter.HistoryViewHolder(binding, null, {}, anchor)
        holder.bind(UrlHistory(1, url, url, "Other", "Cleaned", 100L, "now"))

        binding.buttonCopy.performClick()
        assertClipboard(url, sensitive = true)

        holder.bind(UrlHistory(2, youtubeUrl, youtubeUrl, "YouTube", "Cleaned", 101L, "now"))
        binding.buttonCopy.performClick()
        assertClipboard(youtubeUrl, sensitive = false)

        shadowOf(Looper.getMainLooper()).idle()
        assertAppSnackbarPolicy()
    }

    @Test
    fun `post clean copy marks only final URLs with Link Guard findings`() {
        val preferences: PreferencesManager = mock()
        whenever(preferences.getActionMode()).thenReturn(PreferencesManager.ACTION_MODE_PRIORITY)
        whenever(preferences.getActionPriority()).thenReturn(listOf(PreferencesManager.ACTION_CLIPBOARD))

        PostCleanRunner(activity, preferences).run(Uri.parse(url))
        assertClipboard(url, sensitive = true)

        PostCleanRunner(activity, preferences).run(Uri.parse(youtubeUrl))
        assertClipboard(youtubeUrl, sensitive = false)

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

        assertFalse(UrlActionHelper.copyToClipboard(anchor, unavailable, url))
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

        assertClipboard(url, sensitive = false)
    }

    private fun assertClipboard(expectedUrl: String, sensitive: Boolean) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip!!
        assertEquals(expectedUrl, clip.getItemAt(0).text.toString())

        // ClipDescription extras are available from Android 7.0 (API 24).
        // For ordinary URLs, assert that the key is absent, not merely false.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val extras = clip.description.extras
            val hasSensitiveKey = extras?.containsKey(IS_SENSITIVE_KEY) == true
            assertEquals("IS_SENSITIVE key presence", sensitive, hasSensitiveKey)
            if (sensitive) {
                assertTrue(extras!!.getBoolean(IS_SENSITIVE_KEY))
            }
        }
    }

    private companion object {
        const val IS_SENSITIVE_KEY = "android.content.extra.IS_SENSITIVE"
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
