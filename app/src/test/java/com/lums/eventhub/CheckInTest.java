package com.lums.eventhub;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * CheckInTest.java
 *
 * Unit tests for the two-step check-in flow:
 *   Step 1 — CheckInActivity:             shows Approved events for organiser
 *   Step 2 — CheckInParticipantsActivity: shows Approved-payment attendees only
 *
 * All logic mirrored from activity classes — no Android/Firestore dependencies.
 */
public class CheckInTest {

    // ── Mirror models ─────────────────────────────────────────────────────────

    static class EventItem {
        String id, title, status, venue, date;
        EventItem(String id, String title, String status, String venue, String date) {
            this.id = id; this.title = title; this.status = status;
            this.venue = venue; this.date = date;
        }
    }

    static class Attendee {
        String  id, name, studentId, paymentStatus, checkedInAt;
        boolean checkedIn;
        Attendee(String id, String name, String studentId,
                 String paymentStatus, boolean checkedIn) {
            this.id = id; this.name = name; this.studentId = studentId;
            this.paymentStatus = paymentStatus; this.checkedIn = checkedIn;
            this.checkedInAt = "";
        }
    }

    // ── Mirrored logic helpers ────────────────────────────────────────────────

    private List<EventItem> filterApprovedEvents(List<EventItem> all) {
        List<EventItem> result = new ArrayList<>();
        for (EventItem e : all) if ("Approved".equals(e.status)) result.add(e);
        return result;
    }

    private List<Attendee> filterApprovedAttendees(List<Attendee> all) {
        List<Attendee> result = new ArrayList<>();
        for (Attendee a : all) if ("Approved".equals(a.paymentStatus)) result.add(a);
        return result;
    }

    private List<Attendee> searchAttendees(List<Attendee> list, String query) {
        if (query == null || query.isEmpty()) return new ArrayList<>(list);
        List<Attendee> result = new ArrayList<>();
        String q = query.toLowerCase();
        for (Attendee a : list) {
            if (a.name.toLowerCase().contains(q) || a.studentId.toLowerCase().contains(q))
                result.add(a);
        }
        return result;
    }

    private int countCheckedIn(List<Attendee> list) {
        int c = 0; for (Attendee a : list) if (a.checkedIn) c++; return c;
    }
    private int countRemaining(List<Attendee> list) { return list.size() - countCheckedIn(list); }
    private int calcProgress(int checkedIn, int total) {
        return total > 0 ? (checkedIn * 100) / total : 0;
    }
    private String nvl(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }

    // ── Sample data ───────────────────────────────────────────────────────────

    private List<EventItem> allEvents;
    private List<Attendee>  allAttendees;

