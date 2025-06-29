package com.fixupxer.utils

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import timber.log.Timber

object InputValidator {
    
    private const val MAX_INPUT_LENGTH = 2048
    
    /**
     * Comprehensive input validation and sanitization
     * @param input Raw input string
     * @return Sanitized and validated input, or null if invalid
     */
    suspend fun validateAndSanitizeInput(input: String): String? {
        return try {
            withTimeout(100) { // 100ms timeout to prevent DoS
                // Check length first
                if (!validateInputLength(input)) {
                    Timber.w("Input too long: ${input.length} characters")
                    return@withTimeout null
                }
                
                // Sanitize and decode
                val sanitized = sanitizeInput(input)
                val decoded = decodeUrlSafely(sanitized)
                
                // Check for multiple URLs
                if (hasMultipleUrls(decoded)) {
                    Timber.w("Multiple URLs detected in input")
                    return@withTimeout null
                }
                
                // Additional safety checks
                if (decoded.contains(Regex("[\\u0000-\\u001F]"))) { // Control characters
                    Timber.w("Control characters detected in input")
                    return@withTimeout null
                }
                
                decoded
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w("Input validation timed out")
            null
        } catch (e: Exception) {
            Timber.e(e, "Error during input validation")
            null
        }
    }
    
    /**
     * Sanitize input by removing problematic characters and normalizing
     */
    private fun sanitizeInput(input: String): String {
        return input
            .trim() // Remove leading/trailing whitespace
            .replace(Regex("\\s+"), " ") // Normalize multiple spaces to single
            .replace(Regex("[\\u200B\\uFEFF\\u2060\\u200C\\u200D]"), "") // Remove zero-width characters
            .replace(Regex("[\\u0000-\\u001F\\u007F-\\u009F]"), "") // Remove control characters
            .lowercase() // Normalize case
    }
    
    /**
     * Safely decode URL-encoded strings
     */
    private fun decodeUrlSafely(input: String): String {
        return try {
            URLDecoder.decode(input, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            Timber.w("URL decoding failed, using original input")
            input // Return original if decoding fails
        }
    }
    
    /**
     * Check if input length is within acceptable limits
     */
    private fun validateInputLength(input: String): Boolean {
        return input.length <= MAX_INPUT_LENGTH
    }
    
    /**
     * Enhanced multiple URL detection with timeout protection
     */
    private suspend fun hasMultipleUrls(input: String): Boolean {
        return try {
            withTimeout(50) { // 50ms timeout for URL detection
                val protocolCount = Regex("https?://|ftp://|file://|mailto:", RegexOption.IGNORE_CASE)
                    .findAll(input).count()
                val wwwCount = Regex("www\\.", RegexOption.IGNORE_CASE)
                    .findAll(input).count()
                val domainCount = Regex("\\.[a-z]{2,}(?=\\.[a-z]{2,}|[^a-z]|$)", RegexOption.IGNORE_CASE)
                    .findAll(input).count()
                val dotCount = input.count { it == '.' }
                
                // Multiple indicators suggest multiple URLs
                protocolCount + wwwCount > 1 || domainCount > 1 || dotCount > 3
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w("URL detection timed out, assuming multiple URLs")
            true // Assume multiple URLs if timeout occurs
        } catch (e: Exception) {
            Timber.w("URL detection failed, assuming safe")
            false // Safe fallback
        }
    }
} 