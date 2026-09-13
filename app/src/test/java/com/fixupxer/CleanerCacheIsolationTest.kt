// SPDX-License-Identifier: GPL-3.0-or-later
/* FixupXer - URL Enhancer. Copyright (C) 2020-2026 NeatCode Labs.
 * Licensed under GPL version 3 or (at your option) any later version. */
package com.fixupxer

import com.fixupxer.cleaners.cache.CachedCleanResult
import com.fixupxer.cleaners.cache.CleanerCache
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CleanerCacheIsolationTest {
    @Test fun `cleaner completion status survives cache hits and distinguishes cycles from limits`() {
        fun service(transform: (String) -> String) = com.fixupxer.cleaners.CleanerService(
            com.fixupxer.cleaners.CleanerRegistry().apply {
                register(object : com.fixupxer.cleaners.UrlCleaner {
                    override val id = "bounded-fixture"
                    override fun matches(url: String) = true
                    override fun clean(url: String) = transform(url)
                })
            }, CleanerCache(),
        )
        val cyclic = service { if (it.endsWith("/a")) "https://example.org/b" else "https://example.org/a" }
        repeat(2) {
            assertEquals(com.fixupxer.processing.PipelineStatus.CYCLE,
                cyclic.deepCleanWithDetails("https://example.org/a").status)
        }
        val growing = service { "$it/a" }
        repeat(2) {
            val limited = growing.deepCleanWithDetails("https://example.org", maxPasses = 2)
            assertEquals(com.fixupxer.processing.PipelineStatus.HOP_LIMIT, limited.status)
            assertEquals(2, limited.totalPasses)
        }
        assertEquals(com.fixupxer.processing.PipelineStatus.COMPLETE,
            service { it }.deepCleanWithDetails("https://example.org/a").status)
    }
    private fun result(value: String) = CachedCleanResult(value, emptyList(), 1)

    @Test fun `different roster or max passes cannot reuse output`() {
        val cache = CleanerCache()
        assertEquals("one", cache.getOrCompute("url", "r1:p1") { result("one") }.cleanedUrl)
        assertEquals("two", cache.getOrCompute("url", "r1:p2") { result("two") }.cleanedUrl)
        assertEquals("three", cache.getOrCompute("url", "r2:p2") { result("three") }.cleanedUrl)
        cache.remove("url")
        assertEquals(0, cache.getStats().size)
    }

    @Test fun `clear and sensitive eviction prevent in flight resurrection`() {
        listOf(false, true).forEach { evict ->
            val cache = CleanerCache()
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val executor = Executors.newSingleThreadExecutor()
            try {
                val task = executor.submit<CachedCleanResult> {
                    cache.getOrCompute("url", "old") {
                        started.countDown()
                        check(release.await(5, TimeUnit.SECONDS))
                        result("sensitive")
                    }
                }
                assertTrue(started.await(5, TimeUnit.SECONDS))
                if (evict) cache.remove("url") else cache.clear()
                release.countDown()
                task.get(5, TimeUnit.SECONDS)
                assertEquals(0, cache.getStats().size)
                assertEquals("fresh", cache.getOrCompute("url", "old") { result("fresh") }.cleanedUrl)
            } finally {
                release.countDown()
                executor.shutdownNow()
            }
        }
    }
}
