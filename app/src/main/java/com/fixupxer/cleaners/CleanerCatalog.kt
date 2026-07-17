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

package com.fixupxer.cleaners

import com.fixupxer.cleaners.impl.AmazonCleaner
import com.fixupxer.cleaners.impl.CatalogParameterCleaner
import com.fixupxer.cleaners.impl.FacebookCleaner
import com.fixupxer.cleaners.impl.GeneralTrackingCleaner
import com.fixupxer.cleaners.impl.GoogleMapsCleaner
import com.fixupxer.cleaners.impl.GoogleSearchCleaner
import com.fixupxer.cleaners.impl.InstagramCleaner
import com.fixupxer.cleaners.impl.LinkedInCleaner
import com.fixupxer.cleaners.impl.OfflineRedirectCleaner
import com.fixupxer.cleaners.impl.RedditCleaner
import com.fixupxer.cleaners.impl.SubstackCleaner
import com.fixupxer.cleaners.impl.TikTokCleaner
import com.fixupxer.cleaners.impl.TwitterCleaner
import com.fixupxer.cleaners.impl.YouTubeCleaner

object CleanerCatalog {
    fun createBuiltInCleaners(): List<UrlCleaner> =
        listOf(
            AmazonCleaner,
            YouTubeCleaner,
            GoogleSearchCleaner,
            GoogleMapsCleaner,
            TwitterCleaner,
            InstagramCleaner,
            FacebookCleaner,
            RedditCleaner,
            TikTokCleaner,
            LinkedInCleaner,
            SubstackCleaner,
            OfflineRedirectCleaner
        ) +
            ParameterRuleCatalog.rules.map(::CatalogParameterCleaner) +
            GeneralTrackingCleaner()
}
