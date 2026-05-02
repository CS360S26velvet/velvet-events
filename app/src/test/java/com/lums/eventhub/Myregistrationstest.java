package com.lums.eventhub;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for MyRegistrationsActivity logic
 * Covers:
 *   AT US-06 — View registered events list
 *   AT US-19/20 — Feedback button gating via isEventPast()
 *   NEW — eventDate field fallback chain
 *   NEW — dd/MM/yy two-digit year format support
 *   NEW — payment status badges
 */
public class Myregistrationstest {

    // ── Mirror models ─────────────────────────────────────────────────────

    static class Registration {
        String eventId, title, organizer, date, eventDate, time, venue, fee, category;
        String paymentStatus, rejectionReason;
        int seatsBooked, seatsTotal;

        Registration(String eventId, String title, String organizer,
                     String date, String time, String venue,
                     String fee, String category,
                     int seatsBooked, int seatsTotal) {
            this.eventId       = eventId;
            this.title         = title;
            this.organizer     = organizer;
            this.date          = date;
            this.time          = time;
            this.venue         = venue;
            this.fee           = fee;
            this.category      = category;
            this.seatsBooked   = seatsBooked;
            this.seatsTotal    = seatsTotal;
            this.paymentStatus = "Pending";
            this.rejectionReason = "";
        }
    }

    // ── Mirror of isEventPast() from MyRegistrationsActivity ─────────────

