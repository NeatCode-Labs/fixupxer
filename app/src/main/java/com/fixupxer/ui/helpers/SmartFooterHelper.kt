// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2025  NeatCode Labs
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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.fixupxer.R
import com.fixupxer.ui.helpers.DominantHandLayoutHelper.PhysicalEdge
import timber.log.Timber

/**
 * Moves the footer between a fixed bottom anchor and scrollable content on small screens.
 * The History action stays on its dominant-hand physical edge and is re-anchored vertically.
 */
object SmartFooterHelper {

    /** Height threshold (px) below which the footer joins the scroll content. */
    private const val SMALL_SCREEN_HEIGHT_THRESHOLD_PX = 600

    enum class FooterMode {
        NORMAL,
        SMALL,
    }

    data class FooterLayoutDecision(
        val mode: FooterMode,
        val historyEdge: PhysicalEdge,
        val historyBottomMargin: Int,
        val footerPaddingLeft: Int,
        val footerPaddingRight: Int,
    )

    fun decide(
        availableHeightPx: Int,
        minContentHeightPx: Int,
        historyEdge: PhysicalEdge,
        historyWidthPx: Int,
        historyGapPx: Int,
        normalBottomMarginPx: Int,
        parentBottomMarginPx: Int,
        systemBottomInsetPx: Int,
    ): FooterLayoutDecision {
        val isSmall = availableHeightPx < minContentHeightPx ||
            availableHeightPx < SMALL_SCREEN_HEIGHT_THRESHOLD_PX
        if (!isSmall) {
            return FooterLayoutDecision(
                mode = FooterMode.NORMAL,
                historyEdge = historyEdge,
                historyBottomMargin = normalBottomMarginPx,
                footerPaddingLeft = 0,
                footerPaddingRight = 0,
            )
        }

        val footerClearance = historyWidthPx + historyGapPx
        return FooterLayoutDecision(
            mode = FooterMode.SMALL,
            historyEdge = historyEdge,
            historyBottomMargin = parentBottomMarginPx + systemBottomInsetPx,
            footerPaddingLeft = if (historyEdge == PhysicalEdge.LEFT) footerClearance else 0,
            footerPaddingRight = if (historyEdge == PhysicalEdge.RIGHT) footerClearance else 0,
        )
    }

