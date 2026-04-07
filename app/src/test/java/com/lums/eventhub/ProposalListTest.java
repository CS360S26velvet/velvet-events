package com.lums.eventhub;

import com.lums.eventhub.model.Proposal;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/**
 * ProposalListTest.java
 * Unit tests for proposal list filtering logic.
 * Tests filter behaviour for all status values.
 * Implements: Admin US-02
 */
public class ProposalListTest {

    private List<Proposal> allProposals;

    @Before
    public void setUp() {
        allProposals = new ArrayList<>();

        Proposal p1 = new Proposal();
        p1.setId("1");
        p1.setTitle("Tech Fest");
        p1.setStatus("Submitted");
        p1.setOrganizerUsername("#ORG_spades");

        Proposal p2 = new Proposal();
        p2.setId("2");
        p2.setTitle("Drama Show");
        p2.setStatus("Approved");
        p2.setOrganizerUsername("#ORG_drama");

        Proposal p3 = new Proposal();
        p3.setId("3");
        p3.setTitle("Food Mela");
        p3.setStatus("Rejected");
        p3.setOrganizerUsername("#ORG_food");

        Proposal p4 = new Proposal();
        p4.setId("4");
        p4.setTitle("Sports Day");
        p4.setStatus("Draft");
        p4.setOrganizerUsername("#ORG_sports");

        Proposal p5 = new Proposal();
        p5.setId("5");
        p5.setTitle("Art Show");
        p5.setStatus("Revision Requested");
        p5.setOrganizerUsername("#ORG_arts");

        allProposals.add(p1);
        allProposals.add(p2);
        allProposals.add(p3);
        allProposals.add(p4);
        allProposals.add(p5);
    }

    /** Helper filter method mimicking ProposalListActivity.filterBy() */
    private List<Proposal> filterBy(String status) {
        List<Proposal> filtered = new ArrayList<>();
        for (Proposal p : allProposals) {
            if ("all".equals(status) || status.equals(p.getStatus())) {
                if (!"Draft".equals(p.getStatus())) {
                    filtered.add(p);
                }
            }
        }
        return filtered;
    }

    /** Filter all returns all non-draft proposals */
    @Test
    public void testFilterAllExcludesDraft() {
        List<Proposal> result = filterBy("all");
        assertEquals(4, result.size());
        for (Proposal p : result) {
            assertNotEquals("Draft", p.getStatus());
        }
    }

    /** Filter Submitted returns only pending proposals */
    @Test
    public void testFilterSubmittedReturnsOnlySubmitted() {
        List<Proposal> result = filterBy("Submitted");
        assertEquals(1, result.size());
        assertEquals("Submitted", result.get(0).getStatus());
        assertEquals("Tech Fest", result.get(0).getTitle());
    }

    /** Filter Approved returns only approved proposals */
    @Test
    public void testFilterApprovedReturnsOnlyApproved() {
        List<Proposal> result = filterBy("Approved");
        assertEquals(1, result.size());
        assertEquals("Approved", result.get(0).getStatus());
        assertEquals("Drama Show", result.get(0).getTitle());
    }

    /** Filter Rejected returns only rejected proposals */
    @Test
    public void testFilterRejectedReturnsOnlyRejected() {
        List<Proposal> result = filterBy("Rejected");
        assertEquals(1, result.size());
        assertEquals("Rejected", result.get(0).getStatus());
        assertEquals("Food Mela", result.get(0).getTitle());
    }

    /** Draft proposals never appear in any filter */
    @Test
    public void testDraftNeverAppearsInFilter() {
        List<Proposal> all = filterBy("all");
        for (Proposal p : all) {
            assertNotEquals("Draft", p.getStatus());
        }
    }

    /** Empty list returns empty filter */
    @Test
    public void testEmptyListReturnsEmpty() {
        allProposals.clear();
        List<Proposal> result = filterBy("all");
        assertTrue(result.isEmpty());
    }

    /** Proposal has correct organizer username format */
    @Test
    public void testProposalOrganizerUsernameFormat() {
        for (Proposal p : allProposals) {
            if (!"Draft".equals(p.getStatus())) {
                assertTrue(p.getOrganizerUsername().startsWith("#ORG"));
            }
        }
    }

    /** Total non-draft proposals count is correct */
    @Test
    public void testTotalNonDraftCount() {
        int count = 0;
        for (Proposal p : allProposals) {
            if (!"Draft".equals(p.getStatus())) count++;
        }
        assertEquals(4, count);
    }
}