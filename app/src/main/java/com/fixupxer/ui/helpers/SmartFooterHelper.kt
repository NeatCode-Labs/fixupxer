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
import timber.log.Timber

/**
 * Moves the footer between a fixed bottom anchor and scrollable content on small screens.
 * Also re-anchors the History FAB, which normally sits above the footer: when the footer
 * joins the scroll content, the FAB is pinned to the parent bottom (above the nav bar).
 */
object SmartFooterHelper {

    /** Height threshold (px) below which the footer joins the scroll content. */
    private const val SMALL_SCREEN_HEIGHT_THRESHOLD_PX = 600

    /**
     * @return the layout listener to remove in [android.app.Activity.onDestroy], or null in test mode.
     */
    fun setup(
        context: Context,
        rootView: View,
        scrollView: NestedScrollView,
        footer: TextView
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

                val isSmallScreen =
                    availableHeight < context.resources.getDimensionPixelSize(R.dimen.min_content_height)

                val scrollViewParams = scrollView.layoutParams as ConstraintLayout.LayoutParams

                if (isSmallScreen || availableHeight < SMALL_SCREEN_HEIGHT_THRESHOLD_PX) {
                    scrollViewParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    scrollViewParams.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                    scrollViewParams.bottomMargin = 0

                    if (footer.parent == parentLayout) {
                        anchorFabToParentBottom(parentLayout, rootView, footer)
                        parentLayout.removeView(footer)
                        val scrollContent = scrollView.getChildAt(0) as? LinearLayout

                        val linearParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        linearParams.topMargin =
                            context.resources.getDimensionPixelSize(R.dimen.margin_medium)
                        footer.layoutParams = linearParams

                        scrollContent?.addView(footer)
                    }
                } else {
                    if (footer.parent != parentLayout) {
                        (footer.parent as? ViewGroup)?.removeView(footer)

                        val constraintParams = ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.MATCH_PARENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                        )
                        constraintParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                        constraintParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        constraintParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        footer.layoutParams = constraintParams

                        parentLayout.addView(footer)
                        anchorFabAboveFooter(parentLayout, rootView, footer)
                    }

                    scrollViewParams.bottomToTop = footer.id
                    scrollViewParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                    scrollViewParams.bottomMargin = 0
                }

                scrollView.layoutParams = scrollViewParams
            }
        }
        parentLayout.viewTreeObserver.addOnGlobalLayoutListener(listener)
        return listener
    }

    /** Pins the History FAB to the parent bottom (nav-bar aware) before the footer leaves. */
    private fun anchorFabToParentBottom(
        parentLayout: ConstraintLayout,
        rootView: View,
        footer: TextView
    ) {
        val fab = parentLayout.findViewById<View>(R.id.buttonHistory) ?: return
        val params = fab.layoutParams as ConstraintLayout.LayoutParams
        params.bottomToTop = ConstraintLayout.LayoutParams.UNSET
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        val navBarInset = ViewCompat.getRootWindowInsets(rootView)
            ?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
        params.bottomMargin =
            parentLayout.resources.getDimensionPixelSize(R.dimen.margin_medium) + navBarInset
        fab.layoutParams = params
        ViewCompat.setPaddingRelative(
            footer,
            ViewCompat.getPaddingStart(footer),
            footer.paddingTop,
            fab.width + parentLayout.resources.getDimensionPixelSize(R.dimen.margin_medium),
            footer.paddingBottom
        )
    }

    /** Restores the History FAB anchor above the footer once it is back in the layout. */
    private fun anchorFabAboveFooter(parentLayout: ConstraintLayout, rootView: View, footer: TextView) {
        val fab = parentLayout.findViewById<View>(R.id.buttonHistory) ?: return
        val params = fab.layoutParams as ConstraintLayout.LayoutParams
        params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
        params.bottomToTop = footer.id
        params.bottomMargin = parentLayout.resources.getDimensionPixelSize(R.dimen.margin_small)
        fab.layoutParams = params
        ViewCompat.setPaddingRelative(
            footer,
            ViewCompat.getPaddingStart(footer),
            footer.paddingTop,
            0,
            footer.paddingBottom
        )
    }
}
