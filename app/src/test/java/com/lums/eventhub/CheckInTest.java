package com.lums.eventhub;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for CheckInActivity logic (US-30)
 *
 * Covers: check-in counting, remaining count, stats updates,
 *         search/filter by name and studentId, payment status,
 *         marking attendees as checked in, edge cases.
 *
 * All logic is extracted from CheckInActivity into plain Java helpers —
 * no Android or Firestore dependencies needed.
 */
public class CheckInTest {

    // ─────────────────────────────────────────────
    // Mirror of CheckInActivity.Attendee
    // ─────────────────────────────────────────────

    static class Attendee {
        String id, name, studentId, paymentStatus;
        boolean isCheckedIn;

        Attendee(String id, String name, String studentId,
                 String paymentStatus, boolean isCheckedIn) {
            this.id            = id;
            this.name          = name;
            this.studentId     = studentId;
            this.paymentStatus = paymentStatus;
            this.isCheckedIn   = isCheckedIn;
        }
    }

    // ─────────────────────────────────────────────
    // Mirror of CheckInActivity.filterList()
    // ─────────────────────────────────────────────

    private List<Attendee> filterList(List<Attendee> attendeeList, String query) {
        List<Attendee> filtered = new ArrayList<>();
        for (Attendee a : attendeeList) {
            if (a.name.toLowerCase().contains(query.toLowerCase()) ||
                    a.studentId.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(a);
            }
        }
        return filtered;
    }

    // ─────────────────────────────────────────────
    // Mirror of checkedInCount calculation
    // ─────────────────────────────────────────────

    private int countCheckedIn(List<Attendee> list) {
        int count = 0;
        for (Attendee a : list) {
            if (a.isCheckedIn) count++;
        }
        return count;
    }

    private int countRemaining(List<Attendee> list) {
        return list.size() - countCheckedIn(list);
    }

    // ─────────────────────────────────────────────
    // Sample data — mirrors addSampleData() exactly
    // ─────────────────────────────────────────────

    private List<Attendee> attendeeList;

    @Before
    public void setUp() {
        attendeeList = new ArrayList<>();
        attendeeList.add(new Attendee("", "Fatima Malik",  "AT0023", "Paid",    true));
        attendeeList.add(new Attendee("", "Hassan Raza",   "AT0041", "Paid",    false));
        attendeeList.add(new Attendee("", "Zainab Ali",    "AT0055", "Paid",    true));
        attendeeList.add(new Attendee("", "Bilal Khan",    "AT0067", "Pending", false));
        attendeeList.add(new Attendee("", "Sara Ahmed",    "AT0078", "Paid",    false));
        attendeeList.add(new Attendee("", "Usman Tariq",   "AT0089", "Paid",    false));
    }

    // ═══════════════════════════════════════════════
    // US-30 — Stats: checkedIn, remaining, total
    // ═══════════════════════════════════════════════

    /**
     * US-30: Total attendee count matches sample data size.
     */
    @Test
    public void testTotalAttendeeCount() {
        assertEquals(6, attendeeList.size());
    }

    /**
     * US-30: Checked-in count is correct from sample data (Fatima + Zainab).
     */
    @Test
    public void testCheckedInCountFromSampleData() {
        assertEquals(2, countCheckedIn(attendeeList));
    }

    /**
     * US-30: Remaining count is total minus checked-in.
     */
    @Test
    public void testRemainingCountFromSampleData() {
        assertEquals(4, countRemaining(attendeeList));
    }

    /**
     * US-30: checkedIn + remaining always equals total.
     */
    @Test
    public void testCheckedInPlusRemainingEqualsTotal() {
        int checkedIn = countCheckedIn(attendeeList);
        int remaining = countRemaining(attendeeList);
        assertEquals(attendeeList.size(), checkedIn + remaining);
    }

