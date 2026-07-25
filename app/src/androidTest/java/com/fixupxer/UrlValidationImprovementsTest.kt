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

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fixupxer.MainActivity
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UrlValidationImprovementsTest {
    
    private fun launchMainActivity() {
        ActivityScenario.launch(MainActivity::class.java)
    }

    /**
     * The text watcher validates asynchronously and clears the field when it rejects the input
     * (`MainActivity.afterTextChanged`). Polling cannot prove the text *stays* put, so wait out
     * that window — bounded by the validator's own 200 ms timeout — before asserting it survived.
     */
    private fun assertInputSurvivesValidation(url: String) {
        onView(isRoot()).perform(waitFor(400))
        onView(withId(R.id.editTextUrl))
            .check(matches(withText(url)))
    }
    
    @Test
    fun testFacebookStoryUrlNotRejected() {
        launchMainActivity()
        
        // This URL was incorrectly rejected before the fix
        val facebookStoryUrl = "https://m.facebook.com/story.php?story_fbid=123456789&id=987654321"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(facebookStoryUrl), closeSoftKeyboard())
        
        assertInputSurvivesValidation(facebookStoryUrl)
        
        // Process it to verify it works
        onView(withId(R.id.buttonProcess)).perform(click())
        awaitAssertion {
            // Verify processing occurred successfully
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(not(withText(""))))
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(not(withText(containsString("Multiple URLs detected")))))
        }
    }
    
    @Test
    fun testUrlWithMultipleQueryParameters() {
        launchMainActivity()
        
        // URL with multiple query parameters should be valid
        val complexUrl = "https://www.example.com/page?param1=value1&param2=value2&param3=value3"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(complexUrl), closeSoftKeyboard())
        
        assertInputSurvivesValidation(complexUrl)
    }
    
    @Test
    fun testUrlWithDotsInQueryParameters() {
        launchMainActivity()
        
        // URL with dots in query parameters should be valid
        val urlWithDots = "https://www.site.com/page?email=user.name@example.com&version=1.2.3"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(urlWithDots), closeSoftKeyboard())
        
        assertInputSurvivesValidation(urlWithDots)
    }
    
    @Test
    fun testActualMultipleUrlsStillRejected() {
        launchMainActivity()
        
        // Actual multiple URLs should still be rejected
        val multipleUrls = "https://instagram.com/p/1 https://facebook.com/test"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(multipleUrls), closeSoftKeyboard())
        
        awaitAssertion {
            // Verify input is cleared
            onView(withId(R.id.editTextUrl))
                .check(matches(withText("")))
        }
        
        // Verify error message (shown in the TextInputLayout error slot)
        onView(withText(containsString("Please paste one URL at a time")))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testGluedUrlsWithoutSpaceStillRejected() {
        launchMainActivity()
        
        // Glued URLs without space should still be rejected
        val gluedUrls = "https://instagram.comhttps://facebook.com"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(gluedUrls), closeSoftKeyboard())
        
        awaitAssertion {
            // Verify input is cleared
            onView(withId(R.id.editTextUrl))
                .check(matches(withText("")))
        }
    }
    
    @Test
    fun testUrlWithFileExtensionInPath() {
        launchMainActivity()
        
        // URL with file extension containing dots should be valid
        val fileUrl = "https://example.com/download/file.v2.1.0.tar.gz"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(fileUrl), closeSoftKeyboard())
        
        assertInputSurvivesValidation(fileUrl)
    }
    
    @Test
    fun testUrlWithPortNumber() {
        launchMainActivity()
        
        // URL with port number should be valid
        val portUrl = "https://example.com:8080/api/endpoint"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(portUrl), closeSoftKeyboard())
        
        assertInputSurvivesValidation(portUrl)
    }
    
    @Test
    fun testFacebookezDomainAcceptedAsPlainUrl() {
        launchMainActivity()

        val facebookezUrl = "https://facebookez.com/zuck/posts/123456789"

        onView(withId(R.id.editTextUrl))
            .perform(replaceText(facebookezUrl), closeSoftKeyboard())

        assertInputSurvivesValidation(facebookezUrl)

        onView(withId(R.id.buttonProcess)).perform(click())

        awaitAssertion {
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText(facebookezUrl)))
        }
    }
    
    @Test
    fun testAdamlikesDomainRecognized() {
        launchMainActivity()

        // adamlikes.men URLs (primary v1.4.8 proxy) should be recognized as valid
        val adamlikesUrl = "https://adamlikes.men/p/ABC123/"

        onView(withId(R.id.editTextUrl))
            .perform(replaceText(adamlikesUrl), closeSoftKeyboard())

        assertInputSurvivesValidation(adamlikesUrl)
    }

    @Test
    fun testRetiredKkinstagramUrlAcceptedButNotConverted() {
        launchMainActivity()

        val kkinstagramUrl = "https://www.kkinstagram.com/p/ABC123/"

        onView(withId(R.id.editTextUrl))
            .perform(replaceText(kkinstagramUrl), closeSoftKeyboard())

        assertInputSurvivesValidation(kkinstagramUrl)
    }
    
    @Test
    fun testFixupxDomainRecognized() {
        launchMainActivity()
        
        // Fixupx.com URLs should be recognized as valid
        val fixupxUrl = "https://fixupx.com/user/status/1234567890"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(fixupxUrl), closeSoftKeyboard())
        
        assertInputSurvivesValidation(fixupxUrl)
    }

    @Test
    fun testToinstagramDomainRecognized() {
        launchMainActivity()

        // toinstagram.com URLs (primary v1.4.8 proxy) should be recognized as valid
        val toinstagramUrl = "https://toinstagram.com/p/ABC123/"

        onView(withId(R.id.editTextUrl))
            .perform(replaceText(toinstagramUrl), closeSoftKeyboard())

        assertInputSurvivesValidation(toinstagramUrl)
    }

    @Test
    fun testInstagram7DomainRecognized() {
        launchMainActivity()

        // instagram7.com URLs should be recognized as valid (new Instagram proxy)
        val instagram7Url = "https://instagram7.com/p/XYZ789/"

        onView(withId(R.id.editTextUrl))
            .perform(replaceText(instagram7Url), closeSoftKeyboard())

        assertInputSurvivesValidation(instagram7Url)
    }
} 