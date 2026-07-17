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

package com.fixupxer.cleaners

import com.fixupxer.cleaners.impl.GeneralTrackingCleaner
import org.junit.Assert.assertEquals
import org.junit.Test

class GeneralTrackingCleanerContractTest {

    private val cleaner = GeneralTrackingCleaner()

    @Test
    fun `removes universal trackers but keeps unknown functional parameters`() {
        assertEquals(
            "https://ex.com/?ref=one&source=two&si=three&foo=four",
            cleaner.clean("https://ex.com/?utm_source=a&ref=one&source=two&si=three&foo=four")
        )
    }

    @Test
    fun `keeps flag parameters and removes tracking prefixes only`() {
        assertEquals(
            "https://ex.com/?theater",
            cleaner.clean("https://ex.com/?utm_source=a&theater")
        )
        assertEquals("https://ex.com/?itok=abc", cleaner.clean("https://ex.com/?itok=abc"))
        assertEquals("https://ex.com/?foo=bar", cleaner.clean("https://ex.com/?at_medium=mail&foo=bar"))
    }

    @Test
    fun `preserves raw path fragment and parameter encoding`() {
        assertEquals(
            "https://ex.com//path",
            cleaner.clean("https://ex.com//path")
        )
        assertEquals(
            "https://ex.com/?foo=a%2Bb%26c#frag%2Fvalue",
            cleaner.clean("https://ex.com/?utm_source=a&foo=a%2Bb%26c#frag%2Fvalue")
        )
    }

    @Test
    fun `returns no-query URLs unchanged`() {
        val url = "https://ex.com//path#section?utm_source=a"

        assertEquals(url, cleaner.clean(url))
    }

    @Test
    fun `removes only confirmed Marketo Salesforce and Webtrends trackers`() {
        assertEquals(
            "https://ex.com/?keep=value",
            cleaner.clean(
                "https://ex.com/?mkt_tok=one&sfmc_activityid=two&wt_campaign=three&wt.foo=four&keep=value"
            )
        )
        assertEquals(
            "https://ex.com/?mkt_campaign=one&sfmc_campaign=two",
            cleaner.clean("https://ex.com/?mkt_campaign=one&sfmc_campaign=two")
        )
    }

    @Test
    fun `removes Yahoo Guce referrer keys case insensitively`() {
        assertEquals(
            "https://ex.com/?keep=value",
            cleaner.clean(
                "https://ex.com/?guccounter=1&Guce_Referrer=two&guce_referrer_sig=three&keep=value"
            )
        )
        assertEquals(
            "https://ex.com/?guce_referrer_extra=four&guce_sig=five",
            cleaner.clean(
                "https://ex.com/?guce_referrer_extra=four&guce_sig=five&guccounter=1"
            )
        )
    }

    @Test
    fun `removes only exact Echobox fragments`() {
        assertEquals(
            "https://ex.com/path",
            cleaner.clean("https://ex.com/path#Echobox=123")
        )
        assertEquals(
            "https://ex.com/path",
            cleaner.clean("https://ex.com/path?utm_source=mail#Echobox=123")
        )
        assertEquals(
            "https://ex.com/path#EchoboxFoo",
            cleaner.clean("https://ex.com/path#EchoboxFoo")
        )
        assertEquals(
            "https://ex.com/path#section",
            cleaner.clean("https://ex.com/path#section")
        )
    }
}
