package com.example.event_management;

import org.junit.Test;
import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventDetailsTest {

    // ============================================================
    // SEAT AVAILABILITY TESTS
    // ============================================================

    @Test
    public void testSeatsAvailable_calculatesCorrectly() {
        int seatsBooked = 200;
        int seatsTotal = 500;
        int availableSeats = seatsTotal - seatsBooked;
        assertEquals(300, availableSeats);
    }

    @Test
    public void testEventFullyBooked_returnsTrue() {
        int seatsBooked = 500;
        int seatsTotal = 500;
        int availableSeats = seatsTotal - seatsBooked;
        assertTrue(availableSeats <= 0);
    }

    @Test
    public void testEventNotFullyBooked_returnsFalse() {
        int seatsBooked = 200;
        int seatsTotal = 500;
        int availableSeats = seatsTotal - seatsBooked;
        assertFalse(availableSeats <= 0);
    }

    @Test
    public void testSeatsPercentage_calculatesCorrectly() {
        int seatsBooked = 200;
        int seatsTotal = 500;
        int percentFull = (seatsBooked * 100) / seatsTotal;
        assertEquals(40, percentFull);
    }

    @Test
    public void testSeatsPercentage_zeroTotal_doesNotCrash() {
        int seatsBooked = 0;
        int seatsTotal = 0;
        int percentFull = 0;
        if (seatsTotal > 0) {
            percentFull =(seatsBooked * 100)/seatsTotal;
        }
        assertEquals(0, percentFull); // should stay 0, not divide by zero
    }

    @Test
    public void testAlmostFull_lessThan20Percent_detected() {
        int seatsBooked = 490;
        int seatsTotal = 500;
        int availableSeats = seatsTotal - seatsBooked;
        assertTrue(availableSeats < seatsTotal * 0.2);
    }


    // REGISTRATION OPEN/CLOSED TESTS

    private boolean isRegistrationOpen(String eventDate) throws ParseException {
        Date currentDate = new Date();
        SimpleDateFormat formatted_date = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        Date formatted_deadline = formatted_date.parse(eventDate);
        return currentDate.before(formatted_deadline);
    }

    @Test
    public void testRegistrationOpen_futureDateReturnsTrue() throws ParseException {
        // A date far in the future should always be open
        boolean isOpen = isRegistrationOpen("Dec 31, 2099");
        assertTrue(isOpen);
    }

    @Test
    public void testRegistrationClosed_pastDateReturnsFalse() throws ParseException {
        // A date in the past should always be closed
        boolean isOpen = isRegistrationOpen("Jan 1, 2000");
        assertFalse(isOpen);
    }
}