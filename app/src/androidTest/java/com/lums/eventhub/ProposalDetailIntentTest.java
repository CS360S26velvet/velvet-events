package com.lums.eventhub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.lums.eventhub.admin.proposals.ProposalDetailActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * ProposalDetailIntentTest.java
 * Espresso UI tests for ProposalDetailActivity.
 * Tests Approve, Reject and Revision buttons are visible.
 * scrollTo() used because buttons are below the fold.
 * Implements: Admin US-02
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ProposalDetailIntentTest {

    private ActivityScenario<ProposalDetailActivity> launchDetail() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                ProposalDetailActivity.class
        );
        intent.putExtra("proposalId", "test_proposal_id");
        return ActivityScenario.launch(intent);
    }

    /** Test back button is displayed */
    @Test
    public void testBackButtonDisplayed() {
        launchDetail();
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
    }

    /** Test approve button is displayed (scroll required) */
    @Test
    public void testApproveButtonDisplayed() {
        launchDetail();
        onView(withId(R.id.btnApprove))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /** Test reject button is displayed (scroll required) */
    @Test
    public void testRejectButtonDisplayed() {
        launchDetail();
        onView(withId(R.id.btnReject))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /** Test revision button is displayed (scroll required) */
    @Test
    public void testRevisionButtonDisplayed() {
        launchDetail();
        onView(withId(R.id.btnRevision))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /** Test detail screen is displayed */
    @Test
    public void testDetailScreenDisplayed() {
        launchDetail();
        onView(withId(R.id.btnApprove))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }
}