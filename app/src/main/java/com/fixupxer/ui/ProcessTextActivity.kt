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

package com.fixupxer.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fixupxer.presentation.processtext.ProcessTextUiState
import com.fixupxer.presentation.processtext.ProcessTextViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ProcessTextActivity : AppCompatActivity() {

    private val viewModel: ProcessTextViewModel by viewModels()
    private var terminalStateHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val processText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        } else {
            null
        }
        val readonly = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)
        } else {
            false
        }

        viewModel.onProcessTextRequested(processText, readonly)
        observeProcessTextState()
    }

    private fun observeProcessTextState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (terminalStateHandled || state is ProcessTextUiState.Idle) {
                        return@collect
                    }
                    terminalStateHandled = true

                    when (state) {
                        is ProcessTextUiState.ReplaceInline -> {
                            Timber.d("Returning Process Text replacement result")
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, state.cleanedUrl)
                            )
                            finish()
                        }

                        is ProcessTextUiState.OpenPreview -> {
                            Timber.d("Opening Process Text preview")
                            startActivity(
                                Intent(this@ProcessTextActivity, ShareActivity::class.java)
                                    .setAction(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, state.originalText)
                            )
                            setResult(RESULT_CANCELED)
                            finish()
                        }

                        ProcessTextUiState.Cancel -> {
                            Timber.d("Cancelling Process Text request without input")
                            setResult(RESULT_CANCELED)
                            finish()
                        }

                        ProcessTextUiState.Idle -> Unit
                    }
                }
            }
        }
    }
}