    /**
     * US-30: When no one is checked in, checkedIn=0 and remaining=total.
     */
    @Test
    public void testStatsWhenNobodyCheckedIn() {
        List<Attendee> list = new ArrayList<>();
        list.add(new Attendee("1", "Ali",   "AT001", "Paid", false));
        list.add(new Attendee("2", "Sara",  "AT002", "Paid", false));
        list.add(new Attendee("3", "Bilal", "AT003", "Paid", false));

        assertEquals(0, countCheckedIn(list));
        assertEquals(3, countRemaining(list));
    }

    /**
     * US-30: When everyone is checked in, remaining=0.
     */
    @Test
    public void testStatsWhenEveryoneCheckedIn() {
        List<Attendee> list = new ArrayList<>();
        list.add(new Attendee("1", "Ali",  "AT001", "Paid", true));
        list.add(new Attendee("2", "Sara", "AT002", "Paid", true));

        assertEquals(2, countCheckedIn(list));
        assertEquals(0, countRemaining(list));
    }

    /**
     * US-30: Stats with a single attendee not checked in.
     */
    @Test
    public void testStatsWithSingleAttendeeNotCheckedIn() {
        List<Attendee> list = new ArrayList<>();
        list.add(new Attendee("1", "Ali", "AT001", "Paid", false));

        assertEquals(0, countCheckedIn(list));
        assertEquals(1, countRemaining(list));
    }

    /**
     * US-30: Stats with empty list returns zeros.
     */
    @Test
    public void testStatsWithEmptyList() {
        List<Attendee> list = new ArrayList<>();
        assertEquals(0, countCheckedIn(list));
        assertEquals(0, countRemaining(list));
        assertEquals(0, list.size());
    }

    // ═══════════════════════════════════════════════
    // US-30 — Mark attendee as checked in
    // ═══════════════════════════════════════════════

    /**
     * US-30: Marking an attendee checked in flips isCheckedIn to true.
     */
    @Test
    public void testMarkAttendeeCheckedIn() {
        Attendee hassan = attendeeList.get(1); // Hassan — not checked in
        assertFalse(hassan.isCheckedIn);

        hassan.isCheckedIn = true;

        assertTrue(hassan.isCheckedIn);
    }

    /**
     * US-30: After marking one attendee, checkedIn count increases by 1.
     */
    @Test
    public void testCheckedInCountIncreasesAfterCheckIn() {
        int before = countCheckedIn(attendeeList);
        attendeeList.get(1).isCheckedIn = true; // check in Hassan
        int after = countCheckedIn(attendeeList);

        assertEquals(before + 1, after);
    }

    /**
     * US-30: After marking one attendee, remaining count decreases by 1.
     */
    @Test
    public void testRemainingCountDecreasesAfterCheckIn() {
        int before = countRemaining(attendeeList);
        attendeeList.get(1).isCheckedIn = true; // check in Hassan
        int after = countRemaining(attendeeList);

        assertEquals(before - 1, after);
    }

    /**
     * US-30: Checking in all remaining attendees makes remaining = 0.
     */
    @Test
    public void testCheckingInAllMakesRemainingZero() {
        for (Attendee a : attendeeList) {
            a.isCheckedIn = true;
        }
        assertEquals(0, countRemaining(attendeeList));
        assertEquals(attendeeList.size(), countCheckedIn(attendeeList));
    }

    /**
     * US-30: Already checked-in attendee stays checked in (no double toggle).
     */
    @Test
    public void testAlreadyCheckedInAttendeeRemainsCheckedIn() {
        Attendee fatima = attendeeList.get(0); // already checked in
        assertTrue(fatima.isCheckedIn);

        fatima.isCheckedIn = true; // set again — should still be true
        assertTrue(fatima.isCheckedIn);

        assertEquals(2, countCheckedIn(attendeeList)); // count unchanged
    }

