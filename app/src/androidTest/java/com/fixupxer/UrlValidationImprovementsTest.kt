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
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import android.view.View

@RunWith(AndroidJUnit4::class)
class UrlValidationImprovementsTest {
    
    private fun waitFor(delay: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints() = isRoot()
            override fun getDescription() = "Wait for $delay milliseconds."
            override fun perform(uiController: UiController, view: View?) {
                uiController.loopMainThreadForAtLeast(delay)
            }
        }
    }
    
    private fun launchMainActivity() {
        ActivityScenario.launch(MainActivity::class.java)
    }
    
    @Test
    fun testFacebookStoryUrlNotRejected() {
        launchMainActivity()
        
        // This URL was incorrectly rejected before the fix
        val facebookStoryUrl = "https://m.facebook.com/story.php?story_fbid=123456789&id=987654321"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(facebookStoryUrl), closeSoftKeyboard())
        
        // Wait for validation
        onView(isRoot()).perform(waitFor(1500))
        
        // Verify URL is NOT cleared (it's valid)
        onView(withId(R.id.editTextUrl))
            .check(matches(withText(facebookStoryUrl)))
        
        // Process it to verify it works
        onView(withId(R.id.buttonProcess)).perform(click())
        onView(isRoot()).perform(waitFor(2000))
        
        // Verify processing occurred successfully
        onView(withId(R.id.textViewProcessedUrl))
            .check(matches(not(withText(""))))
        onView(withId(R.id.textViewProcessedUrl))
            .check(matches(not(withText(containsString("Multiple URLs detected")))))
    }
    
    @Test
    fun testUrlWithMultipleQueryParameters() {
        launchMainActivity()
        
        // URL with multiple query parameters should be valid
        val complexUrl = "https://www.example.com/page?param1=value1&param2=value2&param3=value3"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(complexUrl), closeSoftKeyboard())
        
        onView(isRoot()).perform(waitFor(1500))
        
        // Verify URL is not cleared
        onView(withId(R.id.editTextUrl))
            .check(matches(withText(complexUrl)))
    }
    
    @Test
    fun testUrlWithDotsInQueryParameters() {
        launchMainActivity()
        
        // URL with dots in query parameters should be valid
        val urlWithDots = "https://www.site.com/page?email=user.name@example.com&version=1.2.3"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(urlWithDots), closeSoftKeyboard())
        
        onView(isRoot()).perform(waitFor(1500))
        
        // Verify URL is not cleared
        onView(withId(R.id.editTextUrl))
            .check(matches(withText(urlWithDots)))
    }
    
    @Test
    fun testActualMultipleUrlsStillRejected() {
        launchMainActivity()
        
        // Actual multiple URLs should still be rejected
        val multipleUrls = "https://instagram.com/p/1 https://facebook.com/test"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(multipleUrls), closeSoftKeyboard())
        
        onView(isRoot()).perform(waitFor(1500))
        
        // Verify input is cleared
        onView(withId(R.id.editTextUrl))
            .check(matches(withText("")))
        
        // Verify error message
        onView(withId(R.id.textViewProcessedUrl))
            .check(matches(withText(containsString("Please paste one URL at a time"))))
    }
    
    @Test
    fun testGluedUrlsWithoutSpaceStillRejected() {
        launchMainActivity()
        
        // Glued URLs without space should still be rejected
        val gluedUrls = "https://instagram.comhttps://facebook.com"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(gluedUrls), closeSoftKeyboard())
        
        onView(isRoot()).perform(waitFor(1500))
        
        // Verify input is cleared
        onView(withId(R.id.editTextUrl))
            .check(matches(withText("")))
    }
    
    @Test
    fun testUrlWithFileExtensionInPath() {
        launchMainActivity()
        
        // URL with file extension containing dots should be valid
        val fileUrl = "https://example.com/download/file.v2.1.0.tar.gz"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(fileUrl), closeSoftKeyboard())
        
        onView(isRoot()).perform(waitFor(1500))
        
        // Verify URL is not cleared
        onView(withId(R.id.editTextUrl))
            .check(matches(withText(fileUrl)))
    }
    
    @Test
    fun testUrlWithPortNumber() {
        launchMainActivity()
        
        // URL with port number should be valid
        val portUrl = "https://example.com:8080/api/endpoint"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(portUrl), closeSoftKeyboard())
        
        onView(isRoot()).perform(waitFor(1500))
        
        // Verify URL is not cleared
        onView(withId(R.id.editTextUrl))
            .check(matches(withText(portUrl)))
    }
    
    @Test
    fun testFacebookezDomainRecognized() {
        launchMainActivity()
        
        // Facebookez.com URLs should be recognized as valid
        val facebookezUrl = "https://facebookez.com/zuck/posts/123456789"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(facebookezUrl), closeSoftKeyboard())
        
        onView(isRoot()).perform(waitFor(1500))
        
        // Verify URL is not cleared
        onView(withId(R.id.editTextUrl))
            .check(matches(withText(facebookezUrl)))
        
        // Process it
        onView(withId(R.id.buttonProcess)).perform(click())
        onView(isRoot()).perform(waitFor(2000))
        
        // Should show "Nothing to do" if toggles are in default state
        onView(withId(R.id.textViewProcessedUrl))
            .check(matches(not(withText(""))))
    }
    
    @Test
    fun testKkinstagramDomainRecognized() {
        launchMainActivity()
        
        // Kkinstagram.com URLs should be recognized as valid
        val kkinstagramUrl = "https://www.kkinstagram.com/p/ABC123/"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(kkinstagramUrl), closeSoftKeyboard())
        
        onView(isRoot()).perform(waitFor(1500))
        
        // Verify URL is not cleared
        onView(withId(R.id.editTextUrl))
            .check(matches(withText(kkinstagramUrl)))
    }
    
    @Test
    fun testFixupxDomainRecognized() {
        launchMainActivity()
        
        // Fixupx.com URLs should be recognized as valid
        val fixupxUrl = "https://fixupx.com/user/status/1234567890"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(fixupxUrl), closeSoftKeyboard())
        
        onView(isRoot()).perform(waitFor(1500))
        
        // Verify URL is not cleared
        onView(withId(R.id.editTextUrl))
            .check(matches(withText(fixupxUrl)))
    }
} 