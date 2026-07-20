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

import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.fixupxer.ui.dialogs.AnimatedBottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@LooperMode(LooperMode.Mode.PAUSED)
class AnimatedBottomSheetDialogTest {

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
    fun `dialog configures native swipe behavior without Material dismiss animation`() {
        val dialog = createDialog(animatorsEnabled = true)

        assertEquals(BottomSheetBehavior.STATE_EXPANDED, dialog.behavior.state)
        assertTrue(dialog.behavior.skipCollapsed)
        assertTrue(dialog.behavior.isDraggable)
        assertTrue(dialog.behavior.isHideable)
        assertFalse(dialog.dismissWithAnimation)
    }

    @Test
    @Config(sdk = [21])
    fun `dialog loads on min sdk without modern Back API linkage`() {
        val dialog = createDialog(animatorsEnabled = true)

        assertEquals(BottomSheetBehavior.STATE_EXPANDED, dialog.behavior.state)
    }

    @Test
    fun `unlaid-out sheet cancel finishes immediately and keeps external listeners`() {
        val dialog = createDialog(animatorsEnabled = false)
        var showCount = 0
        var dismissCount = 0
        dialog.setOnShowListener { showCount++ }
        dialog.setOnDismissListener { dismissCount++ }
        dialog.setContentView(FrameLayout(dialog.context))
        dialog.show()

        dialog.dismissAnimated()
        shadowOf(activity.mainLooper).idle()

        assertFalse(dialog.isShowing)
        assertEquals(1, showCount)
        assertEquals(1, dismissCount)
    }

    @Test
    fun `programmatic cancel settles behavior to hidden like a swipe`() {
        val dialog = createShownDialog(animatorsEnabled = true)
        val sheet = layOutSheet(dialog)

        dialog.dismissAnimated()

        assertTrue(dialog.isShowing)
        assertEquals(BottomSheetBehavior.STATE_SETTLING, dialog.behavior.state)
        // Parity with swipe: the settle moves the layout position, never translationY.
        assertEquals(0f, sheet.translationY)
        shadowOf(activity.mainLooper).idleFor(Duration.ofSeconds(2))
        assertEquals(BottomSheetBehavior.STATE_HIDDEN, dialog.behavior.state)
        assertFalse(dialog.isShowing)
    }

    @Test
    fun `reduced motion still uses the swipe settle exit`() {
        val dialog = createShownDialog(animatorsEnabled = false)
        layOutSheet(dialog)

        dialog.dismissAnimated()

        assertEquals(BottomSheetBehavior.STATE_SETTLING, dialog.behavior.state)
        shadowOf(activity.mainLooper).idleFor(Duration.ofSeconds(2))
        assertFalse(dialog.isShowing)
    }

    @Test
    fun `direct dismiss during settle fires one onDismiss and no late onCancel`() {
        val dialog = createShownDialog(animatorsEnabled = true)
        layOutSheet(dialog)
        var dismissCount = 0
        var cancelCount = 0
        dialog.setOnDismissListener { dismissCount++ }
        dialog.setOnCancelListener { cancelCount++ }

        dialog.dismissAnimated()
        assertEquals(BottomSheetBehavior.STATE_SETTLING, dialog.behavior.state)
        dialog.dismiss()
        shadowOf(activity.mainLooper).idleFor(Duration.ofSeconds(2))

        assertFalse(dialog.isShowing)
        assertEquals(1, dismissCount)
        assertEquals(0, cancelCount)
    }

    @Test
    fun `predictive gesture exit finishes pending translation with slide animator`() {
        val dialog = createShownDialog(animatorsEnabled = true)
        val sheet = layOutSheet(dialog)
        sheet.translationY = 200f

        dialog.dismissAnimated()

        assertTrue(dialog.isShowing)
        shadowOf(activity.mainLooper).idleFor(Duration.ofMillis(150))
        assertTrue(sheet.translationY > 200f)
        shadowOf(activity.mainLooper).idleFor(Duration.ofSeconds(1))
        assertFalse(dialog.isShowing)
    }

    @Test
    fun `already hidden state takes terminal cancel path without second animation`() {
        val dialog = createShownDialog(animatorsEnabled = true)
        var dismissCount = 0
        dialog.setOnDismissListener { dismissCount++ }

        dialog.behavior.state = BottomSheetBehavior.STATE_HIDDEN
        dialog.dismissAnimated()
        shadowOf(activity.mainLooper).idle()

        assertFalse(dialog.isShowing)
        assertEquals(1, dismissCount)
    }

    @Test
    fun `settle completion after decor detach does not re-enter dismiss`() {
        val dialog = createShownDialog(animatorsEnabled = true)
        layOutSheet(dialog)
        var dismissCount = 0
        var cancelCount = 0
        dialog.setOnDismissListener { dismissCount++ }
        dialog.setOnCancelListener { cancelCount++ }

        dialog.dismissAnimated()
        assertEquals(BottomSheetBehavior.STATE_SETTLING, dialog.behavior.state)

        // Simulate the host Activity dying mid-exit: the window manager force
        // removes the dialog window without any dismiss() call. The late
        // STATE_HIDDEN settle callback must not re-enter Dialog.dismiss().
        dialog.window!!.windowManager.removeViewImmediate(dialog.window!!.decorView)
        shadowOf(activity.mainLooper).idleFor(Duration.ofSeconds(2))

        assertEquals(0, dismissCount)
        assertEquals(0, cancelCount)
    }

    @Test
    fun `predictive back translation clamps progress`() {
        assertEquals(0f, AnimatedBottomSheetDialog.translationForProgress(800f, -0.5f))
        assertEquals(200f, AnimatedBottomSheetDialog.translationForProgress(800f, 0.25f))
        assertEquals(800f, AnimatedBottomSheetDialog.translationForProgress(800f, 1.5f))
    }

    private fun createDialog(animatorsEnabled: Boolean): AnimatedBottomSheetDialog =
        AnimatedBottomSheetDialog(activity, 0, animatorsEnabled)

    private fun createShownDialog(animatorsEnabled: Boolean): AnimatedBottomSheetDialog =
        createDialog(animatorsEnabled).apply {
            setContentView(FrameLayout(context).apply { minimumHeight = 900 })
            show()
        }

    /**
     * Runs a real measure and layout pass through the dialog's CoordinatorLayout
     * so the behavior attaches its ViewDragHelper and can settle like on device.
     */
    private fun layOutSheet(dialog: AnimatedBottomSheetDialog): FrameLayout {
        val decor = dialog.window!!.decorView
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, 1080, 1920)
        return dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)!!
    }
}
