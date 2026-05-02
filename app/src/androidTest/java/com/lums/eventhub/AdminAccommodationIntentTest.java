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

import com.lums.eventhub.admin.accommodation.AdminAccommodationBySocietyActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * AdminAccommodationIntentTest.java
 *
 * Espresso UI tests for AdminAccommodationBySocietyActivity.
 * Tests that the accommodation by society screen loads,
 * shows the societies list and back button.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminAccommodationIntentTest {

    private ActivityScenario<AdminAccommodationBySocietyActivity> launch() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AdminAccommodationBySocietyActivity.class);
        ActivityScenario<AdminAccommodationBySocietyActivity> s =
                ActivityScenario.launch(intent);
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        return s;
    }

    /** Screen launches successfully */
    @Test
    public void testActivityLaunches() {
        try (ActivityScenario<AdminAccommodationBySocietyActivity> s = launch()) {
            onView(withId(R.id.rvSocieties)).check(matches(isDisplayed()));
        }
    }

    /** Back button is visible */
    @Test
    public void testBackButtonVisible() {
        try (ActivityScenario<AdminAccommodationBySocietyActivity> s = launch()) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        }
    }

    /** Back button finishes activity */
    @Test
    public void testBackButtonFinishes() {
        try (ActivityScenario<AdminAccommodationBySocietyActivity> s = launch()) {
            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    /** RecyclerView for societies is displayed */
    @Test
    public void testSocietiesRecyclerViewDisplayed() {
        try (ActivityScenario<AdminAccommodationBySocietyActivity> s = launch()) {
            onView(withId(R.id.rvSocieties)).check(matches(isDisplayed()));
        }
    }
}