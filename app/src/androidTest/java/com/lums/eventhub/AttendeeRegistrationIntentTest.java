package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
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
 * Espresso Intent Tests for AttendeeRegistrationActivity
 * Org US-18: Organizer selects an event to build its registration form.
 * Test environment: No events in Firestore for ORG0012 — empty state shown.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AttendeeRegistrationIntentTest {

    @Before
    public void setUp() {
        Intents.init();
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AttendeeRegistrationActivity.class);
        intent.putExtra("organizerUsername", "ORG0012");
        ActivityScenario.launch(intent);
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * Org US-18: Screen opens successfully — activity does not crash.
     */
    @Test
    public void testScreenOpensSuccessfully() {
        onView(withId(R.id.tvNoEvents)).check(matches(isDisplayed()));
    }

    /**
     * Org US-18: "No events yet" message is displayed.
     */
    @Test
    public void testNoEventsMessageIsDisplayed() {
        onView(withId(R.id.tvNoEvents)).check(matches(isDisplayed()));
    }

    /**
     * Org US-18: "No events yet" message contains correct text.
     */
    @Test
    public void testNoEventsMessageText() {
        onView(withId(R.id.tvNoEvents)).check(matches(
                withText("No events yet. Create an event from the Organizer Dashboard first.")));
    }

    /**
     * Org US-18: RecyclerView is hidden when no events exist.
     */
    @Test
    public void testRecyclerViewHiddenWhenNoEvents() {
        onView(withId(R.id.recyclerViewEvents))
                .check(matches(org.hamcrest.Matchers.not(isDisplayed())));
    }
}