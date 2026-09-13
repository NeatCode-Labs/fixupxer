// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026 NeatCode Labs
 * This program is free software under the GNU General Public License,
 * version 3 or (at your option) any later version.
 */
package com.fixupxer.ui.helpers

import androidx.annotation.StringRes
import com.fixupxer.R
import com.fixupxer.processing.PipelineStatus

object PipelineStatusTextHelper {
    @StringRes
    fun messageRes(status: PipelineStatus): Int = when (status) {
        PipelineStatus.COMPLETE -> R.string.pipeline_status_complete
        PipelineStatus.FRONTEND_UNAVAILABLE -> R.string.pipeline_status_frontend_unavailable
        PipelineStatus.UNSUPPORTED_CONVERSION -> R.string.pipeline_status_unsupported_conversion
        PipelineStatus.INVALID_RESULT -> R.string.pipeline_status_invalid_result
        PipelineStatus.CYCLE -> R.string.pipeline_status_cycle
        PipelineStatus.HOP_LIMIT -> R.string.pipeline_status_hop_limit
        PipelineStatus.STALE_CONFIGURATION -> R.string.pipeline_status_stale_configuration
    }
}
