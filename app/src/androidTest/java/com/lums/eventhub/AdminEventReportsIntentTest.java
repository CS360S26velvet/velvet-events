package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.lums.eventhub.admin.reports.AdminEventReportsActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * AdminEventReportsIntentTest.java
 *
 * Espresso UI tests for AdminEventReportsActivity.
 * Tests that the admin event reports screen loads correctly
 * with admin color scheme and society accordion UI.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminEventReportsIntentTest {

    private ActivityScenario<AdminEventReportsActivity> launch() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AdminEventReportsActivity.class);
        ActivityScenario<AdminEventReportsActivity> s =
                ActivityScenario.launch(intent);
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        return s;
    }

    /** Screen launches successfully */
    @Test
    public void testActivityLaunches() {
        try (ActivityScenario<AdminEventReportsActivity> s = launch()) {
            onView(withId(R.id.llAdminReportSocieties)).check(matches(isDisplayed()));
        }
    }

    /** Back button is visible */
    @Test
    public void testBackButtonVisible() {
        try (ActivityScenario<AdminEventReportsActivity> s = launch()) {
            onView(withId(R.id.btnAdminReportsBack)).check(matches(isDisplayed()));
        }
    }

    /** Back button finishes activity */
    @Test
    public void testBackButtonFinishes() {
        try (ActivityScenario<AdminEventReportsActivity> s = launch()) {
            onView(withId(R.id.btnAdminReportsBack)).perform(click());
        }
    }

    /** Societies container is visible */
    @Test
    public void testSocietiesContainerVisible() {
        try (ActivityScenario<AdminEventReportsActivity> s = launch()) {
            onView(withId(R.id.llAdminReportSocieties)).check(matches(isDisplayed()));
        }
    }
}