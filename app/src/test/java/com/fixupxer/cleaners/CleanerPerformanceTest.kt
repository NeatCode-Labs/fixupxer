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

import com.fixupxer.cleaners.cache.CleanerCache
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Performance benchmarks for URL cleaners
 */
class CleanerPerformanceTest {
    
    private lateinit var cleanerService: CleanerService
    private lateinit var registry: CleanerRegistry
    
    @Before
    fun setup() {
        registry = CleanerRegistry()
        val cache = CleanerCache()
        
        registry.registerAll(CleanerCatalog.createBuiltInCleaners())
        
        cleanerService = CleanerService(registry, cache)
    }
    
    @Test
    fun `benchmark single URL cleaning`() {
        val testUrls = listOf(
            "https://www.amazon.com/dp/B08N5WRWNW/ref=cm_sw_r_cp_api_glt_fabc_ABCD?utm_source=twitter",
            "https://youtube.com/watch?v=dQw4w9WgXcQ&t=30s&utm_source=share",
            "https://twitter.com/user/status/123456?utm_source=share&s=20",
            "https://www.google.com/url?q=https%3A%2F%2Fexample.com&sa=D",
            "https://facebook.com/photo.php?fbid=123456&set=a.789&utm_campaign=test"
        )
        
        println("\n=== Single URL Cleaning Performance ===")
        testUrls.forEach { url ->
            val time = measureTimeMillis {
                cleanerService.deepClean(url)
            }
            println("URL: ${url.take(50)}... -> ${time}ms")
        }
    }
    
    @Test
    fun `benchmark batch URL cleaning`() {
        val batchSizes = listOf(10, 100, 1000)
        val sampleUrl = "https://www.amazon.com/dp/B08N5WRWNW?utm_source=twitter&ref=share"
        
        println("\n=== Batch URL Cleaning Performance ===")
        batchSizes.forEach { size ->
            val urls = List(size) { "$sampleUrl&id=$it" }
            
            val time = measureTimeMillis {
                urls.forEach { cleanerService.deepClean(it) }
            }
            
            val avgTime = time.toDouble() / size
            println("Batch size: $size -> Total: ${time}ms, Avg: ${String.format("%.2f", avgTime)}ms")
        }
    }
    
    @Test
    fun `benchmark cache effectiveness`() {
        val url = "https://youtube.com/watch?v=test&utm_source=share&feature=share"
        
        println("\n=== Cache Performance ===")
        
        // First call (no cache)
        val coldTime = measureTimeMillis {
            cleanerService.deepClean(url)
        }
        println("Cold cache: ${coldTime}ms")
        
        // Second call (with cache)
        val warmTime = measureTimeMillis {
            cleanerService.deepClean(url)
        }
        println("Warm cache: ${warmTime}ms")
        println("Speed improvement: ${String.format("%.1f", coldTime.toDouble() / warmTime)}x")
    }
    
    @Test
    fun `benchmark iterative deep cleaning`() {
        // URLs that require multiple passes
        val complexUrls = listOf(
            // Google redirect to Twitter URL with tracking
            "https://www.google.com/url?q=https%3A%2F%2Ftwitter.com%2Fuser%2Fstatus%2F123%3Futm_source%3Dgoogle",
            // Google redirect to Amazon with tracking
            "https://www.google.com/url?q=https%3A%2F%2Fwww.amazon.com%2Fdp%2FB08N5WRWNW%2Fref%3Dcm_sw%3Futm_source%3Dgoogle"
        )
        
        println("\n=== Iterative Deep Cleaning Performance ===")
        complexUrls.forEach { url ->
            val result = cleanerService.deepCleanWithDetails(url)
            val time = measureTimeMillis {
                cleanerService.deepClean(url)
            }
            println("URL: ${url.take(50)}...")
            println("  Passes: ${result.totalPasses}, Time: ${time}ms")
            println("  Operations: ${result.operations.map { it.source }.joinToString()}")
        }
    }
    
    @Test
    fun `benchmark domain dispatch efficiency`() {
        val domainsToTest = mapOf(
            "amazon.com" to "https://www.amazon.com/dp/test",
            "youtube.com" to "https://youtube.com/watch?v=test",
            "twitter.com" to "https://twitter.com/status/test",
            "unknown.com" to "https://unknown.com/page?utm_source=test"
        )
        
        println("\n=== Domain Dispatch Performance ===")
        domainsToTest.forEach { (domain, url) ->
            val time = measureTimeMillis {
                repeat(1000) {
                    registry.getCleanersFor(url)
                }
            }
            println("$domain dispatch (1000 calls): ${time}ms")
        }
    }
    
    /*
    @Test
    fun `benchmark parameter matching`() {
        // Commented out - DefaultParameterMatcher is no longer used
        // Each cleaner now has its own self-contained parameter matching
    }
    */
    
    @Test
    fun `stress test with concurrent cleaning`() {
        val url = "https://www.amazon.com/dp/B08N5WRWNW?utm_source=twitter"
        val threads = 10
        val callsPerThread = 100
        
        println("\n=== Concurrent Cleaning Stress Test ===")
        val time = measureTimeMillis {
            val threadList = mutableListOf<Thread>()
            
            repeat(threads) {
                val thread = Thread {
                    repeat(callsPerThread) {
                        cleanerService.deepClean(url)
                    }
                }
                threadList.add(thread)
                thread.start()
            }
            
            threadList.forEach { it.join() }
        }
        
        val totalCalls = threads * callsPerThread
        println("$totalCalls concurrent calls: ${time}ms")
        println("Throughput: ${String.format("%.0f", totalCalls * 1000.0 / time)} calls/sec")
    }
} 