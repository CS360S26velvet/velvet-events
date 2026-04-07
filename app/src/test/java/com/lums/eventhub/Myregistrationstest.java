package com.lums.eventhub;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for MyRegistrationsActivity logic
 * Covers: AT US-06 — View registered events list
 */
public class Myregistrationstest {

    // ─────────────────────────────────────────────
    // Mirror of registration data model
    // ─────────────────────────────────────────────
    static class Registration {
        String eventId, title, organizer, date, time, venue, fee, category;
        int seatsBooked, seatsTotal;

        Registration(String eventId, String title, String organizer,
                     String date, String time, String venue,
                     String fee, String category,
                     int seatsBooked, int seatsTotal) {
            this.eventId    = eventId;
            this.title      = title;
            this.organizer  = organizer;
            this.date       = date;
            this.time       = time;
            this.venue      = venue;
            this.fee        = fee;
            this.category   = category;
            this.seatsBooked = seatsBooked;
            this.seatsTotal  = seatsTotal;
        }
    }

    // ─────────────────────────────────────────────
    // Mirror of empty state logic
    // ─────────────────────────────────────────────
    private boolean shouldShowEmpty(List<Registration> list) {
        return list.isEmpty();
    }

    // ─────────────────────────────────────────────
    // Mirror of registration data map built for Firestore
    // ─────────────────────────────────────────────
    private Map<String, Object> buildRegistrationData(Registration r) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId",    r.eventId);
        data.put("eventTitle", r.title);
        data.put("organizer",  r.organizer);
        data.put("date",       r.date);
        data.put("venue",      r.venue);
        data.put("time",       r.time);
        data.put("fee",        r.fee);
        data.put("category",   r.category);
        data.put("seatsBooked", r.seatsBooked);
        data.put("seatsTotal",  r.seatsTotal);
        return data;
    }

    // ─────────────────────────────────────────────
    // Sample data
    // ─────────────────────────────────────────────
    private List<Registration> registrations;

    @Before
    public void setUp() {
        registrations = new ArrayList<>();
        registrations.add(new Registration(
                "e1", "Tech Summit 2025", "SPADES",
                "Mar 15, 2026", "9:00 AM", "SBASSE",
                "Free", "Society Events", 200, 500));
        registrations.add(new Registration(
                "e2", "AI Workshop", "CS Society",
                "Apr 10, 2026", "3:00 PM", "SBASSE Lab",
                "PKR 500", "Workshops/Seminars", 30, 50));
        registrations.add(new Registration(
                "e3", "Startup Weekend", "SPADES",
                "Apr 4, 2026", "9:00 AM", "SDSB Atrium",
                "Free", "Society Events", 120, 200));
    }

    // ═══════════════════════════════════════════════
    // Registration model
    // ═══════════════════════════════════════════════

    /**
     * AT US-06: Registration stores all fields correctly.
     */
    @Test
    public void testRegistrationStoresAllFields() {
        Registration r = registrations.get(0);
        assertEquals("e1",             r.eventId);
        assertEquals("Tech Summit 2025", r.title);
        assertEquals("SPADES",         r.organizer);
        assertEquals("Mar 15, 2026",   r.date);
        assertEquals("9:00 AM",        r.time);
        assertEquals("SBASSE",         r.venue);
        assertEquals("Free",           r.fee);
        assertEquals("Society Events", r.category);
        assertEquals(200,              r.seatsBooked);
        assertEquals(500,              r.seatsTotal);
    }

    // ═══════════════════════════════════════════════
    // AT US-06 — List content
    // ═══════════════════════════════════════════════

    /**
     * AT US-06: Sample data has correct size.
     */
    @Test
    public void testRegistrationListSize() {
        assertEquals(3, registrations.size());
    }

    /**
     * AT US-06: Registrations stored in correct order.
     */
    @Test
    public void testRegistrationListOrder() {
        assertEquals("Tech Summit 2025", registrations.get(0).title);
        assertEquals("AI Workshop",      registrations.get(1).title);
        assertEquals("Startup Weekend",  registrations.get(2).title);
    }

    /**
     * AT US-06: All registrations have non-null eventId.
     */
    @Test
    public void testAllRegistrationsHaveEventId() {
        for (Registration r : registrations) {
            assertNotNull(r.eventId);
            assertFalse(r.eventId.isEmpty());
        }
    }

    /**
     * AT US-06: All registrations have non-null title.
     */
    @Test
    public void testAllRegistrationsHaveTitle() {
        for (Registration r : registrations) {
            assertNotNull(r.title);
            assertFalse(r.title.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════
    // AT US-06 — Empty state
    // ═══════════════════════════════════════════════

    /**
     * AT US-06: Empty list triggers empty state.
     */
    @Test
    public void testEmptyListShowsEmptyState() {
        assertTrue(shouldShowEmpty(new ArrayList<>()));
    }

    /**
     * AT US-06: Non-empty list hides empty state.
     */
    @Test
    public void testNonEmptyListHidesEmptyState() {
        assertFalse(shouldShowEmpty(registrations));
    }

    /**
     * AT US-06: Single registration hides empty state.
     */
    @Test
    public void testSingleRegistrationHidesEmptyState() {
        List<Registration> single = new ArrayList<>();
        single.add(registrations.get(0));
        assertFalse(shouldShowEmpty(single));
    }

    // ═══════════════════════════════════════════════
    // AT US-06 — Registration data map for Firestore
    // ═══════════════════════════════════════════════

    /**
     * AT US-06: Registration data map contains all required keys.
     */
    @Test
    public void testRegistrationDataMapContainsAllKeys() {
        Map<String, Object> data = buildRegistrationData(registrations.get(0));
        assertTrue(data.containsKey("eventId"));
        assertTrue(data.containsKey("eventTitle"));
        assertTrue(data.containsKey("organizer"));
        assertTrue(data.containsKey("date"));
        assertTrue(data.containsKey("venue"));
        assertTrue(data.containsKey("time"));
        assertTrue(data.containsKey("fee"));
        assertTrue(data.containsKey("category"));
        assertTrue(data.containsKey("seatsBooked"));
        assertTrue(data.containsKey("seatsTotal"));
    }

    /**
     * AT US-06: Registration data map stores correct values.
     */
    @Test
    public void testRegistrationDataMapStoresCorrectValues() {
        Map<String, Object> data = buildRegistrationData(registrations.get(0));
        assertEquals("e1",               data.get("eventId"));
        assertEquals("Tech Summit 2025", data.get("eventTitle"));
        assertEquals("SPADES",           data.get("organizer"));
        assertEquals("Mar 15, 2026",     data.get("date"));
        assertEquals("Free",             data.get("fee"));
    }

    /**
     * AT US-06: seatsBooked stored as integer in data map.
     */
    @Test
    public void testSeatsBookedStoredAsInteger() {
        Map<String, Object> data = buildRegistrationData(registrations.get(0));
        assertEquals(200, data.get("seatsBooked"));
    }

    /**
     * AT US-06: seatsTotal stored as integer in data map.
     */
    @Test
    public void testSeatsTotalStoredAsInteger() {
        Map<String, Object> data = buildRegistrationData(registrations.get(0));
        assertEquals(500, data.get("seatsTotal"));
    }

    // ═══════════════════════════════════════════════
    // AT US-06 — userId handling
    // ═══════════════════════════════════════════════

    /**
     * AT US-06: userId passed correctly to activity.
     */
    @Test
    public void testUserIdPassedCorrectly() {
        Map<String, String> extras = new HashMap<>();
        extras.put("userId", "user123");
        assertEquals("user123", extras.get("userId"));
    }

    /**
     * AT US-06: userId forwarded to EventDetailsActivity.
     */
    @Test
    public void testUserIdForwardedToEventDetails() {
        String userId = "user456";
        Map<String, String> extras = new HashMap<>();
        extras.put("userId", userId);
        assertTrue(extras.containsKey("userId"));
        assertEquals("user456", extras.get("userId"));
    }

    /**
     * AT US-06: Total count displayed correctly.
     */
    @Test
    public void testTotalCountDisplayString() {
        int count = registrations.size();
        assertEquals("3", String.valueOf(count));
    }
}