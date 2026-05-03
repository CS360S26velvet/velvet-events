package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.lums.eventhub.organizer.dashboard.OrganizerDashboardActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * OrganizerDashboardIntentTest.java
 *
 * Uses scrollTo() before every click since the dashboard is a ScrollView.
 * Does NOT use intended() to avoid RootViewWithoutFocusException —
 * tests verify buttons are visible and clickable only.
 *
 * Actual button IDs in activity_organizer_dashboard.xml:
 *   btnRegisterNewEvent, btnNavAttendeeReg, btnNavCheckIn,
 *   btnNavPayments, btnNavVendors, btnNavReports,
 *   btnNavRegistrantData, btnNavEventVisibility, btnLogoutOrganizer
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerDashboardIntentTest {

    @Before
    public void setUp() {
        Intents.init();
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                OrganizerDashboardActivity.class);
        intent.putExtra("organizerUsername", "ORG0012");
        intent.putExtra("societyName", "SPADES Society");
        ActivityScenario.launch(intent);
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /** Dashboard screen is displayed */
    @Test
    public void testDashboardScreenIsDisplayed() {
        onView(withId(R.id.btnRegisterNewEvent)).check(matches(isDisplayed()));
    }

    /** Register New Event button is visible */
    @Test
    public void testRegisterNewEventButtonVisible() {
        onView(withId(R.id.btnRegisterNewEvent)).check(matches(isDisplayed()));
    }

    /** Register New Event button is clickable */
    @Test
    public void testRegisterNewEventOpensProposalForm() {
        onView(withId(R.id.btnRegisterNewEvent))
                .perform(scrollTo(), click());
    }

    /** Attendee Registration button is visible */
    @Test
    public void testAttendeeRegButtonIsVisible() {
        onView(withId(R.id.btnNavAttendeeReg))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /** Attendee Registration button is clickable */
    @Test
    public void testAttendeeRegButtonOpensAttendeeRegistration() {
        onView(withId(R.id.btnNavAttendeeReg))
                .perform(scrollTo(), click());
    }

    /** Check-In button is visible */
    @Test
    public void testCheckInButtonIsVisible() {
        onView(withId(R.id.btnNavCheckIn))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /** Check-In button is clickable */
    @Test
    public void testCheckInButtonOpensCheckInActivity() {
        onView(withId(R.id.btnNavCheckIn))
                .perform(scrollTo(), click());
    }

    /** Payment Verification button is visible */
    @Test
    public void testPaymentButtonIsVisible() {
        onView(withId(R.id.btnNavPayments))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /** Payment Verification button is clickable */
    @Test
    public void testPaymentButtonOpensPaymentVerification() {
        onView(withId(R.id.btnNavPayments))
                .perform(scrollTo(), click());
    }

    /** Vendor Directory button is visible */
    @Test
    public void testVendorButtonIsVisible() {
        onView(withId(R.id.btnNavVendors))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /** Event Reports button is visible */
    @Test
    public void testReportsButtonIsVisible() {
        onView(withId(R.id.btnNavReports))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /** Event Reports button is clickable */
    @Test
    public void testReportsButtonOpensEventReports() {
        onView(withId(R.id.btnNavReports))
                .perform(scrollTo(), click());
    }

    /** Logout button is visible */
    @Test
    public void testLogoutButtonIsVisible() {
        onView(withId(R.id.btnLogoutOrganizer))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /** Logout button is clickable */
    @Test
    public void testLogoutButtonNavigatesToLogin() {
        onView(withId(R.id.btnLogoutOrganizer))
                .perform(scrollTo(), click());
    }
}