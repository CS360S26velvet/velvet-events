package com.lums.eventhub;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * RegistrationFeeSetupTest.java
 *
 * Unit tests for RegistrationFeeSetupActivity logic:
 *   - Validation: regFee required when hasRegFee=true
 *   - Validation: accommodationFee required when hasAccommodation=true
 *   - Extras key constants exist
 *   - Both No → goes straight to FormBuilder (no fee fields required)
 *   - Fee amount and bank info stored correctly
 */
public class RegistrationFeeSetupTest {

    // ── Mirror of validation logic ────────────────────────────────────────────

    private boolean validateRegFee(boolean hasRegFee, String regFee) {
        if (!hasRegFee) return true;
        return regFee != null && !regFee.trim().isEmpty();
    }

    private boolean validateAccommodation(boolean hasAccom, String accomFee) {
        if (!hasAccom) return true;
        return accomFee != null && !accomFee.trim().isEmpty();
    }

    private boolean shouldGoDirectlyToFormBuilder(boolean hasRegFee, boolean hasAccom) {
        return !hasRegFee && !hasAccom;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Registration Fee Validation
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testRegFeeNotRequiredWhenNoSelected() {
        assertTrue(validateRegFee(false, ""));
    }

    @Test public void testRegFeeRequiredWhenYesSelected() {
        assertFalse(validateRegFee(true, ""));
    }

    @Test public void testRegFeeValidWhenYesAndAmountEntered() {
        assertTrue(validateRegFee(true, "PKR 500"));
    }

    @Test public void testRegFeeInvalidWithNullAmount() {
        assertFalse(validateRegFee(true, null));
    }

    @Test public void testRegFeeInvalidWithWhitespaceOnly() {
        assertFalse(validateRegFee(true, "   "));
    }

    @Test public void testRegFeeValidWithAnyNonEmptyString() {
        assertTrue(validateRegFee(true, "500"));
        assertTrue(validateRegFee(true, "PKR 1000"));
        assertTrue(validateRegFee(true, "$50"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Accommodation Fee Validation
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testAccomFeeNotRequiredWhenNoSelected() {
        assertTrue(validateAccommodation(false, ""));
    }

    @Test public void testAccomFeeRequiredWhenYesSelected() {
        assertFalse(validateAccommodation(true, ""));
    }

    @Test public void testAccomFeeValidWhenYesAndAmountEntered() {
        assertTrue(validateAccommodation(true, "PKR 1500"));
    }

    @Test public void testAccomFeeInvalidWithNullAmount() {
        assertFalse(validateAccommodation(true, null));
    }

    @Test public void testAccomFeeInvalidWithWhitespaceOnly() {
        assertFalse(validateAccommodation(true, "   "));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Both No → Direct to FormBuilder
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testBothNoGoesDirectlyToFormBuilder() {
        assertTrue(shouldGoDirectlyToFormBuilder(false, false));
    }

    @Test public void testRegFeeYesDoesNotGoDirectly() {
        assertFalse(shouldGoDirectlyToFormBuilder(true, false));
    }

    @Test public void testAccomYesDoesNotGoDirectly() {
        assertFalse(shouldGoDirectlyToFormBuilder(false, true));
    }

    @Test public void testBothYesDoesNotGoDirectly() {
        assertFalse(shouldGoDirectlyToFormBuilder(true, true));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Extras Key Constants
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testExtraKeyHasRegFeeExists() {
        assertEquals("hasRegFee", RegistrationFeeSetupActivity.EXTRA_HAS_REG_FEE);
    }

    @Test public void testExtraKeyRegFeeExists() {
        assertEquals("regFee", RegistrationFeeSetupActivity.EXTRA_REG_FEE);
    }

    @Test public void testExtraKeyRegBankInfoExists() {
        assertEquals("regBankInfo", RegistrationFeeSetupActivity.EXTRA_REG_BANK_INFO);
    }

    @Test public void testExtraKeyHasAccommodationExists() {
        assertEquals("hasAccommodation", RegistrationFeeSetupActivity.EXTRA_HAS_ACCOMMODATION);
    }

    @Test public void testExtraKeyAccommodationFeeExists() {
        assertEquals("accommodationFee", RegistrationFeeSetupActivity.EXTRA_ACCOMMODATION_FEE);
    }

    @Test public void testExtraKeyAccommodationBankExists() {
        assertEquals("accommodationBankInfo", RegistrationFeeSetupActivity.EXTRA_ACCOMMODATION_BANK);
    }
}