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

package com.fixupxer

import com.fixupxer.utils.Constants
import com.fixupxer.utils.NativeLaunchResolver
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class NativeLaunchResolverTest {

    @Before
    fun setup() = ProxyRoster.reset()

    @After
    fun tearDown() = ProxyRoster.reset()

    @Test
    fun `X embed and legacy aliases resolve to exact native uri`() {
        listOf(
            Constants.FIXUPX_DOMAIN,
            Constants.FXTWITTER_DOMAIN,
            Constants.VXTWITTER_DOMAIN,
        ).forEach { domain ->
            assertResolution(
                finalUrl = "https://$domain/Ash%2FCrypto/status/1?lang=en&keep=%2B#media",
                expectedUri = "https://x.com/Ash%2FCrypto/status/1?lang=en&keep=%2B#media",
                expectedPackages = listOf("com.twitter.android"),
            )
        }
    }

    @Test
    fun `Instagram embed and explicitly classified legacy aliases resolve to Instagram`() {
        val domains = listOf(
            Constants.TOINSTAGRAM_DOMAIN,
            Constants.ADAMLIKES_DOMAIN,
            Constants.INSTAGRAM7_DOMAIN,
            "eeinstagram.com",
        )
        domains.forEach { domain ->
            assertResolution(
                finalUrl = "https://www.$domain/reel/A%2Fb/?img_index=2#caption%20part",
                expectedUri = "https://instagram.com/reel/A%2Fb/?img_index=2#caption%20part",
                expectedPackages = listOf("com.instagram.android"),
            )
        }
    }

    @Test
    fun `TikTok embed and legacy aliases preserve supported short subdomains`() {
        val domains = Constants.TIKTOK_PROXY_DOMAINS + Constants.TIKTOK_LEGACY_PROXIES
        domains.forEach { domain ->
            listOf("" to "", "www." to "www.", "vm." to "vm.", "vt." to "vt.")
                .forEach { (inputPrefix, outputPrefix) ->
                    assertResolution(
                        finalUrl = "https://$inputPrefix$domain/Z%2F123/?is_from_webapp=1#slide%202",
                        expectedUri = "https://$outputPrefix${Constants.TIKTOK_DOMAIN}/Z%2F123/?is_from_webapp=1#slide%202",
                        expectedPackages = listOf("com.zhiliaoapp.musically"),
                    )
                }
        }
    }

    @Test
    fun `source hosts retain their exact final uri and existing packages`() {
        assertResolution(
            finalUrl = "https://mobile.twitter.com/user/status/1?keep=%2B#part",
            expectedUri = "https://mobile.twitter.com/user/status/1?keep=%2B#part",
            expectedPackages = listOf("com.twitter.android"),
        )
        assertResolution(
            finalUrl = "https://m.instagram.com/p/abc/",
            expectedUri = "https://m.instagram.com/p/abc/",
            expectedPackages = listOf("com.instagram.android"),
        )
        assertResolution(
            finalUrl = "https://vm.tiktok.com/Z123/",
            expectedUri = "https://vm.tiktok.com/Z123/",
            expectedPackages = listOf("com.zhiliaoapp.musically"),
        )
    }

    @Test
    fun `reader experimental and custom destinations never reverse to native`() {
        ProxyRoster.setCustomProxies(ProxyPlatform.INSTAGRAM, listOf("custom.example"))
        ProxyRoster.setCustomProxies(ProxyPlatform.TIKTOK, listOf("tok.example"))

        listOf(
            "https://xcancel.com/user/status/1",
            "https://farside.link/nitter/user/status/1",
            "https://kittygr.am/p/abc",
            "https://custom.example/p/abc",
            "https://tok.example/@user/video/1",
        ).forEach { url -> assertNull(url, NativeLaunchResolver.resolve(url)) }
    }

    @Test
    fun `unsupported proxy forms retired domains and lookalikes fall back`() {
        listOf(
            "https://d.fixupx.com/user/status/1",
            "https://cards.fxtwitter.com/user/status/1",
            "https://m.tnktok.com/@user/video/1",
            "https://fixupx.com:8443/user/status/1",
            "https://user@toinstagram.com/p/abc",
            "https://fixupx.com.evil.example/user/status/1",
            "https://kkinstagram.com/p/abc",
            "https://facebookez.com/post/1",
        ).forEach { url -> assertNull(url, NativeLaunchResolver.resolve(url)) }
    }

    @Test
    fun `unrelated native mappings stay unchanged and no new platform mapping appears`() {
        assertResolution(
            finalUrl = "https://youtu.be/abc?t=3",
            expectedUri = "https://youtu.be/abc?t=3",
            expectedPackages = listOf(
                "app.revanced.android.youtube",
                "app.morphe.android.youtube",
                "com.google.android.youtube",
            ),
        )
        assertNull(NativeLaunchResolver.resolve("https://bsky.app/profile/user/post/1"))
        assertNull(NativeLaunchResolver.resolve("https://pinterest.com/pin/1"))
        assertNull(NativeLaunchResolver.resolve("https://threads.net/@user/post/1"))
    }

    private fun assertResolution(
        finalUrl: String,
        expectedUri: String,
        expectedPackages: List<String>,
    ) {
        val resolution = NativeLaunchResolver.resolve(finalUrl)
        assertEquals(finalUrl, expectedUri, resolution?.uri)
        assertEquals(finalUrl, expectedPackages, resolution?.packageNames)
    }
}
