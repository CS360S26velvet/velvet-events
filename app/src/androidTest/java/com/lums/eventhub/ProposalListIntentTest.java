package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.lums.eventhub.admin.proposals.ProposalListActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * ProposalListIntentTest.java
 * Espresso UI tests for ProposalListActivity.
 * Tests filter buttons and RecyclerView are displayed.
 * Implements: Admin US-02
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ProposalListIntentTest {

    /** Test proposal list screen is displayed */
    @Test
    public void testProposalListIsDisplayed() {
        ActivityScenario.launch(ProposalListActivity.class);
        onView(withId(R.id.rvProposals)).check(matches(isDisplayed()));
    }

    /** Test filter all button is displayed */
    @Test
    public void testFilterAllButtonDisplayed() {
        ActivityScenario.launch(ProposalListActivity.class);
        onView(withId(R.id.btnFilterAll)).check(matches(isDisplayed()));
    }

    /** Test filter pending button is displayed */
    @Test
    public void testFilterPendingButtonDisplayed() {
        ActivityScenario.launch(ProposalListActivity.class);
        onView(withId(R.id.btnFilterPending)).check(matches(isDisplayed()));
    }

    /** Test filter approved button is displayed */
    @Test
    public void testFilterApprovedButtonDisplayed() {
        ActivityScenario.launch(ProposalListActivity.class);
        onView(withId(R.id.btnFilterApproved)).check(matches(isDisplayed()));
    }

    /** Test filter rejected button is displayed */
    @Test
    public void testFilterRejectedButtonDisplayed() {
        ActivityScenario.launch(ProposalListActivity.class);
        onView(withId(R.id.btnFilterRejected)).check(matches(isDisplayed()));
    }

    /** Test clicking filter pending button does not crash */
    @Test
    public void testClickFilterPendingDoesNotCrash() {
        ActivityScenario.launch(ProposalListActivity.class);
        onView(withId(R.id.btnFilterPending)).perform(click());
        onView(withId(R.id.rvProposals)).check(matches(isDisplayed()));
    }

    /** Test clicking filter approved button does not crash */
    @Test
    public void testClickFilterApprovedDoesNotCrash() {
        ActivityScenario.launch(ProposalListActivity.class);
        onView(withId(R.id.btnFilterApproved)).perform(click());
        onView(withId(R.id.rvProposals)).check(matches(isDisplayed()));
    }

    /** Test stat cards are displayed */
    @Test
    public void testStatCardsDisplayed() {
        ActivityScenario.launch(ProposalListActivity.class);
        onView(withId(R.id.tvStatPending)).check(matches(isDisplayed()));
        onView(withId(R.id.tvStatApproved)).check(matches(isDisplayed()));
        onView(withId(R.id.tvStatRejected)).check(matches(isDisplayed()));
    }
}