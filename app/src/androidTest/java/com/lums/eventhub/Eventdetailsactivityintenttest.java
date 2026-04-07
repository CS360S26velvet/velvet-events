package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

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
 * Espresso Intent Tests for EventDetailsActivity
 * AT US-05: View full event details, register, add to calendar
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class Eventdetailsactivityintenttest {

    @Before
    public void setUp() {
        Intents.init();
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EventDetailsActivity.class);
        intent.putExtra("userId",           "testUser123");
        intent.putExtra("eventId",          "evt001");
        intent.putExtra("eventTitle",       "Tech Summit 2025");
        intent.putExtra("eventOrganizer",   "SPADES");
        intent.putExtra("eventDate",        "Dec 31, 2099");
        intent.putExtra("eventVenue",       "LUMS Auditorium");
        intent.putExtra("eventCategory",    "Society Events");
        intent.putExtra("eventSeatsBooked", 100);
        intent.putExtra("eventSeatsTotal",  500);
        intent.putExtra("Description",      "Annual tech summit");
        intent.putExtra("RegClosingDate",   "Dec 25, 2099");
        intent.putExtra("Time",             "9:00 AM");
        intent.putExtra("fee",              "Free");
        ActivityScenario.launch(intent);
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    // ═══════════════════════════════════════════════
    // AT US-05 — Screen loads with correct data
    // ═══════════════════════════════════════════════

    /**
     * AT US-05: Event details screen loads successfully.
     */
    @Test
    public void testScreenLoadsSuccessfully() {
        onView(withId(R.id.tvHeroTitle)).check(matches(isDisplayed()));
    }

    /**
     * AT US-05: Event title displayed correctly.
     */
    @Test
    public void testEventTitleDisplayed() {
        onView(withId(R.id.tvHeroTitle))
                .check(matches(withText("Tech Summit 2025")));
    }

    /**
     * AT US-05: Event category displayed correctly.
     */
    @Test
    public void testEventCategoryDisplayed() {
        onView(withId(R.id.tvHeroCategory))
                .check(matches(withText("Society Events")));
    }

    /**
     * AT US-05: Event date is displayed.
     */
    @Test
    public void testEventDateDisplayed() {
        onView(withId(R.id.tvDate)).check(matches(isDisplayed()));
    }

    /**
     * AT US-05: Event venue is displayed.
     */
    @Test
    public void testEventVenueDisplayed() {
        onView(withId(R.id.tvVenue)).check(matches(isDisplayed()));
    }

    /**
     * AT US-05: Event organizer is displayed.
     */
    @Test
    public void testEventOrganizerDisplayed() {
        onView(withId(R.id.tvOrganizer)).check(matches(isDisplayed()));
    }

    /**
     * AT US-05: Seat availability is displayed.
     */
    @Test
    public void testSeatAvailabilityDisplayed() {
        onView(withId(R.id.tvSeats)).check(matches(isDisplayed()));
    }

    /**
     * AT US-05: Description section is displayed.
     */
    @Test
    public void testDescriptionDisplayed() {
        onView(withId(R.id.tvDescription)).perform(scrollTo());
        onView(withId(R.id.tvDescription)).check(matches(isDisplayed()));
    }

    // ═══════════════════════════════════════════════
    // AT US-05 — Action buttons
    // ═══════════════════════════════════════════════

    /**
     * AT US-05: Register button is visible.
     */
    @Test
    public void testRegisterButtonVisible() {
        onView(withId(R.id.btnRegister)).perform(scrollTo());
        onView(withId(R.id.btnRegister)).check(matches(isDisplayed()));
    }

    /**
     * AT US-05: Add to Calendar button is visible.
     */
    @Test
    public void testAddToCalendarButtonVisible() {
        onView(withId(R.id.btnAddToCalendar)).perform(scrollTo());
        onView(withId(R.id.btnAddToCalendar)).check(matches(isDisplayed()));
    }

    /**
     * AT US-05: Back button is visible.
     */
    @Test
    public void testBackButtonVisible() {
        onView(withId(R.id.btnBackBottom)).perform(scrollTo());
        onView(withId(R.id.btnBackBottom)).check(matches(isDisplayed()));
    }

    /**
     * AT US-05: Clicking back navigates to EventBrowsingActivity.
     */
    @Test
    public void testBackButtonNavigatesToBrowse() {
        onView(withId(R.id.btnBackBottom)).perform(scrollTo(), click());
        intended(hasComponent(EventBrowsingActivity.class.getName()));
    }

    /**
     * AT US-05: Registration closes note is displayed.
     */
    @Test
    public void testRegistrationClosesNoteDisplayed() {
        onView(withId(R.id.tvRegCloses)).perform(scrollTo());
        onView(withId(R.id.tvRegCloses)).check(matches(isDisplayed()));
    }
}