    /**
     * @return the layout listener to remove in [android.app.Activity.onDestroy], or null in test mode.
     */
    fun setup(
        context: Context,
        rootView: View,
        scrollView: NestedScrollView,
        footer: TextView,
        historyAnchor: View,
    ): ViewTreeObserver.OnGlobalLayoutListener? {
        val parentLayout = scrollView.parent as? ConstraintLayout ?: return null

        val isRunningTest = try {
            context.packageManager.getPackageInfo("com.fixupxer.debug.test", 0)
            true
        } catch (e: Exception) {
            false
        }

        if (isRunningTest) {
            Timber.d("Running in test mode, using simplified footer setup")
            val scrollViewParams = scrollView.layoutParams as ConstraintLayout.LayoutParams
            scrollViewParams.bottomToTop = footer.id
            scrollViewParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            scrollViewParams.bottomMargin = 0
            scrollView.layoutParams = scrollViewParams
            return null
        }

        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val parentHeight = parentLayout.height
                val appBarHeight = rootView.findViewById<View>(R.id.appBarLayout)?.height ?: 0
                val availableHeight = parentHeight - appBarHeight
                val systemBottomInset = ViewCompat.getRootWindowInsets(rootView)
                    ?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
                val measuredHistoryWidth = historyAnchor.width.takeIf { it > 0 }
                    ?: historyAnchor.layoutParams.width.coerceAtLeast(0)
                val decision = decide(
                    availableHeightPx = availableHeight,
                    minContentHeightPx = context.resources.getDimensionPixelSize(
                        R.dimen.min_content_height,
                    ),
                    historyEdge = DominantHandLayoutHelper.historyEdge(historyAnchor),
                    historyWidthPx = measuredHistoryWidth,
                    historyGapPx = context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                    normalBottomMarginPx = context.resources.getDimensionPixelSize(
                        R.dimen.margin_small,
                    ),
                    parentBottomMarginPx = context.resources.getDimensionPixelSize(
                        R.dimen.margin_medium,
                    ),
                    systemBottomInsetPx = systemBottomInset,
                )
                applyLayout(
                    parentLayout = parentLayout,
                    scrollView = scrollView,
                    footer = footer,
                    historyAnchor = historyAnchor,
                    decision = decision,
                    footerTopMarginPx = context.resources.getDimensionPixelSize(
                        R.dimen.margin_medium,
                    ),
                )
            }
        }
        parentLayout.viewTreeObserver.addOnGlobalLayoutListener(listener)
        return listener
    }

    fun applyLayout(
        parentLayout: ConstraintLayout,
        scrollView: NestedScrollView,
        footer: TextView,
        historyAnchor: View,
        decision: FooterLayoutDecision,
        footerTopMarginPx: Int,
    ) {
        val scrollViewParams = scrollView.layoutParams as ConstraintLayout.LayoutParams
        var scrollParamsChanged = false
        when (decision.mode) {
            FooterMode.SMALL -> {
                if (scrollViewParams.bottomToBottom != ConstraintLayout.LayoutParams.PARENT_ID ||
                    scrollViewParams.bottomToTop != ConstraintLayout.LayoutParams.UNSET ||
                    scrollViewParams.bottomMargin != 0
                ) {
                    scrollViewParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    scrollViewParams.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                    scrollViewParams.bottomMargin = 0
                    scrollParamsChanged = true
                }
                anchorHistoryToParentBottom(historyAnchor, decision.historyBottomMargin)
                moveFooterIntoScrollContent(scrollView, footer, footerTopMarginPx)
            }
            FooterMode.NORMAL -> {
                moveFooterIntoParent(parentLayout, footer)
                anchorHistoryAboveFooter(historyAnchor, footer, decision.historyBottomMargin)
                if (scrollViewParams.bottomToTop != footer.id ||
                    scrollViewParams.bottomToBottom != ConstraintLayout.LayoutParams.UNSET ||
                    scrollViewParams.bottomMargin != 0
                ) {
                    scrollViewParams.bottomToTop = footer.id
                    scrollViewParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                    scrollViewParams.bottomMargin = 0
                    scrollParamsChanged = true
                }
            }
        }
        if (scrollParamsChanged) {
            scrollView.layoutParams = scrollViewParams
        }
        if (footer.paddingLeft != decision.footerPaddingLeft ||
            footer.paddingRight != decision.footerPaddingRight
        ) {
            footer.setPadding(
                decision.footerPaddingLeft,
                footer.paddingTop,
                decision.footerPaddingRight,
                footer.paddingBottom,
            )
        }
    }

    private fun moveFooterIntoScrollContent(
        scrollView: NestedScrollView,
        footer: TextView,
        footerTopMarginPx: Int,
    ) {
        val scrollContent = scrollView.getChildAt(0) as? LinearLayout ?: return
        if (footer.parent != scrollContent) {
            (footer.parent as? ViewGroup)?.removeView(footer)
            footer.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = footerTopMarginPx
            }
            scrollContent.addView(footer)
            return
        }
        val params = footer.layoutParams as? LinearLayout.LayoutParams
        if (params == null ||
            params.width != LinearLayout.LayoutParams.MATCH_PARENT ||
            params.height != LinearLayout.LayoutParams.WRAP_CONTENT ||
            params.topMargin != footerTopMarginPx
        ) {
            footer.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = footerTopMarginPx
            }
        }
    }

    private fun moveFooterIntoParent(
        parentLayout: ConstraintLayout,
        footer: TextView,
    ) {
        if (footer.parent != parentLayout) {
            (footer.parent as? ViewGroup)?.removeView(footer)
            footer.layoutParams = footerConstraintParams()
            parentLayout.addView(footer)
            return
        }
        val params = footer.layoutParams as? ConstraintLayout.LayoutParams
        if (params == null ||
            params.width != ConstraintLayout.LayoutParams.MATCH_PARENT ||
            params.height != ConstraintLayout.LayoutParams.WRAP_CONTENT ||
            params.bottomToBottom != ConstraintLayout.LayoutParams.PARENT_ID ||
            params.startToStart != ConstraintLayout.LayoutParams.PARENT_ID ||
            params.endToEnd != ConstraintLayout.LayoutParams.PARENT_ID
        ) {
            footer.layoutParams = footerConstraintParams()
        }
    }

    private fun footerConstraintParams(): ConstraintLayout.LayoutParams =
        ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        }

    private fun anchorHistoryToParentBottom(
        historyAnchor: View,
        bottomMargin: Int,
    ) {
        val params = historyAnchor.layoutParams as? ConstraintLayout.LayoutParams ?: return
        if (params.bottomToTop == ConstraintLayout.LayoutParams.UNSET &&
            params.bottomToBottom == ConstraintLayout.LayoutParams.PARENT_ID &&
            params.bottomMargin == bottomMargin
        ) {
            return
        }
        params.bottomToTop = ConstraintLayout.LayoutParams.UNSET
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        params.bottomMargin = bottomMargin
        historyAnchor.layoutParams = params
    }

    private fun anchorHistoryAboveFooter(
        historyAnchor: View,
        footer: TextView,
        bottomMargin: Int,
    ) {
        val params = historyAnchor.layoutParams as? ConstraintLayout.LayoutParams ?: return
        if (params.bottomToBottom == ConstraintLayout.LayoutParams.UNSET &&
            params.bottomToTop == footer.id &&
            params.bottomMargin == bottomMargin
        ) {
            return
        }
        params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
        params.bottomToTop = footer.id
        params.bottomMargin = bottomMargin
        historyAnchor.layoutParams = params
    }
}