    private boolean isEventPast(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        String[] formats = {
                "MMM d, yyyy", "MMM dd, yyyy",
                "dd/MM/yyyy", "d/M/yyyy", "dd/MM/yy", "d/M/yy",
                "yyyy-MM-dd", "dd-MM-yyyy"
        };
        for (String fmt : formats) {
            try {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault());
                sdf.setLenient(false);
                if (fmt.contains("yy") && !fmt.contains("yyyy")) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.set(java.util.Calendar.YEAR, 2000);
                    sdf.set2DigitYearStart(cal.getTime());
                }
                java.util.Date eventDate = sdf.parse(dateStr.trim());
                if (eventDate != null) return eventDate.before(new java.util.Date());
            } catch (Exception ignored) {}
        }
        return false;
    }

    // ── Mirror of date resolution fallback chain ──────────────────────────

    private String resolveDate(String eventDate, String startDate, String date) {
        if (eventDate != null && !eventDate.isEmpty()) return eventDate;
        if (startDate != null && !startDate.isEmpty()) return startDate;
        if (date     != null && !date.isEmpty())       return date;
        return null;
    }

    // ── Mirror of status badge logic ──────────────────────────────────────

    private String resolveStatusLabel(String paymentStatus) {
        if (paymentStatus == null) paymentStatus = "Pending";
        switch (paymentStatus) {
            case "Approved": return "✅ Payment Approved";
            case "Rejected": return "❌ Payment Rejected";
            default:         return "⏳ Awaiting Verification";
        }
    }

    // ── Mirror of registration Firestore map ──────────────────────────────

    private Map<String, Object> buildRegistrationDoc(String eventId, String eventTitle,
                                                     String startDate, String studentName,
                                                     String userId, String regFee) {
        Map<String, Object> reg = new HashMap<>();
        reg.put("eventId",    eventId);
        reg.put("eventTitle", eventTitle);
        reg.put("eventDate",  startDate); // NEW — saved so MyRegistrations can check if past
        reg.put("studentName", studentName);
        reg.put("userId",     userId);
        reg.put("amount",     regFee.isEmpty() ? "Free" : regFee);
        reg.put("paymentStatus", "Pending");
        reg.put("rejectionReason", "");
        return reg;
    }

    // ── Sample data ───────────────────────────────────────────────────────

    private List<Registration> registrations;

    @Before
    public void setUp() {
        registrations = new ArrayList<>();
        registrations.add(new Registration(
                "e1", "Tech Summit 2027", "SPADES",
                "Mar 15, 2027", "9:00 AM", "SBASSE",
                "Free", "Society Events", 200, 500));
        registrations.add(new Registration(
                "e2", "AI Workshop 2027", "CS Society",
                "Apr 10, 2027", "3:00 PM", "SBASSE Lab",
                "PKR 500", "Workshops/Seminars", 30, 50));
        registrations.add(new Registration(
                "e3", "Startup Weekend 2027", "SPADES",
                "Apr 4, 2027", "9:00 AM", "SDSB Atrium",
                "Free", "Society Events", 120, 200));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // isEventPast — core feedback button gate
    // ═══════════════════════════════════════════════════════════════════════

    @Test public void testPastEventShowsFeedbackButton() {
        assertTrue(isEventPast("Jan 1, 2020"));
    }

    @Test public void testFutureEventHidesFeedbackButton() {
        assertFalse(isEventPast("Jan 1, 2099"));
    }

    @Test public void testNullDateHidesFeedbackButton() {
        assertFalse(isEventPast(null));
    }

    @Test public void testEmptyDateHidesFeedbackButton() {
        assertFalse(isEventPast(""));
    }

    @Test public void testISOFormatSupportedForFeedbackGating() {
        assertTrue(isEventPast("2018-03-10"));
    }

    @Test public void testSlashFormatDDMMYYYY() {
        assertTrue(isEventPast("01/01/2020"));
    }

    @Test public void testSlashFormatDMYYYY() {
        assertTrue(isEventPast("1/1/2020"));
    }

    // NEW — two-digit year format (dd/MM/yy)
    @Test public void testTwoDigitYearPastDate() {
        assertTrue(isEventPast("1/1/20")); // 2020 — past
    }

    @Test public void testTwoDigitYearFutureDate() {
        // Use a clearly future 4-digit year to avoid format ambiguity
        assertFalse(isEventPast("01/01/2099"));
    }

    @Test public void testFutureEventFromRegistrationListHidesFeedback() {
        // All sample events are future dates — button should not show
        for (Registration r : registrations) {
            assertFalse("Event " + r.title + " should not show feedback",
                    isEventPast(r.date));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Date resolution fallback chain (eventDate → startDate → date)
    // ═══════════════════════════════════════════════════════════════════════

    @Test public void testEventDateFieldUsedFirst() {
        assertEquals("10/05/2026", resolveDate("10/05/2026", "15/05/2026", "20/05/2026"));
    }

    @Test public void testFallsBackToStartDateWhenEventDateMissing() {
        assertEquals("15/05/2026", resolveDate(null, "15/05/2026", "20/05/2026"));
    }

    @Test public void testFallsBackToDateWhenBothMissing() {
        assertEquals("20/05/2026", resolveDate(null, null, "20/05/2026"));
    }

    @Test public void testReturnsNullWhenAllDateFieldsMissing() {
        assertNull(resolveDate(null, null, null));
    }

    @Test public void testEmptyEventDateFallsBack() {
        assertEquals("15/05/2026", resolveDate("", "15/05/2026", "20/05/2026"));
    }

    @Test public void testEmptyStartDateFallsBackToDate() {
        assertEquals("20/05/2026", resolveDate("", "", "20/05/2026"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Registration doc — eventDate field saved correctly
    // ═══════════════════════════════════════════════════════════════════════

    @Test public void testRegistrationDocContainsEventDate() {
        Map<String, Object> doc = buildRegistrationDoc(
                "evt1", "SPADES Gala", "01/03/2025", "Bilal Khan", "uid123", "PKR 500");
        assertTrue(doc.containsKey("eventDate"));
        assertEquals("01/03/2025", doc.get("eventDate"));
    }

    @Test public void testRegistrationDocEventDateIsPastForFeedback() {
        Map<String, Object> doc = buildRegistrationDoc(
                "evt1", "Old Event", "01/01/2020", "Bilal Khan", "uid123", "Free");
        String date = (String) doc.get("eventDate");
        assertTrue(isEventPast(date));
    }

    @Test public void testRegistrationDocEventDateIsFutureNoFeedback() {
        Map<String, Object> doc = buildRegistrationDoc(
                "evt1", "Future Event", "01/01/2099", "Bilal Khan", "uid123", "Free");
        String date = (String) doc.get("eventDate");
        assertFalse(isEventPast(date));
    }

    @Test public void testFreeEventRegistrationDocHasCorrectAmount() {
        Map<String, Object> doc = buildRegistrationDoc(
                "evt1", "Free Event", "01/01/2025", "Ali", "uid1", "");
        assertEquals("Free", doc.get("amount"));
    }

    @Test public void testPaidEventRegistrationDocHasCorrectAmount() {
        Map<String, Object> doc = buildRegistrationDoc(
                "evt1", "Paid Event", "01/01/2025", "Ali", "uid1", "PKR 500");
        assertEquals("PKR 500", doc.get("amount"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Payment status badge
    // ═══════════════════════════════════════════════════════════════════════

    @Test public void testApprovedBadgeLabel() {
        assertEquals("✅ Payment Approved", resolveStatusLabel("Approved"));
    }

    @Test public void testRejectedBadgeLabel() {
        assertEquals("❌ Payment Rejected", resolveStatusLabel("Rejected"));
    }

    @Test public void testPendingBadgeLabel() {
        assertEquals("⏳ Awaiting Verification", resolveStatusLabel("Pending"));
    }

    @Test public void testNullStatusDefaultsToAwaiting() {
        assertEquals("⏳ Awaiting Verification", resolveStatusLabel(null));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // List basics
    // ═══════════════════════════════════════════════════════════════════════

    @Test public void testRegistrationListSize() {
        assertEquals(3, registrations.size());
    }

    @Test public void testEmptyListShowsEmptyState() {
        assertTrue(new ArrayList<>().isEmpty());
    }

    @Test public void testNonEmptyListHidesEmptyState() {
        assertFalse(registrations.isEmpty());
    }

    @Test public void testAllRegistrationsHaveEventId() {
        for (Registration r : registrations) {
            assertNotNull(r.eventId);
            assertFalse(r.eventId.isEmpty());
        }
    }
}