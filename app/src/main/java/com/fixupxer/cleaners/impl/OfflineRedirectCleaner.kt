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

import com.fixupxer.cleaners.UrlCleaner
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.utils.Constants
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object OfflineRedirectCleaner : UrlCleaner {
    private val targetNormalizer = UrlNormalizer()

    private data class QueryRule(
        val host: String,
        val exactHost: Boolean,
        val path: String,
        val parameter: String
    )

    private val queryRules = listOf(
        QueryRule(Constants.FACEBOOK_LINK_SHIM_DOMAIN, true, "/l.php", "u"),
        QueryRule(Constants.FACEBOOK_MOBILE_LINK_SHIM_DOMAIN, true, "/l.php", "u"),
        QueryRule(Constants.LINKEDIN_DOMAIN, false, "/safety/go", "url"),
        QueryRule(Constants.YOUTUBE_DOMAIN, false, "/redirect", "q"),
        QueryRule(Constants.BLUESKY_GO_DOMAIN, true, "/redirect", "u"),
        QueryRule(Constants.GOOGLE_ADSERVICES_DOMAIN, false, "/pagead/aclk", "adurl"),
        QueryRule(Constants.GEORIOT_TARGET_DOMAIN, true, "/Proxy.ashx", "GR_URL"),
        QueryRule(Constants.LINKSYNERGY_CLICK_DOMAIN, true, "/link", "murl")
    )

    override val id = "offline_redirect"
    override val priority = UrlCleaner.PRIORITY_EXTRACTION
    override val displayName = "Redirect Unwrapper"

    override fun matches(url: String): Boolean {
        val host = UrlNormalizer.extractAsciiHost(url) ?: return false
        val path = rawPath(url)
        val query = rawQuery(url)
        return queryRules.any { rule ->
            hostMatches(host, rule.host, rule.exactHost) &&
                path == rule.path &&
                firstParameterValue(query, rule.parameter) != null
        } ||
            isExactHost(host, Constants.REDDITMAIL_CLICK_DOMAIN) &&
            encodedPathSegment(url) != null
    }

    override fun clean(url: String): String {
        if (!matches(url)) return url

        return try {
            val host = UrlNormalizer.extractAsciiHost(url) ?: return url
            val query = rawQuery(url)
            val queryRule = queryRules.firstOrNull { rule ->
                hostMatches(host, rule.host, rule.exactHost) &&
                    rawPath(url) == rule.path &&
                    firstParameterValue(query, rule.parameter) != null
            }
            val encodedTarget = if (queryRule != null) {
                firstParameterValue(query, queryRule.parameter)
            } else if (isExactHost(host, Constants.REDDITMAIL_CLICK_DOMAIN)) {
                encodedPathSegment(url)
            } else {
                null
            }

            encodedTarget
                ?.let(::strictPercentDecode)
                ?.takeIf(::isValidTarget)
                ?: url
        } catch (_: Exception) {
            url
        }
    }

    private fun hostMatches(host: String, domain: String, exactHost: Boolean): Boolean =
        UrlNormalizer.hostMatchesDomain(host, domain) &&
            (!exactHost || isExactHost(host, domain))

    private fun isExactHost(host: String, domain: String): Boolean =
        UrlNormalizer.hostMatchesDomain(host, domain) &&
            UrlNormalizer.hostMatchesDomain(domain, host)

    private fun rawPath(url: String): String {
        val authorityStart = url.indexOf("://").let { if (it >= 0) it + 3 else 0 }
        val pathStart = url.indexOfAny(charArrayOf('/', '?', '#'), authorityStart)
        if (pathStart < 0 || url[pathStart] != '/') return ""
        val pathEnd = url.indexOfAny(charArrayOf('?', '#'), pathStart)
            .let { if (it >= 0) it else url.length }
        return url.substring(pathStart, pathEnd)
    }

    private fun rawQuery(url: String): String? {
        val queryStart = url.indexOf('?')
        val fragmentStart = url.indexOf('#')
        if (queryStart < 0 || (fragmentStart >= 0 && fragmentStart < queryStart)) return null
        return url.substring(queryStart + 1, if (fragmentStart >= 0) fragmentStart else url.length)
    }

    private fun firstParameterValue(query: String?, key: String): String? =
        query
            ?.split('&')
            ?.firstOrNull { it.substringBefore('=') == key }
            ?.substringAfter('=', "")

    private fun encodedPathSegment(url: String): String? =
        rawPath(url)
            .split('/')
            .drop(1)
            .getOrNull(1)
            ?.takeIf { it.isNotEmpty() }

    private fun isValidTarget(target: String): Boolean {
        if (target != target.trim() || !hasValidPercentEscapes(target)) return false
        if (!targetNormalizer.isValidHttpUrl(target)) return false
        if (!target.startsWith("http://", ignoreCase = true) &&
            !target.startsWith("https://", ignoreCase = true)
        ) {
            return false
        }
        return UrlNormalizer.extractAsciiHost(target)?.contains('.') == true
    }

    private fun hasValidPercentEscapes(value: String): Boolean {
        var index = 0
        while (index < value.length) {
            if (value[index] == '%') {
                if (index + 2 >= value.length ||
                    value[index + 1].digitToIntOrNull(16) == null ||
                    value[index + 2].digitToIntOrNull(16) == null
                ) {
                    return false
                }
                index += 3
            } else {
                index++
            }
        }
        return true
    }

    private fun strictPercentDecode(value: String): String? = runCatching {
        val output = ByteArrayOutputStream(value.length)
        var index = 0
        while (index < value.length) {
            if (value[index] == '%') {
                require(index + 2 < value.length)
                output.write(value.substring(index + 1, index + 3).toInt(16))
                index += 3
            } else {
                output.write(value[index].toString().toByteArray(StandardCharsets.UTF_8))
                index++
            }
        }
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(output.toByteArray()))
            .toString()
    }.getOrNull()
}
