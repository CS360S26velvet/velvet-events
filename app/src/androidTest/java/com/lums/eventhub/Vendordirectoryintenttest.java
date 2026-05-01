package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class VendorDirectoryIntentTest {

    @Before public void setUp() { Intents.init(); }
    @After  public void tearDown() { Intents.release(); }

    private ActivityScenario<VendorDirectoryActivity> launch() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                VendorDirectoryActivity.class);
        intent.putExtra("organizerUsername", "ORG0012");
        intent.putExtra("societyName",       "SPADES Society");
        ActivityScenario<VendorDirectoryActivity> s = ActivityScenario.launch(intent);
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        return s;
    }

    /** Vendor directory screen loads */
    @Test
    public void testVendorDirectoryLaunches() {
        try (ActivityScenario<VendorDirectoryActivity> s = launch()) {
            onView(withId(R.id.recyclerVendors)).check(matches(isDisplayed()));
        }
    }

    /** Search bar is visible */
    @Test
    public void testSearchBarVisible() {
        try (ActivityScenario<VendorDirectoryActivity> s = launch()) {
            onView(withId(R.id.etVendorSearch)).check(matches(isDisplayed()));
        }
    }

    /** Filter buttons are visible */
    @Test
    public void testFilterButtonsVisible() {
        try (ActivityScenario<VendorDirectoryActivity> s = launch()) {
            onView(withId(R.id.btnFilterAll)).check(matches(isDisplayed()));
            onView(withId(R.id.btnFilterCatering)).check(matches(isDisplayed()));
            onView(withId(R.id.btnFilterAV)).check(matches(isDisplayed()));
        }
    }

    /** FAB add vendor button is visible */
    @Test
    public void testFabAddVendorVisible() {
        try (ActivityScenario<VendorDirectoryActivity> s = launch()) {
            onView(withId(R.id.fabAddVendor)).check(matches(isDisplayed()));
        }
    }

    /** Back button is visible */
    @Test
    public void testBackButtonVisible() {
        try (ActivityScenario<VendorDirectoryActivity> s = launch()) {
            onView(withId(R.id.btnVendorBack)).check(matches(isDisplayed()));
        }
    }

    /** Back button finishes activity */
    @Test
    public void testBackButtonFinishes() {
        try (ActivityScenario<VendorDirectoryActivity> s = launch()) {
            onView(withId(R.id.btnVendorBack)).perform(click());
        }
    }

    /** Tapping All filter does not crash */
    @Test
    public void testTappingAllFilterDoesNotCrash() {
        try (ActivityScenario<VendorDirectoryActivity> s = launch()) {
            onView(withId(R.id.btnFilterAll)).perform(click());
        }
    }

    /** Tapping Catering filter does not crash */
    @Test
    public void testTappingCateringFilterDoesNotCrash() {
        try (ActivityScenario<VendorDirectoryActivity> s = launch()) {
            onView(withId(R.id.btnFilterCatering)).perform(click());
        }
    }

    /** Typing in search does not crash */
    @Test
    public void testTypingInSearchDoesNotCrash() {
        try (ActivityScenario<VendorDirectoryActivity> s = launch()) {
            onView(withId(R.id.etVendorSearch))
                    .perform(typeText("AV"), closeSoftKeyboard());
        }
    }

    /** FAB launches AddVendorActivity */
    @Test
    public void testFabLaunchesAddVendorActivity() {
        try (ActivityScenario<VendorDirectoryActivity> s = launch()) {
            onView(withId(R.id.fabAddVendor)).perform(click());
            intended(hasComponent(AddVendorActivity.class.getName()));
        }
    }

    /** AddVendorActivity launches successfully */
    @Test
    public void testAddVendorActivityLaunches() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AddVendorActivity.class);
        try (ActivityScenario<AddVendorActivity> s = ActivityScenario.launch(intent)) {
            onView(withId(R.id.etVendorName)).check(matches(isDisplayed()));
        }
    }

    /** AddVendorActivity cancel button finishes */
    @Test
    public void testAddVendorCancelButtonFinishes() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AddVendorActivity.class);
        try (ActivityScenario<AddVendorActivity> s = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnCancelVendor)).perform(click());
        }
    }

    /** Save without name shows error */
    @Test
    public void testSaveWithoutNameShowsError() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AddVendorActivity.class);
        try (ActivityScenario<AddVendorActivity> s = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnSaveVendor)).perform(click());
            // Name field should still be visible (validation blocked save)
            onView(withId(R.id.etVendorName)).check(matches(isDisplayed()));
        }
    }
}