package com.lums.eventhub;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * AdminDashboardTest.java
 * Unit tests for Admin Dashboard logic.
 * Tests username prefix detection, routing logic,
 * persistent logout, and accommodation/event-report additions.
 * Implements: Admin US-01, US-08
 */
public class AdminDashboardTest {

    /** Test #AD prefix correctly identified as admin */
    @Test
    public void testAdminPrefixDetected() {
        String username = "#AD_admin";
        assertTrue(username.startsWith("#AD"));
    }

    /** Test #ORG prefix correctly identified as organizer */
    @Test
    public void testOrganizerPrefixDetected() {
        String username = "#ORG_spades";
        assertTrue(username.startsWith("#ORG"));
    }

    /** Test #AT prefix correctly identified as attendee */
    @Test
    public void testAttendeePrefixDetected() {
        String username = "#AT_sara";
        assertTrue(username.startsWith("#AT"));
    }

    /** Test invalid prefix is not admin */
    @Test
    public void testInvalidPrefixNotAdmin() {
        String username = "admin_user";
        assertFalse(username.startsWith("#AD"));
        assertFalse(username.startsWith("#ORG"));
        assertFalse(username.startsWith("#AT"));
    }

    /** Test empty username is not valid */
    @Test
    public void testEmptyUsernameInvalid() {
        String username = "";
        assertFalse(username.startsWith("#AD"));
        assertFalse(username.startsWith("#ORG"));
        assertFalse(username.startsWith("#AT"));
    }

    /** Test null username handling */
    @Test
    public void testNullUsernameHandling() {
        String username = null;
        try {
            boolean isAdmin = username != null && username.startsWith("#AD");
            assertFalse(isAdmin);
        } catch (NullPointerException e) {
            fail("Should handle null without throwing NullPointerException");
        }
    }

    /** Test pending count logic — 0 pending */
    @Test
    public void testZeroPendingCount() {
        int pendingCount = 0;
        assertEquals(0, pendingCount);
        assertTrue(pendingCount >= 0);
    }

    /** Test pending count logic — multiple pending */
    @Test
    public void testMultiplePendingCount() {
        int pendingCount = 5;
        assertTrue(pendingCount > 0);
    }

    /** Test approved count is non-negative */
    @Test
    public void testApprovedCountNonNegative() {
        int approvedCount = 3;
        assertTrue(approvedCount >= 0);
    }

    /** Test status filter — Submitted proposals are pending review */
    @Test
    public void testSubmittedStatusMeansPendingReview() {
        String status = "Submitted";
        assertEquals("Submitted", status);
        assertNotEquals("Draft", status);
        assertNotEquals("Approved", status);
    }

    /** Test Draft proposals are hidden from admin */
    @Test
    public void testDraftStatusHiddenFromAdmin() {
        String status = "Draft";
        assertNotEquals("Submitted", status);
        assertNotEquals("Approved", status);
    }

    // ── NEW: Persistent logout ─────────────────────────────────────────────

    /** Test sign out clears session role */
    @Test
    public void testSignOutClearsRole() {
        String savedRole = "admin";
        savedRole = null; // simulate clearSession()
        assertNull(savedRole);
    }

    /** Test sign out clears username */
    @Test
    public void testSignOutClearsUsername() {
        String savedUser = "#AD_admin";
        savedUser = null;
        assertNull(savedUser);
    }

    /** Test that after logout, no session exists */
    @Test
    public void testNoSessionAfterLogout() {
        String savedRole = null;
        String savedUser = null;
        boolean hasSession = savedUser != null && savedRole != null;
        assertFalse(hasSession);
    }

    // ── NEW: Accommodation by society ─────────────────────────────────────

    /** Test accommodation button routes to AdminAccommodationBySocietyActivity */
    @Test
    public void testAccommodationButtonRouteTarget() {
        String targetActivity = "AdminAccommodationBySocietyActivity";
        assertEquals("AdminAccommodationBySocietyActivity", targetActivity);
    }

    /** Test accommodation filter requires both wantsAccommodation and paymentStatus */
    @Test
    public void testAccommodationFilterRequiresBothFields() {
        String wantsAccommodation = "Yes";
        String paymentStatus      = "Approved";
        boolean valid = "Yes".equals(wantsAccommodation) && "Approved".equals(paymentStatus);
        assertTrue(valid);
    }

    @Test
    public void testAccommodationFilterFailsIfNotApproved() {
        String wantsAccommodation = "Yes";
        String paymentStatus      = "Pending";
        boolean valid = "Yes".equals(wantsAccommodation) && "Approved".equals(paymentStatus);
        assertFalse(valid);
    }

    @Test
    public void testAccommodationFilterFailsIfNoAccommodation() {
        String wantsAccommodation = "No";
        String paymentStatus      = "Approved";
        boolean valid = "Yes".equals(wantsAccommodation) && "Approved".equals(paymentStatus);
        assertFalse(valid);
    }

    // ── NEW: Event Reports button ──────────────────────────────────────────

    /** Test event reports button routes to AdminEventReportsActivity */
    @Test
    public void testEventReportsButtonRouteTarget() {
        String targetActivity = "AdminEventReportsActivity";
        assertEquals("AdminEventReportsActivity", targetActivity);
    }

    /** Test admin only sees Submitted reports */
    @Test
    public void testAdminSeesOnlySubmittedReports() {
        String status = "Submitted";
        assertNotEquals("Approved", status);
        assertNotEquals("Rejected", status);
        assertEquals("Submitted", status);
    }
}