package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.lums.eventhub.organizer.reports.EventReportsActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventReportsIntentTest {

    private ActivityScenario<EventReportsActivity> launch() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EventReportsActivity.class);
        intent.putExtra("organizerUsername", "ORG0012");
        intent.putExtra("societyName",       "SPADES Society");
        ActivityScenario<EventReportsActivity> s = ActivityScenario.launch(intent);
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        return s;
    }

    /** Event reports screen loads */
    @Test
    public void testEventReportsActivityLaunches() {
        try (ActivityScenario<EventReportsActivity> s = launch()) {
            onView(withId(R.id.recyclerViewReports)).check(matches(isDisplayed()));
        }
    }

    /** Back button is visible */
    @Test
    public void testBackButtonVisible() {
        try (ActivityScenario<EventReportsActivity> s = launch()) {
            onView(withId(R.id.btnReportsBack)).check(matches(isDisplayed()));
        }
    }

    /** Back button finishes activity */
    @Test
    public void testBackButtonFinishes() {
        try (ActivityScenario<EventReportsActivity> s = launch()) {
            onView(withId(R.id.btnReportsBack)).perform(click());
        }
    }

    /** Total events stat card is visible */
    @Test
    public void testTotalEventsStatVisible() {
        try (ActivityScenario<EventReportsActivity> s = launch()) {
            onView(withId(R.id.tvReportTotalEvents)).check(matches(isDisplayed()));
        }
    }

    /** Submitted stat card is visible */
    @Test
    public void testSubmittedStatVisible() {
        try (ActivityScenario<EventReportsActivity> s = launch()) {
            onView(withId(R.id.tvReportSubmitted)).check(matches(isDisplayed()));
        }
    }

    /** Pending stat card is visible */
    @Test
    public void testPendingStatVisible() {
        try (ActivityScenario<EventReportsActivity> s = launch()) {
            onView(withId(R.id.tvReportPending)).check(matches(isDisplayed()));
        }
    }
}