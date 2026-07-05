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


package com.fixupxer

import com.fixupxer.utils.InputValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [InputValidator] — the security gate in front of every user
 * input path (Main text watcher, paste, Share intent, browser VIEW intent).
 *
 * The redirect-wrapper cases guard the root-cause fix: any app that wraps an
 * outbound link in a redirect (Gmail's google.com/url?q=, Reddit's
 * out.reddit.com/…?url=, Facebook's l.facebook.com/l.php?u=, …) carries a nested
 * "https://" in the query. That used to trip the multiple-URL rejection and
 * break the link in browser/share mode ("Error processing URL" before
 * UrlProcessor ever ran). A single URL with a nested target is now accepted on
 * ANY host; genuine multi-URL pastes and glued host names stay rejected.
 */
class InputValidatorTest {

    private fun validate(input: String): String? = runBlocking {
        InputValidator.validateAndSanitizeInput(input)
    }

    // --- Google redirect wrapper (Gmail / Google Search) — regression fix ---

    @Test
    fun `gmail redirect with plain nested url is accepted`() {
        val gmail = "https://www.google.com/url?q=https://gls-group.com/HR/hr/pracenje-posiljke?match%3D48610661969&source=gmail&ust=1751234567890000&usg=AOvVaw0GkR3sXyZ"
        assertNotNull(validate(gmail))
    }

    @Test
    fun `gmail redirect with percent-encoded nested url is accepted`() {
        val gmail = "https://www.google.com/url?q=https%3A%2F%2Fexample.com%2Fpath%3Fid%3D42&source=gmail&usg=AOvVaw0"
        assertNotNull(validate(gmail))
    }

    @Test
    fun `google redirect without www is accepted`() {
        val redirect = "https://google.com/url?q=https://example.com/page&sa=D"
        assertNotNull(validate(redirect))
    }

    @Test
    fun `google redirect on regional domain is accepted`() {
        val redirect = "https://www.google.co.uk/url?q=https://example.com/page&sa=D"
        assertNotNull(validate(redirect))
    }

    @Test
    fun `google redirect with nested www url is accepted`() {
        // Nested destination carries its own www. — wwwCount would be 2
        val gmail = "https://www.google.com/url?q=https://www.example.com/article&source=gmail"
        assertNotNull(validate(gmail))
    }

    // --- The exemption must not weaken multi-URL rejection ---

    @Test
    fun `two urls separated by whitespace are still rejected`() {
        assertNull(validate("https://example.com/a https://other.com/b"))
    }

    @Test
    fun `google redirect followed by second url is still rejected`() {
        // Whitespace breaks the full-string wrapper match → normal rejection applies
        val input = "https://www.google.com/url?q=https://example.com/a https://attacker.com/b"
        assertNull(validate(input))
    }

    @Test
    fun `google url wrapper with encoded-space destination is accepted`() {
        // %20 may legitimately appear in a Gmail destination (file names with
        // spaces). The wrapper exemption is decided on the raw form, so this
        // passes the validator; smuggled extra URLs are neutralized downstream
        // (GoogleSearchCleaner extracts a single url=/q= URL — covered by
        // UrlProcessorTest `google url wrapper cannot smuggle extra urls`).
        val input = "https://www.google.com/url?q=https://example.com/my%20file.pdf&source=gmail"
        assertNotNull(validate(input))
    }

    @Test
    fun `glued urls are still rejected`() {
        assertNull(validate("https://www.instagram.comwww.x.com"))
    }

    // --- Root-cause generalization: nested redirect target on ANY host ---
    // A single navigable URL that merely carries another URL in its query is not
    // a multi-URL paste, whatever the host. Maintaining a per-service allow-list
    // (google.*/url? only) broke every other wrapper: Reddit's out.reddit.com,
    // Facebook's l.facebook.com, LinkedIn/YouTube redirects, newsletters, …

    @Test
    fun `reddit outbound redirect with nested url is accepted`() {
        val reddit = "https://out.reddit.com/t3_abc123?url=https%3A%2F%2Fexample.com%2Farticle&token=xyz"
        assertNotNull(validate(reddit))
    }

    @Test
    fun `facebook outbound redirect with nested url is accepted`() {
        val fb = "https://l.facebook.com/l.php?u=https%3A%2F%2Fexample.com%2Fpost&h=AT1"
        assertNotNull(validate(fb))
    }

    @Test
    fun `generic single url with nested query url is accepted regardless of host`() {
        assertNotNull(validate("https://example.org/redir?to=https://target.com/x"))
    }

    @Test
    fun `plain nested query url on any host is accepted`() {
        // Same shape as the old google.*/url? case, now host-agnostic.
        assertNotNull(validate("https://any.host/url?q=https://example.com/a"))
    }

    @Test
    fun `second protocol glued into the path is still rejected`() {
        // The nested URL must live in the query/fragment; a second protocol in
        // the PATH is treated as suspicious and falls through to rejection.
        assertNull(validate("https://example.com/redir/https://attacker.com/x"))
    }

    // --- Baseline behaviour unchanged ---

    @Test
    fun `plain single url is accepted`() {
        assertEquals("https://example.com/page", validate("https://example.com/page"))
    }

    @Test
    fun `single url with tracking params is accepted`() {
        assertNotNull(validate("https://example.com/page?utm_source=x&utm_medium=y"))
    }

    @Test
    fun `google search url is accepted`() {
        assertNotNull(validate("https://www.google.com/search?q=kotlin+coroutines"))
    }

    @Test
    fun `encoded control characters are rejected`() {
        // Raw control chars are STRIPPED by sanitizeInput; the rejection check
        // targets ones that only appear after URL-decoding (e.g. %01)
        assertNull(validate("https://example.com/%01page"))
    }

    @Test
    fun `raw control characters are stripped from accepted url`() {
        assertEquals("https://example.com/page", validate("https://example.com/\u0001page"))
    }

    @Test
    fun `encoded dot attack is rejected`() {
        assertNull(validate("https://example%2Ecom/page"))
    }

    @Test
    fun `overlong input is rejected`() {
        val long = "https://example.com/" + "a".repeat(3000)
        assertNull(validate(long))
    }

    @Test
    fun `zero width characters are stripped from accepted url`() {
        val zw = "https://exam\u200Bple.com/page"
        assertEquals("https://example.com/page", validate(zw))
    }
}
