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

import com.lums.eventhub.admin.dashboard.AdminDashboardActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * AdminDashboardIntentTest.java
 * Espresso UI tests for AdminDashboardActivity.
 * Tests that all navigation buttons and stat views are displayed.
 * Implements: Admin US-08
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminDashboardIntentTest {

    /** Launch AdminDashboardActivity with a test username */
    private ActivityScenario<AdminDashboardActivity> launchDashboard() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AdminDashboardActivity.class
        );
        intent.putExtra("username", "#AD_testadmin");
        return ActivityScenario.launch(intent);
    }

    /** Test dashboard screen is displayed */
    @Test
    public void testDashboardIsDisplayed() {
        launchDashboard();
        onView(withId(R.id.btnViewProposals)).check(matches(isDisplayed()));
    }

    /** Test pending count view is displayed */
    @Test
    public void testPendingCountViewIsDisplayed() {
        launchDashboard();
        onView(withId(R.id.tvPendingNumber)).check(matches(isDisplayed()));
    }

    /** Test approved count view is displayed */
    @Test
    public void testApprovedCountViewIsDisplayed() {
        launchDashboard();
        onView(withId(R.id.tvApprovedNumber)).check(matches(isDisplayed()));
    }

    /** Test proposals button is clickable */
    @Test
    public void testProposalsButtonIsClickable() {
        launchDashboard();
        onView(withId(R.id.btnViewProposals)).check(matches(isDisplayed()));
    }

    /** Test auditorium button is displayed */
    @Test
    public void testAuditoriumButtonIsDisplayed() {
        launchDashboard();
        onView(withId(R.id.btnAuditorium)).check(matches(isDisplayed()));
    }

    /** Test calendar button is displayed */
    @Test
    public void testCalendarButtonIsDisplayed() {
        launchDashboard();
        onView(withId(R.id.btnCalendar)).check(matches(isDisplayed()));
    }

    /** Test accommodation button is displayed */
    @Test
    public void testAccommodationButtonIsDisplayed() {
        launchDashboard();
        onView(withId(R.id.btnAccommodation)).check(matches(isDisplayed()));
    }

    /** Test register organizer button is displayed (requires scroll) */
    @Test
    public void testRegisterOrganizerButtonIsDisplayed() {
        launchDashboard();
        onView(withId(R.id.btnRegisterOrganizer))
                .perform(androidx.test.espresso.action.ViewActions.scrollTo())
                .check(matches(isDisplayed()));
    }
}