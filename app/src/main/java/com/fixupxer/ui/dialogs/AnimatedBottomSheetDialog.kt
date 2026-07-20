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

package com.fixupxer.ui.dialogs

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.view.animation.AnimationUtils
import androidx.annotation.RequiresApi
import com.google.android.material.R as MaterialR
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.motion.MotionUtils
import kotlin.math.max

/**
 * Bottom-sheet dialog whose terminal transitions all reuse the behavior's own
 * hide animation, so Back, Close, option selection, outside tap and swipe-down
 * leave the screen with the exact same motion.
 *
 * The behavior settle (ViewDragHelper) is the mechanism that animates a
 * swipe-down release and it ignores the system animator scale, unlike
 * ValueAnimator-based exits which snap instantly when animations are reduced.
 * Predictive-back gestures still translate the sheet with the finger; the
 * committed exit then settles from that visual position.
 */
class AnimatedBottomSheetDialog private constructor(
    context: Context,
    theme: Int,
    private val animatorsEnabled: () -> Boolean,
) : BottomSheetDialog(context, theme) {

    constructor(context: Context) : this(context, 0, ::systemAnimatorsEnabled)

    constructor(context: Context, theme: Int) : this(context, theme, ::systemAnimatorsEnabled)

    internal constructor(
        context: Context,
        theme: Int,
        animatorsEnabled: Boolean,
    ) : this(context, theme, { animatorsEnabled })

    private var slideAnimator: ValueAnimator? = null
    private var dismissInProgress = false
    private var finishingCancel = false
    private var backGestureInProgress = false
    private var backRegistration: Any? = null
    private var attached = false
    private var acceptsCancellation = true

    init {
        setDismissWithAnimation(false)
        configureBehavior()
        behavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    // The user can catch the sheet mid-settle and drag it back
                    // up; release the guard so a later exit can dismiss again.
                    if (dismissInProgress &&
                        !finishingCancel &&
                        newState == BottomSheetBehavior.STATE_DRAGGING
                    ) {
                        dismissInProgress = false
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
            }
        )
    }

    override fun onStart() {
        super.onStart()
        dismissInProgress = false
        finishingCancel = false
        backGestureInProgress = false
        sheetView()?.translationY = 0f
        configureBehavior()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
        registerBackCallback()
    }

    override fun onDetachedFromWindow() {
        attached = false
        unregisterBackCallback()
        cancelSlideAnimator()
        backGestureInProgress = false
        // The decor can detach while a settle exit is still in flight (host
        // Activity destroyed mid-exit). The behavior's late STATE_HIDDEN
        // callback then re-enters cancel(); reaching Dialog.cancel() at that
        // point would call removeViewImmediate on the already-removed decor
        // and crash. Mark the cancel as finished so late callbacks no-op.
        dismissInProgress = false
        finishingCancel = true
        super.onDetachedFromWindow()
    }

    override fun setCancelable(flag: Boolean) {
        super.setCancelable(flag)
        acceptsCancellation = flag
        if (flag) {
            registerBackCallback()
        } else {
            unregisterBackCallback()
        }
    }

    override fun setCanceledOnTouchOutside(cancel: Boolean) {
        super.setCanceledOnTouchOutside(cancel)
        if (cancel) {
            acceptsCancellation = true
            registerBackCallback()
        }
    }

    fun dismissAnimated() {
        cancel()
    }

    override fun dismiss() {
        if (!finishingCancel) {
            // Direct dismiss: neutralize any pending settle so its late
            // STATE_HIDDEN callback cannot fire an unexpected cancel().
            dismissInProgress = false
            finishingCancel = true
        }
        super.dismiss()
    }

    override fun cancel() {
        if (!isShowing) {
            cancelSlideAnimator()
            if (!finishingCancel) {
                super.cancel()
            }
            return
        }
        val sheet = sheetView()
        if (behavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            cancelSlideAnimator()
            finishCancel()
            return
        }
        if (dismissInProgress || finishingCancel) {
            return
        }

        dismissInProgress = true
        backGestureInProgress = false
        if (sheet == null || !sheet.isLaidOut) {
            finishCancel()
            return
        }

        if (sheet.translationY > 0f) {
            // A predictive-back gesture already pulled the sheet down; finish
            // that translation instead of settling from the layout position.
            if (!animatorsEnabled()) {
                finishCancel()
                return
            }
            val targetTranslation = exitTranslation(sheet)
            if (targetTranslation <= sheet.translationY) {
                finishCancel()
                return
            }
            animateSheet(
                sheet = sheet,
                targetTranslation = targetTranslation,
                duration = exitDuration(),
                onFinished = ::finishCancel,
            )
            return
        }

        // Same exit as a swipe-down release: the behavior settles to HIDDEN and
        // BottomSheetDialog's internal callback re-enters cancel() to finish.
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
        if (!finishingCancel && behavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            // The behavior could not settle (it never went through a layout
            // pass), so no state callback will arrive; finish directly.
            finishCancel()
        }
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Android dispatches legacy Back here only before API 33")
    override fun onBackPressed() {
        dismissAnimated()
    }

    private fun configureBehavior() {
        behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isDraggable = true
            isHideable = true
        }
    }

    private fun sheetView(): View? =
        findViewById(MaterialR.id.design_bottom_sheet)

    private fun exitTranslation(sheet: View): Float {
        val windowHeight = window?.decorView?.height ?: sheet.rootView.height
        return max(sheet.height, windowHeight - sheet.top).coerceAtLeast(0).toFloat()
    }

    private fun exitDuration(): Long =
        MotionUtils.resolveThemeDuration(
            context,
            MaterialR.attr.motionDurationMedium2,
            context.resources.getInteger(android.R.integer.config_mediumAnimTime),
        ).toLong()

    private fun returnDuration(): Long =
        MotionUtils.resolveThemeDuration(
            context,
            MaterialR.attr.motionDurationShort4,
            context.resources.getInteger(android.R.integer.config_shortAnimTime),
        ).toLong()

    private fun animateSheet(
        sheet: View,
        targetTranslation: Float,
        duration: Long,
        onFinished: (() -> Unit)? = null,
    ) {
        cancelSlideAnimator()
        if (!animatorsEnabled() || duration <= 0L) {
            sheet.translationY = targetTranslation
            onFinished?.invoke()
            return
        }

        val animator = ValueAnimator.ofFloat(sheet.translationY, targetTranslation)
        slideAnimator = animator
        animator.duration = duration
        animator.interpolator = MotionUtils.resolveThemeInterpolator(
            context,
            MaterialR.attr.motionEasingEmphasizedInterpolator,
            AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in),
        )
        animator.addUpdateListener {
            sheet.translationY = it.animatedValue as Float
        }
        animator.addListener(
            object : AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (slideAnimator !== animation) return
                    slideAnimator = null
                    if (!cancelled) {
                        onFinished?.invoke()
                    }
                }
            }
        )
        animator.start()
    }

    private fun cancelSlideAnimator() {
        val animator = slideAnimator ?: return
        slideAnimator = null
        animator.cancel()
    }

    private fun finishCancel() {
        if (finishingCancel) return
        finishingCancel = true
        cancelSlideAnimator()
        setDismissWithAnimation(false)
        super.cancel()
    }

    private fun registerBackCallback() {
        if (!attached || !acceptsCancellation || backRegistration != null) return
        backRegistration = when {
            Build.VERSION.SDK_INT >= 34 -> Api34Back.register(this)
            Build.VERSION.SDK_INT >= 33 -> Api33Back.register(this)
            else -> null
        }
    }

    private fun unregisterBackCallback() {
        val registration = backRegistration ?: return
        backRegistration = null
        when {
            Build.VERSION.SDK_INT >= 34 -> Api34Back.unregister(registration)
            Build.VERSION.SDK_INT >= 33 -> Api33Back.unregister(registration)
        }
    }

    private fun onBackStarted() {
        if (dismissInProgress || finishingCancel) return
        cancelSlideAnimator()
        backGestureInProgress = true
        sheetView()?.translationY = 0f
    }

    private fun onBackProgressed(progress: Float) {
        if (!backGestureInProgress || dismissInProgress || finishingCancel) return
        val sheet = sheetView() ?: return
        sheet.translationY = translationForProgress(exitTranslation(sheet), progress)
    }

    private fun onBackCancelled() {
        if (!backGestureInProgress || dismissInProgress || finishingCancel) return
        backGestureInProgress = false
        val sheet = sheetView() ?: return
        animateSheet(
            sheet = sheet,
            targetTranslation = 0f,
            duration = returnDuration(),
        )
    }

    private fun onBackInvoked() {
        backGestureInProgress = false
        dismissAnimated()
    }

    @RequiresApi(33)
    private object Api33Back {
        private data class Registration(
            val dispatcher: android.window.OnBackInvokedDispatcher,
            val callback: android.window.OnBackInvokedCallback,
        )

        fun register(dialog: AnimatedBottomSheetDialog): Any? {
            val dispatcher = dialog.onBackInvokedDispatcher
            val callback = android.window.OnBackInvokedCallback {
                dialog.onBackInvoked()
            }
            dispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                callback,
            )
            return Registration(dispatcher, callback)
        }

        fun unregister(registration: Any) {
            registration as Registration
            registration.dispatcher.unregisterOnBackInvokedCallback(registration.callback)
        }
    }

    @RequiresApi(34)
    private object Api34Back {
        private data class Registration(
            val dispatcher: android.window.OnBackInvokedDispatcher,
            val callback: android.window.OnBackAnimationCallback,
        )

        fun register(dialog: AnimatedBottomSheetDialog): Any? {
            val dispatcher = dialog.onBackInvokedDispatcher
            val callback = object : android.window.OnBackAnimationCallback {
                override fun onBackStarted(backEvent: android.window.BackEvent) {
                    dialog.onBackStarted()
                }

                override fun onBackProgressed(backEvent: android.window.BackEvent) {
                    dialog.onBackProgressed(backEvent.progress)
                }

                override fun onBackCancelled() {
                    dialog.onBackCancelled()
                }

                override fun onBackInvoked() {
                    dialog.onBackInvoked()
                }
            }
            dispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                callback,
            )
            return Registration(dispatcher, callback)
        }

        fun unregister(registration: Any) {
            registration as Registration
            registration.dispatcher.unregisterOnBackInvokedCallback(registration.callback)
        }
    }

    internal companion object {
        fun translationForProgress(exitTranslation: Float, progress: Float): Float =
            exitTranslation.coerceAtLeast(0f) * progress.coerceIn(0f, 1f)

        private fun systemAnimatorsEnabled(): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
    }
}
