package com.lums.eventhub;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

/**
 * RegistrationFeeSetupTest.java
 *
 * Unit tests for RegistrationFeeSetupActivity logic.
 * Covers fee validation, payload building, and the
 * "both No → skip straight to FormBuilder" path.
 */
public class RegistrationFeeSetupTest {

    // ── Mirror of save-button validation from wireSave() ──────────────────

    /**
     * Returns an error message if validation fails, null if valid.
     * Mirrors the guard blocks inside wireSave().
     */
    private String validate(boolean hasRegFee, String regFee,
                            boolean hasAccommodation, String accomFee) {
        if (hasRegFee && (regFee == null || regFee.trim().isEmpty())) {
            return "Please enter the registration fee amount.";
        }
        if (hasAccommodation && (accomFee == null || accomFee.trim().isEmpty())) {
            return "Please enter the accommodation fee amount.";
        }
        return null; // valid
    }

    /** Returns true when both toggles are No — skip Firestore write. */
    private boolean shouldSkipToFormBuilder(boolean hasRegFee, boolean hasAccommodation) {
        return !hasRegFee && !hasAccommodation;
    }

    // ── Mirror of Firestore payload from wireSave() ────────────────────────

    private Map<String, Object> buildPayload(boolean hasRegFee, String regFee, String regBank,
                                             boolean hasAccom, String accomFee, String accomBank,
                                             String deadline) {
        Map<String, Object> data = new HashMap<>();
        data.put("hasRegFee",             hasRegFee);
        data.put("regFee",                regFee);
        data.put("regBankInfo",           regBank);
        data.put("hasAccommodation",      hasAccom);
        data.put("accommodationFee",      accomFee);
        data.put("accommodationBankInfo", accomBank);
        data.put("registrationDeadline",  deadline);
        return data;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Validation — registration fee
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void testRegFeeYesWithAmountPassesValidation() {
        assertNull(validate(true, "500", false, ""));
    }

    @Test
    public void testRegFeeYesWithEmptyAmountFailsValidation() {
        assertNotNull(validate(true, "", false, ""));
    }

    @Test
    public void testRegFeeYesWithNullAmountFailsValidation() {
        assertNotNull(validate(true, null, false, ""));
    }

    @Test
    public void testRegFeeNoSkipsAmountValidation() {
        // Even with blank fee, No means no validation needed
        assertNull(validate(false, "", false, ""));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Validation — accommodation fee
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void testAccomYesWithAmountPassesValidation() {
        assertNull(validate(false, "", true, "1000"));
    }

    @Test
    public void testAccomYesWithEmptyAmountFailsValidation() {
        assertNotNull(validate(false, "", true, ""));
    }

    @Test
    public void testBothYesBothFilledPassesValidation() {
        assertNull(validate(true, "500", true, "1000"));
    }

    @Test
    public void testBothYesOnlyAccomFilledFailsAtRegFee() {
        String err = validate(true, "", true, "1000");
        assertNotNull(err);
        assertTrue(err.contains("registration fee"));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Both-No shortcut path
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void testBothNoSkipsFirestoreAndGoesDirectlyToFormBuilder() {
        assertTrue(shouldSkipToFormBuilder(false, false));
    }

    @Test
    public void testRegFeeYesDoesNotSkipToFormBuilder() {
        assertFalse(shouldSkipToFormBuilder(true, false));
    }

    @Test
    public void testAccomYesDoesNotSkipToFormBuilder() {
        assertFalse(shouldSkipToFormBuilder(false, true));
    }

    @Test
    public void testBothYesDoesNotSkipToFormBuilder() {
        assertFalse(shouldSkipToFormBuilder(true, true));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Payload builder
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void testPayloadContainsAllKeys() {
        Map<String, Object> p = buildPayload(true, "500", "HBL 1234",
                false, "", "", "May 10, 2026");
        assertTrue(p.containsKey("hasRegFee"));
        assertTrue(p.containsKey("regFee"));
        assertTrue(p.containsKey("regBankInfo"));
        assertTrue(p.containsKey("hasAccommodation"));
        assertTrue(p.containsKey("accommodationFee"));
        assertTrue(p.containsKey("accommodationBankInfo"));
        assertTrue(p.containsKey("registrationDeadline"));
    }

    @Test
    public void testPayloadRegFeeStoredCorrectly() {
        Map<String, Object> p = buildPayload(true, "750", "MCB 5678",
                false, "", "", "");
        assertEquals(true, p.get("hasRegFee"));
        assertEquals("750", p.get("regFee"));
        assertEquals("MCB 5678", p.get("regBankInfo"));
    }

    @Test
    public void testPayloadAccomFeeStoredCorrectly() {
        Map<String, Object> p = buildPayload(false, "", "",
                true, "1200", "Jazz 0300", "Apr 30, 2026");
        assertEquals(true, p.get("hasAccommodation"));
        assertEquals("1200", p.get("accommodationFee"));
        assertEquals("Apr 30, 2026", p.get("registrationDeadline"));
    }

    @Test
    public void testPayloadWhenBothNoHasEmptyFees() {
        Map<String, Object> p = buildPayload(false, "", "",
                false, "", "", "");
        assertEquals(false, p.get("hasRegFee"));
        assertEquals(false, p.get("hasAccommodation"));
        assertEquals("", p.get("regFee"));
        assertEquals("", p.get("accommodationFee"));
    }
}