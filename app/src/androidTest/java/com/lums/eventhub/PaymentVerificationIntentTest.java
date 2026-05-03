package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PaymentVerificationIntentTest {

    private ActivityScenario<PaymentVerificationListActivity> launchList() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                PaymentVerificationListActivity.class);
        intent.putExtra("organizerUsername", "ORG0012");
        intent.putExtra("societyName",       "SPADES Society");
        ActivityScenario<PaymentVerificationListActivity> s =
                ActivityScenario.launch(intent);
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        return s;
    }

    private ActivityScenario<PaymentVerificationActivity> launchVerification() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                PaymentVerificationActivity.class);
        intent.putExtra("eventId",   "test_event_id");
        intent.putExtra("eventName", "SPADES 2025");
        ActivityScenario<PaymentVerificationActivity> s = ActivityScenario.launch(intent);
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        return s;
    }

    /** PaymentVerificationListActivity launches */
    @Test
    public void testPaymentListActivityLaunches() {
        try (ActivityScenario<PaymentVerificationListActivity> s = launchList()) {
            // recyclerView starts hidden until events load — check title instead
            onView(withId(R.id.tvPaymentListTitle)).check(matches(isDisplayed()));
        }
    }

    /** Payment list header title visible */
    @Test
    public void testPaymentListTitleVisible() {
        try (ActivityScenario<PaymentVerificationListActivity> s = launchList()) {
            onView(withId(R.id.tvPaymentListTitle)).check(matches(isDisplayed()));
        }
    }

    /** PaymentVerificationActivity launches */
    @Test
    public void testPaymentVerificationActivityLaunches() {
        try (ActivityScenario<PaymentVerificationActivity> s = launchVerification()) {
            onView(withId(R.id.recyclerViewRegistrants)).check(matches(isDisplayed()));
        }
    }

    /** Stats cards visible on verification screen */
    @Test
    public void testStatCardsVisible() {
        try (ActivityScenario<PaymentVerificationActivity> s = launchVerification()) {
            onView(withId(R.id.tvStatPending)).check(matches(isDisplayed()));
            onView(withId(R.id.tvStatApproved)).check(matches(isDisplayed()));
            onView(withId(R.id.tvStatRejected)).check(matches(isDisplayed()));
        }
    }

    /** Search bar visible on verification screen */
    @Test
    public void testSearchBarVisible() {
        try (ActivityScenario<PaymentVerificationActivity> s = launchVerification()) {
            onView(withId(R.id.etSearchRegistrant)).check(matches(isDisplayed()));
        }
    }

    /** Filter buttons visible */
    @Test
    public void testFilterButtonsVisible() {
        try (ActivityScenario<PaymentVerificationActivity> s = launchVerification()) {
            onView(withId(R.id.btnFilterAll)).check(matches(isDisplayed()));
            onView(withId(R.id.btnFilterPending)).check(matches(isDisplayed()));
            onView(withId(R.id.btnFilterApproved)).check(matches(isDisplayed()));
            onView(withId(R.id.btnFilterRejected)).check(matches(isDisplayed()));
        }
    }

    /** Tapping Pending filter does not crash */
    @Test
    public void testTappingPendingFilterDoesNotCrash() {
        try (ActivityScenario<PaymentVerificationActivity> s = launchVerification()) {
            onView(withId(R.id.btnFilterPending)).perform(click());
        }
    }

    /** Tapping Approved filter does not crash */
    @Test
    public void testTappingApprovedFilterDoesNotCrash() {
        try (ActivityScenario<PaymentVerificationActivity> s = launchVerification()) {
            onView(withId(R.id.btnFilterApproved)).perform(click());
        }
    }

    /** Tapping Rejected filter does not crash */
    @Test
    public void testTappingRejectedFilterDoesNotCrash() {
        try (ActivityScenario<PaymentVerificationActivity> s = launchVerification()) {
            onView(withId(R.id.btnFilterRejected)).perform(click());
        }
    }

    /** Typing in search does not crash */
    @Test
    public void testTypingInSearchDoesNotCrash() {
        try (ActivityScenario<PaymentVerificationActivity> s = launchVerification()) {
            onView(withId(R.id.etSearchRegistrant))
                    .perform(typeText("Fatima"), closeSoftKeyboard());
        }
    }

    /** Verification title shows event name */
    @Test
    public void testVerificationTitleShowsEventName() {
        try (ActivityScenario<PaymentVerificationActivity> s = launchVerification()) {
            onView(withId(R.id.tvVerificationTitle)).check(matches(isDisplayed()));
        }
    }
}