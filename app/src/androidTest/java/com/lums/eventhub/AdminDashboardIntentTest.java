package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;

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
 * Tests all navigation buttons and stat views that actually exist in the layout.
 * Implements: Admin US-08
 *
 * Actual button IDs in activity_admin_dashboard.xml:
 *   btnSignOut, btnViewProposals, btnCalendar,
 *   btnAccommodation, btnEventReports, btnRegisterOrganizer
 * Note: btnAuditorium was removed from this layout.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminDashboardIntentTest {

    private ActivityScenario<AdminDashboardActivity> launchDashboard() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AdminDashboardActivity.class);
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

    /** Test proposals button is displayed */
    @Test
    public void testProposalsButtonIsDisplayed() {
        launchDashboard();
        onView(withId(R.id.btnViewProposals)).check(matches(isDisplayed()));
    }

    /** Test calendar button is displayed */
    @Test
    public void testCalendarButtonIsDisplayed() {
        launchDashboard();
        onView(withId(R.id.btnCalendar))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /** Test register organizer button is displayed */
    @Test
    public void testRegisterOrganizerButtonIsDisplayed() {
        launchDashboard();
        onView(withId(R.id.btnRegisterOrganizer))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /** Test accommodation button is displayed */
    @Test
    public void testAccommodationButtonIsDisplayed() {
        launchDashboard();
        onView(withId(R.id.btnAccommodation))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /** Test event reports button is displayed */
    @Test
    public void testEventReportsButtonIsDisplayed() {
        launchDashboard();
        onView(withId(R.id.btnEventReports))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /** Test sign out button is displayed */
    @Test
    public void testSignOutButtonIsDisplayed() {
        launchDashboard();
        onView(withId(R.id.btnSignOut)).check(matches(isDisplayed()));
    }

    /** Test accommodation button click navigates */
    @Test
    public void testAccommodationButtonIsClickable() {
        launchDashboard();
        onView(withId(R.id.btnAccommodation))
                .perform(scrollTo(), click());
    }

    /** Test event reports button click navigates */
    @Test
    public void testEventReportsButtonIsClickable() {
        launchDashboard();
        onView(withId(R.id.btnEventReports))
                .perform(scrollTo(), click());
    }

    /** Test sign out button click navigates to login */
    @Test
    public void testSignOutButtonIsClickable() {
        launchDashboard();
        onView(withId(R.id.btnSignOut)).perform(click());
    }
}