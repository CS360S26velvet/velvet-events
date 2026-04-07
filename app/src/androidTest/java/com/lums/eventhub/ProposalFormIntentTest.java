package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

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

/**
 * Espresso Intent Tests for ProposalFormActivity
 * Org US-02: Create a new proposal
 * Org US-03: Save as draft (no validation required)
 * Org US-04: Submit to CCA (validation required)
 *
 * Note: btnSubmitCCA and btnSaveDraft are in a fixed bottom bar (not in ScrollView)
 * so scrollTo() cannot be used on them — click() directly instead.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ProposalFormIntentTest {

    @Before
    public void setUp() {
        Intents.init();
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                ProposalFormActivity.class);
        intent.putExtra("organizerUsername", "ORG0012");
        intent.putExtra("societyName", "SPADES Society");
        ActivityScenario.launch(intent);
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    // ═══════════════════════════════════════════════
    // Org US-02 — Form screen loads
    // ═══════════════════════════════════════════════

    /**
     * Org US-02: Proposal form screen is displayed on launch.
     */
    @Test
    public void testProposalFormScreenIsDisplayed() {
        onView(withId(R.id.etTitle)).perform(scrollTo());
        onView(withId(R.id.etTitle)).check(matches(isDisplayed()));
    }

    /**
     * Org US-02: Title and description fields are visible.
     */
    @Test
    public void testAllRequiredFieldsVisible() {
        onView(withId(R.id.etTitle)).perform(scrollTo());
        onView(withId(R.id.etTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.etDescription)).perform(scrollTo());
        onView(withId(R.id.etDescription)).check(matches(isDisplayed()));
    }

    /**
     * Org US-03: Save Draft button is visible in bottom bar.
     */
    @Test
    public void testSaveDraftButtonIsVisible() {
        onView(withId(R.id.btnSaveDraft)).check(matches(isDisplayed()));
    }

    /**
     * Org US-04: Submit button is visible in bottom bar.
     */
    @Test
    public void testSubmitButtonIsVisible() {
        onView(withId(R.id.btnSubmitCCA)).check(matches(isDisplayed()));
    }

    // ═══════════════════════════════════════════════
    // Org US-04 — Submit validation blocks empty fields
    // ═══════════════════════════════════════════════

    /**
     * Org US-04: Submit with empty title stays on form (blocked).
     */
    @Test
    public void testSubmitBlockedWhenTitleEmpty() {
        onView(withId(R.id.etTitle))
                .perform(scrollTo(), click(), clearText(), closeSoftKeyboard());
        androidx.test.espresso.Espresso.closeSoftKeyboard();
        onView(withId(R.id.btnSubmitCCA)).perform(click()); // NO scrollTo — fixed bottom bar
        onView(withId(R.id.etTitle)).perform(scrollTo());
        onView(withId(R.id.etTitle)).check(matches(isDisplayed()));
    }

    /**
     * Org US-04: Submit with empty description stays on form (blocked).
     */
    @Test
    public void testSubmitBlockedWhenDescriptionEmpty() {
        onView(withId(R.id.etTitle))
                .perform(scrollTo(), click(), typeText("Tech Summit"), closeSoftKeyboard());
        onView(withId(R.id.etDescription))
                .perform(scrollTo(), click(), clearText(), closeSoftKeyboard());
        androidx.test.espresso.Espresso.closeSoftKeyboard();
        onView(withId(R.id.btnSubmitCCA)).perform(click());
        onView(withId(R.id.etTitle)).perform(scrollTo());
        onView(withId(R.id.etTitle)).check(matches(isDisplayed()));
    }

    /**
     * Org US-04: Submit with no event type selected stays on form (blocked).
     */
    @Test
    public void testSubmitBlockedWhenEventTypeNotSelected() {
        onView(withId(R.id.etTitle))
                .perform(scrollTo(), click(), typeText("Tech Summit"), closeSoftKeyboard());
        onView(withId(R.id.etDescription))
                .perform(scrollTo(), click(), typeText("Annual event"), closeSoftKeyboard());
        androidx.test.espresso.Espresso.closeSoftKeyboard();
        onView(withId(R.id.btnSubmitCCA)).perform(click());
        onView(withId(R.id.etTitle)).perform(scrollTo());
        onView(withId(R.id.etTitle)).check(matches(isDisplayed()));
    }

    /**
     * Org US-04: Submit with empty date stays on form (blocked).
     */
    @Test
    public void testSubmitBlockedWhenDateEmpty() {
        onView(withId(R.id.etTitle))
                .perform(scrollTo(), click(), typeText("Tech Summit"), closeSoftKeyboard());
        onView(withId(R.id.etDescription))
                .perform(scrollTo(), click(), typeText("Annual event"), closeSoftKeyboard());
        onView(withId(R.id.rbSocietyEvent)).perform(scrollTo(), click());
        onView(withId(R.id.etDate))
                .perform(scrollTo(), click(), clearText(), closeSoftKeyboard());
        androidx.test.espresso.Espresso.closeSoftKeyboard();
        onView(withId(R.id.btnSubmitCCA)).perform(click());
        onView(withId(R.id.etTitle)).perform(scrollTo());
        onView(withId(R.id.etTitle)).check(matches(isDisplayed()));
    }

    /**
     * Org US-04: Submit with empty venue stays on form (blocked).
     */
    @Test
    public void testSubmitBlockedWhenVenueEmpty() {
        onView(withId(R.id.etTitle))
                .perform(scrollTo(), click(), typeText("Tech Summit"), closeSoftKeyboard());
        onView(withId(R.id.etDescription))
                .perform(scrollTo(), click(), typeText("Annual event"), closeSoftKeyboard());
        onView(withId(R.id.rbSocietyEvent)).perform(scrollTo(), click());
        onView(withId(R.id.etDate))
                .perform(scrollTo(), click(), typeText("2025-12-01"), closeSoftKeyboard());
        onView(withId(R.id.etVenue))
                .perform(scrollTo(), click(), clearText(), closeSoftKeyboard());
        androidx.test.espresso.Espresso.closeSoftKeyboard();
        onView(withId(R.id.btnSubmitCCA)).perform(click());
        onView(withId(R.id.etTitle)).perform(scrollTo());
        onView(withId(R.id.etTitle)).check(matches(isDisplayed()));
    }

}