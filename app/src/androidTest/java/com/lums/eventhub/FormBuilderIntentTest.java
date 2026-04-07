package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount;

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
public class FormBuilderIntentTest {

    @Before
    public void setUp() {
        Intents.init();
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                FormBuilderActivity.class);
        intent.putExtra("eventId",   "SPADES2025");
        intent.putExtra("eventName", "SPADES Annual Event");
        ActivityScenario.launch(intent);
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * Org US-18: Form builder screen is displayed on launch.
     */
    @Test
    public void testFormBuilderScreenIsDisplayed() {
        onView(withId(R.id.recyclerViewQuestions)).check(matches(isDisplayed()));
    }

    /**
     * Org US-18: Add Short Text button is visible.
     */
    @Test
    public void testAddShortTextButtonIsVisible() {
        onView(withId(R.id.btnAddShortText)).check(matches(isDisplayed()));
    }

    /**
     * Org US-18: Clicking Add Short Text adds a question to the RecyclerView.
     */
    @Test
    public void testAddShortTextAddsQuestionToList() {
        onView(withId(R.id.btnAddShortText)).perform(click());
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        onView(withId(R.id.recyclerViewQuestions))
                .check(matches(hasMinimumChildCount(1)));
    }

    /**
     * Org US-18: Clicking Add Paragraph adds a question to the list.
     */
    @Test
    public void testAddParagraphAddsQuestionToList() {
        onView(withId(R.id.btnAddParagraph)).perform(click());
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        onView(withId(R.id.recyclerViewQuestions))
                .check(matches(hasMinimumChildCount(1)));
    }

    /**
     * Org US-18: Adding two Short Text questions gives at least 2 items.
     */
    @Test
    public void testAddingMultipleQuestionsIncreasesListSize() {
        onView(withId(R.id.btnAddShortText)).perform(click());
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        onView(withId(R.id.btnAddShortText)).perform(click());
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        onView(withId(R.id.recyclerViewQuestions))
                .check(matches(hasMinimumChildCount(2)));
    }

    /**
     * Org US-19: Short Text and Paragraph buttons are visible (always on screen).
     */
    @Test
    public void testMainQuestionTypeButtonsVisible() {
        onView(withId(R.id.btnAddShortText)).check(matches(isDisplayed()));
        onView(withId(R.id.btnAddParagraph)).check(matches(isDisplayed()));
    }

    /**
     * Org US-19: Adding Multiple Choice question adds it to the list.
     */
//


    /**
     * Org US-19: Adding Multiple Choice question verifies list grows adds it to the list.
     */
//    @Test
//    public void testAddMultiChoiceAddsQuestionAgain() {
//        onView(withId(R.id.btnAddMultiChoice)).perform(click());
//        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
//        onView(withId(R.id.recyclerViewQuestions))
//                .check(matches(hasMinimumChildCount(1)));
//    }

    /**
     * Org US-18: Preview button is visible.
     */
    @Test
    public void testPreviewButtonIsVisible() {
        onView(withId(R.id.btnPreview)).check(matches(isDisplayed()));
    }

    /**
     * Org US-18: Clicking Preview with no questions stays on screen.
     */
    @Test
    public void testPreviewWithNoQuestionsStaysOnScreen() {
        onView(withId(R.id.btnPreview)).perform(click());
        onView(withId(R.id.recyclerViewQuestions)).check(matches(isDisplayed()));
    }

    /**
     * Org US-18: Save Form button is visible.
     */
    @Test
    public void testSaveFormButtonIsVisible() {
        onView(withId(R.id.btnSaveForm)).check(matches(isDisplayed()));
    }

    /**
     * Org US-18: Clicking Save with no questions stays on screen.
     */
    @Test
    public void testSaveWithNoQuestionsStaysOnScreen() {
        onView(withId(R.id.btnSaveForm)).perform(click());
        onView(withId(R.id.recyclerViewQuestions)).check(matches(isDisplayed()));
    }
}