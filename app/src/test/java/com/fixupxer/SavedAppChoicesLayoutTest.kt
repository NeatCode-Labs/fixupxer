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
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fixupxer.backup.RememberedRoute
import com.fixupxer.backup.RememberedRouteKind
import com.fixupxer.ui.adapters.SavedAppChoiceRow
import com.fixupxer.ui.adapters.SavedAppChoicesAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SavedAppChoicesLayoutTest {

    @Test
    fun `dialog lets recycler adapt to available dialog height`() {
        val context = themedContext()
        val root = LayoutInflater.from(context)
            .inflate(R.layout.dialog_remembered_destinations, null)
        val recycler = root.findViewById<RecyclerView>(R.id.recyclerViewSavedAppChoices)

        assertNotNull(recycler)
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, recycler.layoutParams.height)
        assertTrue(recycler.isNestedScrollingEnabled)
    }

    @Test
    fun `row shows exact route details and only delete button deletes`() {
        val context = themedContext()
        var deletedHost: String? = null
        val adapter = SavedAppChoicesAdapter { deletedHost = it }
        val row = SavedAppChoiceRow(
            host = "www.example.com",
            route = RememberedRoute(
                kind = RememberedRouteKind.BROWSER,
                packageName = "org.example.browser",
            ),
            appLabel = "Example Browser",
            installed = false,
        )
        adapter.submitList(listOf(row))
        shadowOf(Looper.getMainLooper()).idle()

        val parent = FrameLayout(context)
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        val item = holder.itemView
        val delete = item.findViewById<ImageButton>(R.id.buttonDeleteSavedChoice)

        assertEquals(
            row.host,
            item.findViewById<TextView>(R.id.textSavedChoiceHost).text.toString(),
        )
        assertEquals(
            row.route.packageName,
            item.findViewById<TextView>(R.id.textSavedChoicePackage).text.toString(),
        )
        assertEquals(
            "Example Browser",
            item.findViewById<TextView>(R.id.textSavedChoiceAppLabel).text.toString(),
        )
        assertEquals(
            View.VISIBLE,
            item.findViewById<View>(R.id.textSavedChoiceMissing).visibility,
        )
        assertFalse(item.isClickable)
        item.performClick()
        assertNull(deletedHost)

        val touchTarget =
            context.resources.getDimensionPixelSize(R.dimen.min_touch_target_size)
        assertEquals(touchTarget, delete.layoutParams.width)
        assertEquals(touchTarget, delete.layoutParams.height)
        assertTrue(delete.contentDescription.toString().contains(row.host))
        delete.performClick()
        assertEquals(row.host, deletedHost)
    }

    private fun themedContext() = ContextThemeWrapper(
        RuntimeEnvironment.getApplication(),
        R.style.Theme_FixupXer,
    )
}
