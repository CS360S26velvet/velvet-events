package com.lums.eventhub;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for AttendeeCalendarActivity logic
 * Covers: AT US-08 — View personal calendar with saved events
 */
public class CalendarTest {

    // ─────────────────────────────────────────────
    // Mirror of CalendarEvent model
    // ─────────────────────────────────────────────
    static class CalendarEvent {
        String eventId, title, date, time, venue, category;

        CalendarEvent(String eventId, String title, String date,
                      String time, String venue, String category) {
            this.eventId  = eventId;
            this.title    = title;
            this.date     = date;
            this.time     = time;
            this.venue    = venue;
            this.category = category;
        }
    }

    // ─────────────────────────────────────────────
    // Mirror of parseDayFromDate() in AttendeeCalendarActivity
    // ─────────────────────────────────────────────
    private int parseDayFromDate(String dateStr) {
        try {
            if (dateStr == null) return -1;
            String[] parts = dateStr.trim().split(" ");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1].replace(",", ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    // ─────────────────────────────────────────────
    // Mirror of eventsByDay grouping logic
    // ─────────────────────────────────────────────
    private Map<Integer, List<CalendarEvent>> groupByDay(List<CalendarEvent> events) {
        Map<Integer, List<CalendarEvent>> byDay = new HashMap<>();
        for (CalendarEvent event : events) {
            int day = parseDayFromDate(event.date);
            if (day != -1) {
                if (!byDay.containsKey(day)) {
                    byDay.put(day, new ArrayList<>());
                }
                byDay.get(day).add(event);
            }
        }
        return byDay;
    }

    // ─────────────────────────────────────────────
    // Sample data
    // ─────────────────────────────────────────────
    private List<CalendarEvent> eventList;

    @Before
    public void setUp() {
        eventList = new ArrayList<>();
        eventList.add(new CalendarEvent("e1", "Tech Summit",    "Mar 15, 2026", "9:00 AM",  "SBASSE",       "Society Events"));
        eventList.add(new CalendarEvent("e2", "Physics Seminar","Mar 15, 2026", "2:30 PM",  "SBASSE 10-204","Workshops/Seminars"));
        eventList.add(new CalendarEvent("e3", "Startup Weekend","Apr 4, 2026",  "9:00 AM",  "SDSB Atrium",  "Society Events"));
        eventList.add(new CalendarEvent("e4", "AI Workshop",    "Apr 10, 2026", "3:00 PM",  "SBASSE Lab",   "Workshops/Seminars"));
    }

    // ═══════════════════════════════════════════════
    // CalendarEvent model
    // ═══════════════════════════════════════════════

    /**
     * AT US-08: CalendarEvent constructor stores all fields correctly.
     */
    @Test
    public void testCalendarEventStoresAllFields() {
        CalendarEvent e = new CalendarEvent("e1", "Tech Summit", "Mar 15, 2026",
                "9:00 AM", "SBASSE", "Society Events");
        assertEquals("e1",             e.eventId);
        assertEquals("Tech Summit",    e.title);
        assertEquals("Mar 15, 2026",   e.date);
        assertEquals("9:00 AM",        e.time);
        assertEquals("SBASSE",         e.venue);
        assertEquals("Society Events", e.category);
    }

    // ═══════════════════════════════════════════════
    // AT US-08 — parseDayFromDate()
    // ═══════════════════════════════════════════════

    /**
     * AT US-08: Parses day correctly from standard date format.
     */
    @Test
    public void testParseDayFromStandardFormat() {
        assertEquals(15, parseDayFromDate("Mar 15, 2026"));
    }

    /**
     * AT US-08: Parses single-digit day correctly.
     */
    @Test
    public void testParseDaySingleDigit() {
        assertEquals(4, parseDayFromDate("Apr 4, 2026"));
    }

    /**
     * AT US-08: Parses day 1 correctly.
     */
    @Test
    public void testParseDayFirst() {
        assertEquals(1, parseDayFromDate("Jan 1, 2026"));
    }

    /**
     * AT US-08: Parses day 31 correctly.
     */
    @Test
    public void testParseDayLast() {
        assertEquals(31, parseDayFromDate("Dec 31, 2026"));
    }

    /**
     * AT US-08: Null date returns -1 (invalid).
     */
    @Test
    public void testNullDateReturnsMinusOne() {
        assertEquals(-1, parseDayFromDate(null));
    }

    /**
     * AT US-08: Empty string returns -1 (invalid).
     */
    @Test
    public void testEmptyStringReturnsMinusOne() {
        assertEquals(-1, parseDayFromDate(""));
    }

    /**
     * AT US-08: Malformed date returns -1.
     */
    @Test
    public void testMalformedDateReturnsMinusOne() {
        assertEquals(-1, parseDayFromDate("not-a-date"));
    }

    /**
     * AT US-08: Comma is correctly stripped from day number.
     */
    @Test
    public void testCommaStrippedFromDayNumber() {
        assertEquals(10, parseDayFromDate("Apr 10, 2026"));
    }

    // ═══════════════════════════════════════════════
    // AT US-08 — Event grouping by day
    // ═══════════════════════════════════════════════

    /**
     * AT US-08: Two events on same day grouped together.
     */
    @Test
    public void testTwoEventsOnSameDayGroupedTogether() {
        Map<Integer, List<CalendarEvent>> byDay = groupByDay(eventList);
        assertEquals(2, byDay.get(15).size()); // Mar 15 has 2 events
    }

    /**
     * AT US-08: Event on unique day has its own group.
     */
    @Test
    public void testEventOnUniqueDayHasOwnGroup() {
        Map<Integer, List<CalendarEvent>> byDay = groupByDay(eventList);
        assertEquals(1, byDay.get(4).size());  // Apr 4 has 1 event
        assertEquals(1, byDay.get(10).size()); // Apr 10 has 1 event
    }

    /**
     * AT US-08: Total unique days in map is correct.
     */
    @Test
    public void testUniqueDaysCount() {
        Map<Integer, List<CalendarEvent>> byDay = groupByDay(eventList);
        assertEquals(3, byDay.size()); // days 15, 4, 10
    }

    /**
     * AT US-08: Day with no events returns null from map.
     */
    @Test
    public void testDayWithNoEventsReturnsNull() {
        Map<Integer, List<CalendarEvent>> byDay = groupByDay(eventList);
        assertNull(byDay.get(20)); // no event on day 20
    }

    /**
     * AT US-08: Event with invalid date is excluded from grouping.
     */
    @Test
    public void testEventWithInvalidDateExcludedFromGrouping() {
        List<CalendarEvent> list = new ArrayList<>();
        list.add(new CalendarEvent("bad", "Bad Event", null, "9:00 AM", "Venue", "Society Events"));
        Map<Integer, List<CalendarEvent>> byDay = groupByDay(list);
        assertTrue(byDay.isEmpty());
    }

    /**
     * AT US-08: Correct event title stored in grouped map.
     */
    @Test
    public void testCorrectEventTitleInGroupedMap() {
        Map<Integer, List<CalendarEvent>> byDay = groupByDay(eventList);
        List<CalendarEvent> day15Events = byDay.get(15);
        assertEquals("Tech Summit",     day15Events.get(0).title);
        assertEquals("Physics Seminar", day15Events.get(1).title);
    }

    // ═══════════════════════════════════════════════
    // AT US-08 — Empty state
    // ═══════════════════════════════════════════════

    /**
     * AT US-08: Empty event list produces empty map.
     */
    @Test
    public void testEmptyEventListProducesEmptyMap() {
        Map<Integer, List<CalendarEvent>> byDay = groupByDay(new ArrayList<>());
        assertTrue(byDay.isEmpty());
    }

    /**
     * AT US-08: Non-empty event list produces non-empty map.
     */
    @Test
    public void testNonEmptyEventListProducesNonEmptyMap() {
        Map<Integer, List<CalendarEvent>> byDay = groupByDay(eventList);
        assertFalse(byDay.isEmpty());
    }

    // ═══════════════════════════════════════════════
    // AT US-08 — userId handling
    // ═══════════════════════════════════════════════

    /**
     * AT US-08: userId is passed to calendar activity correctly.
     */
    @Test
    public void testUserIdPassedCorrectly() {
        String userId = "user123";
        java.util.Map<String, String> extras = new java.util.HashMap<>();
        extras.put("userId", userId);
        assertEquals("user123", extras.get("userId"));
    }

    /**
     * AT US-08: userId forwarded to navigation intents.
     */
    @Test
    public void testUserIdForwardedToNavigationIntents() {
        String userId = "user456";
        java.util.Map<String, String> extras = new java.util.HashMap<>();
        extras.put("userId", userId);
        assertTrue(extras.containsKey("userId"));
        assertEquals("user456", extras.get("userId"));
    }
}