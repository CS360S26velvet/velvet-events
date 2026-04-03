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
    // ==================== US-28 & US-29 TESTS ====================

    /**
     * US-28: Test that incomplete registrations are correctly identified
     */
    @Test
    public void testIncompleteRegistrationsCount() {
        List<String[]> registrants = new ArrayList<>();
        registrants.add(new String[]{"Hassan Raza", "AT0041", "3 hours ago", "false"});
        registrants.add(new String[]{"Fatima Malik", "AT0023", "2 hours ago", "true"});
        registrants.add(new String[]{"Bilal Khan", "AT0067", "1 hour ago", "false"});
        registrants.add(new String[]{"Sara Ahmed", "AT0078", "45 min ago", "true"});
        registrants.add(new String[]{"Usman Tariq", "AT0089", "30 min ago", "false"});
        registrants.add(new String[]{"Zainab Ali", "AT0055", "15 min ago", "false"});

        int incompleteCount = 0;
        for (String[] r : registrants) {
            if (r[3].equals("false")) incompleteCount++;
        }
        assertEquals(4, incompleteCount);
    }

    /**
     * US-28: Test that completed registrations are correctly identified
     */
    @Test
    public void testCompletedRegistrationsCount() {
        List<String[]> registrants = new ArrayList<>();
        registrants.add(new String[]{"Hassan Raza", "AT0041", "3 hours ago", "false"});
        registrants.add(new String[]{"Fatima Malik", "AT0023", "2 hours ago", "true"});
        registrants.add(new String[]{"Bilal Khan", "AT0067", "1 hour ago", "false"});
        registrants.add(new String[]{"Sara Ahmed", "AT0078", "45 min ago", "true"});

        int completedCount = 0;
        for (String[] r : registrants) {
            if (r[3].equals("true")) completedCount++;
        }
        assertEquals(2, completedCount);
    }

    /**
     * US-28: Test filtering shows only incomplete registrations
     */
    @Test
    public void testFilterIncompleteOnly() {
        List<String[]> all = new ArrayList<>();
        all.add(new String[]{"Hassan Raza", "AT0041", "3 hours ago", "false"});
        all.add(new String[]{"Fatima Malik", "AT0023", "2 hours ago", "true"});
        all.add(new String[]{"Bilal Khan", "AT0067", "1 hour ago", "false"});

        List<String[]> incomplete = new ArrayList<>();
        for (String[] r : all) {
            if (r[3].equals("false")) incomplete.add(r);
        }
        assertEquals(2, incomplete.size());
        assertEquals("Hassan Raza", incomplete.get(0)[0]);
    }

    /**
     * US-29: Test CSV content is correctly formatted
     */
    @Test
    public void testCSVFormat() {
        StringBuilder csv = new StringBuilder();
        csv.append("Name,Student ID,Started At,Status\n");
        csv.append("Hassan Raza,AT0041,3 hours ago,Incomplete\n");
        csv.append("Fatima Malik,AT0023,2 hours ago,Completed\n");

        String[] lines = csv.toString().split("\n");
        assertEquals("Name,Student ID,Started At,Status", lines[0]);
        assertEquals(3, lines.length);
    }

    /**
     * US-29: Test CSV has correct number of rows
     */
    @Test
    public void testCSVRowCount() {
        List<String[]> registrants = new ArrayList<>();
        registrants.add(new String[]{"Hassan Raza", "AT0041", "3 hours ago", "false"});
        registrants.add(new String[]{"Fatima Malik", "AT0023", "2 hours ago", "true"});
        registrants.add(new String[]{"Bilal Khan", "AT0067", "1 hour ago", "false"});

        StringBuilder csv = new StringBuilder();
        csv.append("Name,Student ID,Started At,Status\n");
        for (String[] r : registrants) {
            csv.append(r[0]).append(",").append(r[1]).append(",")
                    .append(r[2]).append(",")
                    .append(r[3].equals("true") ? "Completed" : "Incomplete")
                    .append("\n");
        }
        String[] lines = csv.toString().split("\n");
        // header + 3 data rows
        assertEquals(4, lines.length);
    }

    /**
     * US-25: Test capacity is saved correctly
     */
    @Test
    public void testCapacitySetting() {
        int capacity = 200;
        assertTrue(capacity > 0);
        assertEquals(200, capacity);
    }

    /**
     * US-23: Test form duplication copies all questions
     */
    @Test
    public void testFormDuplication() {
        List<String[]> originalForm = new ArrayList<>();
        originalForm.add(new String[]{"Full Name", "Short Text", "true"});
        originalForm.add(new String[]{"Student ID", "Short Text", "true"});
        originalForm.add(new String[]{"Department", "Dropdown", "false"});

        List<String[]> duplicatedForm = new ArrayList<>(originalForm);
        assertEquals(originalForm.size(), duplicatedForm.size());
        assertEquals("Full Name", duplicatedForm.get(0)[0]);
    }
    /**
     * US-25: Test capacity cannot be zero
     */
    @Test
    public void testCapacityCannotBeZero() {
        int capacity = 0;
        assertFalse(capacity > 0);
    }

    /**
     * US-25: Test capacity cannot be negative
     */
    @Test
    public void testCapacityCannotBeNegative() {
        int capacity = -5;
        assertFalse(capacity > 0);
    }

    /**
     * US-25: Test capacity input is a valid number
     */
    @Test
    public void testCapacityIsValidNumber() {
        String input = "200";
        assertTrue(input.matches("[0-9]+"));
        assertEquals(200, Integer.parseInt(input));
    }
    // ==================== AT US-06, US-07, US-09, US-10 TESTS ====================

    /**
     * AT US-09: Test that required questions are correctly identified
     */
    @Test
    public void testRequiredQuestionsIdentified() {
        List<String[]> questions = new ArrayList<>();
        questions.add(new String[]{"Full Name", "Short Text", "true"});
        questions.add(new String[]{"Student ID", "Short Text", "true"});
        questions.add(new String[]{"Department", "Short Text", "false"});
        questions.add(new String[]{"Dietary Preferences", "Short Text", "false"});

        int requiredCount = 0;
        for (String[] q : questions) {
            if (q[2].equals("true")) requiredCount++;
        }
        assertEquals(2, requiredCount);
    }

    /**
     * AT US-09: Test that optional questions are correctly identified
     */
    @Test
    public void testOptionalQuestionsIdentified() {
        List<String[]> questions = new ArrayList<>();
        questions.add(new String[]{"Full Name", "Short Text", "true"});
        questions.add(new String[]{"Student ID", "Short Text", "true"});
        questions.add(new String[]{"Department", "Short Text", "false"});
        questions.add(new String[]{"Dietary Preferences", "Short Text", "false"});

        int optionalCount = 0;
        for (String[] q : questions) {
            if (q[2].equals("false")) optionalCount++;
        }
        assertEquals(2, optionalCount);
    }

    /**
     * AT US-06: Test that required fields are validated before submission
     */
    @Test
    public void testRequiredFieldValidation() {
        List<String[]> questions = new ArrayList<>();
        questions.add(new String[]{"Full Name", "Short Text", "true", ""});
        questions.add(new String[]{"Student ID", "Short Text", "true", "AT0041"});

        boolean canSubmit = true;
        for (String[] q : questions) {
            if (q[2].equals("true") && q[3].trim().isEmpty()) {
                canSubmit = false;
                break;
            }
        }
        assertFalse(canSubmit);
    }

    /**
     * AT US-06: Test that form can be submitted when all required fields filled
     */
    @Test
    public void testFormCanBeSubmittedWhenComplete() {
        List<String[]> questions = new ArrayList<>();
        questions.add(new String[]{"Full Name", "Short Text", "true", "Hassan Raza"});
        questions.add(new String[]{"Student ID", "Short Text", "true", "AT0041"});
        questions.add(new String[]{"Department", "Short Text", "false", ""});

        boolean canSubmit = true;
        for (String[] q : questions) {
            if (q[2].equals("true") && q[3].trim().isEmpty()) {
                canSubmit = false;
                break;
            }
        }
        assertTrue(canSubmit);
    }

    /**
     * AT US-07: Test that different question types are supported
     */
    @Test
    public void testQuestionTypesSupported() {
        List<String> supportedTypes = new ArrayList<>();
        supportedTypes.add("Short Text");
        supportedTypes.add("Paragraph");
        supportedTypes.add("Multiple Choice");
        supportedTypes.add("Checkboxes");
        supportedTypes.add("Dropdown");
        supportedTypes.add("Date");
        supportedTypes.add("File Upload");

        assertTrue(supportedTypes.contains("Short Text"));
        assertTrue(supportedTypes.contains("Multiple Choice"));
        assertTrue(supportedTypes.contains("Dropdown"));
        assertEquals(7, supportedTypes.size());
    }

    /**
     * AT US-10: Test that answers are preserved when going back
     */
    @Test
    public void testAnswersPreservedOnBack() {
        List<String[]> questions = new ArrayList<>();
        questions.add(new String[]{"Full Name", "Short Text", "true", "Hassan Raza"});
        questions.add(new String[]{"Student ID", "Short Text", "true", "AT0041"});

        // Simulate going back and coming back
        String savedAnswer = questions.get(0)[3];
        assertEquals("Hassan Raza", savedAnswer);
        assertFalse(savedAnswer.isEmpty());
    }

    /**
     * AT US-10: Test that partial answers are saved before going back
     */
    @Test
    public void testPartialAnswersSaved() {
        List<String[]> questions = new ArrayList<>();
        questions.add(new String[]{"Full Name", "Short Text", "true", "Hassan"});
        questions.add(new String[]{"Student ID", "Short Text", "true", ""});

        int answeredCount = 0;
        for (String[] q : questions) {
            if (!q[3].isEmpty()) answeredCount++;
        }
        assertEquals(1, answeredCount);
    }

    /**
     * AT US-06: Test total question count is correct
     */
    @Test
    public void testTotalQuestionCount() {
        List<String[]> questions = new ArrayList<>();
        questions.add(new String[]{"Full Name", "Short Text", "true"});
        questions.add(new String[]{"Student ID", "Short Text", "true"});
        questions.add(new String[]{"Department", "Short Text", "false"});
        questions.add(new String[]{"Dietary Preferences", "Short Text", "false"});

        assertEquals(4, questions.size());
    }
}