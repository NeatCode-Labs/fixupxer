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

package com.fixupxer

import android.os.SystemClock
import android.view.View
import android.view.ViewParent
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingRootException
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher

fun waitFor(millis: Long): ViewAction {
    return object : ViewAction {
        override fun getConstraints() = isRoot()
        override fun getDescription() = "Wait for $millis milliseconds"
        override fun perform(uiController: UiController, view: View) {
            uiController.loopMainThreadForAtLeast(millis)
        }
    }
}

fun nestedScrollTo(): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(View::class.java)
        override fun getDescription() = "Scroll enclosing NestedScrollView to target view"
        override fun perform(uiController: UiController, view: View) {
            var y = view.top
            var parent: ViewParent? = view.parent
            while (parent is View && parent !is NestedScrollView) {
                y += parent.top
                parent = (parent as View).parent
            }
            (parent as? NestedScrollView)?.scrollTo(0, y)
            uiController.loopMainThreadUntilIdle()
        }
    }
}

/**
 * Polls [assertion] until it passes or [timeoutMs] elapses, then runs it once more so a genuine
 * failure surfaces with its real message.
 *
 * The lambda is re-executed on every poll, so it must contain assertions only — never an Espresso
 * action such as `click()` or `replaceText()`, which would then run repeatedly. Perform actions
 * before or after the block.
 *
 * Beware of assertions that hold trivially at the first poll (`doesNotExist()`, `not(...)`) — they
 * pass instantly against a screen that has not rendered yet. Anchor those on a positive condition
 * first.
 */
fun awaitAssertion(timeoutMs: Long = 10_000, pollMs: Long = 100, assertion: () -> Unit) {
    val deadline = SystemClock.uptimeMillis() + timeoutMs
    while (SystemClock.uptimeMillis() < deadline) {
        try {
            assertion()
            return
        } catch (_: AssertionError) {
            // View state not ready yet.
        } catch (_: NoMatchingViewException) {
            // View hierarchy not ready yet.
        } catch (_: NoMatchingRootException) {
            // Window/dialog root not attached yet.
        } catch (e: RuntimeException) {
            // RootViewWithoutFocusException is not public API, so it is matched by name.
            if (!e.javaClass.name.endsWith("RootViewWithoutFocusException")) {
                throw e
            }
            // Dialog dismiss or IME transition has not settled yet.
        }
        SystemClock.sleep(pollMs)
    }
    assertion()
}

/**
 * Waits until the processed-URL field holds a finished result.
 *
 * ShareActivity parks a "Processing…" placeholder in that field while it works, so plain
 * `isDisplayed()` or `not(withText(""))` checks are satisfied before processing completes.
 */
fun awaitProcessedUrl(timeoutMs: Long = 10_000) {
    val processingPlaceholder = InstrumentationRegistry.getInstrumentation()
        .targetContext.getString(R.string.processing)
    awaitAssertion(timeoutMs) {
        onView(withId(R.id.textViewProcessedUrl)).check(
            matches(
                allOf(
                    isDisplayed(),
                    not(withText("")),
                    not(withText(processingPlaceholder))
                )
            )
        )
    }
}
