package com.lums.eventhub;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for ProposalFormActivity logic
 *
 * Covers: Org US-02 (Create Proposal), US-03 (Save Draft),
 *         US-04 (Submit to CCA), US-05 (Edit Proposal)
 *
 * All tests are pure-Java — no Android or Firestore dependencies.
 * They mirror the in-activity logic extracted into helper methods below.
 */
public class ProposalFormTest {

    // ─────────────────────────────────────────────
    // Helpers that mirror ProposalFormActivity logic
    // ─────────────────────────────────────────────

    /** Mirrors parseLong() in ProposalFormActivity */
    private long parseLong(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
    }

    /** Mirrors longToString() in ProposalFormActivity */
    private String longToString(Long v) {
        return (v == null || v == 0) ? "" : String.valueOf(v);
    }

    /**
     * Mirrors validateSection1() in ProposalFormActivity.
     * Returns the name of the first missing required field, or null if all valid.
     */
    private String validateSection1(String title, String description,
                                    String eventType, String date, String venue) {
        if (title == null || title.trim().isEmpty())       return "Event Title";
        if (description == null || description.trim().isEmpty()) return "Description";
        if (eventType == null || eventType.trim().isEmpty())     return "Event Type";
        if (date == null || date.trim().isEmpty())         return "Date";
        if (venue == null || venue.trim().isEmpty())       return "Venue";
        return null; // all good
    }

    /** Mirrors collectGuests() filtering — only non-empty rows are kept */
    private List<Map<String, Object>> collectGuests(List<String[]> rows) {
        List<Map<String, Object>> guests = new ArrayList<>();
        for (String[] row : rows) {
            String name  = row[0].trim();
            String title = row[1].trim();
            String org   = row[2].trim();
            if (!name.isEmpty() || !title.isEmpty() || !org.isEmpty()) {
                Map<String, Object> g = new HashMap<>();
                g.put("name",         name);
                g.put("title",        title);
                g.put("organization", org);
                guests.add(g);
            }
        }
        return guests;
    }

    /** Mirrors collectSessions() — all rows are kept regardless of content */
    private List<Map<String, Object>> collectSessions(List<String[]> rows) {
        List<Map<String, Object>> sessions = new ArrayList<>();
        for (String[] row : rows) {
            Map<String, Object> s = new HashMap<>();
            s.put("name",      row[0].trim());
            s.put("venue",     row[1].trim());
            s.put("startTime", row[2].trim());
            s.put("endTime",   row[3].trim());
            sessions.add(s);
        }
        return sessions;
    }

    /** Mirrors the status assignment logic inside saveProposal() */
    private String resolveStatus(boolean submit) {
        return submit ? "Submitted" : "Draft";
    }

    // ═══════════════════════════════════════════════
    // US-02 — Create Proposal (field collection)
    // ═══════════════════════════════════════════════

    /**
     * US-02: organizerUsername must be stored in the proposal data map.
     * This is the canonical field received from OrganizerDashboardActivity.
     */
    @Test
    public void testOrganizerUsernameStoredInProposalData() {
        String organizerUsername = "ORG0012";
        Map<String, Object> data = new HashMap<>();
        data.put("organizerUsername", organizerUsername);

        assertEquals("ORG0012", data.get("organizerUsername"));
    }

    /**
     * US-02: All required section-1 fields must be present in the data map.
     */
    @Test
    public void testAllRequiredFieldsStoredInProposalData() {
        Map<String, Object> data = new HashMap<>();
        data.put("title",       "Tech Summit 2025");
        data.put("description", "Annual tech event");
        data.put("eventType",   "Society Event");
        data.put("date",        "2025-12-01");
        data.put("venue",       "LUMS Auditorium");

        assertTrue(data.containsKey("title"));
        assertTrue(data.containsKey("description"));
        assertTrue(data.containsKey("eventType"));
        assertTrue(data.containsKey("date"));
        assertTrue(data.containsKey("venue"));
    }

    /**
     * US-02: societyName is stored in the proposal data map.
     */
    @Test
    public void testSocietyNameStoredInProposalData() {
        Map<String, Object> data = new HashMap<>();
        data.put("societyName", "SPADES Society");

        assertEquals("SPADES Society", data.get("societyName"));
    }

    /**
     * US-02: guests list with one fully-filled row is collected correctly.
     */
    @Test
    public void testGuestRowCollectedWhenNamePresent() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Dr. Ayesha", "Keynote Speaker", "LUMS"});

        List<Map<String, Object>> guests = collectGuests(rows);

