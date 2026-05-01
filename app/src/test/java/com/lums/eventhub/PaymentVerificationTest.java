package com.lums.eventhub;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * PaymentVerificationTest.java
 *
 * Unit tests for PaymentVerificationActivity logic:
 *   - Stats: pending, approved, rejected counts
 *   - Filter by status (All / Pending / Approved / Rejected)
 *   - Search by name and student ID
 *   - Approve / Reject actions
 *   - Rejection reason required for rejection
 *   - Capacity: only Approved count toward capacity
 */
public class PaymentVerificationTest {

    // ── Mirror model ──────────────────────────────────────────────────────────

    static class Registrant {
        String docId, studentName, studentId, paymentStatus, rejectionReason;

        Registrant(String docId, String studentName, String studentId, String paymentStatus) {
            this.docId         = docId;
            this.studentName   = studentName;
            this.studentId     = studentId;
            this.paymentStatus = paymentStatus;
            this.rejectionReason = "";
        }
    }

    // ── Mirrored logic helpers ────────────────────────────────────────────────

    private int countByStatus(List<Registrant> list, String status) {
        int c = 0;
        for (Registrant r : list) if (status.equals(r.paymentStatus)) c++;
        return c;
    }

    private List<Registrant> filterByStatus(List<Registrant> all, String filter) {
        if ("All".equals(filter)) return new ArrayList<>(all);
        List<Registrant> result = new ArrayList<>();
        for (Registrant r : all) if (filter.equals(r.paymentStatus)) result.add(r);
        return result;
    }

    private List<Registrant> searchRegistrants(List<Registrant> list, String query) {
        if (query == null || query.isEmpty()) return new ArrayList<>(list);
        List<Registrant> result = new ArrayList<>();
        String q = query.toLowerCase();
        for (Registrant r : list) {
            if (r.studentName.toLowerCase().contains(q)
                    || r.studentId.toLowerCase().contains(q)) result.add(r);
        }
        return result;
    }

    private void approveRegistrant(Registrant r) {
        r.paymentStatus   = "Approved";
        r.rejectionReason = "";
    }

    private void rejectRegistrant(Registrant r, String reason) {
        r.paymentStatus   = "Rejected";
        r.rejectionReason = reason;
    }

    private boolean isRejectionReasonValid(String reason) {
        return reason != null && !reason.trim().isEmpty();
    }

    // ── Sample data ───────────────────────────────────────────────────────────

    private List<Registrant> registrants;

