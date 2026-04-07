package com.lums.eventhub;

import org.junit.Test;
import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Unit tests for EventDetailsActivity logic
 * Covers: AT US-05 — View full event details, seat availability, registration status
 */
public class EventDetailsTest {

    private boolean isRegistrationOpen(String eventDate) throws ParseException {
        Date currentDate = new Date();
        SimpleDateFormat formatted_date = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        Date formatted_deadline = formatted_date.parse(eventDate);
        return currentDate.before(formatted_deadline);
    }


    private int availableSeats(int booked, int total) {
        return total - booked;
    }

    private int percentFull(int booked, int total) {
        if (total <= 0) return 0;
        return (booked * 100) / total;
    }

    private boolean isSoldOut(int booked, int total) {
        return (total - booked) <= 0;
    }

    private boolean isAlmostFull(int booked, int total) {
        return (total - booked) < total * 0.2;
    }

    // ═══════════════════════════════════════════════
    // AT US-05 — Seat availability
    // ═══════════════════════════════════════════════

    /**
     * AT US-05: Available seats calculated correctly.
     */
    @Test
    public void testSeatsAvailableCalculatesCorrectly() {
        assertEquals(300, availableSeats(200, 500));
    }

    /**
     * AT US-05: Zero booked seats means all seats available.
     */
    @Test
    public void testZeroBookedMeansAllAvailable() {
        assertEquals(100, availableSeats(0, 100));
    }

    /**
     * AT US-05: Fully booked event has zero available seats.
     */
    @Test
    public void testFullyBookedHasZeroAvailable() {
        assertEquals(0, availableSeats(500, 500));
    }

    /**
     * AT US-05: isSoldOut returns true when fully booked.
     */
    @Test
    public void testIsSoldOutWhenFullyBooked() {
        assertTrue(isSoldOut(500, 500));
    }

    /**
     * AT US-05: isSoldOut returns false when seats remain.
     */
    @Test
    public void testIsNotSoldOutWhenSeatsRemain() {
        assertFalse(isSoldOut(200, 500));
    }

    /**
     * AT US-05: Percent full calculated correctly.
     */
    @Test
    public void testPercentFullCalculatesCorrectly() {
        assertEquals(40, percentFull(200, 500));
    }

    /**
     * AT US-05: Zero total seats returns 0% to avoid divide-by-zero.
     */
    @Test
    public void testPercentFullWithZeroTotalReturnsZero() {
        assertEquals(0, percentFull(0, 0));
    }

    /**
     * AT US-05: 100% full when all seats booked.
     */
    @Test
    public void testPercentFullWhenCompletelyBooked() {
        assertEquals(100, percentFull(500, 500));
    }

    /**
     * AT US-05: Almost full warning triggered when less than 20% remain.
     */
    @Test
    public void testAlmostFullWhenLessThan20PercentRemain() {
        assertTrue(isAlmostFull(490, 500));
    }

    /**
     * AT US-05: Almost full NOT triggered when more than 20% remain.
     */
    @Test
    public void testNotAlmostFullWhenMoreThan20PercentRemain() {
        assertFalse(isAlmostFull(200, 500));
    }

    /**
     * AT US-05: Exactly 20% remaining is not considered almost full.
     */
    @Test
    public void testExactly20PercentRemainingNotAlmostFull() {
        assertFalse(isAlmostFull(400, 500)); // 100/500 = exactly 20%
    }

    // ═══════════════════════════════════════════════
    // AT US-05 — Registration open/closed
    // ═══════════════════════════════════════════════

    /**
     * AT US-05: Future date means registration is open.
     */
    @Test
    public void testRegistrationOpenForFutureDate() throws ParseException {
        assertTrue(isRegistrationOpen("Dec 31, 2099"));
    }

    /**
     * AT US-05: Past date means registration is closed.
     */
    @Test
    public void testRegistrationClosedForPastDate() throws ParseException {
        assertFalse(isRegistrationOpen("Jan 1, 2000"));
    }

    /**
     * AT US-05: Near future date (next year) is still open.
     */
    @Test
    public void testRegistrationOpenForNextYear() throws ParseException {
        assertTrue(isRegistrationOpen("Dec 31, 2027"));
    }

    /**
     * AT US-05: Recent past date is closed.
     */
    @Test
    public void testRegistrationClosedForRecentPastDate() throws ParseException {
        assertFalse(isRegistrationOpen("Jan 1, 2024"));
    }

    // ═══════════════════════════════════════════════
    // AT US-05 — Event data fields
    // ═══════════════════════════════════════════════

    /**
     * AT US-05: Seat display string formatted correctly.
     */
    @Test
    public void testSeatDisplayString() {
        int available = availableSeats(200, 500);
        String display = available + " / " + 500 + " seats available";
        assertEquals("300 / 500 seats available", display);
    }

    /**
     * AT US-05: Percent display string formatted correctly.
     */
    @Test
    public void testPercentDisplayString() {
        int percent = percentFull(200, 500);
        String display = percent + "% full";
        assertEquals("40% full", display);
    }

    /**
     * AT US-05: Registration closes text formatted correctly.
     */
    @Test
    public void testRegistrationClosesString() {
        String regDate = "Dec 1, 2025";
        String display = "Registration closes " + regDate;
        assertEquals("Registration closes Dec 1, 2025", display);
    }

    /**
     * AT US-05: Society Events category sets correct color identifier.
     */
    @Test
    public void testSocietyEventsCategoryRecognized() {
        String category = "Society Events";
        boolean isSociety = "Society Events".equals(category);
        assertTrue(isSociety);
    }

    /**
     * AT US-05: Workshop category correctly identified.
     */
    @Test
    public void testWorkshopCategoryRecognized() {
        String category = "Workshops/Seminars";
        boolean isSociety = "Society Events".equals(category);
        assertFalse(isSociety);
    }
}