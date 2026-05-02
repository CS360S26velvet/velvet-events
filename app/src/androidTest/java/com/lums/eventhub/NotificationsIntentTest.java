package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
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

/**
 * NotificationsIntentTest.java
 *
 * Espresso intent tests for NotificationsActivity.
 * AT US-07 — View notifications (including payment approval/rejection).
 *
 * Only references view IDs that actually exist in the layout:
 *   R.id.notificationsList, R.id.tvEmpty, R.id.navDashboard,
 *   R.id.navBrowseEvents, R.id.navMyRegistrations, R.id.navNotifications
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NotificationsIntentTest {

    private ActivityScenario<NotificationsActivity> launch() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                NotificationsActivity.class);
        intent.putExtra("userId", "testUser001");
        ActivityScenario<NotificationsActivity> s = ActivityScenario.launch(intent);
        try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
        return s;
    }

    /** AT US-07: Bottom nav dashboard button is present. */
    @Test
    public void testNavDashboardVisible() {
        try (ActivityScenario<NotificationsActivity> s = launch()) {
            onView(withId(R.id.navDashboard)).check(matches(isDisplayed()));
        }
    }

    /** AT US-07: Bottom nav browse events button is present. */
    @Test
    public void testNavBrowseEventsVisible() {
        try (ActivityScenario<NotificationsActivity> s = launch()) {
            onView(withId(R.id.navBrowseEvents)).check(matches(isDisplayed()));
        }
    }

    /** AT US-07: Bottom nav my registrations button is present. */
    @Test
    public void testNavMyRegistrationsVisible() {
        try (ActivityScenario<NotificationsActivity> s = launch()) {
            onView(withId(R.id.navMyRegistrations)).check(matches(isDisplayed()));
        }
    }

    /**
     * AT US-07: For a user with no notifications, the empty-state view
     * is shown (testUser001 has no Firestore data in the test environment).
     */
    @Test
    public void testEmptyStateShownForUserWithNoNotifications() {
        try (ActivityScenario<NotificationsActivity> s = launch()) {
            onView(withId(R.id.tvEmpty)).check(matches(isDisplayed()));
        }
    }
}