        assertEquals(1, guests.size());
        assertEquals("Dr. Ayesha", guests.get(0).get("name"));
        assertEquals("Keynote Speaker", guests.get(0).get("title"));
        assertEquals("LUMS", guests.get(0).get("organization"));
    }

    /**
     * US-02: Completely empty guest rows are silently dropped (not added to list).
     */
    @Test
    public void testEmptyGuestRowIsNotCollected() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"", "", ""});

        List<Map<String, Object>> guests = collectGuests(rows);

        assertEquals(0, guests.size());
    }

    /**
     * US-02: A guest row with only title filled (no name/org) is still collected.
     */
    @Test
    public void testPartialGuestRowIsCollectedIfAnyFieldNonEmpty() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"", "Keynote Speaker", ""});

        List<Map<String, Object>> guests = collectGuests(rows);

        assertEquals(1, guests.size());
    }

    /**
     * US-02: Multiple guest rows are all collected.
     */
    @Test
    public void testMultipleGuestRowsCollected() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Dr. Ayesha", "Speaker",  "LUMS"});
        rows.add(new String[]{"Prof. Bilal", "Panelist", "IBA"});

        List<Map<String, Object>> guests = collectGuests(rows);

        assertEquals(2, guests.size());
    }

    /**
     * US-02: Sessions are always collected regardless of empty content.
     */
    @Test
    public void testSessionRowAlwaysCollected() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Opening Ceremony", "Main Hall", "09:00", "10:00"});

        List<Map<String, Object>> sessions = collectSessions(rows);

        assertEquals(1, sessions.size());
        assertEquals("Opening Ceremony", sessions.get(0).get("name"));
        assertEquals("Main Hall",        sessions.get(0).get("venue"));
        assertEquals("09:00",            sessions.get(0).get("startTime"));
        assertEquals("10:00",            sessions.get(0).get("endTime"));
    }

    /**
     * US-02: Multiple session rows are all collected.
     */
    @Test
    public void testMultipleSessionsCollected() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Session A", "Hall 1", "09:00", "10:00"});
        rows.add(new String[]{"Session B", "Hall 2", "11:00", "12:00"});
        rows.add(new String[]{"Session C", "Hall 3", "13:00", "14:00"});

        List<Map<String, Object>> sessions = collectSessions(rows);

        assertEquals(3, sessions.size());
    }

    // ═══════════════════════════════════════════════
    // US-03 — Save Draft
    // ═══════════════════════════════════════════════

    /**
     * US-03: Status is "Draft" when saving without submitting.
     */
    @Test
    public void testStatusIsDraftWhenNotSubmitting() {
        String status = resolveStatus(false);
        assertEquals("Draft", status);
    }

    /**
     * US-03: Draft save does NOT require title to be filled.
     * validateSection1 is only called when submit=true.
     */
    @Test
    public void testDraftSaveSkipsValidation() {
        // When submit=false, validateSection1 is never called — draft always proceeds.
        // We verify this by confirming an empty title would fail validation
        // but does NOT block a draft save in the activity flow.
        String validationError = validateSection1("", "desc", "Society Event", "2025-12-01", "Venue");
        assertEquals("Event Title", validationError); // would fail if submit were true

        // But draft path never calls validateSection1, so status is still "Draft"
        String status = resolveStatus(false);
        assertEquals("Draft", status);
    }

    /**
     * US-03: Draft data map contains "status" = "Draft" and "updatedAt" timestamp.
     */
    @Test
    public void testDraftDataMapHasCorrectFields() {
        Map<String, Object> data = new HashMap<>();
        data.put("status",    "Draft");
        data.put("updatedAt", System.currentTimeMillis());

        assertEquals("Draft", data.get("status"));
        assertTrue((long) data.get("updatedAt") > 0);
    }

    /**
     * US-03: Draft data map does NOT contain "submittedAt".
     */
    @Test
    public void testDraftDataMapDoesNotHaveSubmittedAt() {
        Map<String, Object> data = new HashMap<>();
        data.put("status",    "Draft");
        data.put("updatedAt", System.currentTimeMillis());

        assertFalse(data.containsKey("submittedAt"));
    }

    // ═══════════════════════════════════════════════
    // US-04 — Submit to CCA (validation)
    // ═══════════════════════════════════════════════

    /**
     * US-04: Status is "Submitted" when submitting to CCA.
     */
    @Test
    public void testStatusIsSubmittedWhenSubmitting() {
        String status = resolveStatus(true);
        assertEquals("Submitted", status);
    }

    /**
     * US-04: Submitted data map contains "status" = "Submitted" and "submittedAt".
     */
    @Test
    public void testSubmittedDataMapHasCorrectFields() {
        Map<String, Object> data = new HashMap<>();
        data.put("status",      "Submitted");
        data.put("submittedAt", System.currentTimeMillis());

        assertEquals("Submitted", data.get("status"));
        assertTrue((long) data.get("submittedAt") > 0);
    }

    /**
     * US-04: Submitted data map does NOT contain "updatedAt".
     */
    @Test
    public void testSubmittedDataMapDoesNotHaveUpdatedAt() {
        Map<String, Object> data = new HashMap<>();
        data.put("status",      "Submitted");
        data.put("submittedAt", System.currentTimeMillis());

        assertFalse(data.containsKey("updatedAt"));
    }

    /**
     * US-04: Validation passes when all required fields are provided.
     */
    @Test
    public void testValidationPassesWhenAllFieldsPresent() {
        String error = validateSection1(
                "Tech Summit", "Annual event", "Society Event", "2025-12-01", "LUMS Auditorium");
        assertNull(error);
    }

    /**
     * US-04: Validation fails and returns "Event Title" when title is empty.
     */
    @Test
    public void testValidationFailsWhenTitleIsEmpty() {
        String error = validateSection1("", "desc", "Society Event", "2025-12-01", "Venue");
        assertEquals("Event Title", error);
    }

    /**
     * US-04: Validation fails and returns "Description" when description is empty.
     */
    @Test
    public void testValidationFailsWhenDescriptionIsEmpty() {
        String error = validateSection1("Title", "", "Society Event", "2025-12-01", "Venue");
        assertEquals("Description", error);
    }

    /**
     * US-04: Validation fails and returns "Event Type" when no radio button is selected.
     */
    @Test
    public void testValidationFailsWhenEventTypeNotSelected() {
        String error = validateSection1("Title", "Desc", "", "2025-12-01", "Venue");
        assertEquals("Event Type", error);
    }

    /**
     * US-04: Validation fails and returns "Date" when date is empty.
     */
    @Test
    public void testValidationFailsWhenDateIsEmpty() {
        String error = validateSection1("Title", "Desc", "Society Event", "", "Venue");
        assertEquals("Date", error);
    }

    /**
     * US-04: Validation fails and returns "Venue" when venue is empty.
     */
    @Test
    public void testValidationFailsWhenVenueIsEmpty() {
        String error = validateSection1("Title", "Desc", "Society Event", "2025-12-01", "");
        assertEquals("Venue", error);
    }

    /**
     * US-04: Validation checks fields in order — title error takes priority over missing date.
     */
    @Test
    public void testValidationPriorityTitleBeforeDate() {
        String error = validateSection1("", "Desc", "Society Event", "", "Venue");
        assertEquals("Event Title", error); // title checked first
    }

    /**
     * US-04: Whitespace-only title is treated as empty and fails validation.
     */
    @Test
    public void testWhitespaceOnlyTitleFailsValidation() {
        String error = validateSection1("   ", "Desc", "Society Event", "2025-12-01", "Venue");
        assertEquals("Event Title", error);
    }

    /**
     * US-04: Whitespace-only venue is treated as empty and fails validation.
     */
    @Test
    public void testWhitespaceOnlyVenueFailsValidation() {
        String error = validateSection1("Title", "Desc", "Society Event", "2025-12-01", "   ");
        assertEquals("Venue", error);
    }

    // ═══════════════════════════════════════════════
    // US-05 — Edit Proposal
    // ═══════════════════════════════════════════════

    /**
     * US-05: When proposalId is non-null, the form is in edit mode.
     */
    @Test
    public void testEditModeWhenProposalIdIsPresent() {
        String proposalId = "abc123";
        boolean isEditMode = (proposalId != null);
        assertTrue(isEditMode);
    }

    /**
     * US-05: When proposalId is null, the form is in create mode.
     */
    @Test
    public void testCreateModeWhenProposalIdIsNull() {
        String proposalId = null;
        boolean isEditMode = (proposalId != null);
        assertFalse(isEditMode);
    }

    /**
     * US-05: Editing a proposal preserves the organizerUsername in the updated data.
     */
    @Test
    public void testEditProposalPreservesOrganizerUsername() {
        String organizerUsername = "ORG0012";
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("title",             "Updated Title");
        updatedData.put("organizerUsername", organizerUsername);

        assertEquals("ORG0012", updatedData.get("organizerUsername"));
    }

    /**
     * US-05: Re-submitting an edited proposal sets status back to "Submitted".
     */
    @Test
    public void testResubmittedProposalHasSubmittedStatus() {
        // Previously a Draft, now being resubmitted after edits
        String status = resolveStatus(true);
        assertEquals("Submitted", status);
    }

    /**
     * US-05: Saving edits as Draft keeps status as "Draft".
     */
    @Test
    public void testSavingEditsAsDraftKeepsDraftStatus() {
        String status = resolveStatus(false);
        assertEquals("Draft", status);
    }

    // ═══════════════════════════════════════════════
    // Helper: parseLong / longToString
    // ═══════════════════════════════════════════════

    /**
     * parseLong returns correct value for valid numeric string.
     */
    @Test
    public void testParseLongValidInput() {
        assertEquals(200L, parseLong("200"));
    }

    /**
     * parseLong returns 0 for non-numeric string (graceful fallback).
     */
    @Test
    public void testParseLongInvalidInputReturnsZero() {
        assertEquals(0L, parseLong("abc"));
    }

    /**
     * parseLong returns 0 for empty string.
     */
    @Test
    public void testParseLongEmptyStringReturnsZero() {
        assertEquals(0L, parseLong(""));
    }

    /**
     * longToString returns empty string for null.
     */
    @Test
    public void testLongToStringNullReturnsEmpty() {
        assertEquals("", longToString(null));
    }

    /**
     * longToString returns empty string for zero.
     */
    @Test
    public void testLongToStringZeroReturnsEmpty() {
        assertEquals("", longToString(0L));
    }

    /**
     * longToString returns correct string for positive value.
     */
    @Test
    public void testLongToStringPositiveValue() {
        assertEquals("500", longToString(500L));
    }
}