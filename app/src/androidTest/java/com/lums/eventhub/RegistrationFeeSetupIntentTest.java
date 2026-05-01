package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static org.hamcrest.Matchers.not;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RegistrationFeeSetupIntentTest {

    private ActivityScenario<RegistrationFeeSetupActivity> launch() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                RegistrationFeeSetupActivity.class);
        intent.putExtra("eventId",   "test_event_id");
        intent.putExtra("eventName", "SPADES 2025");
        return ActivityScenario.launch(intent);
    }

    /** Screen loads successfully */
    @Test
    public void testScreenLoads() {
        try (ActivityScenario<RegistrationFeeSetupActivity> s = launch()) {
            onView(withId(R.id.rgRegFee)).check(matches(isDisplayed()));
        }
    }

    /** Registration fee radio group is visible */
    @Test
    public void testRegFeeRadioGroupVisible() {
        try (ActivityScenario<RegistrationFeeSetupActivity> s = launch()) {
            onView(withId(R.id.rbRegFeeYes)).check(matches(isDisplayed()));
            onView(withId(R.id.rbRegFeeNo)).check(matches(isDisplayed()));
        }
    }

    /** Accommodation radio group is visible */
    @Test
    public void testAccomRadioGroupVisible() {
        try (ActivityScenario<RegistrationFeeSetupActivity> s = launch()) {
            onView(withId(R.id.rbAccomYes)).check(matches(isDisplayed()));
            onView(withId(R.id.rbAccomNo)).check(matches(isDisplayed()));
        }
    }

    /** No is checked by default for reg fee */
    @Test
    public void testRegFeeNoCheckedByDefault() {
        try (ActivityScenario<RegistrationFeeSetupActivity> s = launch()) {
            onView(withId(R.id.rbRegFeeNo)).check(matches(isChecked()));
        }
    }

    /** No is checked by default for accommodation */
    @Test
    public void testAccomNoCheckedByDefault() {
        try (ActivityScenario<RegistrationFeeSetupActivity> s = launch()) {
            onView(withId(R.id.rbAccomNo)).check(matches(isChecked()));
        }
    }

    /** Reg fee fields hidden initially */
    @Test
    public void testRegFeeFieldsHiddenInitially() {
        try (ActivityScenario<RegistrationFeeSetupActivity> s = launch()) {
            onView(withId(R.id.layoutRegFeeFields))
                    .check(matches(not(isDisplayed())));
        }
    }

    /** Accom fields hidden initially */
    @Test
    public void testAccomFieldsHiddenInitially() {
        try (ActivityScenario<RegistrationFeeSetupActivity> s = launch()) {
            onView(withId(R.id.layoutAccommodationFields))
                    .check(matches(not(isDisplayed())));
        }
    }

    /** Selecting Yes for reg fee shows fee fields */
    @Test
    public void testSelectingRegFeeYesShowsFields() {
        try (ActivityScenario<RegistrationFeeSetupActivity> s = launch()) {
            onView(withId(R.id.rbRegFeeYes)).perform(click());
            onView(withId(R.id.layoutRegFeeFields)).check(matches(isDisplayed()));
        }
    }

    /** Selecting Yes for accommodation shows accom fields */
    @Test
    public void testSelectingAccomYesShowsFields() {
        try (ActivityScenario<RegistrationFeeSetupActivity> s = launch()) {
            onView(withId(R.id.rbAccomYes)).perform(click());
            onView(withId(R.id.layoutAccommodationFields)).check(matches(isDisplayed()));
        }
    }

    /** Save button is visible */
    @Test
    public void testSaveButtonVisible() {
        try (ActivityScenario<RegistrationFeeSetupActivity> s = launch()) {
            onView(withId(R.id.btnSaveSetup)).check(matches(isDisplayed()));
        }
    }

    /** Tapping save with both No does not crash */
    @Test
    public void testSavingBothNoDoesNotCrash() {
        try (ActivityScenario<RegistrationFeeSetupActivity> s = launch()) {
            onView(withId(R.id.btnSaveSetup)).perform(click());
        }
    }

    /** Entering reg fee amount and saving does not crash */
    @Test
    public void testEnteringRegFeeAndSavingDoesNotCrash() {
        try (ActivityScenario<RegistrationFeeSetupActivity> s = launch()) {
            onView(withId(R.id.rbRegFeeYes)).perform(click());
            onView(withId(R.id.etRegFee))
                    .perform(typeText("PKR 500"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveSetup)).perform(click());
        }
    }
}