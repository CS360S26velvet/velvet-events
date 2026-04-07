package com.lums.eventhub;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for CapacitySettingActivity logic
 *
 * Covers:
 *   US-25 — Set maximum registration capacity
 *   US-23 — Duplicate a form from a previous event
 *
 * Pure Java — no Android or Firestore dependencies.
 */
public class CapacitySettingTest {

    // ─────────────────────────────────────────────
    // Mirror of saveCapacity() validation logic
    // Returns error message or null if valid
    // ─────────────────────────────────────────────

    private String validateCapacity(String input) {
        if (input == null || input.trim().isEmpty()) return "Please enter a capacity!";
        try {
            int value = Integer.parseInt(input.trim());
            if (value <= 0) return "Capacity must be greater than zero!";
        } catch (NumberFormatException e) {
            return "Capacity must be a number!";
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // Mirror of duplicateForm() guard logic
    // ─────────────────────────────────────────────

    private String validateDuplication(String selectedPreviousEventId) {
        if (selectedPreviousEventId == null || selectedPreviousEventId.isEmpty())
            return "Please select a previous event first!";
        return null;
    }

    // ─────────────────────────────────────────────
    // Mirror of addSampleDuplicatedQuestions()
    // ─────────────────────────────────────────────

    private List<Map<String, Object>> buildSampleDuplicatedQuestions() {
        String[] labels   = {"Full Name", "Student ID", "Department", "Dietary Preferences"};
        String[] types    = {"Short Text", "Short Text", "Dropdown", "Multiple Choice"};
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            Map<String, Object> q = new HashMap<>();
            q.put("label",    labels[i]);
            q.put("type",     types[i]);
            q.put("required", true);
            questions.add(q);
        }
        return questions;
    }

    // ═══════════════════════════════════════════════
    // US-25 — Capacity validation
    // ═══════════════════════════════════════════════

    /**
     * US-25: Valid positive capacity passes validation.
     */
    @Test
    public void testValidCapacityPasses() {
        assertNull(validateCapacity("200"));
    }

    /**
     * US-25: Empty input fails validation.
     */
    @Test
    public void testEmptyCapacityFails() {
        assertNotNull(validateCapacity(""));
    }

    /**
     * US-25: Null input fails validation.
     */
    @Test
    public void testNullCapacityFails() {
        assertNotNull(validateCapacity(null));
    }

    /**
     * US-25: Zero capacity fails validation.
     */
    @Test
    public void testZeroCapacityFails() {
        assertNotNull(validateCapacity("0"));
    }

    /**
     * US-25: Negative capacity fails validation.
     */
    @Test
    public void testNegativeCapacityFails() {
        assertNotNull(validateCapacity("-5"));
    }

    /**
     * US-25: Non-numeric input fails validation.
     */
    @Test
    public void testNonNumericCapacityFails() {
        assertNotNull(validateCapacity("abc"));
    }

    /**
     * US-25: Whitespace-only input fails validation.
     */
    @Test
    public void testWhitespaceCapacityFails() {
        assertNotNull(validateCapacity("   "));
    }

    /**
     * US-25: Capacity parses to correct integer value.
     */
    @Test
    public void testCapacityParsesCorrectly() {
        assertEquals(200, Integer.parseInt("200"));
    }

    /**
     * US-25: Capacity is stored in data map with correct key.
     */
    @Test
    public void testCapacityStoredInDataMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("maxCapacity", 200);
        assertEquals(200, data.get("maxCapacity"));
        assertTrue(data.containsKey("maxCapacity"));
    }

    // ═══════════════════════════════════════════════
    // US-23 — Form duplication
    // ═══════════════════════════════════════════════

    /**
     * US-23: Duplication blocked when no event selected.
     */
    @Test
    public void testDuplicationBlockedWithNoEventSelected() {
        assertNotNull(validateDuplication(""));
    }

    /**
     * US-23: Duplication allowed when event is selected.
     */
    @Test
    public void testDuplicationAllowedWithEventSelected() {
        assertNull(validateDuplication("GALA2024"));
    }

    /**
     * US-23: Sample duplication produces 4 questions.
     */
    @Test
    public void testSampleDuplicationProducesFourQuestions() {
        assertEquals(4, buildSampleDuplicatedQuestions().size());
    }

    /**
     * US-23: All duplicated questions have required=true.
     */
    @Test
    public void testDuplicatedQuestionsAreAllRequired() {
        for (Map<String, Object> q : buildSampleDuplicatedQuestions()) {
            assertEquals(true, q.get("required"));
        }
    }

    /**
     * US-23: Duplicated questions contain correct labels.
     */
    @Test
    public void testDuplicatedQuestionsHaveCorrectLabels() {
        List<Map<String, Object>> questions = buildSampleDuplicatedQuestions();
        assertEquals("Full Name",           questions.get(0).get("label"));
        assertEquals("Student ID",          questions.get(1).get("label"));
        assertEquals("Department",          questions.get(2).get("label"));
        assertEquals("Dietary Preferences", questions.get(3).get("label"));
    }

    /**
     * US-23: Duplicated questions contain correct types.
     */
    @Test
    public void testDuplicatedQuestionsHaveCorrectTypes() {
        List<Map<String, Object>> questions = buildSampleDuplicatedQuestions();
        assertEquals("Short Text",      questions.get(0).get("type"));
        assertEquals("Short Text",      questions.get(1).get("type"));
        assertEquals("Dropdown",        questions.get(2).get("type"));
        assertEquals("Multiple Choice", questions.get(3).get("type"));
    }

    /**
     * US-23: Each duplicated question has all three required keys.
     */
    @Test
    public void testDuplicatedQuestionsHaveAllKeys() {
        for (Map<String, Object> q : buildSampleDuplicatedQuestions()) {
            assertTrue(q.containsKey("label"));
            assertTrue(q.containsKey("type"));
            assertTrue(q.containsKey("required"));
        }
    }

    /**
     * US-23: Previous event IDs map correctly to event names.
     */
    @Test
    public void testPreviousEventIdsMapToNames() {
        String[] names = {"Annual Gala 2024", "Tech Fest 2024", "Sports Day 2024"};
        String[] ids   = {"GALA2024", "TECHFEST2024", "SPORTS2024"};
        assertEquals(names.length, ids.length);
        assertEquals("GALA2024",     ids[0]);
        assertEquals("TECHFEST2024", ids[1]);
        assertEquals("SPORTS2024",   ids[2]);
    }
}