    /**
     * US-30: Checking in multiple attendees one by one updates count correctly.
     */
    @Test
    public void testSequentialCheckInsUpdateCountCorrectly() {
        // Start: 2 checked in
        assertEquals(2, countCheckedIn(attendeeList));

        attendeeList.get(1).isCheckedIn = true; // Hassan
        assertEquals(3, countCheckedIn(attendeeList));

        attendeeList.get(3).isCheckedIn = true; // Bilal
        assertEquals(4, countCheckedIn(attendeeList));

        attendeeList.get(4).isCheckedIn = true; // Sara
        assertEquals(5, countCheckedIn(attendeeList));
    }

    // ═══════════════════════════════════════════════
    // US-30 — Search / filter by name
    // ═══════════════════════════════════════════════

    /**
     * US-30: Empty search query returns all attendees.
     */
    @Test
    public void testEmptySearchReturnsAll() {
        List<Attendee> result = filterList(attendeeList, "");
        assertEquals(attendeeList.size(), result.size());
    }

    /**
     * US-30: Search by exact first name (case-insensitive) returns correct attendee.
     */
    @Test
    public void testSearchByFirstNameCaseInsensitive() {
        List<Attendee> result = filterList(attendeeList, "fatima");
        assertEquals(1, result.size());
        assertEquals("Fatima Malik", result.get(0).name);
    }

    /**
     * US-30: Search by last name returns correct attendee.
     */
    @Test
    public void testSearchByLastName() {
        List<Attendee> result = filterList(attendeeList, "malik");
        assertEquals(1, result.size());
        assertEquals("Fatima Malik", result.get(0).name);
    }

    /**
     * US-30: Search with uppercase query still matches (case-insensitive).
     */
    @Test
    public void testSearchCaseInsensitiveUppercase() {
        List<Attendee> result = filterList(attendeeList, "HASSAN");
        assertEquals(1, result.size());
        assertEquals("Hassan Raza", result.get(0).name);
    }

    /**
     * US-30: Search with mixed case query matches correctly.
     */
    @Test
    public void testSearchCaseInsensitiveMixedCase() {
        List<Attendee> result = filterList(attendeeList, "ZaInAb");
        assertEquals(1, result.size());
        assertEquals("Zainab Ali", result.get(0).name);
    }

    /**
     * US-30: Search query that matches no attendee returns empty list.
     */
    @Test
    public void testSearchNoMatchReturnsEmpty() {
        List<Attendee> result = filterList(attendeeList, "xyz_no_match");
        assertEquals(0, result.size());
    }

    /**
     * US-30: Search by partial name returns correct attendee.
     */
    @Test
    public void testSearchByPartialName() {
        List<Attendee> result = filterList(attendeeList, "zai");
        assertEquals(1, result.size());
        assertEquals("Zainab Ali", result.get(0).name);
    }

    /**
     * US-30: Search query matching multiple names returns all matches.
     */
    @Test
    public void testSearchMatchingMultipleNames() {
        // "a" appears in Fatima, Zainab, Sara, Hassan, Bilal, Usman — all 6
        List<Attendee> result = filterList(attendeeList, "a");
        assertEquals(6, result.size());
    }

    // ═══════════════════════════════════════════════
    // US-30 — Search / filter by student ID
    // ═══════════════════════════════════════════════

    /**
     * US-30: Search by exact student ID returns correct attendee.
     */
    @Test
    public void testSearchByExactStudentId() {
        List<Attendee> result = filterList(attendeeList, "AT0041");
        assertEquals(1, result.size());
        assertEquals("Hassan Raza", result.get(0).name);
    }

    /**
     * US-30: Search by partial student ID returns correct attendee.
     */
    @Test
    public void testSearchByPartialStudentId() {
        List<Attendee> result = filterList(attendeeList, "AT0067");
        assertEquals(1, result.size());
        assertEquals("Bilal Khan", result.get(0).name);
    }

    /**
     * US-30: Student ID search is case-insensitive.
     */
    @Test
    public void testSearchByStudentIdCaseInsensitive() {
        List<Attendee> result = filterList(attendeeList, "at0023");
        assertEquals(1, result.size());
        assertEquals("Fatima Malik", result.get(0).name);
    }