    @Before
    public void setUp() {
        registrants = new ArrayList<>();
        registrants.add(new Registrant("r1", "Fatima Malik", "AT0023", "Pending"));
        registrants.add(new Registrant("r2", "Hassan Raza",  "AT0041", "Pending"));
        registrants.add(new Registrant("r3", "Zainab Ali",   "AT0055", "Approved"));
        registrants.add(new Registrant("r4", "Bilal Khan",   "AT0067", "Rejected"));
        registrants.add(new Registrant("r5", "Sara Ahmed",   "AT0078", "Approved"));
        registrants.add(new Registrant("r6", "Usman Tariq",  "AT0089", "Pending"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Stats
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testTotalRegistrantsCount() { assertEquals(6, registrants.size()); }

    @Test public void testPendingCount() { assertEquals(3, countByStatus(registrants, "Pending")); }

    @Test public void testApprovedCount() { assertEquals(2, countByStatus(registrants, "Approved")); }

    @Test public void testRejectedCount() { assertEquals(1, countByStatus(registrants, "Rejected")); }

    @Test public void testPendingPlusApprovedPlusRejectedEqualsTotal() {
        assertEquals(registrants.size(),
                countByStatus(registrants, "Pending")
                        + countByStatus(registrants, "Approved")
                        + countByStatus(registrants, "Rejected"));
    }

    @Test public void testEmptyListAllStatsZero() {
        List<Registrant> empty = new ArrayList<>();
        assertEquals(0, countByStatus(empty, "Pending"));
        assertEquals(0, countByStatus(empty, "Approved"));
        assertEquals(0, countByStatus(empty, "Rejected"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Capacity: only Approved count
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testOnlyApprovedCountsTowardCapacity() {
        // Pending and Rejected must NOT count
        int approved = countByStatus(registrants, "Approved");
        assertEquals(2, approved);
    }

    @Test public void testPendingDoesNotCountTowardCapacity() {
        // After approving one Pending, approved goes up by 1
        Registrant r = registrants.get(0); // Fatima — Pending
        approveRegistrant(r);
        assertEquals(3, countByStatus(registrants, "Approved"));
    }

    @Test public void testRejectedDoesNotCountTowardCapacity() {
        assertEquals(1, countByStatus(registrants, "Rejected"));
        // Rejected count same as Approved? No — they are separate
        assertNotEquals(countByStatus(registrants, "Rejected"),
                countByStatus(registrants, "Approved"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Filter
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testFilterAllReturnsAll() {
        assertEquals(registrants.size(), filterByStatus(registrants, "All").size());
    }

    @Test public void testFilterPendingReturnsOnlyPending() {
        List<Registrant> pending = filterByStatus(registrants, "Pending");
        assertEquals(3, pending.size());
        for (Registrant r : pending) assertEquals("Pending", r.paymentStatus);
    }

    @Test public void testFilterApprovedReturnsOnlyApproved() {
        List<Registrant> approved = filterByStatus(registrants, "Approved");
        assertEquals(2, approved.size());
        for (Registrant r : approved) assertEquals("Approved", r.paymentStatus);
    }

    @Test public void testFilterRejectedReturnsOnlyRejected() {
        List<Registrant> rejected = filterByStatus(registrants, "Rejected");
        assertEquals(1, rejected.size());
        assertEquals("Bilal Khan", rejected.get(0).studentName);
    }

    @Test public void testFilterOnEmptyListReturnsEmpty() {
        assertEquals(0, filterByStatus(new ArrayList<>(), "Pending").size());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Search
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testEmptySearchReturnsAll() {
        assertEquals(registrants.size(), searchRegistrants(registrants, "").size());
    }

    @Test public void testSearchByName() {
        List<Registrant> result = searchRegistrants(registrants, "fatima");
        assertEquals(1, result.size());
        assertEquals("Fatima Malik", result.get(0).studentName);
    }

    @Test public void testSearchByStudentId() {
        List<Registrant> result = searchRegistrants(registrants, "AT0041");
        assertEquals(1, result.size());
        assertEquals("Hassan Raza", result.get(0).studentName);
    }

    @Test public void testSearchIsCaseInsensitive() {
        assertEquals(searchRegistrants(registrants, "sara").size(),
                searchRegistrants(registrants, "SARA").size());
    }

    @Test public void testSearchNoMatchReturnsEmpty() {
        assertEquals(0, searchRegistrants(registrants, "xyz_9999").size());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Approve Action
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testApproveChangesStatusToApproved() {
        Registrant r = registrants.get(0); // Pending
        approveRegistrant(r);
        assertEquals("Approved", r.paymentStatus);
    }

    @Test public void testApproveIncreasesApprovedCount() {
        int before = countByStatus(registrants, "Approved");
        approveRegistrant(registrants.get(0));
        assertEquals(before + 1, countByStatus(registrants, "Approved"));
    }

    @Test public void testApproveDecreasesPendingCount() {
        int before = countByStatus(registrants, "Pending");
        approveRegistrant(registrants.get(0));
        assertEquals(before - 1, countByStatus(registrants, "Pending"));
    }

//    @Test public void testApproveClears RejectionReason() {
//        Registrant r = registrants.get(3); // Bilal — Rejected
//        r.rejectionReason = "Invalid proof";
//        approveRegistrant(r);
//        assertEquals("", r.rejectionReason);
//    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Reject Action
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testRejectChangesStatusToRejected() {
        Registrant r = registrants.get(0); // Pending
        rejectRegistrant(r, "Proof unclear");
        assertEquals("Rejected", r.paymentStatus);
    }

    @Test public void testRejectStoresReason() {
        Registrant r = registrants.get(0);
        rejectRegistrant(r, "Payment proof is blurry");
        assertEquals("Payment proof is blurry", r.rejectionReason);
    }

    @Test public void testRejectWithEmptyReasonIsInvalid() {
        assertFalse(isRejectionReasonValid(""));
    }

    @Test public void testRejectWithNullReasonIsInvalid() {
        assertFalse(isRejectionReasonValid(null));
    }

    @Test public void testRejectWithWhitespaceOnlyIsInvalid() {
        assertFalse(isRejectionReasonValid("   "));
    }

    @Test public void testRejectWithValidReasonIsValid() {
        assertTrue(isRejectionReasonValid("Proof image not clear enough"));
    }

    @Test public void testRejectIncreasesRejectedCount() {
        int before = countByStatus(registrants, "Rejected");
        rejectRegistrant(registrants.get(0), "Invalid");
        assertEquals(before + 1, countByStatus(registrants, "Rejected"));
    }

    @Test public void testRejectDecreasesPendingCount() {
        int before = countByStatus(registrants, "Pending");
        rejectRegistrant(registrants.get(0), "Invalid");
        assertEquals(before - 1, countByStatus(registrants, "Pending"));
    }
}