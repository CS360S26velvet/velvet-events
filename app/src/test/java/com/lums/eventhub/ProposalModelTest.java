package com.lums.eventhub;

import com.lums.eventhub.model.Proposal;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * ProposalModelTest.java
 * Unit tests for the Proposal model class.
 * Tests all getters, setters, and default status behaviour.
 * Implements: Admin US-02
 */
public class ProposalModelTest {

    private Proposal proposal;

    @Before
    public void setUp() {
        proposal = new Proposal();
    }

    /** Test that default constructor creates a non-null object */
    @Test
    public void testDefaultConstructorNotNull() {
        assertNotNull(proposal);
    }

    /** Test id getter and setter */
    @Test
    public void testSetAndGetId() {
        proposal.setId("proposal_001");
        assertEquals("proposal_001", proposal.getId());
    }

    /** Test title getter and setter */
    @Test
    public void testSetAndGetTitle() {
        proposal.setTitle("Tech Fest 2026");
        assertEquals("Tech Fest 2026", proposal.getTitle());
    }

    /** Test status getter and setter */
    @Test
    public void testSetAndGetStatus() {
        proposal.setStatus("Submitted");
        assertEquals("Submitted", proposal.getStatus());
    }

    /** Test all valid status values */
    @Test
    public void testAllValidStatusValues() {
        String[] validStatuses = {
                "Draft", "Submitted", "Approved",
                "Rejected", "Revision Requested"
        };
        for (String status : validStatuses) {
            proposal.setStatus(status);
            assertEquals(status, proposal.getStatus());
        }
    }

    /** Test organizerUsername getter and setter */
    @Test
    public void testSetAndGetOrganizerUsername() {
        proposal.setOrganizerUsername("#ORG_spades");
        assertEquals("#ORG_spades", proposal.getOrganizerUsername());
    }

    /** Test societyName getter and setter */
    @Test
    public void testSetAndGetSocietyName() {
        proposal.setSocietyName("SPADES Society");
        assertEquals("SPADES Society", proposal.getSocietyName());
    }

    /** Test venue getter and setter */
    @Test
    public void testSetAndGetVenue() {
        proposal.setVenue("Auditorium A");
        assertEquals("Auditorium A", proposal.getVenue());
    }

    /** Test date getter and setter */
    @Test
    public void testSetAndGetDate() {
        proposal.setDate("15/05/2026");
        assertEquals("15/05/2026", proposal.getDate());
    }

    /** Test legacy getEventDate falls back to date field */
    @Test
    public void testGetEventDateFallsBackToDate() {
        proposal.setDate("20/05/2026");
        assertEquals("20/05/2026", proposal.getEventDate());
    }

    /** Test estimatedBudget getter and setter */
    @Test
    public void testSetAndGetEstimatedBudget() {
        proposal.setEstimatedBudget(50000L);
        assertEquals(50000L, proposal.getEstimatedBudget());
    }

    /** Test expectedParticipants getter and setter */
    @Test
    public void testSetAndGetExpectedParticipants() {
        proposal.setExpectedParticipants(200L);
        assertEquals(200L, proposal.getExpectedParticipants());
    }

    /** Test description getter and setter */
    @Test
    public void testSetAndGetDescription() {
        proposal.setDescription("Annual tech festival");
        assertEquals("Annual tech festival", proposal.getDescription());
    }

    /** Test requiresAccommodation getter and setter */
    @Test
    public void testSetAndGetRequiresAccommodation() {
        proposal.setRequiresAccommodation(true);
        assertTrue(proposal.isRequiresAccommodation());

        proposal.setRequiresAccommodation(false);
        assertFalse(proposal.isRequiresAccommodation());
    }

    /** Test accommodationCount getter and setter */
    @Test
    public void testSetAndGetAccommodationCount() {
        proposal.setAccommodationCount(5L);
        assertEquals(5L, proposal.getAccommodationCount());
    }

    /** Test that two proposals with same data are independent objects */
    @Test
    public void testProposalsAreIndependent() {
        Proposal p1 = new Proposal();
        Proposal p2 = new Proposal();
        p1.setTitle("Event A");
        p2.setTitle("Event B");
        assertNotEquals(p1.getTitle(), p2.getTitle());
    }
}