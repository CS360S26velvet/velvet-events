package com.lums.eventhub;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for Check-In functionality (US-30)
 *
 * NOTE: Tests for other user stories have been moved to dedicated files:
 *   US-28, US-29  → RegistrantDashboardTest.java
 *   US-25, US-23  → CapacitySettingTest.java
 *   AT US-06/07/09/10 → AttendeeRegistrationTest.java
 */
public class ExampleUnitTest {

    // ─────────────────────────────────────────────
    // Sample attendee data class for testing
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

    List<Attendee> getSampleAttendees() {
        List<Attendee> list = new ArrayList<>();
        list.add(new Attendee("1", "Fatima Malik", "AT0023", "Paid",    true));
        list.add(new Attendee("2", "Hassan Raza",  "AT0041", "Paid",    false));
        list.add(new Attendee("3", "Zainab Ali",   "AT0055", "Paid",    true));
        list.add(new Attendee("4", "Bilal Khan",   "AT0067", "Pending", false));
        list.add(new Attendee("5", "Sara Ahmed",   "AT0078", "Paid",    false));
        list.add(new Attendee("6", "Usman Tariq",  "AT0089", "Paid",    false));
        return list;
    }

    // ═══════════════════════════════════════════════
    // US-30 — Check-In Tests
    // ═══════════════════════════════════════════════

    /**
     * US-30: Checked-in count is calculated correctly.
     */
    @Test
    public void testCheckedInCount() {
        List<Attendee> attendees = getSampleAttendees();
        int count = 0;
        for (Attendee a : attendees) {
            if (a.isCheckedIn) count++;
        }
        assertEquals(2, count);
    }

    /**
     * US-30: Remaining count is total minus checked-in.
     */
    @Test
    public void testRemainingCount() {
        List<Attendee> attendees = getSampleAttendees();
        int checkedIn = 0;
        for (Attendee a : attendees) {
            if (a.isCheckedIn) checkedIn++;
        }
        assertEquals(4, attendees.size() - checkedIn);
    }

    /**
     * US-30: Marking an attendee as checked in flips the flag.
     */
    @Test
    public void testMarkAttendeeCheckedIn() {
        Attendee hassan = getSampleAttendees().get(1);
        assertFalse(hassan.isCheckedIn);
        hassan.isCheckedIn = true;
        assertTrue(hassan.isCheckedIn);
    }

    /**
     * US-30: Search by name returns correct attendee.
     */
    @Test
    public void testSearchByName() {
        List<Attendee> filtered = new ArrayList<>();
        for (Attendee a : getSampleAttendees()) {
            if (a.name.toLowerCase().contains("fatima")) filtered.add(a);
        }
        assertEquals(1, filtered.size());
        assertEquals("Fatima Malik", filtered.get(0).name);
    }

    /**
     * US-30: Search by student ID returns correct attendee.
     */
    @Test
    public void testSearchByStudentId() {
        List<Attendee> filtered = new ArrayList<>();
        for (Attendee a : getSampleAttendees()) {
            if (a.studentId.equalsIgnoreCase("AT0041")) filtered.add(a);
        }
        assertEquals(1, filtered.size());
        assertEquals("Hassan Raza", filtered.get(0).name);
    }

    /**
     * US-30: Total attendee count is correct.
     */
    @Test
    public void testTotalAttendeeCount() {
        assertEquals(6, getSampleAttendees().size());
    }

    /**
     * US-30: Payment status is stored correctly.
     */
    @Test
    public void testPaymentStatus() {
        assertEquals("Pending", getSampleAttendees().get(3).paymentStatus);
    }
}   