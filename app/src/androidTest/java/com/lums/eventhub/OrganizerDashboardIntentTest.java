package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
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
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * Org US-01: Dashboard screen is displayed.
     */
    @Test
    public void testDashboardScreenIsDisplayed() {
        onView(withId(R.id.btnRegisterNewEvent)).check(matches(isDisplayed()));
    }

    /**
     * Org US-01: Register New Event button is visible.
     */
    @Test
    public void testRegisterNewEventButtonVisible() {
        onView(withId(R.id.btnRegisterNewEvent)).check(matches(isDisplayed()));
    }

    /**
     * Org US-01: Clicking Register New Event opens ProposalFormActivity.
     */
    @Test
    public void testRegisterNewEventOpensProposalForm() {
        onView(withId(R.id.btnRegisterNewEvent)).perform(click());
        intended(hasComponent(ProposalFormActivity.class.getName()));
    }

    /**
     * Org US-11: Attendee Registration button opens AttendeeRegistrationActivity.
     */
    @Test
    public void testAttendeeRegButtonOpensAttendeeRegistration() {
        onView(withId(R.id.btnNavAttendeeReg)).perform(click());
        intended(hasComponent(AttendeeRegistrationActivity.class.getName()));
    }

    /**
     * Org US-11: Registrants button opens RegistrantDashboardActivity.
     */
    @Test
    public void testRegistrantsButtonOpensRegistrantDashboard() {
        onView(withId(R.id.btnNavRegistrants)).perform(click());
        intended(hasComponent(RegistrantDashboardActivity.class.getName()));
    }

    /**
     * Org US-11: Check-In button opens CheckInActivity.
     */
    @Test
    public void testCheckInButtonOpensCheckInActivity() {
        onView(withId(R.id.btnNavCheckIn)).perform(click());
        intended(hasComponent(CheckInActivity.class.getName()));
    }

    /**
     * Org US-11: Form Settings button opens CapacitySettingActivity.
     */
    @Test
    public void testFormSettingsButtonOpensCapacitySetting() {
        onView(withId(R.id.btnNavFormSettings)).perform(click());
        intended(hasComponent(CapacitySettingActivity.class.getName()));
    }
}