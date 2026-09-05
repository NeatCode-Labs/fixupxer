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
import android.view.View
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = FixupXerApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MainActivityInputDraftTest {
    private lateinit var controller: ActivityController<MainActivity>
    private lateinit var activity: MainActivity
    private lateinit var input: EditText
    private lateinit var process: View
    private lateinit var inputLayout: TextInputLayout
    private val validationLogs = ConcurrentLinkedQueue<String>()

    @Before
    fun setUp() {
        // The real Application plants a DebugTree for every Robolectric test.
        // Timber is static in the sandbox: stacked trees duplicate expensive
        // Android logging and can consume the validator's 50/100 ms budget.
        Timber.uprootAll()
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                validationLogs.add(message)
            }
        })
        controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        activity = controller.get()
        activity.preferencesManager.setHistoryEnabled(false)
        input = activity.findViewById(R.id.editTextUrl)
        process = activity.findViewById(R.id.buttonProcess)
        inputLayout = activity.findViewById(R.id.textInputLayoutUrl)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @After
    fun tearDown() {
        try {
            controller.pause().stop().destroy()
        } finally {
            Timber.uprootAll()
        }
    }

    @Test
    fun `ordinary URL survives validation after every typed character`() {
        val url = "https://example.com"
        val draft = StringBuilder()

        url.forEach { character ->
            draft.append(character)
            input.append(character.toString())
            settleValidation()
            assertEquals("Draft after '$character'", draft.toString(), input.text.toString())
        }

        assertTrue(validationDiagnostic(), process.isEnabled)
        assertNull(inputLayout.error)
    }

    @Test
    fun `percent escape draft stays editable and valid completion restores Process`() {
        val prefix = "https://example.com/file"
        input.setText(prefix)
        settleValidation()
        assertTrue(validationDiagnostic(), process.isEnabled)

        listOf("%", "2").forEachIndexed { index, character ->
            input.append(character)
            settleValidation()
            assertEquals(prefix + (if (index == 0) "%" else "%2"), input.text.toString())
            assertFalse(process.isEnabled)
            assertEquals(activity.getString(R.string.error_invalid_input), inputLayout.error)
        }

        input.append("0name")
        settleValidation()

        assertEquals("$prefix%20name", input.text.toString())
        assertTrue(validationDiagnostic(), process.isEnabled)
        assertNull(inputLayout.error)
    }

    @Test
    fun `malformed draft cannot dispatch a programmatic Process click`() {
        val malformed = "https://example.com/file%2"
        input.setText(malformed)
        settleValidation()
        assertFalse(process.isEnabled)

        process.performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(malformed, input.text.toString())
        assertEquals(activity.getString(R.string.error_invalid_input), inputLayout.error)
        assertFalse(activity.findViewById<View>(R.id.buttonCopy).isEnabled)
        assertFalse(process.isEnabled)
    }

    /** Drain the real TextWatcher's coroutine; Default work is not an Espresso idle resource. */
    private fun settleValidation() {
        val jobField = MainActivity::class.java.getDeclaredField("textValidationJob").apply {
            isAccessible = true
        }
        repeat(500) {
            shadowOf(Looper.getMainLooper()).idle()
            val job = jobField.get(activity) as? Job
            if (job == null || job.isCompleted) {
                shadowOf(Looper.getMainLooper()).idle()
                // A main-loop callback can start another validation; do not
                // return based only on the job observed before draining it.
                val latestJob = jobField.get(activity) as? Job
                if (latestJob === job && (latestJob == null || latestJob.isCompleted)) {
                    return
                }
            }
            Thread.sleep(10)
        }
        fail("TextWatcher validation did not complete. ${validationDiagnostic()}")
    }

    private fun validationDiagnostic(): String =
        "Input=${input.text}; error=${inputLayout.error}; " +
            validationLogs.toList().takeLast(12).joinToString(" | ")
}
