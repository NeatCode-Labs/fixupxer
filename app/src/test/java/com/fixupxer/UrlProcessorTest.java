package com.fixupxer;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

/**
 * Unit tests for the URL processor
 */
public class UrlProcessorTest {
    private UrlProcessor processor;
    
    @Before
    public void setUp() {
        processor = new UrlProcessor();
    }
    
    @Test
    public void testCleanTrackingParameters() {
        String testUrl = "https://www.instagram.com/reel/DKKAq4zuObs/?igsh=MTVzOTcwdGV5bWNoOQ==";
        String cleaned = processor.processUrl(testUrl, true, false, false);
        
        // Should remove tracking parameters
        assertFalse(cleaned.contains("igsh="));
        assertTrue(cleaned.contains("instagram.com"));
    }
    
    @Test
    public void testInstagramConversion() {
        String testUrl = "https://www.instagram.com/reel/DKKAq4zuObs/";
        String converted = processor.processUrl(testUrl, true, false, true);
        
        // Should convert to kkinstagram.com
        assertTrue(converted.contains("kkinstagram.com"));
        assertFalse(converted.contains("instagram.com"));
    }
    
    @Test
    public void testTwitterConversion() {
        String testUrl = "https://twitter.com/username/status/1234567890";
        String converted = processor.processUrl(testUrl, true, true, false);
        
        // Should convert to fixupx.com
        assertTrue(converted.contains("fixupx.com"));
        assertFalse(converted.contains("twitter.com"));
    }
    
    @Test
    public void testExtractUrls() {
        String text = "Check out https://example.com and also www.test.org";
        List<String> urls = UrlProcessor.extractUrls(text);
        
        // Should extract both URLs
        assertEquals(2, urls.size());
        assertTrue(urls.get(0).contains("example.com"));
        assertTrue(urls.get(1).contains("test.org"));
    }
    
    @Test
    public void testHandleInvalidUrl() {
        String invalidUrl = "not a url";
        String result = processor.processUrl(invalidUrl, true, true, true);
        
        // Should return the original string if it's not a valid URL
        assertEquals(invalidUrl, result);
    }
} 