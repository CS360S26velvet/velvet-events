package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
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
 * Espresso Intent Tests for EventBrowsingActivity
 * AT US-02: View list of approved events
 * AT US-03: Filter by category
 * AT US-04: Search by keyword
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventBrowsingIntentTest {

    @Before
    public void setUp() {
        Intents.init();
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EventBrowsingActivity.class);
        intent.putExtra("userId", "testUser123");
        ActivityScenario.launch(intent);
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    // ═══════════════════════════════════════════════
    // AT US-02 — Screen loads
    // ═══════════════════════════════════════════════

    /**
     * AT US-02: Event browsing screen loads successfully.
     */
    @Test
    public void testScreenLoadsSuccessfully() {
        onView(withId(R.id.eventGrid)).check(matches(isDisplayed()));
    }

    /**
     * AT US-02: Results count TextView is visible.
     */
    @Test
    public void testResultsCountVisible() {
        onView(withId(R.id.tvResultsCount)).check(matches(isDisplayed()));
    }

    /**
     * AT US-02: Event grid is visible.
     */
    @Test
    public void testEventGridVisible() {
        onView(withId(R.id.eventGrid)).check(matches(isDisplayed()));
    }

    // ═══════════════════════════════════════════════
    // AT US-03 — Filter buttons
    // ═══════════════════════════════════════════════

    /**
     * AT US-03: Filter All button is visible.
     */
    @Test
    public void testFilterAllButtonVisible() {
        onView(withId(R.id.btnFilterAll)).check(matches(isDisplayed()));
    }

    /**
     * AT US-03: Filter Society Events button is visible.
     */
    @Test
    public void testFilterSocietyButtonVisible() {
        onView(withId(R.id.btnFilterSociety)).check(matches(isDisplayed()));
    }

    /**
     * AT US-03: Filter Workshops button is visible.
     */
    @Test
    public void testFilterWorkshopsButtonVisible() {
        onView(withId(R.id.btnFilterWorkshops)).check(matches(isDisplayed()));
    }

    /**
     * AT US-03: Clicking Filter All stays on screen and updates results.
     */
    @Test
    public void testClickFilterAllStaysOnScreen() {
        onView(withId(R.id.btnFilterAll)).perform(click());
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        onView(withId(R.id.tvResultsCount)).check(matches(isDisplayed()));
    }



    // ═══════════════════════════════════════════════
    // AT US-04 — Search
    // ═══════════════════════════════════════════════

    /**
     * AT US-04: Search field is visible.
     */
    @Test
    public void testSearchFieldVisible() {
        onView(withId(R.id.etSearch)).check(matches(isDisplayed()));
    }

    /**
     * AT US-04: Search button is visible.
     */
    @Test
    public void testSearchButtonVisible() {
        onView(withId(R.id.btnSearch)).check(matches(isDisplayed()));
    }

    /**
     * AT US-04: Typing in search field and clicking search stays on screen.
     */
    @Test
    public void testSearchByKeywordStaysOnScreen() {
        onView(withId(R.id.etSearch))
                .perform(typeText("Tech"), closeSoftKeyboard());
        onView(withId(R.id.btnSearch)).perform(click());
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        onView(withId(R.id.tvResultsCount)).check(matches(isDisplayed()));
    }

    /**
     * AT US-04: Empty search returns results without crashing.
     */
    @Test
    public void testEmptySearchDoesNotCrash() {
        onView(withId(R.id.btnSearch)).perform(click());
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        onView(withId(R.id.tvResultsCount)).check(matches(isDisplayed()));
    }

    // ═══════════════════════════════════════════════
    // Navigation
    // ═══════════════════════════════════════════════

    /**
     * AT US-02: Bottom nav Home opens AttendeeActivity.
     */
    @Test
    public void testNavHomeOpensAttendeeActivity() {
        onView(withId(R.id.navDashboard)).perform(click());
        intended(hasComponent(AttendeeActivity.class.getName()));
    }

    /**
     * AT US-02: Bottom nav My Registrations opens MyRegistrationsActivity.
     */
    @Test
    public void testNavMyRegistrationsNavigatesCorrectly() {
        onView(withId(R.id.navMyRegistrations)).perform(click());
        intended(hasComponent(MyRegistrationsActivity.class.getName()));
    }
}