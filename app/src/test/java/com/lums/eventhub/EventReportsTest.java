package com.lums.eventhub;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * EventReportsTest.java
 *
 * Unit tests for EventReportsActivity logic:
 *   - Report status filtering (Submitted vs Pending)
 *   - Stats calculation (total, submitted, pending counts)
 *   - EventReportItem model fields
 *   - Report submission (status flip)
 *   - Notes stored correctly
 *   - Edge cases: empty list, all submitted, none submitted
 */
public class EventReportsTest {

    // ── Mirror model ──────────────────────────────────────────────────────────

    static class EventReportItem {
        String eventId, eventTitle, eventDate, reportStatus, submittedAt, notes;
        int    attendees;

        EventReportItem(String eventId, String eventTitle, String eventDate,
                        String reportStatus, int attendees) {
            this.eventId      = eventId;
            this.eventTitle   = eventTitle;
            this.eventDate    = eventDate;
            this.reportStatus = reportStatus;
            this.attendees    = attendees;
            this.submittedAt  = "";
            this.notes        = "";
        }
    }

    // ── Mirrored logic helpers ────────────────────────────────────────────────

    private int countSubmitted(List<EventReportItem> items) {
        int c = 0;
        for (EventReportItem i : items) if ("Submitted".equals(i.reportStatus)) c++;
        return c;
    }

    private int countPending(List<EventReportItem> items) {
        return items.size() - countSubmitted(items);
    }

    private List<EventReportItem> filterByStatus(List<EventReportItem> items, String status) {
        List<EventReportItem> result = new ArrayList<>();
        for (EventReportItem i : items) if (status.equals(i.reportStatus)) result.add(i);
        return result;
    }

    private void submitReport(EventReportItem item, String notes) {
        item.reportStatus = "Submitted";
        item.notes        = notes;
        item.submittedAt  = "May 1, 2026";
    }

    // ── Sample data ───────────────────────────────────────────────────────────

    private List<EventReportItem> items;

    @Before
    public void setUp() {
        items = new ArrayList<>();
        items.add(new EventReportItem("e1", "SPADES 2025",      "Mar 20, 2026", "Submitted", 312));
        items.add(new EventReportItem("e2", "PSiFi 2026",       "Mar 22, 2026", "Pending",   425));
        items.add(new EventReportItem("e3", "Tech Workshop",    "Mar 18, 2026", "Pending",   89));
        items.add(new EventReportItem("e4", "Alumni Mixer",     "Mar 15, 2026", "Submitted", 156));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Stats
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testTotalEventsCount() {
        assertEquals(4, items.size());
    }

    @Test public void testSubmittedCount() {
        assertEquals(2, countSubmitted(items));
    }

    @Test public void testPendingCount() {
        assertEquals(2, countPending(items));
    }

    @Test public void testSubmittedPlusPendingEqualsTotal() {
        assertEquals(items.size(), countSubmitted(items) + countPending(items));
    }

    @Test public void testAllSubmittedReturnsZeroPending() {
        List<EventReportItem> all = new ArrayList<>();
        all.add(new EventReportItem("e1", "A", "—", "Submitted", 100));
        all.add(new EventReportItem("e2", "B", "—", "Submitted", 200));
        assertEquals(0, countPending(all));
        assertEquals(2, countSubmitted(all));
    }

    @Test public void testNoneSubmittedReturnsZeroSubmitted() {
        List<EventReportItem> all = new ArrayList<>();
        all.add(new EventReportItem("e1", "A", "—", "Pending", 100));
        all.add(new EventReportItem("e2", "B", "—", "Pending", 200));
        assertEquals(0, countSubmitted(all));
        assertEquals(2, countPending(all));
    }

    @Test public void testEmptyListReturnsZeroStats() {
        List<EventReportItem> empty = new ArrayList<>();
        assertEquals(0, countSubmitted(empty));
        assertEquals(0, countPending(empty));
    }

    @Test public void testSingleSubmittedItem() {
        List<EventReportItem> list = new ArrayList<>();
        list.add(new EventReportItem("e1", "Event", "—", "Submitted", 50));
        assertEquals(1, countSubmitted(list));
        assertEquals(0, countPending(list));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Filter by Status
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testFilterSubmittedReturnsOnlySubmitted() {
        List<EventReportItem> submitted = filterByStatus(items, "Submitted");
        assertEquals(2, submitted.size());
        for (EventReportItem i : submitted) assertEquals("Submitted", i.reportStatus);
    }

    @Test public void testFilterPendingReturnsOnlyPending() {
        List<EventReportItem> pending = filterByStatus(items, "Pending");
        assertEquals(2, pending.size());
        for (EventReportItem i : pending) assertEquals("Pending", i.reportStatus);
    }

    @Test public void testFilterNonExistentStatusReturnsEmpty() {
        assertEquals(0, filterByStatus(items, "Rejected").size());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Report Submission
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testSubmitReportChangesStatusToSubmitted() {
        EventReportItem item = items.get(1); // PSiFi 2026 — Pending
        assertEquals("Pending", item.reportStatus);
        submitReport(item, "Great event!");
        assertEquals("Submitted", item.reportStatus);
    }

    @Test public void testSubmitReportStoresNotes() {
        EventReportItem item = items.get(2);
        submitReport(item, "Well attended workshop.");
        assertEquals("Well attended workshop.", item.notes);
    }

    @Test public void testSubmitReportStoresSubmittedAt() {
        EventReportItem item = items.get(1);
        submitReport(item, "Good event");
        assertFalse(item.submittedAt.isEmpty());
    }

    @Test public void testSubmitReportWithEmptyNotesIsAllowed() {
        EventReportItem item = items.get(2);
        submitReport(item, "");
        assertEquals("Submitted", item.reportStatus);
        assertEquals("", item.notes);
    }

    @Test public void testSubmittingIncreasesSubmittedCount() {
        int before = countSubmitted(items);
        submitReport(items.get(1), "Notes");
        assertEquals(before + 1, countSubmitted(items));
    }

    @Test public void testSubmittingDecreasesPendingCount() {
        int before = countPending(items);
        submitReport(items.get(1), "Notes");
        assertEquals(before - 1, countPending(items));
    }

    @Test public void testAlreadySubmittedItemRemainsSubmitted() {
        EventReportItem item = items.get(0); // already Submitted
        int before = countSubmitted(items);
        submitReport(item, "Updated notes");
        assertEquals(before, countSubmitted(items)); // count unchanged
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EventReportItem Model
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testModelFieldsStoredCorrectly() {
        EventReportItem item = items.get(0);
        assertEquals("e1",         item.eventId);
        assertEquals("SPADES 2025", item.eventTitle);
        assertEquals("Mar 20, 2026", item.eventDate);
        assertEquals("Submitted",   item.reportStatus);
        assertEquals(312,           item.attendees);
    }

    @Test public void testAttendeeCountStoredCorrectly() {
        assertEquals(425, items.get(1).attendees);
        assertEquals(89,  items.get(2).attendees);
    }

    @Test public void testInitialNotesAreEmpty() {
        assertTrue(items.get(1).notes.isEmpty());
        assertTrue(items.get(2).notes.isEmpty());
    }

    @Test public void testInitialSubmittedAtIsEmpty() {
        assertTrue(items.get(1).submittedAt.isEmpty());
    }
}