// SPDX-License-Identifier: GPL-3.0-or-later
/* FixupXer - URL Enhancer. Copyright (C) 2020-2026 NeatCode Labs.
 * Licensed under GPL version 3 or (at your option) any later version. */
package com.fixupxer

import com.fixupxer.processing.*
import com.fixupxer.utils.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.net.URI

class BrowserFrontendConversionTest {
    @Before fun setup() = ProxyRoster.reset()
    @After fun teardown() = ProxyRoster.reset()

    private val fixtures = mapOf(
        ProxyPlatform.X to "https://x.com/user/status/123?keep=a%2Fb&keep=2#fxtwitter.com",
        ProxyPlatform.INSTAGRAM to "https://www.instagram.com/reel/Abc/?img_index=2#fragment",
        ProxyPlatform.TIKTOK to "https://vm.tiktok.com/Z123/?keep=a%2Fb#fragment",
        ProxyPlatform.BLUESKY to "https://bsky.app/profile/a.test/post/123?keep=a%2Fb#fragment",
        ProxyPlatform.FACEBOOK to "https://www.facebook.com/story.php?story_fbid=123&id=45#fragment",
        ProxyPlatform.REDDIT to "https://www.reddit.com/r/test/comments/123/title/?keep=a%2Fb#fragment",
        ProxyPlatform.PINTEREST to "https://www.pinterest.com/pin/123/?keep=a%2Fb#fragment",
    )

    @Test fun `every eligible built in and custom target preserves functional suffix`() {
        fixtures.forEach { (platform, input) ->
            ProxyRoster.setCustomProxies(platform, listOf("${platform.name.lowercase()}.example.org"))
            BrowserFrontendPolicy.allowedTargets(platform, ProxyRoster.activeTargets(platform)).forEach { target ->
                val snapshot = BrowserFrontendSnapshot(mapOf(platform to BrowserFrontendPreference(BrowserFrontendPolicy.modeFor(target), target.id)))
                val result = snapshot.convert(input)
                assertEquals("$platform ${target.id}", PipelineStatus.COMPLETE, result.status)
                assertTrue("$platform ${target.id}: ${result.url}", UrlNormalizer.hostMatchesDomain(URI(result.url).host, target.domain))
                assertEquals(URI(input).rawQuery, URI(result.url).rawQuery)
                assertEquals(URI(input).rawFragment, URI(result.url).rawFragment)
                if (platform == ProxyPlatform.TIKTOK) assertEquals("vm.${target.domain}", URI(result.url).host)
            }
        }
    }

    @Test fun `clean only preserves known frontend hosts and query literals`() {
        listOf("https://fxtwitter.com/u/status/123?next=fxtwitter.com#fxtwitter.com", "https://vm.tnktok.com/Z123/").forEach { input ->
            assertEquals(input, BrowserFrontendSnapshot(emptyMap()).convert(input).url)
        }
    }

    @Test fun `fixupx replaces only the host including from old frontend`() {
        val target = AlternativeFrontendCatalog.builtIn(ProxyPlatform.X).first { it.domain == Constants.FIXUPX_DOMAIN }
        val snapshot = BrowserFrontendSnapshot(mapOf(ProxyPlatform.X to BrowserFrontendPreference(BrowserConversionMode.EMBED, target.id)))
        val input = "https://fxtwitter.com/u/status/123/photo/2?next=fxtwitter.com#fxtwitter.com"
        assertEquals("https://fixupx.com/u/status/123/photo/2?next=fxtwitter.com#fxtwitter.com", snapshot.convert(input).url)
    }

    @Test fun `unsupported paths authority and missing embed stay local`() {
        val target = AlternativeFrontendCatalog.builtIn(ProxyPlatform.BLUESKY).first { it.role == FrontendRole.EMBED }
        val preference = BrowserFrontendPreference(BrowserConversionMode.EMBED, target.id)
        val snapshot = BrowserFrontendSnapshot(mapOf(ProxyPlatform.BLUESKY to preference))
        listOf("https://bsky.app/profile/a.test", "https://a:b@bsky.app/profile/a.test/post/123", "https://bsky.app:8080/profile/a.test/post/123").forEach { input ->
            assertEquals(input, snapshot.convert(input).url)
            assertEquals(PipelineStatus.UNSUPPORTED_CONVERSION, snapshot.convert(input).status)
        }
        ProxyRoster.setDisabledBuiltIns(ProxyPlatform.BLUESKY, setOf(target.id))
        assertEquals(PipelineStatus.STALE_CONFIGURATION, snapshot.convert(fixtures.getValue(ProxyPlatform.BLUESKY)).status)
        assertEquals(PipelineStatus.FRONTEND_UNAVAILABLE, BrowserFrontendSnapshot(mapOf(ProxyPlatform.BLUESKY to preference)).convert(fixtures.getValue(ProxyPlatform.BLUESKY)).status)
    }

    @Test fun `lookalike and query host do not select a frontend`() {
        val target = AlternativeFrontendCatalog.builtIn(ProxyPlatform.TIKTOK).first()
        val snapshot = BrowserFrontendSnapshot(mapOf(ProxyPlatform.TIKTOK to BrowserFrontendPreference(BrowserConversionMode.EMBED, target.id)))
        listOf("https://tiktok.com.example.org/@a/video/123", "https://example.org/?next=https://tiktok.com/").forEach { input ->
            assertEquals(input, snapshot.convert(input).url)
            assertNull(snapshot.platformFor(input))
        }
    }
}
