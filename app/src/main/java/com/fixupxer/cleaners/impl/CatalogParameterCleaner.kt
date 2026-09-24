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

package com.fixupxer.cleaners.impl

import com.fixupxer.cleaners.PlatformParameterRule
import com.fixupxer.cleaners.UrlCleaner
import com.fixupxer.processing.UrlNormalizer
import java.util.Locale

class CatalogParameterCleaner(
    val rule: PlatformParameterRule
) : UrlCleaner {
    override val id: String = rule.id
    override val displayName: String = rule.displayName
    override val category = rule.category
    private val removeKeys = rule.removeKeys.mapTo(mutableSetOf(), ::comparisonKey)
    private val preserveKeys = rule.preserveKeys.mapTo(mutableSetOf(), ::comparisonKey)
    private val removePrefixes = rule.removePrefixes.map(::comparisonKey)

    override fun matches(url: String): Boolean =
        if (rule.includeSubdomains) {
            UrlNormalizer.urlMatchesAnyDomain(url, rule.domains)
        } else {
            val host = UrlNormalizer.extractAsciiHost(url)
            rule.domains.any { it.equals(host, ignoreCase = true) }
        }

    private fun comparisonKey(key: String): String =
        if (rule.ignoreKeyCase) key.lowercase(Locale.ROOT) else key

    override fun clean(url: String): String {
        if (!matches(url)) return url

        return try {
            val queryIndex = url.indexOf('?')
            val fragmentIndex = url.indexOf('#')
            if (queryIndex < 0 || (fragmentIndex >= 0 && fragmentIndex < queryIndex)) {
                return url
            }

            val base = url.substring(0, queryIndex)
            val queryEnd = if (fragmentIndex >= 0) fragmentIndex else url.length
            val query = url.substring(queryIndex + 1, queryEnd)
            val fragment = if (fragmentIndex >= 0) url.substring(fragmentIndex) else ""

            // Keep an explicit empty query byte-for-byte.
            if (query.isEmpty()) return url

            val kept = query.split('&').mapNotNull { token ->
                val key = comparisonKey(token.substringBefore('='))
                when {
                    preserveKeys.contains(key) -> token
                    removeKeys.contains(key) -> null
                    removePrefixes.any { key.startsWith(it) } -> null
                    else -> token
                }
            }.filter { it.isNotEmpty() }

            if (kept.isEmpty()) base + fragment else "$base?${kept.joinToString("&")}$fragment"
        } catch (_: Exception) {
            url
        }
    }
}
