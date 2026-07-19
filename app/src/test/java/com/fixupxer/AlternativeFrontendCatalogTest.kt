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


package com.fixupxer

import com.fixupxer.ui.helpers.BrowserConversionDefaultsHelper
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.Constants
import com.fixupxer.utils.FrontendRole
import com.fixupxer.utils.ProxyPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternativeFrontendCatalogTest {

    @Test
    fun `all target ids are unique`() {
        val ids = AlternativeFrontendCatalog.builtInTargets.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `domains are unique within each platform`() {
        ProxyPlatform.entries.forEach { platform ->
            val domains = AlternativeFrontendCatalog.builtIn(platform).map { it.domain }
            assertEquals(
                "duplicate domain on $platform",
                domains.size,
                domains.toSet().size,
            )
        }
    }

    @Test
    fun `no domain appears in two platforms`() {
        val seen = mutableMapOf<String, ProxyPlatform>()
        AlternativeFrontendCatalog.builtInTargets.forEach { target ->
            val previous = seen.put(target.domain, target.platform)
            assertNull(
                "domain ${target.domain} shared by $previous and ${target.platform}",
                previous,
            )
        }
    }

    @Test
    fun `default targets exist and are active built-ins`() {
        ProxyPlatform.entries.forEach { platform ->
            val defaultId = AlternativeFrontendCatalog.defaultTargetId(platform)
            val target = AlternativeFrontendCatalog.byId(defaultId)
            assertNotNull("missing default for $platform", target)
            assertEquals(platform, target!!.platform)
            assertTrue(
                AlternativeFrontendCatalog.builtIn(platform).any { it.id == defaultId },
            )
        }
    }

    @Test
    fun `farside has pathPrefix`() {
        val farside = AlternativeFrontendCatalog.byId("x_farside")
        assertNotNull(farside)
        assertEquals("/nitter", farside!!.pathPrefix)
        assertEquals(Constants.FARSIDE_DOMAIN, farside.domain)
    }

    @Test
    fun `safereddit is sfwOnly`() {
        val safereddit = AlternativeFrontendCatalog.byId("rd_safereddit")
        assertNotNull(safereddit)
        assertTrue(safereddit!!.sfwOnly)
    }

    @Test
    fun `legacy and source domains match spec`() {
        assertEquals(
            listOf(Constants.FXTWITTER_DOMAIN, Constants.VXTWITTER_DOMAIN),
            AlternativeFrontendCatalog.legacyDomains(ProxyPlatform.X),
        )
        assertEquals(
            listOf(Constants.TWITTER_DOMAIN, Constants.X_DOMAIN),
            AlternativeFrontendCatalog.sourceDomains(ProxyPlatform.X),
        )
        assertEquals(
            listOf(Constants.REDDIT_DOMAIN, Constants.REDDIT_SHORT_DOMAIN),
            AlternativeFrontendCatalog.sourceDomains(ProxyPlatform.REDDIT),
        )
    }

    @Test
    fun `byDomain finds built-in targets`() {
        assertEquals(
            "ig_toinstagram",
            AlternativeFrontendCatalog.byDomain(ProxyPlatform.INSTAGRAM, Constants.TOINSTAGRAM_DOMAIN)?.id,
        )
        assertNull(
            AlternativeFrontendCatalog.byDomain(ProxyPlatform.X, Constants.TOINSTAGRAM_DOMAIN),
        )
    }

    @Test
    fun `instagram embed targets preserve primary backup ordering`() {
        val embedDomains = AlternativeFrontendCatalog.builtIn(ProxyPlatform.INSTAGRAM)
            .filter { it.role == FrontendRole.EMBED }
            .map { it.domain }
        assertEquals(Constants.INSTAGRAM_PROXY_DOMAINS, embedDomains)
    }

    @Test
    fun `privacyCapablePlatforms lists reader platforms in enum order`() {
        assertEquals(
            listOf(
                ProxyPlatform.X,
                ProxyPlatform.BLUESKY,
                ProxyPlatform.REDDIT,
                ProxyPlatform.PINTEREST,
            ),
            AlternativeFrontendCatalog.privacyCapablePlatforms(),
        )
    }

    @Test
    fun `browser conversion defaults map every privacy capable platform`() {
        assertEquals(
            AlternativeFrontendCatalog.privacyCapablePlatforms(),
            BrowserConversionDefaultsHelper.entries.map { it.platform },
        )
    }

    @Test
    fun `builtInReaders returns only readers with allowNativeApp false`() {
        AlternativeFrontendCatalog.builtInReaders(ProxyPlatform.X).forEach { target ->
            assertEquals(FrontendRole.READER, target.role)
            assertEquals(false, target.allowNativeApp)
        }
        listOf(
            ProxyPlatform.TIKTOK,
            ProxyPlatform.FACEBOOK,
            ProxyPlatform.INSTAGRAM,
            ProxyPlatform.YOUTUBE,
            ProxyPlatform.THREADS,
        ).forEach { platform ->
            assertTrue(
                "expected no readers for $platform",
                AlternativeFrontendCatalog.builtInReaders(platform).isEmpty(),
            )
        }
    }
}
