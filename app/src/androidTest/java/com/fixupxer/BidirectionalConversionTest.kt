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

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.ui.ShareActivity
import org.hamcrest.CoreMatchers.containsString
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Before
import com.fixupxer.PreferencesManager

/**
 * Tests for bidirectional URL conversion scenarios that were missing
 */
@RunWith(AndroidJUnit4::class)
class BidirectionalConversionTest {
    
    private lateinit var preferencesManager: PreferencesManager
    
    @Before
    fun setup() {
        preferencesManager = PreferencesManager(InstrumentationRegistry.getInstrumentation().targetContext)
    }
    
    
    private fun launchShareActivityWithText(text: String) {
        val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, ShareActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        ActivityScenario.launch<ShareActivity>(intent)
    }
    
    // ============ INSTAGRAM BIDIRECTIONAL TESTS ============
    
    @Test
    fun testCleanInstagramToProxyConversionStripsWww() {
        runBlocking {
            // v1.4.8: Clean instagram.com → toinstagram.com (default proxy), www. prefix is stripped
            preferencesManager.setConvertInstagramEnabled(true)
            delay(100)

            launchShareActivityWithText("https://www.instagram.com/p/test123/")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://toinstagram.com/p/test123/")))
            }
        }
    }

    @Test
    fun testCleanProxyToInstagramConversion() {
        runBlocking {
            // Clean toinstagram.com → instagram.com (toggle OFF)
            preferencesManager.setConvertInstagramEnabled(false)
            delay(100)

            launchShareActivityWithText("https://toinstagram.com/p/test123/")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://instagram.com/p/test123/")))
            }
        }
    }

    @Test
    fun testRetiredKkinstagramStaysUnchangedWhenToggleOff() {
        runBlocking {
            preferencesManager.setConvertInstagramEnabled(false)
            delay(100)

            launchShareActivityWithText("https://www.kkinstagram.com/p/test123/?utm_source=app&igshid=abc")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://www.kkinstagram.com/p/test123/?igshid=abc")))
            }
        }
    }
    
    @Test
    fun testCleanInstagramNothingToDoWithToggleOff() {
        runBlocking {
            // Clean instagram.com with toggle OFF should show URL and "Already clean"
            preferencesManager.setConvertInstagramEnabled(false)
            delay(100)
            
            launchShareActivityWithText("https://www.instagram.com/p/test123/")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://www.instagram.com/p/test123/")))
                onView(withId(R.id.textViewResultStatus))
                    .check(matches(withText(containsString("Already clean"))))
            }
        }
    }
    
    // ============ TWITTER/X BIDIRECTIONAL TESTS ============
    
    @Test
    fun testCleanTwitterToFixupxConversion() {
        runBlocking {
            // Clean twitter.com → fixupx.com (toggle ON)
            preferencesManager.setConvertTwitterEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://twitter.com/user/status/123456789")
            awaitAssertion {
                // Should convert to fixupx with toggle ON
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://fixupx.com/user/status/123456789")))
            }
        }
    }
    
    @Test
    fun testCleanXToFixupxConversion() {
        runBlocking {
            // Clean x.com → fixupx.com (toggle ON)
            preferencesManager.setConvertTwitterEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://x.com/user/status/123456789")
            awaitAssertion {
                // Should convert to fixupx with toggle ON
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://fixupx.com/user/status/123456789")))
            }
        }
    }
    
    @Test
    fun testCleanFixupxToXConversion() {
        runBlocking {
            // Clean fixupx.com → x.com (toggle OFF)
            preferencesManager.setConvertTwitterEnabled(false)
            delay(100)
            
            launchShareActivityWithText("https://fixupx.com/user/status/123456789")
            awaitAssertion {
                // Should convert back to x.com
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://x.com/user/status/123456789")))
            }
        }
    }
    
    @Test
    fun testDirtyFixupxToCleanX() {
        runBlocking {
            // Dirty fixupx.com → Clean x.com (toggle OFF)
            preferencesManager.setConvertTwitterEnabled(false)
            delay(100)
            
            launchShareActivityWithText("https://fixupx.com/user/status/123456789?t=test&s=09")
            awaitAssertion {
                // Should clean and convert to x.com
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://x.com/user/status/123456789")))
            }
        }
    }
    
    @Test
    fun testDirtyFixupxToCleanFixupx() {
        runBlocking {
            // Dirty fixupx.com → Clean fixupx.com (toggle ON)
            preferencesManager.setConvertTwitterEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://fixupx.com/user/status/123456789?utm_source=share")
            awaitAssertion {
                // Should clean but stay fixupx.com
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://fixupx.com/user/status/123456789")))
            }
        }
    }
    
    @Test
    fun testCleanXNothingToDoWithToggleOff() {
        runBlocking {
            // Clean x.com with toggle OFF should show URL and "Already clean"
            preferencesManager.setConvertTwitterEnabled(false)
            delay(100)
            
            launchShareActivityWithText("https://x.com/user/status/123456789")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://x.com/user/status/123456789")))
                onView(withId(R.id.textViewResultStatus))
                    .check(matches(withText(containsString("Already clean"))))
            }
        }
    }
    
    // ============ FACEBOOK BIDIRECTIONAL TESTS ============
    
    @Test
    fun testCleanFacebookStaysUnchangedWithToggleOn() {
        runBlocking {
            preferencesManager.setConvertFacebookEnabled(true)
            delay(100)

            launchShareActivityWithText("https://www.facebook.com/zuck/posts/123456789")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://www.facebook.com/zuck/posts/123456789")))
            }
        }
    }

    @Test
    fun testRetiredFacebookezStaysUnchangedWithToggleOff() {
        runBlocking {
            preferencesManager.setConvertFacebookEnabled(false)
            delay(100)

            launchShareActivityWithText("https://facebookez.com/zuck/posts/123456789")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://facebookez.com/zuck/posts/123456789")))
            }
        }
    }

    @Test
    fun testDirtyRetiredFacebookezTrackingRemovedOnly() {
        runBlocking {
            preferencesManager.setConvertFacebookEnabled(false)
            delay(100)

            launchShareActivityWithText("https://facebookez.com/story.php?story_fbid=123&id=456&fbclid=abc")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://facebookez.com/story.php?story_fbid=123&id=456")))
            }
        }
    }

    @Test
    fun testWebFacebookStaysUnchangedWithToggleOn() {
        runBlocking {
            preferencesManager.setConvertFacebookEnabled(true)
            delay(100)

            launchShareActivityWithText("https://web.facebook.com/story.php?story_fbid=123&id=456")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://web.facebook.com/story.php?story_fbid=123&id=456")))
            }
        }
    }

    @Test
    fun testWwwFacebookStaysUnchangedWithToggleOn() {
        runBlocking {
            preferencesManager.setConvertFacebookEnabled(true)
            delay(100)

            launchShareActivityWithText("https://www.facebook.com/zuck/posts/123456789")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://www.facebook.com/zuck/posts/123456789")))
            }
        }
    }
    
    @Test
    fun testCleanFacebookNothingToDoWithToggleOff() {
        runBlocking {
            // Clean facebook.com with toggle OFF should show URL and "Already clean"
            preferencesManager.setConvertFacebookEnabled(false) // v2.4.0: dedicated Facebook toggle
            delay(100)
            
            launchShareActivityWithText("https://www.facebook.com/zuck/posts/123456789")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://www.facebook.com/zuck/posts/123456789")))
                onView(withId(R.id.textViewResultStatus))
                    .check(matches(withText(containsString("Already clean"))))
            }
        }
    }
    
    // ============ TIKTOK BIDIRECTIONAL TESTS (v1.7.0) ============
    
    @Test
    fun testCleanTikTokToProxyConversionKeepsWww() {
        runBlocking {
            // v1.7.0: Clean tiktok.com → tnktok.com (default proxy), host prefix is PRESERVED
            preferencesManager.setConvertTikTokEnabled(true)
            delay(100)

            launchShareActivityWithText("https://www.tiktok.com/@user/video/123456789")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://www.tnktok.com/@user/video/123456789")))
            }
        }
    }

    @Test
    fun testCleanProxyToTikTokConversion() {
        runBlocking {
            // Clean tnktok.com → tiktok.com (toggle OFF)
            preferencesManager.setConvertTikTokEnabled(false)
            delay(100)

            launchShareActivityWithText("https://tnktok.com/@user/video/123456789")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://tiktok.com/@user/video/123456789")))
            }
        }
    }

    @Test
    fun testVmTikTokShortLinkKeepsPrefix() {
        runBlocking {
            // vm. short links keep their subdomain on conversion
            preferencesManager.setConvertTikTokEnabled(true)
            delay(100)

            launchShareActivityWithText("https://vm.tiktok.com/ZMabcdef/")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://vm.tnktok.com/ZMabcdef/")))
            }
        }
    }

    @Test
    fun testLegacyVxtiktokMigratesToActiveProxy() {
        runBlocking {
            // Legacy vxtiktok.com (dead service) auto-migrates to the selected active proxy
            preferencesManager.setConvertTikTokEnabled(true)
            delay(100)

            launchShareActivityWithText("https://vxtiktok.com/@user/video/123456789")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://tnktok.com/@user/video/123456789")))
            }
        }
    }

    @Test
    fun testDirtyTikTokToCleanProxy() {
        runBlocking {
            // Dirty tiktok.com → Clean tnktok.com (toggle ON)
            preferencesManager.setConvertTikTokEnabled(true)
            delay(100)

            launchShareActivityWithText("https://www.tiktok.com/@user/video/123?is_from_webapp=1&_r=1&_t=abc")
            awaitAssertion {
                // is_from_webapp is an unknown/functional key and survives the
                // keep-unknown contract; _r and _t are known tracking keys.
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://www.tnktok.com/@user/video/123?is_from_webapp=1")))
            }
        }
    }

    @Test
    fun testCleanTikTokNothingToDoWithToggleOff() {
        runBlocking {
            // Clean tiktok.com with toggle OFF should show URL and "Already clean"
            preferencesManager.setConvertTikTokEnabled(false)
            delay(100)

            launchShareActivityWithText("https://www.tiktok.com/@user/video/123456789")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://www.tiktok.com/@user/video/123456789")))
                onView(withId(R.id.textViewResultStatus))
                    .check(matches(withText(containsString("Already clean"))))
            }
        }
    }

    // ============ EDGE CASES ============
    
    @Test
    fun testMixedCaseUrlHandling() {
        runBlocking {
            // INSTAGRAM.COM should still be processed correctly
            preferencesManager.setConvertInstagramEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://WWW.INSTAGRAM.COM/p/TEST123/")
            awaitAssertion {
                // v1.4.8: case-insensitive match + www. (any case) is stripped → default = toinstagram.com
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://toinstagram.com/p/TEST123/")))
            }
        }
    }
    
    @Test
    fun testUrlWithFragment() {
        runBlocking {
            // URL with fragment should preserve it
            preferencesManager.setConvertTwitterEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://x.com/user/status/123456789#reply")
            awaitAssertion {
                // Should convert and preserve fragment
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://fixupx.com/user/status/123456789#reply")))
            }
        }
    }
    
    @Test
    fun testFxTwitterToFixupxConversion() {
        runBlocking {
            // fxtwitter.com should convert to fixupx.com
            preferencesManager.setConvertTwitterEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://fxtwitter.com/user/status/123456789")
            awaitAssertion {
                // Should convert fxtwitter to fixupx
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://fixupx.com/user/status/123456789")))
            }
        }
    }
    
    @Test
    fun testDirtyToinstagramStaysOnToinstagramWhenAlreadyDefault() {
        runBlocking {
            // v1.4.8: Dirty toinstagram.com → Clean toinstagram.com (toggle ON, default proxy)
            preferencesManager.setConvertInstagramEnabled(true)
            delay(100)

            launchShareActivityWithText("https://toinstagram.com/p/test/?utm_source=ig_web&igshid=test")
            awaitAssertion {
                onView(withId(R.id.textViewProcessedUrl))
                    .check(matches(withText("https://toinstagram.com/p/test/")))
            }
        }
    }
} 