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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import com.fixupxer.ui.helpers.DominantHandLayoutHelper
import com.fixupxer.ui.helpers.DominantHandLayoutHelper.PhysicalEdge
import com.fixupxer.ui.helpers.SmartFooterHelper
import com.fixupxer.ui.helpers.SmartFooterHelper.FooterMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SmartFooterHelperTest {

    @Test
    fun `decision models normal and small footer geometry`() {
        val normal = decision(availableHeight = 800, edge = PhysicalEdge.LEFT)
        assertEquals(FooterMode.NORMAL, normal.mode)
        assertEquals(NORMAL_BOTTOM_MARGIN, normal.historyBottomMargin)
        assertEquals(0, normal.footerPaddingLeft)
        assertEquals(0, normal.footerPaddingRight)

        val smallLeft = decision(availableHeight = 500, edge = PhysicalEdge.LEFT)
        assertEquals(FooterMode.SMALL, smallLeft.mode)
        assertEquals(PARENT_BOTTOM_MARGIN + SYSTEM_BOTTOM_INSET, smallLeft.historyBottomMargin)
        assertEquals(HISTORY_WIDTH + HISTORY_GAP, smallLeft.footerPaddingLeft)
        assertEquals(0, smallLeft.footerPaddingRight)

        val smallRight = decision(availableHeight = 500, edge = PhysicalEdge.RIGHT)
        assertEquals(0, smallRight.footerPaddingLeft)
        assertEquals(HISTORY_WIDTH + HISTORY_GAP, smallRight.footerPaddingRight)
    }

    @Test
    fun `runtime layout reparents footer and clears stale physical padding`() {
        val fixture = createFixture()

        applyHand(fixture, PreferencesManager.DOMINANT_HAND_RIGHT)
        SmartFooterHelper.applyLayout(
            parentLayout = fixture.parent,
            scrollView = fixture.scrollView,
            footer = fixture.footer,
            historyAnchor = fixture.history,
            decision = decision(availableHeight = 500, edge = PhysicalEdge.LEFT),
            footerTopMarginPx = FOOTER_TOP_MARGIN,
        )

        assertSame(fixture.scrollContent, fixture.footer.parent)
        assertEquals(HISTORY_WIDTH + HISTORY_GAP, fixture.footer.paddingLeft)
        assertEquals(0, fixture.footer.paddingRight)
        assertEquals(
            ConstraintLayout.LayoutParams.PARENT_ID,
            fixture.scrollParams().bottomToBottom,
        )
        assertEquals(
            ConstraintLayout.LayoutParams.PARENT_ID,
            fixture.historyParams().bottomToBottom,
        )
        assertEquals(
            PARENT_BOTTOM_MARGIN + SYSTEM_BOTTOM_INSET,
            fixture.historyParams().bottomMargin,
        )

        applyHand(fixture, PreferencesManager.DOMINANT_HAND_LEFT)
        SmartFooterHelper.applyLayout(
            parentLayout = fixture.parent,
            scrollView = fixture.scrollView,
            footer = fixture.footer,
            historyAnchor = fixture.history,
            decision = decision(availableHeight = 500, edge = PhysicalEdge.RIGHT),
            footerTopMarginPx = FOOTER_TOP_MARGIN,
        )

        assertEquals(0, fixture.footer.paddingLeft)
        assertEquals(HISTORY_WIDTH + HISTORY_GAP, fixture.footer.paddingRight)

        SmartFooterHelper.applyLayout(
            parentLayout = fixture.parent,
            scrollView = fixture.scrollView,
            footer = fixture.footer,
            historyAnchor = fixture.history,
            decision = decision(availableHeight = 800, edge = PhysicalEdge.RIGHT),
            footerTopMarginPx = FOOTER_TOP_MARGIN,
        )

        assertSame(fixture.parent, fixture.footer.parent)
        assertEquals(0, fixture.footer.paddingLeft)
        assertEquals(0, fixture.footer.paddingRight)
        assertEquals(fixture.footer.id, fixture.scrollParams().bottomToTop)
        assertEquals(
            ConstraintLayout.LayoutParams.UNSET,
            fixture.scrollParams().bottomToBottom,
        )
        assertEquals(fixture.footer.id, fixture.historyParams().bottomToTop)
        assertEquals(
            ConstraintLayout.LayoutParams.UNSET,
            fixture.historyParams().bottomToBottom,
        )
        assertEquals(NORMAL_BOTTOM_MARGIN, fixture.historyParams().bottomMargin)
    }

    private fun decision(
        availableHeight: Int,
        edge: PhysicalEdge,
    ): SmartFooterHelper.FooterLayoutDecision = SmartFooterHelper.decide(
        availableHeightPx = availableHeight,
        minContentHeightPx = MIN_CONTENT_HEIGHT,
        historyEdge = edge,
        historyWidthPx = HISTORY_WIDTH,
        historyGapPx = HISTORY_GAP,
        normalBottomMarginPx = NORMAL_BOTTOM_MARGIN,
        parentBottomMarginPx = PARENT_BOTTOM_MARGIN,
        systemBottomInsetPx = SYSTEM_BOTTOM_INSET,
    )

    private fun applyHand(fixture: Fixture, hand: String) {
        DominantHandLayoutHelper.apply(
            actionRow = fixture.actionRow,
            openButton = fixture.open,
            copyButton = fixture.copy,
            shareButton = fixture.share,
            historyButton = fixture.history,
            hand = hand,
            actionGapPx = 8,
            historyEdgeMarginPx = 16,
        )
    }

    private fun createFixture(): Fixture {
        val context = RuntimeEnvironment.getApplication()
        val parent = ConstraintLayout(context)
        val scrollView = NestedScrollView(context).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(300, 400).apply {
                bottomToTop = R.id.footerTextView
            }
        }
        val scrollContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(scrollContent)
        parent.addView(scrollView)

        val footer = TextView(context).apply {
            id = R.id.footerTextView
            setPadding(0, 7, 0, 11)
            layoutParams = ConstraintLayout.LayoutParams(300, 40).apply {
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }
        parent.addView(footer)

        val history = View(context).apply {
            id = R.id.buttonHistory
            layoutParams = ConstraintLayout.LayoutParams(HISTORY_WIDTH, HISTORY_WIDTH).apply {
                bottomToTop = R.id.footerTextView
                leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }
        parent.addView(history)

        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        val open = actionView(R.id.buttonOpen)
        val copy = actionView(R.id.buttonCopy)
        val share = actionView(R.id.buttonShare)
        actionRow.addView(open)
        actionRow.addView(copy)
        actionRow.addView(share)

        return Fixture(
            parent = parent,
            scrollView = scrollView,
            scrollContent = scrollContent,
            footer = footer,
            history = history,
            actionRow = actionRow,
            open = open,
            copy = copy,
            share = share,
        )
    }

    private fun actionView(id: Int): View {
        val context = RuntimeEnvironment.getApplication()
        return View(context).apply {
            this.id = id
            layoutParams = LinearLayout.LayoutParams(0, 48, 1f)
        }
    }

    private data class Fixture(
        val parent: ConstraintLayout,
        val scrollView: NestedScrollView,
        val scrollContent: LinearLayout,
        val footer: TextView,
        val history: View,
        val actionRow: LinearLayout,
        val open: View,
        val copy: View,
        val share: View,
    ) {
        fun scrollParams(): ConstraintLayout.LayoutParams =
            scrollView.layoutParams as ConstraintLayout.LayoutParams

        fun historyParams(): ConstraintLayout.LayoutParams =
            history.layoutParams as ConstraintLayout.LayoutParams
    }

    private companion object {
        const val MIN_CONTENT_HEIGHT = 500
        const val HISTORY_WIDTH = 48
        const val HISTORY_GAP = 16
        const val NORMAL_BOTTOM_MARGIN = 8
        const val PARENT_BOTTOM_MARGIN = 16
        const val SYSTEM_BOTTOM_INSET = 24
        const val FOOTER_TOP_MARGIN = 16
    }
}
