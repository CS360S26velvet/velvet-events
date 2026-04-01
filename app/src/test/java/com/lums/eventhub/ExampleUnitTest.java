package com.lums.eventhub;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for Check-In functionality (US-30)
 * Tests attendee check-in logic, search filtering, and stats calculation
 */
public class ExampleUnitTest {

    // Sample attendee data class for testing
    static class Attendee {
        String id, name, studentId, paymentStatus;
        boolean isCheckedIn;
        Attendee(String id, String name, String studentId, String paymentStatus, boolean isCheckedIn) {
            this.id = id;
            this.name = name;
            this.studentId = studentId;
            this.paymentStatus = paymentStatus;
            this.isCheckedIn = isCheckedIn;
        }
    }

    // Helper to create sample attendee list
    List<Attendee> getSampleAttendees() {
        List<Attendee> list = new ArrayList<>();
        list.add(new Attendee("1", "Fatima Malik", "AT0023", "Paid", true));
        list.add(new Attendee("2", "Hassan Raza", "AT0041", "Paid", false));
        list.add(new Attendee("3", "Zainab Ali", "AT0055", "Paid", true));
        list.add(new Attendee("4", "Bilal Khan", "AT0067", "Pending", false));
        list.add(new Attendee("5", "Sara Ahmed", "AT0078", "Paid", false));
        list.add(new Attendee("6", "Usman Tariq", "AT0089", "Paid", false));
        return list;
    }

    /**
     * US-30: Test that checked-in count is calculated correctly
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
     * US-30: Test that remaining count is correct
     */
    @Test
    public void testRemainingCount() {
        List<Attendee> attendees = getSampleAttendees();
        int checkedIn = 0;
        for (Attendee a : attendees) {
            if (a.isCheckedIn) checkedIn++;
        }
        int remaining = attendees.size() - checkedIn;
        assertEquals(4, remaining);
    }

    /**
     * US-30: Test marking an attendee as checked in
     */
    @Test
    public void testMarkAttendeeCheckedIn() {
        List<Attendee> attendees = getSampleAttendees();
        Attendee hassan = attendees.get(1);
        assertFalse(hassan.isCheckedIn);
        hassan.isCheckedIn = true;
        assertTrue(hassan.isCheckedIn);
    }

    /**
     * US-30: Test search by name works correctly
     */
    @Test
    public void testSearchByName() {
        List<Attendee> attendees = getSampleAttendees();
        List<Attendee> filtered = new ArrayList<>();
        String query = "fatima";
        for (Attendee a : attendees) {
            if (a.name.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(a);
            }
        }
        assertEquals(1, filtered.size());
        assertEquals("Fatima Malik", filtered.get(0).name);
    }

    /**
     * US-30: Test search by student ID works correctly
     */
    @Test
    public void testSearchByStudentId() {
        List<Attendee> attendees = getSampleAttendees();
        List<Attendee> filtered = new ArrayList<>();
        String query = "AT0041";
        for (Attendee a : attendees) {
            if (a.studentId.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(a);
            }
        }
        assertEquals(1, filtered.size());
        assertEquals("Hassan Raza", filtered.get(0).name);
    }

    /**
     * US-30: Test total attendee count
     */
    @Test
    public void testTotalAttendeeCount() {
        List<Attendee> attendees = getSampleAttendees();
        assertEquals(6, attendees.size());
    }

    /**
     * US-30: Test payment status is correctly stored
     */
    @Test
    public void testPaymentStatus() {
        List<Attendee> attendees = getSampleAttendees();
        Attendee bilal = attendees.get(3);
        assertEquals("Pending", bilal.paymentStatus);
    }
}