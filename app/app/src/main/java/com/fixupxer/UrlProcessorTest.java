package com.fixupxer;

import android.util.Log;

/**
 * Simple test class for the URL processor
 */
public class UrlProcessorTest {
    public static void main(String[] args) {
        UrlProcessor processor = new UrlProcessor();
        
        // Test URL from the user
        String testUrl = "@https://www.instagram.com/reel/DKKAq4zuObs/?igsh=MTVzOTcwdGV5bWNoOQ==";
        
        System.out.println("Original URL: " + testUrl);
        
        // Test cleaning tracking parameters
        String cleaned = processor.processUrl(testUrl, true, false);
        System.out.println("Cleaned URL (no conversion): " + cleaned);
        
        // Test with conversion
        String cleanedAndConverted = processor.processUrl(testUrl, true, true);
        System.out.println("Cleaned and converted URL: " + cleanedAndConverted);
        
        // Test with processUrlForSharing
        String forSharing = processor.processUrlForSharing(testUrl);
        System.out.println("URL processed for sharing: " + forSharing);
    }
} 