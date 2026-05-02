package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * FeedbackIntentTest.java
 *
 * Espresso intent tests for FeedbackActivity.
 * AT US-19 — Star rating + feedback text submission.
 * AT US-20 — Anonymous toggle.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class FeedbackIntentTest {

    private ActivityScenario<FeedbackActivity> launch() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                FeedbackActivity.class);
        intent.putExtra("userId",         "testUser001");
        intent.putExtra("eventId",        "evt_test_001");
        intent.putExtra("eventTitle",     "Tech Summit 2025");
        intent.putExtra("eventOrganizer", "SPADES Society");
        intent.putExtra("eventDate",      "Jan 10, 2025");
        ActivityScenario<FeedbackActivity> s = ActivityScenario.launch(intent);
        try { Thread.sleep(600); } catch (InterruptedException ignored) {}
        return s;
    }

    /** AT US-19: Event title is shown on the screen. */
    @Test
    public void testEventTitleDisplayed() {
        try (ActivityScenario<FeedbackActivity> s = launch()) {
            onView(withId(R.id.tvFeedbackEventTitle)).check(matches(isDisplayed()));
        }
    }

    /** AT US-19: Star row is visible. */
    @Test
    public void testStarRowVisible() {
        try (ActivityScenario<FeedbackActivity> s = launch()) {
            onView(withId(R.id.starRow)).check(matches(isDisplayed()));
        }
    }

    /** AT US-19: Feedback text input is visible. */
    @Test
    public void testFeedbackTextInputVisible() {
        try (ActivityScenario<FeedbackActivity> s = launch()) {
            onView(withId(R.id.etFeedbackText)).check(matches(isDisplayed()));
        }
    }

    /** AT US-19: Typing in the feedback box does not crash. */
    @Test
    public void testTypingFeedbackDoesNotCrash() {
        try (ActivityScenario<FeedbackActivity> s = launch()) {
            onView(withId(R.id.etFeedbackText))
                    .perform(typeText("Really enjoyed it!"), closeSoftKeyboard());
        }
    }

    /** AT US-20: Anonymous checkbox is present and unchecked by default. */
    @Test
    public void testAnonymousCheckboxUncheckedByDefault() {
        try (ActivityScenario<FeedbackActivity> s = launch()) {
            onView(withId(R.id.cbAnonymous))
                    .check(matches(isDisplayed()))
                    .check(matches(isNotChecked()));
        }
    }



    /** AT US-19: Back button is visible. */
    @Test
    public void testBackButtonVisible() {
        try (ActivityScenario<FeedbackActivity> s = launch()) {
            onView(withId(R.id.btnFeedbackBack)).check(matches(isDisplayed()));
        }
    }

    /** AT US-19: Tapping back button without selecting a rating does not crash. */
    @Test
    public void testBackButtonDoesNotCrash() {
        try (ActivityScenario<FeedbackActivity> s = launch()) {
            onView(withId(R.id.btnFeedbackBack)).perform(click());
        }
    }
}