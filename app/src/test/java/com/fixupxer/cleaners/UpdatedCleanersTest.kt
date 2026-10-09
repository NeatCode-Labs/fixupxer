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


package com.fixupxer.cleaners

import com.fixupxer.cleaners.impl.*
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatedCleanersTest {
    @Test
    fun testInstagramCleanerRemovesReportedExlnFromReelUrl() {
        val url = "https://www.instagram.com/reel/DeQMt21oZ9Y/?exln=MWQ2dWVhcm1pYndsag=="
        assertEquals(
            "https://www.instagram.com/reel/DeQMt21oZ9Y/",
            InstagramCleaner.clean(url)
        )
    }

    @Test
    fun testInstagramCleanerRemovesReportedObrfFromReelUrl() {
        val url = "https://www.instagram.com/reel/DeO0lB3ub9j/?obrf=MWozZ3k1aGgyYzlnMQ=="
        assertEquals(
            "https://www.instagram.com/reel/DeO0lB3ub9j/",
            InstagramCleaner.clean(url)
        )
    }

    @Test
    fun testInstagramCanonicalCleaningPreservesOnlyCarouselAndIsIdempotent() {
        val url = "https://www.instagram.com/reel/DeQMt21oZ9Y/?keep=a%26b%3Dc+z&exln=MWQ2dWVhcm1pYndsag==&obrf=MWozZ3k1aGgyYzlnMQ==&img_index=2#slide"
        val expected = "https://www.instagram.com/reel/DeQMt21oZ9Y/?img_index=2"
        val cleaned = InstagramCleaner.clean(url)

        assertEquals(expected, cleaned)
        assertEquals(expected, InstagramCleaner.clean(cleaned))
        listOf(
            "https://example.org/reel/DeQMt21oZ9Y/?exln=keep&obrf=keep",
            "https://instagram.com.example.org/reel/DeQMt21oZ9Y/?exln=keep&obrf=keep"
        ).forEach { unrelatedUrl ->
            assertEquals(unrelatedUrl, InstagramCleaner.clean(unrelatedUrl))
        }
        val fragmentOnly = "https://www.instagram.com/reel/DeQMt21oZ9Y/#?exln=fragment-value&obrf=fragment-value"
        assertEquals("https://www.instagram.com/reel/DeQMt21oZ9Y/", InstagramCleaner.clean(fragmentOnly))
    }

    @Test
    fun testInstagramExlnAndObrfCleaningAlsoCoversKnownProxyHosts() {
        com.fixupxer.utils.InstagramProxyStore.allKnownProxies().forEach { host ->
            assertEquals(
                "https://$host/reel/DeQMt21oZ9Y/?img_index=2#slide",
                InstagramCleaner.clean(
                    "https://$host/reel/DeQMt21oZ9Y/?exln=MWQ2dWVhcm1pYndsag==&obrf=MWozZ3k1aGgyYzlnMQ==&img_index=2#slide"
                )
            )
        }
    }

    @Test
    fun testInstagramCleanerRemovesReportedStknShareParameter() {
        val url = "https://www.instagram.com/reel/Dc4fAOCs97R/?stkn=anBpYnlkeG82MDJz"
        assertEquals("https://www.instagram.com/reel/Dc4fAOCs97R/", InstagramCleaner.clean(url))
    }

    @Test
    fun testInstagramCleanerRemovesShareRidAlongsideCurrentAndOlderShareIds() {
        val url = "https://www.instagram.com/p/ABC123/?ig_rid=share&stkn=current&igsi=previous&igsh=older&igshid=legacy&img_index=2&keep=a%26b#slide"
        val expected = "https://www.instagram.com/p/ABC123/?img_index=2"
        assertEquals(expected, InstagramCleaner.clean(url))
        assertEquals(expected, InstagramCleaner.clean(expected))
        val otherHost = "https://example.org/?ig_rid=keep&stkn=keep"
        assertEquals(otherHost, InstagramCleaner.clean(otherHost))
    }

    @Test
    fun testInstagramStknDuplicatesPreserveOnlyCarouselOnCanonicalPosts() {
        val url = "https://www.instagram.com/p/ABC123/?stkn=first&img_index=2&keep=a%26b%3Dc+z&stkn=second&stkn#slide"
        val expected = "https://www.instagram.com/p/ABC123/?img_index=2"
        val cleaned = InstagramCleaner.clean(url)
        assertEquals(expected, cleaned)
        assertEquals(expected, InstagramCleaner.clean(cleaned))
    }

    @Test
    fun testInstagramStknCleaningAlsoCoversKnownProxyHosts() {
        com.fixupxer.utils.InstagramProxyStore.allKnownProxies().forEach { host ->
            assertEquals(
                "https://$host/reel/ABC123/?img_index=2#slide",
                InstagramCleaner.clean("https://$host/reel/ABC123/?stkn=share&img_index=2#slide")
            )
        }
    }

    @Test
    fun testInstagramStknDoesNotAffectOtherHostsOrFragmentContents() {
        listOf(
            "https://instagram.com.example.org/reel/ABC123/?stkn=keep",
            "https://notinstagram.com/reel/ABC123/?stkn=keep",
            "https://example.org/instagram.com?stkn=keep",
            "https://www.instagram.com/stories/user/123/#?stkn=keep",
            "https://www.instagram.com/reel/ABC123/"
        ).forEach { url -> assertEquals(url, InstagramCleaner.clean(url)) }
    }

    @Test
    fun testInstagramCanonicalPostsRemoveRotatingNamesOnEverySupportedPath() {
        val reported = "https://www.instagram.com/reel/Dd_BUoKTr-N/?vrfl=MXVyMTZhZnU4OWNjMA=="
        assertEquals("https://www.instagram.com/reel/Dd_BUoKTr-N/", InstagramCleaner.clean(reported))
        listOf("instagram.com", "www.instagram.com", "m.instagram.com").forEach { host ->
            listOf("p", "reel", "reels", "tv").forEach { kind ->
                listOf("/$kind/Dd_BUoKTr-N/", "/creator.name/$kind/Dd_BUoKTr-N").forEach { path ->
                    val base = "https://$host$path"
                    val result = InstagramCleaner.clean("$base?future_key=opaque&VRFL=other&img_index=02#opaque")
                    assertEquals("$base?img_index=2", result)
                    assertEquals(result, InstagramCleaner.clean(result))
                }
            }
        }
    }

    @Test
    fun testInstagramCanonicalCarouselSelectorRejectsOpaqueAndAmbiguousValues() {
        val base = "https://www.instagram.com/p/ABC/"
        listOf("", "0", "-1", "+2", "2.0", "2147483648", "opaque", "2%26tracking", "%32").forEach { value ->
            assertEquals(base, InstagramCleaner.clean("$base?img_index=$value&future=token"))
        }
        assertEquals("$base?img_index=2", InstagramCleaner.clean("$base?img_index=02&img_index=2&future=token"))
        listOf("img_index=2&img_index=3", "img_index=2&img_index=opaque", "img_index", "IMG_INDEX=2", "img%5Findex=2").forEach { query ->
            assertEquals(base, InstagramCleaner.clean("$base?$query"))
        }
    }

    @Test
    fun testInstagramCanonicalPolicyLeavesSpecialRoutesAndAuthoritiesAlone() {
        listOf(
            "https://www.instagram.com/accounts/login/?next=%2Fp%2FABC%2F#state",
            "https://www.instagram.com/share/reel/ABC/?future=token#state",
            "https://www.instagram.com/stories/highlights/123/?story_media_id=456&future=token#state",
            "https://www.instagram.com/explore/search/?q=photo#state",
            "https://www.instagram.com/creator/?future=token#state",
            "https://www.instagram.com/p/ABC/embed/?future=token#state",
            "https://www.instagram.com/p/A%2FB/?future=token#state",
            "https://www.instagram.com/p/ABC/2/?future=token#state",
            "https://www.instagram.com/p/ABC/../DEF/?future=token#state",
            "https://www.instagram.com/./p/ABC/?future=token#state",
            "https://www.instagram.com/../p/ABC/?future=token#state",
            "https://www.instagram.com/accounts/p/ABC/?future=token#state",
            "https://l.instagram.com/p/ABC/?future=token#state",
            "https://user@www.instagram.com/p/ABC/?future=token#state",
            "https://www.instagram.com:444/p/ABC/?future=token#state",
            "https://instagram.com:/reel/X/?vrfl=future#state",
            "http://www.instagram.com:/p/ABC/?future=token#state",
            "https://www.instagram.com:99999/p/ABC/?future=token#state",
            "https://www.instagram.com/p/ABC/?future=%ZZ#state",
            "https://instagram.com.example.org/p/ABC/?future=token#state"
        ).forEach { url -> assertEquals(url, InstagramCleaner.clean(url)) }
        assertEquals("https://www.instagram.com:443/p/ABC/", InstagramCleaner.clean("https://www.instagram.com:443/p/ABC/?future=token"))
        assertEquals("http://instagram.com/p/ABC/", InstagramCleaner.clean("http://instagram.com/p/ABC/?future=token"))
    }

    @Test
    fun testInstagramProxyOptionsAreOutsideCanonicalPolicy() {
        com.fixupxer.utils.InstagramProxyStore.allKnownProxies().forEach { host ->
            val url = "https://$host/p/ABC/?img_index=2&direct=true&gallery=true&future=custom#part"
            assertEquals(url, InstagramCleaner.clean(url))
        }
        val functional = "img_index=2&story_media_id=456&h=200&w=300&carousel_index=3&media_id=123&reel_ids=123&highlight_reel_ids=456"
        listOf("https://www.instagram.com/stories/user/123/", "https://toinstagram.com/p/ABC/").forEach { base ->
            assertEquals("$base?$functional#part", InstagramCleaner.clean("$base?stkn=share&$functional#part"))
        }
    }

    
    @Test
    fun testInstagramCleanerRemovesIgshParameter() {
        val url = "https://www.instagram.com/p/DLRNJjEx45S/?igsh=cWdtYXd0NmE3YnI0"
        val expected = "https://www.instagram.com/p/DLRNJjEx45S/"
        assertEquals(expected, InstagramCleaner.clean(url))
    }

    @Test
    fun testInstagramCleanerRemovesIgsiParameter() {
        // Instagram renamed the share identifier to "igsi" (same tracker, new name)
        val url = "https://www.instagram.com/reel/Da0a2ylvv4z/?igsi=Nm44MGppNTFIZXNw"
        val expected = "https://www.instagram.com/reel/Da0a2ylvv4z/"
        assertEquals(expected, InstagramCleaner.clean(url))
    }

    @Test
    fun testInstagramCleanerRemovesIgsiAndPreservesCarouselIndex() {
        val url = "https://www.instagram.com/p/ABC123/?igsi=Nm44MGppNTFIZXNw&img_index=2"
        val expected = "https://www.instagram.com/p/ABC123/?img_index=2"
        assertEquals(expected, InstagramCleaner.clean(url))
    }
    
    @Test
    fun testInstagramCleanerPreservesEssentialParams() {
        val url = "https://www.instagram.com/p/ABC123/?igsh=test&img_index=2&share_id=xyz"
        val expected = "https://www.instagram.com/p/ABC123/?img_index=2"
        assertEquals(expected, InstagramCleaner.clean(url))
    }
    
    @Test
    fun testInstagramCleanerHandlesStoryMediaId() {
        val url = "https://www.instagram.com/stories/highlights/123/?story_media_id=456&ig_cache_key=abc"
        val expected = "https://www.instagram.com/stories/highlights/123/?story_media_id=456"
        assertEquals(expected, InstagramCleaner.clean(url))
    }
    
    @Test
    fun testTwitterCleanerRemovesTrackingParams() {
        val url = "https://x.com/naiivememe/status/1939300095859827030?t=dRpS7q5ckejABEIxq3Hd_w&s=09"
        val expected = "https://x.com/naiivememe/status/1939300095859827030"
        assertEquals(expected, TwitterCleaner.clean(url))
    }
    
    @Test
    fun testTwitterCleanerPreservesLang() {
        val url = "https://twitter.com/user/status/123?lang=en&t=abc&s=def&ref_src=twsrc"
        val expected = "https://twitter.com/user/status/123?lang=en"
        assertEquals(expected, TwitterCleaner.clean(url))
    }
    
    @Test
    fun testYouTubeCleanerRemovesSiParameter() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&feature=share&si=abc123"
        val expected = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertEquals(expected, YouTubeCleaner.clean(url))
    }
    
    @Test
    fun testYouTubeCleanerRemovesIsParameter() {
        // YouTube renamed the "si" share identifier to "is" in early 2026
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&feature=share&is=abc123"
        val expected = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertEquals(expected, YouTubeCleaner.clean(url))
    }

    @Test
    fun testYouTubeCleanerPreservesTimestamp() {
        val url = "https://youtu.be/dQw4w9WgXcQ?t=42&si=xyz789"
        val expected = "https://youtu.be/dQw4w9WgXcQ?t=42"
        assertEquals(expected, YouTubeCleaner.clean(url))
    }

    @Test
    fun testYouTubeCleanerRemovesIsFromShortUrl() {
        val url = "https://youtu.be/dQw4w9WgXcQ?t=42&is=xyz789"
        val expected = "https://youtu.be/dQw4w9WgXcQ?t=42"
        assertEquals(expected, YouTubeCleaner.clean(url))
    }
    
    @Test
    fun testYouTubeCleanerPreservesPlaylist() {
        val url = "https://www.youtube.com/watch?v=ABC&list=PLxyz&index=3&pp=tracking"
        val expected = "https://www.youtube.com/watch?v=ABC&list=PLxyz&index=3"
        assertEquals(expected, YouTubeCleaner.clean(url))
    }
    
    @Test
    fun testAmazonCleanerExtractsProductId() {
        val url = "https://www.amazon.com/dp/B08N5WRWNW?tag=affiliate&linkCode=123"
        val expected = "https://www.amazon.com/dp/B08N5WRWNW"
        assertEquals(expected, AmazonCleaner.clean(url))
    }
    
    @Test
    fun testAmazonCleanerHandlesLongProductUrl() {
        val url = "https://www.amazon.com/Some-Product-Name/dp/B08N5WRWNW/ref=sr_1_1?keywords=test&qid=123"
        val expected = "https://www.amazon.com/dp/B08N5WRWNW"
        assertEquals(expected, AmazonCleaner.clean(url))
    }
    
    @Test
    fun testAmazonCleanerPreservesSearchParams() {
        val url = "https://www.amazon.com/s?k=laptop&ref=nb_sb_noss&tag=affiliate"
        val expected = "https://www.amazon.com/s?k=laptop"
        assertEquals(expected, AmazonCleaner.clean(url))
    }
    
    @Test
    fun testFacebookCleanerRemovesTracking() {
        val url = "https://www.facebook.com/photo.php?fbid=123&set=a.456&type=3&theater&__tn__=K-R"
        val expected = "https://www.facebook.com/photo.php?fbid=123&set=a.456&type=3&theater"
        assertEquals(expected, FacebookCleaner.clean(url))
    }
    
    @Test
    fun testFacebookCleanerHandlesStoryUrls() {
        val url = "https://www.facebook.com/story.php?story_fbid=789&id=012&__tn__=K-R&mibextid=abc"
        val expected = "https://www.facebook.com/story.php?story_fbid=789&id=012"
        assertEquals(expected, FacebookCleaner.clean(url))
    }
    
    @Test
    fun testRedditOutboundWrapperExtractsDestination() {
        // The Reddit app wraps external links in out.reddit.com; url= and token=
        // are functional, not tracking. Stripping them used to leave a redirect
        // without a destination → reddit.com/invalid_token in the browser.
        val url = "https://out.reddit.com/t3_abc123?url=https%3A%2F%2Fexample.com%2Farticle&token=AQAAtoken&app_name=android"
        val expected = "https://example.com/article"
        assertEquals(expected, RedditCleaner.clean(url))
    }
    
    @Test
    fun testRedditOutboundWrapperWithPlainDestination() {
        val url = "https://out.reddit.com/t3_abc123?url=https://example.com/article&token=AQAAtoken"
        val expected = "https://example.com/article"
        assertEquals(expected, RedditCleaner.clean(url))
    }
    
    @Test
    fun testRedditOutboundWrapperWithoutUrlParamKeptIntact() {
        // No extractable destination — the wrapper (incl. token) must survive so
        // the server-side redirect still works.
        val url = "https://out.reddit.com/t3_abc123?token=AQAAtoken"
        assertEquals(url, RedditCleaner.clean(url))
    }
    
    @Test
    fun testRedditPostUrlStillCleaned() {
        // Regular reddit.com URLs keep the aggressive param cleaning.
        val url = "https://www.reddit.com/r/androiddev/comments/abc123/title/?utm_source=share&ref=share"
        val expected = "https://www.reddit.com/r/androiddev/comments/abc123/title/"
        assertEquals(expected, RedditCleaner.clean(url))
    }
    
    @Test
    fun testDomainCleanersPreserveUnknownParams() {
        val instagramUrl = "https://www.instagram.com/explore/search/?unknown_param=123"
        assertEquals(instagramUrl, InstagramCleaner.clean(instagramUrl))
        
        val twitterUrl = "https://x.com/status/123?unknown_param=xyz"
        assertEquals("https://x.com/status/123?unknown_param=xyz", TwitterCleaner.clean(twitterUrl))
        
        val youtubeUrl = "https://www.youtube.com/watch?v=ABC&unknown_param=test"
        assertEquals("https://www.youtube.com/watch?v=ABC&unknown_param=test", YouTubeCleaner.clean(youtubeUrl))
    }

    @Test
    fun testAdoptedExistingCleanerKeysRemainHostScoped() {
        assertEquals(
            "https://www.facebook.com/page?keep=value",
            FacebookCleaner.clean("https://www.facebook.com/page?sfnsn=one&fb_source=two&keep=value")
        )
        assertEquals(
            "https://www.linkedin.com/feed?keep=value",
            LinkedInCleaner.clean("https://www.linkedin.com/feed?rcm=one&keep=value")
        )
        assertEquals(
            "https://www.amazon.com/s?k=laptop",
            AmazonCleaner.clean("https://www.amazon.com/s?k=laptop&ref=nav&ref_=legacy")
        )
    }
} 
