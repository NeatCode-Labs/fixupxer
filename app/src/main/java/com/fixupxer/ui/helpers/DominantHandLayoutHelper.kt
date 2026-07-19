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

package com.fixupxer.ui.helpers

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.fixupxer.PreferencesManager
import com.fixupxer.R

/**
 * Applies a fixed physical action order without changing text layout direction.
 */
object DominantHandLayoutHelper {

    enum class PhysicalEdge {
        LEFT,
        RIGHT,
    }

    data class LayoutSpec(
        val physicalActionOrder: List<Int>,
        val insertionOrder: List<Int>,
        val historyEdge: PhysicalEdge,
    )

    fun resolve(hand: String, layoutDirection: Int): LayoutSpec {
        val isLeftHanded = hand == PreferencesManager.DOMINANT_HAND_LEFT
        val physicalOrder = if (isLeftHanded) {
            listOf(R.id.buttonShare, R.id.buttonCopy, R.id.buttonOpen)
        } else {
            listOf(R.id.buttonOpen, R.id.buttonCopy, R.id.buttonShare)
        }
        val insertionOrder = if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            physicalOrder.reversed()
        } else {
            physicalOrder
        }
        return LayoutSpec(
            physicalActionOrder = physicalOrder,
            insertionOrder = insertionOrder,
            historyEdge = if (isLeftHanded) PhysicalEdge.RIGHT else PhysicalEdge.LEFT,
        )
    }

    fun apply(
        actionRow: LinearLayout,
        openButton: View,
        copyButton: View,
        shareButton: View,
        historyButton: View,
        hand: String,
        actionGapPx: Int,
        historyEdgeMarginPx: Int,
    ) {
        val layoutDirection = actionRow.layoutDirection
        val spec = resolve(hand, layoutDirection)
        val actionViews = listOf(openButton, copyButton, shareButton)
        val viewsById = actionViews.associateBy { it.id }
        val paramsById = actionViews.associate { view ->
            view.id to copyLinearLayoutParams(view.layoutParams)
        }

        actionViews.forEach { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
        spec.insertionOrder.forEach { id ->
            val view = checkNotNull(viewsById[id])
            actionRow.addView(view, paramsById.getValue(id))
        }

        spec.physicalActionOrder.forEachIndexed { index, id ->
            val view = checkNotNull(viewsById[id])
            val params = view.layoutParams as LinearLayout.LayoutParams
            val hasPhysicalRightNeighbor = index < spec.physicalActionOrder.lastIndex
            val rightGap = if (hasPhysicalRightNeighbor) actionGapPx else 0
            if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                params.marginStart = rightGap
                params.marginEnd = 0
            } else {
                params.marginStart = 0
                params.marginEnd = rightGap
            }
            view.layoutParams = params
        }

        applyHistoryEdge(
            historyButton = historyButton,
            edge = spec.historyEdge,
            layoutDirection = layoutDirection,
            edgeMarginPx = historyEdgeMarginPx,
        )
    }

    fun historyEdge(historyButton: View): PhysicalEdge {
        val params = historyButton.layoutParams as? ConstraintLayout.LayoutParams
            ?: return PhysicalEdge.LEFT
        return when {
            params.leftToLeft == ConstraintLayout.LayoutParams.PARENT_ID -> PhysicalEdge.LEFT
            params.rightToRight == ConstraintLayout.LayoutParams.PARENT_ID -> PhysicalEdge.RIGHT
            else -> PhysicalEdge.LEFT
        }
    }

    private fun copyLinearLayoutParams(
        source: ViewGroup.LayoutParams,
    ): LinearLayout.LayoutParams = when (source) {
        is LinearLayout.LayoutParams -> LinearLayout.LayoutParams(source)
        is ViewGroup.MarginLayoutParams -> LinearLayout.LayoutParams(source)
        else -> LinearLayout.LayoutParams(source)
    }

    private fun applyHistoryEdge(
        historyButton: View,
        edge: PhysicalEdge,
        layoutDirection: Int,
        edgeMarginPx: Int,
    ) {
        val params = historyButton.layoutParams as? ConstraintLayout.LayoutParams ?: return
        params.startToStart = ConstraintLayout.LayoutParams.UNSET
        params.startToEnd = ConstraintLayout.LayoutParams.UNSET
        params.endToStart = ConstraintLayout.LayoutParams.UNSET
        params.endToEnd = ConstraintLayout.LayoutParams.UNSET
        params.leftToLeft = ConstraintLayout.LayoutParams.UNSET
        params.leftToRight = ConstraintLayout.LayoutParams.UNSET
        params.rightToLeft = ConstraintLayout.LayoutParams.UNSET
        params.rightToRight = ConstraintLayout.LayoutParams.UNSET

        if (edge == PhysicalEdge.LEFT) {
            params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
        } else {
            params.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
        }

        val marginStart = when {
            edge == PhysicalEdge.LEFT && layoutDirection != View.LAYOUT_DIRECTION_RTL ->
                edgeMarginPx
            edge == PhysicalEdge.RIGHT && layoutDirection == View.LAYOUT_DIRECTION_RTL ->
                edgeMarginPx
            else -> 0
        }
        val marginEnd = when {
            edge == PhysicalEdge.RIGHT && layoutDirection != View.LAYOUT_DIRECTION_RTL ->
                edgeMarginPx
            edge == PhysicalEdge.LEFT && layoutDirection == View.LAYOUT_DIRECTION_RTL ->
                edgeMarginPx
            else -> 0
        }
        params.marginStart = marginStart
        params.marginEnd = marginEnd
        params.resolveLayoutDirection(layoutDirection)
        historyButton.layoutParams = params
    }
}
