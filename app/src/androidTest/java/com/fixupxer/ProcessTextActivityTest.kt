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

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.PreferencesManager
import com.fixupxer.ui.ProcessTextActivity
import com.fixupxer.ui.ShareActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessTextActivityTest {

    private fun processTextIntent(text: String? = null, readonly: Boolean = false): Intent {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return Intent(context, ProcessTextActivity::class.java).apply {
            action = Intent.ACTION_PROCESS_TEXT
            type = "text/plain"
            text?.let { putExtra(Intent.EXTRA_PROCESS_TEXT, it) }
            putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, readonly)
        }
    }

    @Test
    fun editableExactUrlReturnsCleanedText() {
        val input = "https://example.com/article?utm_source=test"

        ActivityScenario.launchActivityForResult<ProcessTextActivity>(
            processTextIntent(input)
        ).use { scenario ->
            val result = scenario.result

            assertEquals(Activity.RESULT_OK, result.resultCode)
            assertEquals(
                "https://example.com/article",
                result.resultData?.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
            )
        }
    }

    @Test
    fun editableUrlRespectsConversionToggleOn() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferencesManager(context).setConvertTwitterEnabled(true)

        ActivityScenario.launchActivityForResult<ProcessTextActivity>(
            processTextIntent("https://x.com/user/status/123456789?t=abc")
        ).use { scenario ->
            val result = scenario.result

            assertEquals(Activity.RESULT_OK, result.resultCode)
            assertEquals(
                "https://fixupx.com/user/status/123456789",
                result.resultData?.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
            )
        }
    }

    @Test
    fun editableUrlRespectsConversionToggleOff() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferencesManager(context).setConvertTwitterEnabled(false)

        ActivityScenario.launchActivityForResult<ProcessTextActivity>(
            processTextIntent("https://x.com/user/status/123456789?t=abc")
        ).use { scenario ->
            val result = scenario.result

            assertEquals(Activity.RESULT_OK, result.resultCode)
            assertEquals(
                "https://x.com/user/status/123456789",
                result.resultData?.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
            )
        }
    }

    @Test
    fun readonlyTextLaunchesSharePreview() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(ShareActivity::class.java.name, null, false)

        try {
            ActivityScenario.launchActivityForResult<ProcessTextActivity>(
                processTextIntent("Selected prose", readonly = true)
            ).use { scenario ->
                val preview = monitor.waitForActivityWithTimeout(3_000)

                assertNotNull("Read-only Process Text should launch ShareActivity", preview)
                // Finish the preview first: while ShareActivity is in the
                // foreground the scenario's result is never delivered.
                instrumentation.runOnMainSync { preview?.finish() }
                assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test
    fun missingProcessTextExtraCancels() {
        ActivityScenario.launchActivityForResult<ProcessTextActivity>(
            processTextIntent()
        ).use { scenario ->
            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
        }
    }
}
