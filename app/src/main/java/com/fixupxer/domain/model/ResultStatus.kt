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

package com.fixupxer.domain.model

import java.net.IDN
import java.net.URI

/**
 * Describes how a processed URL differs from the original input.
 */
enum class ResultStatus {
    ALREADY_CLEAN,
    CLEANED,
    CONVERTED,
    CLEANED_AND_CONVERTED
}

/**
 * Maps an input/output URL pair to a [ResultStatus] for UI display.
 *
 * - Identical strings → [ResultStatus.ALREADY_CLEAN]
 * - Different site, same query → [ResultStatus.CONVERTED] (domain swap only)
 * - Different site, query changed → [ResultStatus.CLEANED_AND_CONVERTED]
 *   (tracking removed AND domain swapped)
 * - Same site, different URL → [ResultStatus.CLEANED] (tracking removed,
 *   subdomain normalized, etc.)
 */
fun resolveResultStatus(inputUrl: String, processedUrl: String): ResultStatus? {
    if (processedUrl.isBlank()) return null
    if (inputUrl == processedUrl) return ResultStatus.ALREADY_CLEAN

    val inputHost = extractComparableHost(inputUrl)
    val outputHost = extractComparableHost(processedUrl)

    return if (inputHost != null && outputHost != null &&
        !isSameSite(inputHost, outputHost)
    ) {
        if (extractQuery(inputUrl) != extractQuery(processedUrl)) {
            ResultStatus.CLEANED_AND_CONVERTED
        } else {
            ResultStatus.CONVERTED
        }
    } else {
        ResultStatus.CLEANED
    }
}

/**
 * Subdomain-tolerant host comparison: `m.facebook.com` → `facebook.com` is a
 * cleaning normalization, not a proxy conversion. The dot boundary keeps
 * lookalike proxy domains apart (`toinstagram.com` is NOT `instagram.com`).
 */
private fun isSameSite(hostA: String, hostB: String): Boolean =
    hostA == hostB ||
        hostA.endsWith(".$hostB") ||
        hostB.endsWith(".$hostA")

private fun extractComparableHost(url: String): String? {
    val host = runCatching { URI(url).host }.getOrNull() ?: return null
    return IDN.toASCII(host.removeSuffix(".")).lowercase().removePrefix("www.")
}

private fun extractQuery(url: String): String? {
    val queryStart = url.indexOf('?')
    if (queryStart < 0) return null
    val fragmentStart = url.indexOf('#', queryStart)
    return url.substring(queryStart + 1, if (fragmentStart >= 0) fragmentStart else url.length)
}
