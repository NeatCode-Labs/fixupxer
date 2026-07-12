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

package com.fixupxer.processing

import com.fixupxer.utils.Constants
import java.net.IDN
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class NormalizedUrl(
    val comparisonUrl: String,
    val scheme: String,
    val rawAuthority: String,
    val userInfo: String?,
    val asciiHost: String,
    val port: Int?,
    val rawPath: String,
    val rawQuery: String?,
    val rawFragment: String?
) {
    val cycleKey: String
        get() = buildString {
            append(scheme)
            append("://")
            append(asciiHost)
            if (port != null) append(":$port")
            append(rawPath)
            if (rawQuery != null) append('?').append(rawQuery)
            if (rawFragment != null) append('#').append(rawFragment)
        }
}

/**
 * Structural HTTP(S) parser that never decodes path, query, or fragment.
 */
@Singleton
class UrlNormalizer @Inject constructor() {
    fun normalize(input: String): NormalizedUrl {
        val value = input.trim().removePrefix("@")
        require(value.length <= Constants.MAX_URL_LENGTH) { "URL is too long" }
        require(value.none { it == '\\' || it.code == 0 || it.code in 1..31 }) {
            "URL contains invalid characters"
        }

        val schemeEnd = value.indexOf("://")
        require(schemeEnd > 0) { "URL must be absolute" }
        val scheme = value.substring(0, schemeEnd).lowercase(Locale.ROOT)
        require(scheme == "http" || scheme == "https") { "Only HTTP(S) URLs are supported" }

        val authorityStart = schemeEnd + 3
        val authorityEnd = value.indexOfAny(charArrayOf('/', '?', '#'), authorityStart)
            .let { if (it == -1) value.length else it }
        val authority = value.substring(authorityStart, authorityEnd)
        require(authority.isNotBlank()) { "URL host is required" }

        val at = authority.lastIndexOf('@')
        val userInfo = authority.takeIf { at >= 0 }?.substring(0, at)
        val hostPort = authority.substring(at + 1)
        val (rawHost, port) = parseHostPort(hostPort)
        val asciiHost = if (rawHost.contains(':')) {
            rawHost.lowercase(Locale.ROOT)
        } else {
            IDN.toASCII(rawHost.removeSuffix("."), IDN.USE_STD3_ASCII_RULES)
                .lowercase(Locale.ROOT)
        }
        require(asciiHost.isNotBlank()) { "URL host is required" }

        val fragmentIndex = value.indexOf('#', authorityEnd)
        val queryIndex = value.indexOf('?', authorityEnd)
            .takeIf { it >= 0 && (fragmentIndex < 0 || it < fragmentIndex) } ?: -1
        val pathEnd = listOf(queryIndex, fragmentIndex).filter { it >= 0 }.minOrNull() ?: value.length
        val rawPath = value.substring(authorityEnd, pathEnd)
        val rawQuery = if (queryIndex >= 0) {
            value.substring(queryIndex + 1, if (fragmentIndex >= 0) fragmentIndex else value.length)
        } else {
            null
        }
        val rawFragment = if (fragmentIndex >= 0) value.substring(fragmentIndex + 1) else null

        return NormalizedUrl(
            comparisonUrl = value,
            scheme = scheme,
            rawAuthority = authority,
            userInfo = userInfo,
            asciiHost = asciiHost,
            port = port,
            rawPath = rawPath,
            rawQuery = rawQuery,
            rawFragment = rawFragment
        )
    }

    fun isValidHttpUrl(value: String): Boolean = runCatching { normalize(value) }.isSuccess

    private fun parseHostPort(hostPort: String): Pair<String, Int?> {
        if (hostPort.startsWith("[")) {
            val close = hostPort.indexOf(']')
            require(close > 1) { "Invalid IPv6 host" }
            val host = hostPort.substring(1, close)
            val suffix = hostPort.substring(close + 1)
            val port = when {
                suffix.isEmpty() -> null
                suffix.startsWith(":") -> parsePort(suffix.substring(1))
                else -> throw IllegalArgumentException("Invalid authority")
            }
            return host to port
        }

        val colon = hostPort.lastIndexOf(':')
        if (colon <= 0 || hostPort.indexOf(':') != colon) return hostPort to null
        val possiblePort = hostPort.substring(colon + 1)
        return if (possiblePort.all(Char::isDigit)) {
            hostPort.substring(0, colon) to parsePort(possiblePort)
        } else {
            hostPort to null
        }
    }

    private fun parsePort(value: String): Int {
        val port = value.toIntOrNull() ?: throw IllegalArgumentException("Invalid port")
        require(port in 1..65535) { "Invalid port" }
        return port
    }
}
