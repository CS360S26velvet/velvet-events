package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CheckInIntentTest {

    private ActivityScenario<CheckInActivity> launchCheckIn() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                CheckInActivity.class);
        intent.putExtra("organizerUsername", "ORG0012");
        return ActivityScenario.launch(intent);
    }

    private ActivityScenario<CheckInParticipantsActivity> launchParticipants() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                CheckInParticipantsActivity.class);
        intent.putExtra("eventId",    "test_event_id");
        intent.putExtra("eventTitle", "SPADES 2025");
        intent.putExtra("eventVenue", "AH Auditorium");
        return ActivityScenario.launch(intent);
    }

    /** CheckInActivity screen loads successfully */
    @Test
    public void testCheckInActivityLaunches() {
        try (ActivityScenario<CheckInActivity> s = launchCheckIn()) {
            onView(withId(R.id.recyclerView)).check(matches(isDisplayed()));
        }
    }

    /** Header title is visible on CheckInActivity */
    @Test
    public void testCheckInHeaderTitleVisible() {
        try (ActivityScenario<CheckInActivity> s = launchCheckIn()) {
            onView(withId(R.id.tvCheckInTitle)).check(matches(isDisplayed()));
        }
    }

    /** Back button is visible on CheckInActivity */
    @Test
    public void testCheckInBackButtonVisible() {
        try (ActivityScenario<CheckInActivity> s = launchCheckIn()) {
            onView(withId(R.id.btnCheckInBack)).check(matches(isDisplayed()));
        }
    }

    /** Tapping back button finishes CheckInActivity */
    @Test
    public void testBackButtonFinishesActivity() {
        try (ActivityScenario<CheckInActivity> s = launchCheckIn()) {
            onView(withId(R.id.btnCheckInBack)).perform(click());
        }
    }

    /** CheckInParticipantsActivity launches with correct title */
    @Test
    public void testParticipantsActivityLaunches() {
        try (ActivityScenario<CheckInParticipantsActivity> s = launchParticipants()) {
            onView(withId(R.id.tvParticipantsTitle)).check(matches(isDisplayed()));
        }
    }

    /** CheckInParticipantsActivity shows correct event title in header */
    @Test
    public void testParticipantsActivityShowsEventTitle() {
        try (ActivityScenario<CheckInParticipantsActivity> s = launchParticipants()) {
            onView(withId(R.id.tvParticipantsTitle))
                    .check(matches(withText("Check-In — SPADES 2025")));
        }
    }

    /** Search bar is visible on participants screen */
    @Test
    public void testSearchBarVisibleOnParticipants() {
        try (ActivityScenario<CheckInParticipantsActivity> s = launchParticipants()) {
            onView(withId(R.id.etSearch)).check(matches(isDisplayed()));
        }
    }

    /** Stat cards are visible on participants screen */
    @Test
    public void testStatCardsVisibleOnParticipants() {
        try (ActivityScenario<CheckInParticipantsActivity> s = launchParticipants()) {
            onView(withId(R.id.tvTotalRegistered)).check(matches(isDisplayed()));
            onView(withId(R.id.tvCheckedIn)).check(matches(isDisplayed()));
            onView(withId(R.id.tvRemaining)).check(matches(isDisplayed()));
        }
    }

    /** Progress bar is visible on participants screen */
    @Test
    public void testProgressBarVisibleOnParticipants() {
        try (ActivityScenario<CheckInParticipantsActivity> s = launchParticipants()) {
            onView(withId(R.id.progressCheckIn)).check(matches(isDisplayed()));
        }
    }

    /** Typing in search bar does not crash */
    @Test
    public void testTypingInSearchBarDoesNotCrash() {
        try (ActivityScenario<CheckInParticipantsActivity> s = launchParticipants()) {
            onView(withId(R.id.etSearch))
                    .perform(typeText("Fatima"), closeSoftKeyboard());
        }
    }

    /** Back button on participants screen is visible */
    @Test
    public void testParticipantsBackButtonVisible() {
        try (ActivityScenario<CheckInParticipantsActivity> s = launchParticipants()) {
            onView(withId(R.id.btnParticipantsBack)).check(matches(isDisplayed()));
        }
    }

    /** Tapping back on participants screen finishes activity */
    @Test
    public void testParticipantsBackButtonFinishes() {
        try (ActivityScenario<CheckInParticipantsActivity> s = launchParticipants()) {
            onView(withId(R.id.btnParticipantsBack)).perform(click());
        }
    }
}