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

/**
 * Espresso Intent Tests for AttendeeActivity
 * AT US-01: Dashboard quick action buttons and navigation
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class Attendeeactivityintenttest {

    @Before
    public void setUp() {
        Intents.init();
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AttendeeActivity.class);
        intent.putExtra("userId", "testUser123");
        ActivityScenario.launch(intent);
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * AT US-01: Dashboard screen loads successfully.
     */
    @Test
    public void testDashboardScreenLoads() {
        onView(withId(R.id.btnBrowseEvents)).check(matches(isDisplayed()));
    }

    /**
     * AT US-01: Registered count TextView is visible.
     */
    @Test
    public void testRegisteredCountVisible() {
        onView(withId(R.id.tvRegisteredCount)).check(matches(isDisplayed()));
    }

    /**
     * AT US-01: Notification count TextView is visible.
     */
    @Test
    public void testNotifCountVisible() {
        onView(withId(R.id.tvNotifCount)).check(matches(isDisplayed()));
    }

    /**
     * AT US-01: Browse Events button opens EventBrowsingActivity.
     */
    @Test
    public void testBrowseEventsButtonNavigatesCorrectly() {
        onView(withId(R.id.btnBrowseEvents)).perform(click());
        intended(hasComponent(EventBrowsingActivity.class.getName()));
    }

    /**
     * AT US-01: My Registrations button opens MyRegistrationsActivity.
     */
    @Test
    public void testMyRegistrationsButtonNavigatesCorrectly() {
        onView(withId(R.id.btnMyRegistrations)).perform(click());
        intended(hasComponent(MyRegistrationsActivity.class.getName()));
    }

    /**
     * AT US-01: Notifications button opens NotificationsActivity.
     */
    @Test
    public void testNotificationsButtonNavigatesCorrectly() {
        onView(withId(R.id.btnNotifications)).perform(click());
        intended(hasComponent(NotificationsActivity.class.getName()));
    }

    /**
     * AT US-01: Calendar button opens AttendeeCalendarActivity.
     */
    @Test
    public void testCalendarButtonNavigatesCorrectly() {
        onView(withId(R.id.btnCalendar)).perform(click());
        intended(hasComponent(AttendeeCalendarActivity.class.getName()));
    }

    /**
     * AT US-01: Bottom nav Browse Events opens EventBrowsingActivity.
     */
    @Test
    public void testNavBrowseEventsNavigatesCorrectly() {
        onView(withId(R.id.navBrowseEvents)).perform(click());
        intended(hasComponent(EventBrowsingActivity.class.getName()));
    }

    /**
     * AT US-01: Bottom nav My Registrations opens MyRegistrationsActivity.
     */
    @Test
    public void testNavMyRegistrationsNavigatesCorrectly() {
        onView(withId(R.id.navMyRegistrations)).perform(click());
        intended(hasComponent(MyRegistrationsActivity.class.getName()));
    }

    /**
     * AT US-01: Bottom nav Notifications opens NotificationsActivity.
     */
    @Test
    public void testNavNotificationsNavigatesCorrectly() {
        onView(withId(R.id.navNotifications)).perform(click());
        intended(hasComponent(NotificationsActivity.class.getName()));
    }
}