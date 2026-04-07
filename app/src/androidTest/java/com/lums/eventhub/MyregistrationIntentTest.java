package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
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
 * Espresso Intent Tests for MyRegistrationsActivity
 * AT US-06: View registered events list
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MyregistrationIntentTest {

    @Before
    public void setUp() {
        Intents.init();
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                MyRegistrationsActivity.class);
        intent.putExtra("userId", "testUser123");
        ActivityScenario.launch(intent);
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * AT US-06: Total count TextView is visible.
     */
    @Test
    public void testTotalCountVisible() {
        onView(withId(R.id.tvTotalCount)).check(matches(isDisplayed()));
    }

    /**
     * AT US-06: Bottom nav Browse Events opens EventBrowsingActivity.
     */
    @Test
    public void testNavBrowseEventsNavigatesCorrectly() {
        onView(withId(R.id.navBrowseEvents)).perform(click());
        intended(hasComponent(EventBrowsingActivity.class.getName()));
    }

    /**
     * AT US-06: Bottom nav Notifications opens NotificationsActivity.
     */
    @Test
    public void testNavNotificationsNavigatesCorrectly() {
        onView(withId(R.id.navNotifications)).perform(click());
        intended(hasComponent(NotificationsActivity.class.getName()));
    }
}