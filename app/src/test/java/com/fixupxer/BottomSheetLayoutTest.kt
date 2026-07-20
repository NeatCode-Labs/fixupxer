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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BottomSheetLayoutTest {

    @Test
    fun `proxy picker layout includes decorative drag handle`() {
        val context = themedContext()
        val root = LayoutInflater.from(context).inflate(R.layout.dialog_proxy_picker, null)
        val handle = root.findViewById<View>(R.id.proxyPickerDragHandle)

        assertNotNull(handle)
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, handle.importantForAccessibility)
        assertEquals(
            context.resources.getDimensionPixelSize(R.dimen.padding_xlarge),
            handle.layoutParams.width,
        )
        assertEquals(
            context.resources.getDimensionPixelSize(R.dimen.margin_extra_small),
            handle.layoutParams.height,
        )
    }

    @Test
    fun `configuration status layout omits removed footnote`() {
        val context = themedContext()
        val root = LayoutInflater.from(context).inflate(R.layout.dialog_configuration_status, null)

        assertFalse(
            layoutContainsTextFragment(
                root,
                "Apps with verified App Links may open their links directly",
            ),
        )
    }

    @Test
    fun `settings browser integration card omits inline instructions`() {
        val context = themedContext()
        val root = LayoutInflater.from(context).inflate(R.layout.activity_settings, null)

        assertFalse(
            layoutContainsTextFragment(
                root,
                "Clean eligible web links locally before handing them to another app.",
            ),
        )
        assertFalse(
            layoutContainsTextFragment(
                root,
                "Setup: 1. Enable FixupXer below.",
            ),
        )
    }

    @Test
    fun `remembered destinations description lives in its dialog`() {
        val context = themedContext()
        val settings = LayoutInflater.from(context).inflate(R.layout.activity_settings, null)
        val dialog = LayoutInflater.from(context).inflate(R.layout.dialog_remembered_destinations, null)
        val description = context.getString(R.string.remembered_destinations_scope)

        assertFalse(layoutContainsTextFragment(settings, description))
        assertTrue(layoutContainsTextFragment(dialog, description))
    }

    @Test
    fun `history urls can expand for large text`() {
        val context = themedContext()
        val item = LayoutInflater.from(context).inflate(R.layout.item_history, null)

        listOf(R.id.textViewOriginalUrl, R.id.textViewProcessedUrl).forEach { id ->
            val text = item.findViewById<TextView>(id)
            assertEquals(Int.MAX_VALUE, text.maxLines)
            assertNull(text.ellipsize)
        }
    }

    @Test
    fun `settings delegates browser controls to browser sub screen`() {
        val context = themedContext()
        val settings = LayoutInflater.from(context).inflate(R.layout.activity_settings, null)
        val browser = LayoutInflater.from(context).inflate(R.layout.activity_browser_settings, null)

        assertNotNull(settings.findViewById<View>(R.id.browserModeNavigation))
        assertNull(settings.findViewById<View>(R.id.switchBrowserMode))
        assertNull(settings.findViewById<View>(R.id.actionPrioritySection))

        val actionPriority = browser.findViewById<View>(R.id.actionPrioritySection)
        val actionMode = browser.findViewById<View>(R.id.radioGroupActionMode)
        assertTrue(actionPriority.parent === actionMode.parent)

        val settingsContainer = settings.findViewById<View>(R.id.browserModeNavigation).parent
            ?.parent
            ?.parent as ViewGroup
        val appearanceCard = directChildOf(
            settingsContainer,
            settings.findViewById(R.id.themeToggleGroup),
        )
        val layoutCard = directChildOf(
            settingsContainer,
            settings.findViewById(R.id.handToggleGroup),
        )
        assertTrue(appearanceCard === layoutCard)
    }

    private fun themedContext() = ContextThemeWrapper(
        RuntimeEnvironment.getApplication(),
        R.style.Theme_FixupXer,
    )

    private fun layoutContainsTextFragment(root: View, textFragment: String): Boolean {
        if (root is TextView && root.text?.toString()?.contains(textFragment) == true) {
            return true
        }
        if (root is android.view.ViewGroup) {
            for (index in 0 until root.childCount) {
                if (layoutContainsTextFragment(root.getChildAt(index), textFragment)) {
                    return true
                }
            }
        }
        return false
    }

    private fun directChildOf(parent: ViewGroup, descendant: View): View {
        var current = descendant
        while (current.parent !== parent) {
            current = current.parent as View
        }
        return current
    }
}