    /**
     * US-30: Search by shared prefix "AT00" matches all attendees.
     */
    @Test
    public void testSearchBySharedStudentIdPrefixMatchesAll() {
        List<Attendee> result = filterList(attendeeList, "AT00");
        assertEquals(6, result.size());
    }

    /**
     * US-30: Search by non-existent student ID returns empty list.
     */
    @Test
    public void testSearchByNonExistentStudentIdReturnsEmpty() {
        List<Attendee> result = filterList(attendeeList, "AT9999");
        assertEquals(0, result.size());
    }

    // ═══════════════════════════════════════════════
    // US-30 — Payment status
    // ═══════════════════════════════════════════════

    /**
     * US-30: Payment status "Paid" is stored correctly.
     */
    @Test
    public void testPaymentStatusPaidStoredCorrectly() {
        Attendee fatima = attendeeList.get(0);
        assertEquals("Paid", fatima.paymentStatus);
    }

    /**
     * US-30: Payment status "Pending" is stored correctly.
     */
    @Test
    public void testPaymentStatusPendingStoredCorrectly() {
        Attendee bilal = attendeeList.get(3);
        assertEquals("Pending", bilal.paymentStatus);
    }

    /**
     * US-30: Correct count of Paid attendees in sample data.
     */
    @Test
    public void testPaidAttendeeCount() {
        int paidCount = 0;
        for (Attendee a : attendeeList) {
            if (a.paymentStatus.equals("Paid")) paidCount++;
        }
        assertEquals(5, paidCount);
    }

    /**
     * US-30: Correct count of Pending attendees in sample data.
     */
    @Test
    public void testPendingAttendeeCount() {
        int pendingCount = 0;
        for (Attendee a : attendeeList) {
            if (a.paymentStatus.equals("Pending")) pendingCount++;
        }
        assertEquals(1, pendingCount);
    }

    /**
     * US-30: Paid + Pending counts add up to total.
     */
    @Test
    public void testPaidPlusPendingEqualsTotal() {
        int paid = 0, pending = 0;
        for (Attendee a : attendeeList) {
            if (a.paymentStatus.equals("Paid"))    paid++;
            if (a.paymentStatus.equals("Pending")) pending++;
        }
        assertEquals(attendeeList.size(), paid + pending);
    }

    /**
     * US-30: Pending attendee can still be checked in regardless of payment.
     */
    @Test
    public void testPendingAttendeeCanBeCheckedIn() {
        Attendee bilal = attendeeList.get(3);
        assertEquals("Pending", bilal.paymentStatus);
        assertFalse(bilal.isCheckedIn);

        bilal.isCheckedIn = true;
        assertTrue(bilal.isCheckedIn);
    }

    // ═══════════════════════════════════════════════
    // US-30 — Attendee model fields
    // ═══════════════════════════════════════════════

    /**
     * US-30: Attendee constructor stores all fields correctly.
     */
    @Test
    public void testAttendeeConstructorStoresAllFields() {
        Attendee a = new Attendee("doc123", "Ali Hassan", "AT0099", "Paid", false);

        assertEquals("doc123",    a.id);
        assertEquals("Ali Hassan", a.name);
        assertEquals("AT0099",    a.studentId);
        assertEquals("Paid",      a.paymentStatus);
        assertFalse(a.isCheckedIn);
    }

    /**
     * US-30: Attendee with empty id string (not yet in Firestore) is valid.
     */
    @Test
    public void testAttendeeWithEmptyIdIsValid() {
        Attendee a = new Attendee("", "Test User", "AT0001", "Paid", false);
        assertNotNull(a);
        assertTrue(a.id.isEmpty());
    }

    /**
     * US-30: isCheckedIn defaults to false for new unchecked attendee.
     */
    @Test
    public void testNewAttendeeDefaultNotCheckedIn() {
        Attendee a = new Attendee("1", "New User", "AT0100", "Paid", false);
        assertFalse(a.isCheckedIn);
    }
}