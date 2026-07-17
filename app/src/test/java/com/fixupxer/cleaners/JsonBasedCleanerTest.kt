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
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.InputStreamReader

/**
 * Data classes for JSON test cases
 */
data class TestCaseFile(
    @SerializedName("testCases") val testCases: List<TestCategory>
)

data class TestCategory(
    @SerializedName("category") val category: String,
    @SerializedName("cases") val cases: List<TestCase>
)

data class TestCase(
    @SerializedName("input") val input: String,
    @SerializedName("expected") val expected: String,
    @SerializedName("description") val description: String
)

/**
 * Parameterized test that runs test cases loaded from JSON
 */
@RunWith(Parameterized::class)
class JsonBasedCleanerTest(
    private val testCase: TestCase,
    private val category: String
) {
    
    private lateinit var cleanerService: CleanerService
    
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1}: {0}")
        fun loadTestCases(): Collection<Array<Any>> {
            val gson = Gson()
            val inputStream = JsonBasedCleanerTest::class.java.getResourceAsStream("/test-cases.json")
                ?: throw IllegalStateException("test-cases.json not found")
            
            val testFile = InputStreamReader(inputStream).use { reader ->
                gson.fromJson(reader, TestCaseFile::class.java)
            }
            
            return testFile.testCases.flatMap { category ->
                category.cases.map { testCase ->
                    arrayOf(testCase, category.category)
                }
            }
        }
    }
    
    @Before
    fun setup() {
        val registry = CleanerRegistry()
        val cache = CleanerCache()
        
        registry.registerAll(CleanerCatalog.createBuiltInCleaners())
        
        cleanerService = CleanerService(registry, cache)
    }
    
    @Test
    fun `test cleaner with JSON case`() {
        val actual = cleanerService.deepClean(testCase.input)
        assertEquals(
            "Failed: ${testCase.description}\nCategory: $category",
            testCase.expected,
            actual
        )
    }
}

/**
 * Additional test class for non-parameterized JSON tests
 */
class JsonTestLoader {
    
    private lateinit var cleanerService: CleanerService
    
    @Before
    fun setup() {
        val registry = CleanerRegistry()
        val cache = CleanerCache()
        
        registry.registerAll(CleanerCatalog.createBuiltInCleaners())
        
        cleanerService = CleanerService(registry, cache)
    }
    
    @Test
    fun `validate all JSON test cases`() {
        val gson = Gson()
        val inputStream = javaClass.getResourceAsStream("/test-cases.json")
            ?: throw IllegalStateException("test-cases.json not found")
        
        val testFile = InputStreamReader(inputStream).use { reader ->
            gson.fromJson(reader, TestCaseFile::class.java)
        }
        
        var totalTests = 0
        var passedTests = 0
        val failures = mutableListOf<String>()
        
        testFile.testCases.forEach { category ->
            println("\n=== ${category.category} ===")
            category.cases.forEach { testCase ->
                totalTests++
                try {
                    val actual = cleanerService.deepClean(testCase.input)
                    if (actual == testCase.expected) {
                        passedTests++
                        println("✓ ${testCase.description}")
                    } else {
                        failures.add("${category.category}: ${testCase.description}\n  Input: ${testCase.input}\n  Expected: ${testCase.expected}\n  Actual: $actual")
                        println("✗ ${testCase.description}")
                    }
                } catch (e: Exception) {
                    failures.add("${category.category}: ${testCase.description}\n  Error: ${e.message}")
                    println("✗ ${testCase.description} - ERROR: ${e.message}")
                }
            }
        }
        
        println("\n=== Summary ===")
        println("Total tests: $totalTests")
        println("Passed: $passedTests")
        println("Failed: ${totalTests - passedTests}")
        
        if (failures.isNotEmpty()) {
            println("\n=== Failures ===")
            failures.forEach { println(it) }
            throw AssertionError("${failures.size} test(s) failed")
        }
    }
} 