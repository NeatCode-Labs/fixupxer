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
import androidx.constraintlayout.widget.ConstraintLayout
import com.fixupxer.ui.helpers.DominantHandLayoutHelper
import com.fixupxer.ui.helpers.DominantHandLayoutHelper.PhysicalEdge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DominantHandLayoutHelperTest {

    @Test
    fun `resolve covers two hands and two layout directions`() {
        val cases = listOf(
            Case(
                hand = PreferencesManager.DOMINANT_HAND_RIGHT,
                layoutDirection = View.LAYOUT_DIRECTION_LTR,
                physicalOrder = listOf(R.id.buttonOpen, R.id.buttonCopy, R.id.buttonShare),
                insertionOrder = listOf(R.id.buttonOpen, R.id.buttonCopy, R.id.buttonShare),
                historyEdge = PhysicalEdge.LEFT,
            ),
            Case(
                hand = PreferencesManager.DOMINANT_HAND_RIGHT,
                layoutDirection = View.LAYOUT_DIRECTION_RTL,
                physicalOrder = listOf(R.id.buttonOpen, R.id.buttonCopy, R.id.buttonShare),
                insertionOrder = listOf(R.id.buttonShare, R.id.buttonCopy, R.id.buttonOpen),
                historyEdge = PhysicalEdge.LEFT,
            ),
            Case(
                hand = PreferencesManager.DOMINANT_HAND_LEFT,
                layoutDirection = View.LAYOUT_DIRECTION_LTR,
                physicalOrder = listOf(R.id.buttonShare, R.id.buttonCopy, R.id.buttonOpen),
                insertionOrder = listOf(R.id.buttonShare, R.id.buttonCopy, R.id.buttonOpen),
                historyEdge = PhysicalEdge.RIGHT,
            ),
            Case(
                hand = PreferencesManager.DOMINANT_HAND_LEFT,
                layoutDirection = View.LAYOUT_DIRECTION_RTL,
                physicalOrder = listOf(R.id.buttonShare, R.id.buttonCopy, R.id.buttonOpen),
                insertionOrder = listOf(R.id.buttonOpen, R.id.buttonCopy, R.id.buttonShare),
                historyEdge = PhysicalEdge.RIGHT,
            ),
        )

        cases.forEach { case ->
            val spec = DominantHandLayoutHelper.resolve(case.hand, case.layoutDirection)
            assertEquals(case.physicalOrder, spec.physicalActionOrder)
            assertEquals(case.insertionOrder, spec.insertionOrder)
            assertEquals(case.historyEdge, spec.historyEdge)
        }
    }

    @Test
    fun `apply preserves view state weights and listeners across all combinations`() {
        val hands = listOf(
            PreferencesManager.DOMINANT_HAND_RIGHT,
            PreferencesManager.DOMINANT_HAND_LEFT,
        )
        val layoutDirections = listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)

        hands.forEach { hand ->
            layoutDirections.forEach { layoutDirection ->
                val fixture = createFixture(layoutDirection)
                var clickCount = 0
                fixture.open.setOnClickListener { clickCount++ }
                fixture.open.isEnabled = false
                fixture.open.contentDescription = "Open preserved"

                apply(fixture, hand)
                apply(fixture, hand)

                val spec = DominantHandLayoutHelper.resolve(hand, layoutDirection)
                assertEquals(
                    spec.insertionOrder,
                    (0 until fixture.actionRow.childCount).map {
                        fixture.actionRow.getChildAt(it).id
                    },
                )
                assertEquals(3, fixture.actionRow.childCount)
                assertFalse(fixture.open.isEnabled)
                assertEquals("Open preserved", fixture.open.contentDescription)
                assertEquals(
                    1f,
                    (fixture.open.layoutParams as LinearLayout.LayoutParams).weight,
                )
                fixture.open.isEnabled = true
                fixture.open.performClick()
                assertEquals(1, clickCount)

                spec.physicalActionOrder.forEachIndexed { index, id ->
                    val view = fixture.actionRow.findViewById<View>(id)
                    val params = view.layoutParams as LinearLayout.LayoutParams
                    val expectedRightGap = if (index < spec.physicalActionOrder.lastIndex) GAP else 0
                    if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                        assertEquals(expectedRightGap, params.marginStart)
                        assertEquals(0, params.marginEnd)
                    } else {
                        assertEquals(0, params.marginStart)
                        assertEquals(expectedRightGap, params.marginEnd)
                    }
                }

                val historyParams =
                    fixture.history.layoutParams as ConstraintLayout.LayoutParams
                if (spec.historyEdge == PhysicalEdge.LEFT) {
                    assertEquals(
                        ConstraintLayout.LayoutParams.PARENT_ID,
                        historyParams.leftToLeft,
                    )
                    assertEquals(ConstraintLayout.LayoutParams.UNSET, historyParams.rightToRight)
                    assertEquals(EDGE_MARGIN, historyParams.leftMargin)
                } else {
                    assertEquals(
                        ConstraintLayout.LayoutParams.PARENT_ID,
                        historyParams.rightToRight,
                    )
                    assertEquals(ConstraintLayout.LayoutParams.UNSET, historyParams.leftToLeft)
                    assertEquals(EDGE_MARGIN, historyParams.rightMargin)
                }
                assertEquals(ConstraintLayout.LayoutParams.UNSET, historyParams.startToStart)
                assertEquals(ConstraintLayout.LayoutParams.UNSET, historyParams.endToEnd)
            }
        }
    }

    private fun createFixture(layoutDirection: Int): Fixture {
        val context = RuntimeEnvironment.getApplication()
        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            this.layoutDirection = layoutDirection
        }
        val open = actionView(R.id.buttonOpen)
        val copy = actionView(R.id.buttonCopy)
        val share = actionView(R.id.buttonShare)
        actionRow.addView(open)
        actionRow.addView(copy)
        actionRow.addView(share)

        val parent = ConstraintLayout(context)
        val history = View(context).apply {
            id = R.id.buttonHistory
            this.layoutParams = ConstraintLayout.LayoutParams(48, 48).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }
        parent.addView(history)
        return Fixture(actionRow, open, copy, share, history)
    }

    private fun actionView(id: Int): View {
        val context = RuntimeEnvironment.getApplication()
        return View(context).apply {
            this.id = id
            layoutParams = LinearLayout.LayoutParams(0, 48, 1f)
        }
    }

    private fun apply(fixture: Fixture, hand: String) {
        DominantHandLayoutHelper.apply(
            actionRow = fixture.actionRow,
            openButton = fixture.open,
            copyButton = fixture.copy,
            shareButton = fixture.share,
            historyButton = fixture.history,
            hand = hand,
            actionGapPx = GAP,
            historyEdgeMarginPx = EDGE_MARGIN,
        )
    }

    private data class Fixture(
        val actionRow: LinearLayout,
        val open: View,
        val copy: View,
        val share: View,
        val history: View,
    )

    private data class Case(
        val hand: String,
        val layoutDirection: Int,
        val physicalOrder: List<Int>,
        val insertionOrder: List<Int>,
        val historyEdge: PhysicalEdge,
    )

    private companion object {
        const val GAP = 8
        const val EDGE_MARGIN = 16
    }
}