    @Before
    public void setUp() {
        allEvents = new ArrayList<>();
        allEvents.add(new EventItem("e1", "SPADES 2025",      "Approved",  "AH Auditorium", "Mar 20, 2026"));
        allEvents.add(new EventItem("e2", "PSiFi 2026",       "Approved",  "CS Lawn",       "Mar 22, 2026"));
        allEvents.add(new EventItem("e3", "Tech Workshop",    "Submitted", "SSE Building",  "Apr 1, 2026"));
        allEvents.add(new EventItem("e4", "Alumni Mixer",     "Draft",     "Faculty Club",  "Apr 5, 2026"));
        allEvents.add(new EventItem("e5", "Networking Night", "Rejected",  "—",             "—"));

        allAttendees = new ArrayList<>();
        allAttendees.add(new Attendee("r1", "Fatima Malik", "AT0023", "Approved", true));
        allAttendees.add(new Attendee("r2", "Hassan Raza",  "AT0041", "Approved", false));
        allAttendees.add(new Attendee("r3", "Zainab Ali",   "AT0055", "Approved", true));
        allAttendees.add(new Attendee("r4", "Bilal Khan",   "AT0067", "Pending",  false));
        allAttendees.add(new Attendee("r5", "Sara Ahmed",   "AT0078", "Approved", false));
        allAttendees.add(new Attendee("r6", "Usman Tariq",  "AT0089", "Pending",  false));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STEP 1 — CheckInActivity: Event list
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testOnlyApprovedEventsShown() {
        assertEquals(2, filterApprovedEvents(allEvents).size());
    }

    @Test public void testSubmittedEventsNotShown() {
        for (EventItem e : filterApprovedEvents(allEvents))
            assertNotEquals("Submitted", e.status);
    }

    @Test public void testDraftEventsNotShown() {
        for (EventItem e : filterApprovedEvents(allEvents))
            assertNotEquals("Draft", e.status);
    }

    @Test public void testRejectedEventsNotShown() {
        for (EventItem e : filterApprovedEvents(allEvents))
            assertNotEquals("Rejected", e.status);
    }

    @Test public void testAllShownEventsAreApproved() {
        for (EventItem e : filterApprovedEvents(allEvents))
            assertEquals("Approved", e.status);
    }

    @Test public void testApprovedEventTitlesCorrect() {
        List<EventItem> approved = filterApprovedEvents(allEvents);
        assertEquals("SPADES 2025", approved.get(0).title);
        assertEquals("PSiFi 2026",  approved.get(1).title);
    }

    @Test public void testEmptyEventListReturnsNoApproved() {
        assertEquals(0, filterApprovedEvents(new ArrayList<>()).size());
    }

    @Test public void testAllSubmittedReturnsNoApproved() {
        List<EventItem> events = new ArrayList<>();
        events.add(new EventItem("x1", "A", "Submitted", "—", "—"));
        events.add(new EventItem("x2", "B", "Submitted", "—", "—"));
        assertEquals(0, filterApprovedEvents(events).size());
    }

    @Test public void testEventVenueStoredCorrectly() {
        List<EventItem> approved = filterApprovedEvents(allEvents);
        assertEquals("AH Auditorium", approved.get(0).venue);
    }

    @Test public void testEventDateStoredCorrectly() {
        List<EventItem> approved = filterApprovedEvents(allEvents);
        assertEquals("Mar 20, 2026", approved.get(0).date);
    }

    @Test public void testSingleApprovedEventShown() {
        List<EventItem> events = new ArrayList<>();
        events.add(new EventItem("e1", "Only Event", "Approved", "Room 101", "May 1, 2026"));
        assertEquals(1, filterApprovedEvents(events).size());
    }

    @Test public void testEventIdStoredCorrectly() {
        List<EventItem> approved = filterApprovedEvents(allEvents);
        assertEquals("e1", approved.get(0).id);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STEP 2 — Attendee filtering (paymentStatus == Approved only)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testOnlyApprovedPaymentAttendeesShown() {
        assertEquals(4, filterApprovedAttendees(allAttendees).size());
    }

    @Test public void testPendingPaymentAttendeesNotShown() {
        for (Attendee a : filterApprovedAttendees(allAttendees))
            assertNotEquals("Pending", a.paymentStatus);
    }

    @Test public void testAllShownAttendeesHaveApprovedPayment() {
        for (Attendee a : filterApprovedAttendees(allAttendees))
            assertEquals("Approved", a.paymentStatus);
    }

    @Test public void testEmptyAttendeeListReturnsEmpty() {
        assertEquals(0, filterApprovedAttendees(new ArrayList<>()).size());
    }

    @Test public void testAllPendingAttendeesReturnsEmpty() {
        List<Attendee> list = new ArrayList<>();
        list.add(new Attendee("1", "Ali",  "AT001", "Pending", false));
        list.add(new Attendee("2", "Sara", "AT002", "Pending", false));
        assertEquals(0, filterApprovedAttendees(list).size());
    }

    @Test public void testApprovedAttendeeNamesCorrect() {
        List<Attendee> approved = filterApprovedAttendees(allAttendees);
        assertEquals("Fatima Malik", approved.get(0).name);
        assertEquals("Hassan Raza",  approved.get(1).name);
        assertEquals("Zainab Ali",   approved.get(2).name);
        assertEquals("Sara Ahmed",   approved.get(3).name);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STEP 2 — Search / Filter
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testEmptySearchReturnsAll() {
        List<Attendee> approved = filterApprovedAttendees(allAttendees);
        assertEquals(approved.size(), searchAttendees(approved, "").size());
    }

    @Test public void testSearchByFirstName() {
        List<Attendee> approved = filterApprovedAttendees(allAttendees);
        List<Attendee> result   = searchAttendees(approved, "fatima");
        assertEquals(1, result.size());
        assertEquals("Fatima Malik", result.get(0).name);
    }

    @Test public void testSearchByLastName() {
        List<Attendee> approved = filterApprovedAttendees(allAttendees);
        List<Attendee> result   = searchAttendees(approved, "raza");
        assertEquals(1, result.size());
        assertEquals("Hassan Raza", result.get(0).name);
    }

    @Test public void testSearchIsCaseInsensitive() {
        List<Attendee> approved = filterApprovedAttendees(allAttendees);
        assertEquals(searchAttendees(approved, "ZAINAB").size(),
                searchAttendees(approved, "zainab").size());
    }

    @Test public void testSearchByExactStudentId() {
        List<Attendee> approved = filterApprovedAttendees(allAttendees);
        List<Attendee> result   = searchAttendees(approved, "AT0023");
        assertEquals(1, result.size());
        assertEquals("Fatima Malik", result.get(0).name);
    }

    @Test public void testSearchByPartialStudentId() {
        List<Attendee> approved = filterApprovedAttendees(allAttendees);
        List<Attendee> result   = searchAttendees(approved, "0041");
        assertEquals(1, result.size());
        assertEquals("Hassan Raza", result.get(0).name);
    }

    @Test public void testSearchNoMatchReturnsEmpty() {
        List<Attendee> approved = filterApprovedAttendees(allAttendees);
        assertEquals(0, searchAttendees(approved, "xyz_no_match_9999").size());
    }

    @Test public void testSearchNullQueryReturnsAll() {
        List<Attendee> approved = filterApprovedAttendees(allAttendees);
        assertEquals(approved.size(), searchAttendees(approved, null).size());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STEP 2 — Stats and Progress
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testCheckedInCountFromApprovedList() {
        // Fatima (r1) and Zainab (r3) are checked in
        assertEquals(2, countCheckedIn(filterApprovedAttendees(allAttendees)));
    }

    @Test public void testRemainingCountFromApprovedList() {
        assertEquals(2, countRemaining(filterApprovedAttendees(allAttendees)));
    }

    @Test public void testCheckedInPlusRemainingEqualsTotal() {
        List<Attendee> approved = filterApprovedAttendees(allAttendees);
        assertEquals(approved.size(), countCheckedIn(approved) + countRemaining(approved));
    }

    @Test public void testProgressZeroWhenNoneCheckedIn() {
        assertEquals(0, calcProgress(0, 10));
    }

    @Test public void testProgressHundredWhenAllCheckedIn() {
        assertEquals(100, calcProgress(10, 10));
    }

    @Test public void testProgressFiftyPercent() {
        assertEquals(50, calcProgress(5, 10));
    }

    @Test public void testProgressZeroForEmptyList() {
        assertEquals(0, calcProgress(0, 0));
    }

    @Test public void testStatsWhenNobodyCheckedIn() {
        List<Attendee> list = new ArrayList<>();
        list.add(new Attendee("1", "Ali",  "AT001", "Approved", false));
        list.add(new Attendee("2", "Sara", "AT002", "Approved", false));
        assertEquals(0, countCheckedIn(list));
        assertEquals(2, countRemaining(list));
    }

    @Test public void testStatsWhenEveryoneCheckedIn() {
        List<Attendee> list = new ArrayList<>();
        list.add(new Attendee("1", "Ali",  "AT001", "Approved", true));
        list.add(new Attendee("2", "Sara", "AT002", "Approved", true));
        assertEquals(2, countCheckedIn(list));
        assertEquals(0, countRemaining(list));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STEP 2 — Check-In Action
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testMarkAttendeeCheckedIn() {
        Attendee hassan = allAttendees.get(1);
        assertFalse(hassan.checkedIn);
        hassan.checkedIn = true;
        assertTrue(hassan.checkedIn);
    }

    @Test public void testCheckedInCountIncreasesAfterCheckIn() {
        List<Attendee> approved = filterApprovedAttendees(allAttendees);
        int before = countCheckedIn(approved);
        approved.get(1).checkedIn = true; // Hassan
        assertEquals(before + 1, countCheckedIn(approved));
    }

    @Test public void testRemainingDecreasesAfterCheckIn() {
        List<Attendee> approved = filterApprovedAttendees(allAttendees);
        int before = countRemaining(approved);
        approved.get(1).checkedIn = true;
        assertEquals(before - 1, countRemaining(approved));
    }

    @Test public void testCheckingInAllMakesRemainingZero() {
        List<Attendee> approved = filterApprovedAttendees(allAttendees);
        for (Attendee a : approved) a.checkedIn = true;
        assertEquals(0, countRemaining(approved));
    }

    @Test public void testAlreadyCheckedInRemainsCheckedIn() {
        List<Attendee> approved = filterApprovedAttendees(allAttendees);
        int before = countCheckedIn(approved);
        approved.get(0).checkedIn = true; // Fatima already checked in
        assertEquals(before, countCheckedIn(approved));
    }

    @Test public void testSequentialCheckInsUpdateCount() {
        List<Attendee> approved = filterApprovedAttendees(allAttendees);
        assertEquals(2, countCheckedIn(approved));
        approved.get(1).checkedIn = true; assertEquals(3, countCheckedIn(approved));
        approved.get(3).checkedIn = true; assertEquals(4, countCheckedIn(approved));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // nvl helper
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testNvlReturnsFallbackForNull() {
        assertEquals("fallback", nvl(null, "fallback"));
    }

    @Test public void testNvlReturnsFallbackForEmpty() {
        assertEquals("fallback", nvl("", "fallback"));
    }

    @Test public void testNvlReturnsValueWhenPresent() {
        assertEquals("value", nvl("value", "fallback"));
    }
}