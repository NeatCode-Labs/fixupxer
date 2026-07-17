// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
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

package com.fixupxer.processing

import com.fixupxer.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkLeakAnalyzerTest {

    @Test
    fun `finds credentials emails JWTs sensitive parameters and precise coordinates`() {
        assertContains(
            "https://user:pass@example.com/",
            LeakCategory.CREDENTIALS,
            LeakComponent.USERINFO
        )
        assertContains(
            "https://example.com/?contact=person@example.com",
            LeakCategory.EMAIL,
            LeakComponent.QUERY,
            "contact"
        )
        assertContains(
            "https://example.com/person%40example.com",
            LeakCategory.EMAIL,
            LeakComponent.PATH
        )
        assertContains(
            "https://example.com/?value=$JWT",
            LeakCategory.JWT,
            LeakComponent.QUERY,
            "value"
        )
        assertContains(
            "https://example.com/#jwt=$JWT",
            LeakCategory.JWT,
            LeakComponent.FRAGMENT,
            "jwt"
        )
        assertContains(
            "https://example.com/?access_token=abcdef123456",
            LeakCategory.TOKEN_PARAM,
            LeakComponent.QUERY,
            "access_token"
        )
        assertContains(
            "https://example.com/?reset_token=abcdefgh",
            LeakCategory.TOKEN_PARAM,
            LeakComponent.QUERY,
            "reset_token"
        )
        assertContains(
            "https://example.com/?invite=abcdefgh",
            LeakCategory.TOKEN_PARAM,
            LeakComponent.QUERY,
            "invite"
        )
        assertContains(
            "https://example.com/?sig=abcdef123456",
            LeakCategory.TOKEN_PARAM,
            LeakComponent.QUERY,
            "sig"
        )

        val coordinates = LinkLeakAnalyzer.analyze(
            "https://example.com/?lat=45.8151&lon=15.9819"
        )
        assertContains(coordinates, LeakCategory.COORDINATES, LeakComponent.QUERY, "lat")
        assertContains(coordinates, LeakCategory.COORDINATES, LeakComponent.QUERY, "lon")
    }

    @Test
    fun `avoids ambiguous and low confidence indicators`() {
        val urls = listOf(
            "https://user@example.com/",
            "https://example.com/?auth=1",
            "https://example.com/?code=abcdef123456",
            "https://example.com/?key=abcdef123456",
            "https://example.com/?sid=abcdef123456",
            "https://example.com/?id=123456789",
            "https://google.com/maps/@45.8150,15.9819,15z",
            "https://example.com/?lat=45.8&lon=15.9",
            "https://example.com/AN1krLciyv07WZqXBp6d3M4t5R8s0V2u9Yz",
            "https://open.spotify.com/track/123?si=AbCdEf123456"
        )

        urls.forEach { url ->
            assertTrue("Expected no finding for $url", LinkLeakAnalyzer.analyze(url).isEmpty())
        }
    }

    @Test
    fun `reports a repeated email only once`() {
        val findings = LinkLeakAnalyzer.analyze(
            "https://example.com/?first=person@example.com#second=person@example.com"
        )

        assertEquals(1, findings.count { it.category == LeakCategory.EMAIL })
    }

    @Test
    fun `malformed urls produce no findings`() {
        assertTrue(LinkLeakAnalyzer.analyze("not a URL").isEmpty())
        assertTrue(LinkLeakAnalyzer.analyze("https://").isEmpty())
        assertTrue(LinkLeakAnalyzer.analyze("ftp://user:pass@example.com").isEmpty())
    }

    @Test
    fun `findings never retain sensitive values`() {
        val email = "person@example.com"
        val token = "SECRET123456"
        val findings = LinkLeakAnalyzer.analyze(
            "https://user:pass@example.com/$email?access_token=$token&jwt=$JWT"
        )
        val serialized = findings.toString()

        assertFalse(serialized.contains("pass"))
        assertFalse(serialized.contains(email))
        assertFalse(serialized.contains(token))
        assertFalse(serialized.contains(JWT))
    }

    @Test
    fun `caps findings`() {
        val url = buildString {
            append("https://example.com/?")
            repeat(Constants.MAX_LEAK_FINDINGS + 4) { index ->
                if (index > 0) append('&')
                append("token=abcdefgh")
            }
        }

        assertEquals(Constants.MAX_LEAK_FINDINGS, LinkLeakAnalyzer.analyze(url).size)
    }

    private fun assertContains(
        url: String,
        category: LeakCategory,
        component: LeakComponent,
        parameterName: String? = null
    ) {
        assertContains(LinkLeakAnalyzer.analyze(url), category, component, parameterName)
    }

    private fun assertContains(
        findings: List<LeakFinding>,
        category: LeakCategory,
        component: LeakComponent,
        parameterName: String? = null
    ) {
        assertTrue(
            "Expected $category/$component/$parameterName in $findings",
            findings.any {
                it.category == category &&
                    it.component == component &&
                    it.parameterName == parameterName
            }
        )
    }

    private companion object {
        const val JWT = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature"
    }
}
