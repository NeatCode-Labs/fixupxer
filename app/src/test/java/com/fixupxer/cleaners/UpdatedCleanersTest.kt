package com.fixupxer.cleaners

import com.fixupxer.cleaners.impl.*
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatedCleanersTest {
    
    @Test
    fun testInstagramCleanerRemovesIgshParameter() {
        val url = "https://www.instagram.com/p/DLRNJjEx45S/?igsh=cWdtYXd0NmE3YnI0"
        val expected = "https://www.instagram.com/p/DLRNJjEx45S/"
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
    fun testYouTubeCleanerPreservesTimestamp() {
        val url = "https://youtu.be/dQw4w9WgXcQ?t=42&si=xyz789"
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
    fun testAggressiveCleaningRemovesUnknownParams() {
        // Test that our cleaners remove unknown parameters (aggressive cleaning)
        val instagramUrl = "https://www.instagram.com/p/ABC/?unknown_param=123"
        assertEquals("https://www.instagram.com/p/ABC/", InstagramCleaner.clean(instagramUrl))
        
        val twitterUrl = "https://x.com/status/123?unknown_param=xyz"
        assertEquals("https://x.com/status/123", TwitterCleaner.clean(twitterUrl))
        
        val youtubeUrl = "https://www.youtube.com/watch?v=ABC&unknown_param=test"
        assertEquals("https://www.youtube.com/watch?v=ABC", YouTubeCleaner.clean(youtubeUrl))
    }
} 