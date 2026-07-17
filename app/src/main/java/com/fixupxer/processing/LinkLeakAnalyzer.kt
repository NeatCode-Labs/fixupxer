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

package com.fixupxer.processing

import com.fixupxer.utils.Constants
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

enum class LeakCategory {
    CREDENTIALS,
    EMAIL,
    JWT,
    TOKEN_PARAM,
    COORDINATES
}

enum class LeakComponent {
    USERINFO,
    PATH,
    QUERY,
    FRAGMENT
}

data class LeakFinding(
    val category: LeakCategory,
    val component: LeakComponent,
    /** Query parameter name when applicable; never the value. */
    val parameterName: String? = null
)

/**
 * Finds only high-confidence sensitive values that are literally present in an HTTP(S) URL.
 *
 * Values are percent-decoded at most once and '+' remains a literal plus. The guard does not
 * inspect path coordinates: deliberately shared paths such as Google Maps' @lat,lng form are
 * not treated as a leak.
 */
object LinkLeakAnalyzer {

    fun analyze(url: String): List<LeakFinding> = runCatching {
        analyzeNormalized(UrlNormalizer().normalize(url))
    }.getOrDefault(emptyList())

    private fun analyzeNormalized(url: NormalizedUrl): List<LeakFinding> {
        val findings = mutableListOf<LeakFinding>()
        val queryParameters = parseParameters(url.rawQuery)
        val fragmentParameters = parseParameters(fragmentQuery(url.rawFragment))

        fun add(finding: LeakFinding) {
            if (findings.size < Constants.MAX_LEAK_FINDINGS) findings += finding
        }

        val userInfo = url.userInfo
        if (userInfo != null) {
            val separator = userInfo.indexOf(':')
            if (separator >= 0 && separator < userInfo.lastIndex) {
                add(LeakFinding(LeakCategory.CREDENTIALS, LeakComponent.USERINFO))
            }
        }

        val reportedEmails = mutableSetOf<String>()
        fun findEmails(component: LeakComponent, value: String, parameterName: String? = null) {
            Constants.LEAK_EMAIL_PATTERN.findAll(percentDecodeOnce(value)).forEach { match ->
                if (reportedEmails.add(match.value.lowercase(Locale.ROOT))) {
                    add(LeakFinding(LeakCategory.EMAIL, component, parameterName))
                }
            }
        }

        findEmails(LeakComponent.PATH, url.rawPath)
        queryParameters.forEach { parameter ->
            findEmails(LeakComponent.QUERY, parameter.value, parameter.name.takeIf { it.isNotEmpty() })
        }
        fragmentParameters.forEach { parameter ->
            findEmails(LeakComponent.FRAGMENT, parameter.value, parameter.name.takeIf { it.isNotEmpty() })
        }
        url.rawFragment?.let { findEmails(LeakComponent.FRAGMENT, it) }

        val reportedJwtValues = mutableSetOf<String>()
        fun findJwt(component: LeakComponent, value: String, parameterName: String? = null) {
            Constants.LEAK_JWT_PATTERN.findAll(percentDecodeOnce(value)).forEach { match ->
                if (reportedJwtValues.add(match.value)) {
                    add(LeakFinding(LeakCategory.JWT, component, parameterName))
                }
            }
        }

        findJwt(LeakComponent.PATH, url.rawPath)
        queryParameters.forEach { parameter ->
            findJwt(LeakComponent.QUERY, parameter.value, parameter.name.takeIf { it.isNotEmpty() })
        }
        url.rawQuery?.let { findJwt(LeakComponent.QUERY, it) }
        fragmentParameters.forEach { parameter ->
            findJwt(LeakComponent.FRAGMENT, parameter.value, parameter.name.takeIf { it.isNotEmpty() })
        }
        url.rawFragment?.let { findJwt(LeakComponent.FRAGMENT, it) }

        findSensitiveParameters(queryParameters, LeakComponent.QUERY, ::add)
        findSensitiveParameters(fragmentParameters, LeakComponent.FRAGMENT, ::add)
        findPreciseCoordinates(queryParameters, LeakComponent.QUERY, ::add)
        findPreciseCoordinates(fragmentParameters, LeakComponent.FRAGMENT, ::add)

        return findings
    }

    private fun findSensitiveParameters(
        parameters: List<RawParameter>,
        component: LeakComponent,
        add: (LeakFinding) -> Unit
    ) {
        parameters.forEach { parameter ->
            if (
                parameter.name.lowercase(Locale.ROOT) in Constants.LEAK_SENSITIVE_PARAM_NAMES &&
                percentDecodeOnce(parameter.value).length >= Constants.LEAK_MIN_TOKEN_VALUE_LENGTH
            ) {
                add(LeakFinding(LeakCategory.TOKEN_PARAM, component, parameter.name))
            }
        }
    }

    private fun findPreciseCoordinates(
        parameters: List<RawParameter>,
        component: LeakComponent,
        add: (LeakFinding) -> Unit
    ) {
        val latitude = parameters.firstOrNull {
            it.name.lowercase(Locale.ROOT) in Constants.LEAK_LATITUDE_PARAM_NAMES &&
                Constants.LEAK_PRECISE_COORDINATE_PATTERN.matches(percentDecodeOnce(it.value))
        }
        val longitude = parameters.firstOrNull {
            it.name.lowercase(Locale.ROOT) in Constants.LEAK_LONGITUDE_PARAM_NAMES &&
                Constants.LEAK_PRECISE_COORDINATE_PATTERN.matches(percentDecodeOnce(it.value))
        }
        if (latitude != null && longitude != null) {
            add(LeakFinding(LeakCategory.COORDINATES, component, latitude.name))
            add(LeakFinding(LeakCategory.COORDINATES, component, longitude.name))
        }
    }

    private fun fragmentQuery(fragment: String?): String? {
        if (fragment.isNullOrEmpty() || '=' !in fragment) return null
        return fragment.substringAfter('?', fragment)
    }

    private fun parseParameters(rawQuery: String?): List<RawParameter> =
        rawQuery
            ?.split('&')
            ?.map { token ->
                RawParameter(
                    name = token.substringBefore('='),
                    value = token.substringAfter('=', "")
                )
            }
            ?: emptyList()

    /**
     * Decodes percent escapes once without applying form-urlencoding's '+' → space conversion.
     * Malformed escapes remain literal, keeping analysis fail-closed and non-throwing.
     */
    private fun percentDecodeOnce(value: String): String {
        val result = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            if (value[index] != '%' || index + 2 >= value.length) {
                result.append(value[index])
                index++
                continue
            }

            val bytes = ByteArrayOutputStream()
            var cursor = index
            while (cursor + 2 < value.length && value[cursor] == '%') {
                val byte = value.substring(cursor + 1, cursor + 3).toIntOrNull(16) ?: break
                bytes.write(byte)
                cursor += 3
            }
            if (cursor == index) {
                result.append(value[index])
                index++
            } else {
                result.append(bytes.toByteArray().toString(StandardCharsets.UTF_8))
                index = cursor
            }
        }
        return result.toString()
    }

    private data class RawParameter(
        val name: String,
        val value: String
    )
}
