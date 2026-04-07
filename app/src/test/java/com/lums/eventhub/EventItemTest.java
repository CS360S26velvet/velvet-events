package com.lums.eventhub;
//package com.example.event_management;

import com.lums.eventhub.Event;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for OrganizerDashboardActivity.EventItem and getRelativeTime logic.
 *
 * Covers: Org US-01 (Organizer Dashboard — view event list, statuses, timestamps)
 *
 * EventItem is a static inner class — accessible without launching the Activity.
 * getRelativeTime() logic is extracted inline since it is private in the Activity.
 * All tests are pure-Java, no Android dependencies.
 */
public class EventItemTest {

    // ─────────────────────────────────────────────
    // Mirror of OrganizerDashboardActivity.getRelativeTime()
    // ─────────────────────────────────────────────

    private String getRelativeTime(long timestamp) {
        long diff    = System.currentTimeMillis() - timestamp;
        long minutes = diff / 60000;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24)   return hours + "h ago";
        return (hours / 24) + "d ago";
    }

    // ═══════════════════════════════════════════════
    // EventItem — constructor & field storage
    // ═══════════════════════════════════════════════

    /**
     * US-01: Constructor stores all five fields correctly.
     */
    @Test
    public void testConstructorStoresAllFields() {
        OrganizerDashboardActivity.EventItem item =
                new OrganizerDashboardActivity.EventItem(
                        "abc123", "Tech Summit", "2025-12-01", "Draft", true);

        assertEquals("abc123",     item.id);
        assertEquals("Tech Summit", item.title);
        assertEquals("2025-12-01", item.date);
        assertEquals("Draft",      item.status);
        assertTrue(item.isProposal);
    }

    /**
     * US-01: id field is stored correctly.
     */
    @Test
    public void testIdFieldStoredCorrectly() {
        OrganizerDashboardActivity.EventItem item =
                new OrganizerDashboardActivity.EventItem(
                        "proposal-99", "Event", "2025-01-01", "Draft", true);
        assertEquals("proposal-99", item.id);
    }

    /**
     * US-01: title field is stored correctly.
     */
    @Test
    public void testTitleFieldStoredCorrectly() {
        OrganizerDashboardActivity.EventItem item =
                new OrganizerDashboardActivity.EventItem(
                        "id1", "Annual Gala Night", "2025-06-15", "Approved", false);
        assertEquals("Annual Gala Night", item.title);
    }

    /**
     * US-01: date field is stored correctly.
     */
    @Test
    public void testDateFieldStoredCorrectly() {
        OrganizerDashboardActivity.EventItem item =
                new OrganizerDashboardActivity.EventItem(
                        "id1", "Event", "2025-11-30", "Submitted", true);
        assertEquals("2025-11-30", item.date);
    }

    /**
     * US-01: status field is stored correctly.
     */
    @Test
    public void testStatusFieldStoredCorrectly() {
        OrganizerDashboardActivity.EventItem item =
                new OrganizerDashboardActivity.EventItem(
                        "id1", "Event", "2025-01-01", "Revision Requested", true);
        assertEquals("Revision Requested", item.status);
    }

    // ═══════════════════════════════════════════════
    // isProposal flag — proposals/ vs events/
    // ═══════════════════════════════════════════════

    /**
     * US-01: isProposal=true for items from proposals/ collection (Draft, Submitted, etc.).
     */
    @Test
    public void testIsProposalTrueForProposalItems() {
        OrganizerDashboardActivity.EventItem item =
                new OrganizerDashboardActivity.EventItem(
                        "p1", "Proposal Event", "2025-12-01", "Draft", true);
        assertTrue(item.isProposal);
    }

    /**
     * US-01: isProposal=false for items from events/ collection (Approved/Completed).
     */
    @Test
    public void testIsProposalFalseForApprovedEvents() {
        OrganizerDashboardActivity.EventItem item =
                new OrganizerDashboardActivity.EventItem(
                        "e1", "Approved Event", "2025-12-01", "Approved", false);
        assertFalse(item.isProposal);
    }

    /**
     * US-01: Draft status always has isProposal=true (comes from proposals/).
     */
    @Test
    public void testDraftIsAlwaysProposal() {
        OrganizerDashboardActivity.EventItem item =
                new OrganizerDashboardActivity.EventItem(
                        "d1", "Draft Event", "TBD", "Draft", true);
        assertEquals("Draft", item.status);
        assertTrue(item.isProposal);
    }

    /**
     * US-01: Submitted status has isProposal=true (still in proposals/ collection).
     */
    @Test
    public void testSubmittedIsProposal() {
        OrganizerDashboardActivity.EventItem item =
                new OrganizerDashboardActivity.EventItem(
                        "s1", "Submitted Event", "2025-10-01", "Submitted", true);
        assertTrue(item.isProposal);
    }

    /**
     * US-01: "Revision Requested" status has isProposal=true.
     */
    @Test
    public void testRevisionRequestedIsProposal() {
        OrganizerDashboardActivity.EventItem item =
                new OrganizerDashboardActivity.EventItem(
                        "r1", "Event", "2025-10-01", "Revision Requested", true);
        assertTrue(item.isProposal);
    }

    /**
     * US-01: "Rejected" status has isProposal=true (stays in proposals/).
     */
    @Test
    public void testRejectedIsProposal() {
        OrganizerDashboardActivity.EventItem item =
                new OrganizerDashboardActivity.EventItem(
                        "r2", "Event", "2025-10-01", "Rejected", true);
        assertTrue(item.isProposal);
    }

    /**
     * US-01: "Completed" status has isProposal=false (comes from events/).
     */
    @Test
    public void testCompletedIsNotProposal() {
        OrganizerDashboardActivity.EventItem item =
                new OrganizerDashboardActivity.EventItem(
                        "c1", "Completed Event", "2025-08-01", "Completed", false);
        assertFalse(item.isProposal);
    }

    // ═══════════════════════════════════════════════
    // All 6 valid status values accepted
    // ═══════════════════════════════════════════════

    /**
     * US-01: All six valid status strings are stored without modification.
     */
    @Test
    public void testAllSixStatusValuesStoredCorrectly() {
        String[] statuses = {
                "Draft", "Submitted", "Revision Requested",
                "Rejected", "Approved", "Completed"
        };
        for (String status : statuses) {
            OrganizerDashboardActivity.EventItem item =
                    new OrganizerDashboardActivity.EventItem(
                            "id", "Event", "2025-01-01", status, true);
            assertEquals(status, item.status);
        }
    }

    /**
     * US-01: Fallback "—" date string is accepted (used when date is null in Firestore).
     */
    @Test
    public void testFallbackDashDateStoredCorrectly() {
        OrganizerDashboardActivity.EventItem item =
                new OrganizerDashboardActivity.EventItem(
                        "id", "Event", "—", "Draft", true);
        assertEquals("—", item.date);
    }

    /**
     * US-01: Fallback "Untitled" title is accepted (used when title is null in Firestore).
     */
    @Test
    public void testFallbackUntitledTitleStoredCorrectly() {
        OrganizerDashboardActivity.EventItem item =
                new OrganizerDashboardActivity.EventItem(
                        "id", "Untitled", "—", "Draft", true);
        assertEquals("Untitled", item.title);
    }

    // ═══════════════════════════════════════════════
    // getRelativeTime — timestamp formatting
    // ═══════════════════════════════════════════════

    /**
     * US-01: Timestamp from 0 minutes ago returns "0m ago".
     */
    @Test
    public void testRelativeTimeJustNowReturnsMinutes() {
        long now = System.currentTimeMillis();
        String result = getRelativeTime(now);
        assertTrue(result.endsWith("m ago"));
    }

    /**
     * US-01: Timestamp from 5 minutes ago returns "5m ago".
     */
    @Test
    public void testRelativeTimeFiveMinutesAgo() {
        long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
        String result = getRelativeTime(fiveMinutesAgo);
        assertEquals("5m ago", result);
    }

    /**
     * US-01: Timestamp from 59 minutes ago still returns minutes format.
     */
    @Test
    public void testRelativeTime59MinutesAgoStillMinutes() {
        long fiftyNineMinutesAgo = System.currentTimeMillis() - (59 * 60 * 1000);
        String result = getRelativeTime(fiftyNineMinutesAgo);
        assertTrue(result.endsWith("m ago"));
    }

    /**
     * US-01: Timestamp from exactly 60 minutes ago switches to hours format.
     */
    @Test
    public void testRelativeTime60MinutesAgoSwitchesToHours() {
        long sixtyMinutesAgo = System.currentTimeMillis() - (60 * 60 * 1000);
        String result = getRelativeTime(sixtyMinutesAgo);
        assertEquals("1h ago", result);
    }

    /**
     * US-01: Timestamp from 3 hours ago returns "3h ago".
     */
    @Test
    public void testRelativeTimeThreeHoursAgo() {
        long threeHoursAgo = System.currentTimeMillis() - (3 * 60 * 60 * 1000);
        String result = getRelativeTime(threeHoursAgo);
        assertEquals("3h ago", result);
    }

    /**
     * US-01: Timestamp from 23 hours ago still returns hours format.
     */
    @Test
    public void testRelativeTime23HoursAgoStillHours() {
        long twentyThreeHoursAgo = System.currentTimeMillis() - (23 * 60 * 60 * 1000);
        String result = getRelativeTime(twentyThreeHoursAgo);
        assertTrue(result.endsWith("h ago"));
    }

    /**
     * US-01: Timestamp from exactly 24 hours ago switches to days format.
     */
    @Test
    public void testRelativeTime24HoursAgoSwitchesToDays() {
        long oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        String result = getRelativeTime(oneDayAgo);
        assertEquals("1d ago", result);
    }

    /**
     * US-01: Timestamp from 3 days ago returns "3d ago".
     */
    @Test
    public void testRelativeTimeThreeDaysAgo() {
        long threeDaysAgo = System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000);
        String result = getRelativeTime(threeDaysAgo);
        assertEquals("3d ago", result);
    }

    /**
     * US-01: Timestamp from 7 days ago returns "7d ago".
     */
    @Test
    public void testRelativeTimeOneWeekAgo() {
        long oneWeekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        String result = getRelativeTime(oneWeekAgo);
        assertEquals("7d ago", result);
    }

    /**
     * US-01: Result always ends with "m ago", "h ago", or "d ago" — never null or empty.
     */
    @Test
    public void testRelativeTimeNeverNullOrEmpty() {
        long[] timestamps = {
                System.currentTimeMillis(),
                System.currentTimeMillis() - (30L * 60 * 1000),
                System.currentTimeMillis() - (5L  * 60 * 60 * 1000),
                System.currentTimeMillis() - (2L  * 24 * 60 * 60 * 1000)
        };
        for (long ts : timestamps) {
            String result = getRelativeTime(ts);
            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertTrue(result.endsWith("m ago")
                    || result.endsWith("h ago")
                    || result.endsWith("d ago"));
        }
    }
}