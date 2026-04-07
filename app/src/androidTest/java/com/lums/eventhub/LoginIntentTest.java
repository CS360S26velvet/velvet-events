package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.lums.eventhub.auth.LoginActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso Intent Tests for LoginActivity - Admin US-01
 * Tests empty field validation only (no Firestore credentials needed).
 * Routing tests (#AD, #ORG, #AT) require live Firestore — tested manually.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginIntentTest {

    @Before
    public void setUp() {
        Intents.init();
        ActivityScenario.launch(LoginActivity.class);
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * Admin US-01: Login screen is displayed on launch.
     */
    @Test
    public void testLoginScreenIsDisplayed() {
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
    }

    /**
     * Admin US-01: Both input fields are visible.
     */
    @Test
    public void testInputFieldsAreVisible() {
        onView(withId(R.id.etUsername)).check(matches(isDisplayed()));
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
    }

    /**
     * Admin US-01: Empty username and password blocks login.
     */
    @Test
    public void testEmptyFieldsBlocksLogin() {
        onView(withId(R.id.etUsername)).perform(clearText(), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(clearText(), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
    }

    /**
     * Admin US-01: Empty password blocks login.
     */
    @Test
    public void testEmptyPasswordBlocksLogin() {
        onView(withId(R.id.etUsername))
                .perform(typeText("#AD_001"), closeSoftKeyboard());
        onView(withId(R.id.etPassword))
                .perform(clearText(), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
    }

    /**
     * Admin US-01: Empty username blocks login.
     */
    @Test
    public void testEmptyUsernameBlocksLogin() {
        onView(withId(R.id.etUsername))
                .perform(clearText(), closeSoftKeyboard());
        onView(withId(R.id.etPassword))
                .perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
    }
}