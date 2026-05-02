package com.lums.eventhub;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminEventReportsTest.java
 *
 * Unit tests for AdminEventReportsActivity logic:
 *   - Report grouping by society
 *   - Approve logic (status update)
 *   - Reject logic (status + reason)
 *   - Rejection reason stored correctly
 *   - Empty state handling
 *   - Society count
 *   - Reports removed from list after review
 */
public class AdminEventReportsTest {

    // ── Mirror model ──────────────────────────────────────────────────────────

    static class ReportDoc {
        String id, eventTitle, societyName, eventDate;
        String imageBase64, notes, submittedAt, status, rejectionReason;
        int attendees;

        ReportDoc(String id, String eventTitle, String societyName, String status) {
            this.id              = id;
            this.eventTitle      = eventTitle;
            this.societyName     = societyName;
            this.status          = status;
            this.rejectionReason = "";
            this.attendees       = 100;
            this.eventDate       = "May 1, 2026";
            this.submittedAt     = "May 2, 2026";
            this.notes           = "";
            this.imageBase64     = "";
        }
    }

    // ── Mirror logic helpers ──────────────────────────────────────────────────

    private Map<String, List<ReportDoc>> groupBySociety(List<ReportDoc> reports) {
        Map<String, List<ReportDoc>> grouped = new LinkedHashMap<>();
        for (ReportDoc r : reports) {
            if (!grouped.containsKey(r.societyName)) {
                grouped.put(r.societyName, new ArrayList<>());
            }
            grouped.get(r.societyName).add(r);
        }
        return grouped;
    }

    private void approveReport(ReportDoc r, List<ReportDoc> allReports) {
        r.status          = "Approved";
        r.rejectionReason = "";
        allReports.removeIf(rep -> rep.id.equals(r.id));
    }

    private void rejectReport(ReportDoc r, String reason, List<ReportDoc> allReports) {
        r.status          = "Rejected";
        r.rejectionReason = reason;
        allReports.removeIf(rep -> rep.id.equals(r.id));
    }

    // ── Sample data ───────────────────────────────────────────────────────────

    private List<ReportDoc> reports;

    @Before
    public void setUp() {
        reports = new ArrayList<>();
        reports.add(new ReportDoc("r1", "SPADES Annual Gala",  "Spades",  "Submitted"));
        reports.add(new ReportDoc("r2", "Science Convention",  "Spades",  "Submitted"));
        reports.add(new ReportDoc("r3", "Tech Summit",         "IEEE",    "Submitted"));
        reports.add(new ReportDoc("r4", "Drama Night",         "Dramatics","Submitted"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Grouping by society
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    public void testReportsGroupedBySociety() {
        Map<String, List<ReportDoc>> grouped = groupBySociety(reports);
        assertEquals(3, grouped.size()); // Spades, IEEE, Dramatics
    }

    @Test
    public void testSpadesHasTwoReports() {
        Map<String, List<ReportDoc>> grouped = groupBySociety(reports);
        assertEquals(2, grouped.get("Spades").size());
    }

    @Test
    public void testIEEEHasOneReport() {
        Map<String, List<ReportDoc>> grouped = groupBySociety(reports);
        assertEquals(1, grouped.get("IEEE").size());
    }

    @Test
    public void testEmptyReportsListProducesEmptyGrouping() {
        Map<String, List<ReportDoc>> grouped = groupBySociety(new ArrayList<>());
        assertTrue(grouped.isEmpty());
    }

    @Test
    public void testSingleReportGroupedCorrectly() {
        List<ReportDoc> single = new ArrayList<>();
        single.add(new ReportDoc("r1", "Event A", "SocietyX", "Submitted"));
        Map<String, List<ReportDoc>> grouped = groupBySociety(single);
        assertEquals(1, grouped.size());
        assertTrue(grouped.containsKey("SocietyX"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Approve
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    public void testApproveChangesStatusToApproved() {
        ReportDoc r = reports.get(0);
        approveReport(r, reports);
        assertEquals("Approved", r.status);
    }

    @Test
    public void testApproveRemovesReportFromList() {
        int before = reports.size();
        approveReport(reports.get(0), reports);
        assertEquals(before - 1, reports.size());
    }

    @Test
    public void testApproveDoesNotAffectOtherReports() {
        ReportDoc target = reports.get(0);
        approveReport(target, reports);
        for (ReportDoc r : reports) {
            assertNotEquals(target.id, r.id);
            assertEquals("Submitted", r.status);
        }
    }

    @Test
    public void testApproveClsarsRejectionReason() {
        ReportDoc r = reports.get(0);
        r.rejectionReason = "Some old reason";
        approveReport(r, reports);
        assertEquals("", r.rejectionReason);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Reject
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    public void testRejectChangesStatusToRejected() {
        ReportDoc r = reports.get(0);
        rejectReport(r, "Incomplete data", reports);
        assertEquals("Rejected", r.status);
    }

    @Test
    public void testRejectStoresReason() {
        ReportDoc r = reports.get(0);
        rejectReport(r, "Missing attendance proof", reports);
        assertEquals("Missing attendance proof", r.rejectionReason);
    }

    @Test
    public void testRejectRemovesReportFromList() {
        int before = reports.size();
        rejectReport(reports.get(0), "Bad report", reports);
        assertEquals(before - 1, reports.size());
    }

    @Test
    public void testRejectWithEmptyReasonStillSetsStatus() {
        ReportDoc r = reports.get(1);
        rejectReport(r, "", reports);
        assertEquals("Rejected", r.status);
        assertEquals("", r.rejectionReason);
    }

    @Test
    public void testRejectDoesNotAffectOtherReports() {
        ReportDoc target = reports.get(0);
        rejectReport(target, "reason", reports);
        for (ReportDoc r : reports) {
            assertNotEquals(target.id, r.id);
            assertEquals("Submitted", r.status);
        }
    }

    @Test
    public void testRejectReasonIsVisibleToOrganizer() {
        ReportDoc r = reports.get(0);
        String reason = "Time collision with another event";
        rejectReport(r, reason, reports);
        assertFalse(r.rejectionReason.isEmpty());
        assertEquals(reason, r.rejectionReason);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Status logic
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    public void testOnlySubmittedReportsShownToAdmin() {
        reports.add(new ReportDoc("r5", "Old Event", "Spades", "Approved"));
        reports.add(new ReportDoc("r6", "Old Event 2", "IEEE", "Rejected"));
        List<ReportDoc> submitted = new ArrayList<>();
        for (ReportDoc r : reports) if ("Submitted".equals(r.status)) submitted.add(r);
        assertEquals(4, submitted.size()); // original 4 only
    }

    @Test
    public void testReportStatusIsNotNullAfterApproval() {
        ReportDoc r = reports.get(0);
        approveReport(r, reports);
        assertNotNull(r.status);
    }

    @Test
    public void testReportStatusIsNotNullAfterRejection() {
        ReportDoc r = reports.get(0);
        rejectReport(r, "reason", reports);
        assertNotNull(r.status);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Model fields
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    public void testReportModelFieldsStoredCorrectly() {
        ReportDoc r = reports.get(0);
        assertEquals("r1",               r.id);
        assertEquals("SPADES Annual Gala", r.eventTitle);
        assertEquals("Spades",           r.societyName);
        assertEquals("Submitted",        r.status);
        assertEquals("",                 r.rejectionReason);
    }

    @Test
    public void testInitialRejectionReasonIsEmpty() {
        for (ReportDoc r : reports) {
            assertEquals("", r.rejectionReason);
        }
    }
}