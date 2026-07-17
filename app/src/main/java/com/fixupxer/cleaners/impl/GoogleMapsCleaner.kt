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

import com.fixupxer.cleaners.CleanerCategory
import com.fixupxer.cleaners.UrlCleaner
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.utils.Constants

object GoogleMapsCleaner : UrlCleaner {
    override val id = "google_maps"
    override val displayName = "Google Maps"
    override val priority = UrlCleaner.PRIORITY_DOMAIN
    override val category = CleanerCategory.SEARCH_ENGINES

    private val coordinateSegment = Regex(
        "(?:^|/)@(-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?),(\\d+(?:\\.\\d+)?z)(?=/|$)"
    )

    override fun matches(url: String): Boolean {
        val host = UrlNormalizer.extractAsciiHost(url)
        val isMapsHost = UrlNormalizer.hostMatchesDomain(host, Constants.GOOGLE_MAPS_DOMAIN)
        val isGoogleMapsPath = UrlNormalizer.hostMatchesDomain(host, Constants.GOOGLE_DOMAIN) &&
            isPathSegment(rawPath(url), "/maps")

        return (isMapsHost || isGoogleMapsPath) &&
            coordinateSegment.containsMatchIn(rawPath(url))
    }

    override fun clean(url: String): String {
        if (!matches(url)) return url

        return try {
            val schemeEnd = url.indexOf("://")
            if (schemeEnd <= 0) return url

            val coordinates = coordinateSegment.find(rawPath(url)) ?: return url
            val scheme = url.substring(0, schemeEnd)
            "$scheme://www.google.com/maps/@${coordinates.groupValues[1]}," +
                "${coordinates.groupValues[2]},${coordinates.groupValues[3]}"
        } catch (_: Exception) {
            url
        }
    }

    private fun isPathSegment(path: String, segment: String): Boolean =
        path == segment || path.startsWith("$segment/")

    private fun rawPath(url: String): String {
        val authorityStart = url.indexOf("://").let { if (it >= 0) it + 3 else 0 }
        val pathStart = url.indexOfAny(charArrayOf('/', '?', '#'), authorityStart)
        if (pathStart < 0 || url[pathStart] != '/') return ""
        val pathEnd = url.indexOfAny(charArrayOf('?', '#'), pathStart)
            .let { if (it >= 0) it else url.length }
        return url.substring(pathStart, pathEnd)
    }
}
