// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fixupxer

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.fixupxer.ui.ShareActivity
import com.fixupxer.ui.helpers.UrlActionHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21, 23, 33])
class UrlShareChooserTest {
    private lateinit var activity: AppCompatActivity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(AppCompatActivity::class.java)
            .setup()
            .get()
    }

    @After
    fun tearDown() {
        activity.finish()
    }

    @Test
    fun `chooser carries a plain text send intent and loop prevention marker`() {
        val url = "https://example.com/clean"

        val chooser = UrlActionHelper.createShareChooser(activity, url)
        val sendIntent = chooserPayload(chooser)

        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertEquals(activity.getString(R.string.share_via), chooser.getCharSequenceExtra(Intent.EXTRA_TITLE))
        assertNotNull(sendIntent)
        assertEquals(Intent.ACTION_SEND, sendIntent?.action)
        assertEquals("text/plain", sendIntent?.type)
        assertEquals(url, sendIntent?.getStringExtra(Intent.EXTRA_TEXT))
        assertTrue(sendIntent?.getBooleanExtra(UrlActionHelper.EXTRA_SKIP_AUTO_SHARE, false) == true)
    }

    @Test
    fun `chooser excludes FixupXer where supported and keeps marker fallback on older Android`() {
        val chooser = UrlActionHelper.createShareChooser(activity, "https://example.com/clean")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val excluded = chooser.getParcelableArrayExtra(Intent.EXTRA_EXCLUDE_COMPONENTS)
            assertNotNull(excluded)
            assertEquals(
                ComponentName(activity.packageName, ShareActivity::class.java.name),
                excluded?.single(),
            )
        } else {
            assertNull(chooser.getParcelableArrayExtra(Intent.EXTRA_EXCLUDE_COMPONENTS))
            assertTrue(
                chooserPayload(chooser)
                    ?.getBooleanExtra(UrlActionHelper.EXTRA_SKIP_AUTO_SHARE, false) == true,
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun chooserPayload(chooser: Intent): Intent? =
        chooser.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
}
