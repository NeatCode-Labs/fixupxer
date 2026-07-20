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

import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import com.google.android.material.button.MaterialButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Guards action-button ergonomics shared by the Main and Share screens.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HistoryButtonStyleTest {

    @Test
    fun `primary action buttons use a prominent 56dp visual container`() {
        val context = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            R.style.Theme_FixupXer,
        )
        val expectedHeight =
            context.resources.getDimensionPixelSize(R.dimen.cta_height)
        val expectedIconSize =
            context.resources.getDimensionPixelSize(R.dimen.icon_small_size)
        val expectedCornerRadius =
            context.resources.getDimensionPixelSize(R.dimen.button_corner_radius)

        listOf(R.layout.activity_main, R.layout.activity_share).forEach { layoutRes ->
            val root = LayoutInflater.from(context).inflate(layoutRes, null)
            listOf(R.id.buttonOpen, R.id.buttonCopy, R.id.buttonShare).forEach { buttonId ->
                val button = root.findViewById<MaterialButton>(buttonId)
                assertEquals(expectedHeight, button.layoutParams.height)
                assertEquals(0, button.insetTop)
                assertEquals(0, button.insetBottom)
                assertEquals(expectedIconSize, button.iconSize)
                assertEquals(expectedCornerRadius, button.cornerRadius)
            }
        }
    }

    @Test
    fun `history buttons keep touch target centered icon and description`() {
        val context = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            R.style.Theme_FixupXer,
        )
        val expectedSize =
            context.resources.getDimensionPixelSize(R.dimen.min_touch_target_size)

        listOf(R.layout.activity_main, R.layout.activity_share).forEach { layoutRes ->
            val root = LayoutInflater.from(context).inflate(layoutRes, null)
            val button = root.findViewById<MaterialButton>(R.id.buttonHistory)
            assertNotNull("buttonHistory missing in $layoutRes", button)

            assertEquals(expectedSize, button.layoutParams.width)
            assertEquals(expectedSize, button.layoutParams.height)
            assertEquals(Gravity.CENTER, button.gravity)
            assertEquals(0, button.insetTop)
            assertEquals(0, button.insetBottom)
            assertEquals(MaterialButton.ICON_GRAVITY_TEXT_START, button.iconGravity)
            assertEquals(0, button.iconPadding)
            assertNotNull(button.icon)
            assertTrue(button.text.isNullOrEmpty())
            assertEquals(
                context.getString(R.string.history_title),
                button.contentDescription,
            )
        }
    }
}
