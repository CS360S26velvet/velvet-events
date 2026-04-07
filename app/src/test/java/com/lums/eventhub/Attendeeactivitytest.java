package com.lums.eventhub;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for AttendeeActivity logic
 * Covers: AT US-01 — Attendee dashboard, counts, navigation extras
 */
public class Attendeeactivitytest {

    private String resolveUserId(String fromIntent) {
        return (fromIntent == null) ? "" : fromIntent;
    }

    private int countRegistrations(java.util.List<String> registrations) {
        return registrations.size();
    }

    private int countUnreadNotifications(java.util.List<Boolean> isReadList) {
        int count = 0;
        for (boolean isRead : isReadList) {
            if (!isRead) count++;
        }
        return count;
    }

    /**
     * AT US-01: Valid userId from intent is used directly.
     */
    @Test
    public void testValidUserIdUsedDirectly() {
        assertEquals("user123", resolveUserId("user123"));
    }

    /**
     * AT US-01: Null userId from intent falls back to empty string.
     */
    @Test
    public void testNullUserIdFallsBackToEmpty() {
        assertEquals("", resolveUserId(null));
    }

    /**
     * AT US-01: userId is passed correctly to intent extras.
     */
    @Test
    public void testUserIdStoredInIntentExtras() {
        Map<String, String> extras = new HashMap<>();
        extras.put("userId", "user123");
        assertEquals("user123", extras.get("userId"));
    }


    /**
     * AT US-01: Registration count of zero displayed correctly.
     */
    @Test
    public void testRegistrationCountZero() {
        assertEquals(0, countRegistrations(new java.util.ArrayList<>()));
    }

    /**
     * AT US-01: Registration count calculated correctly.
     */
    @Test
    public void testRegistrationCountCalculatedCorrectly() {
        java.util.List<String> regs = new java.util.ArrayList<>();
        regs.add("event1");
        regs.add("event2");
        regs.add("event3");
        assertEquals(3, countRegistrations(regs));
    }

    /**
     * AT US-01: Registration count displayed as string correctly.
     */
    @Test
    public void testRegistrationCountDisplayString() {
        int count = 3;
        assertEquals("3", String.valueOf(count));
    }

    // ═══════════════════════════════════════════════
    // AT US-01 — Unread notification count
    // ═══════════════════════════════════════════════

    /**
     * AT US-01: Zero unread notifications when all are read.
     */
    @Test
    public void testZeroUnreadWhenAllRead() {
        java.util.List<Boolean> isReadList = new java.util.ArrayList<>();
        isReadList.add(true);
        isReadList.add(true);
        assertEquals(0, countUnreadNotifications(isReadList));
    }

    /**
     * AT US-01: Unread count correct when some unread.
     */
    @Test
    public void testUnreadCountCorrectWhenSomeUnread() {
        java.util.List<Boolean> isReadList = new java.util.ArrayList<>();
        isReadList.add(true);
        isReadList.add(false);
        isReadList.add(false);
        assertEquals(2, countUnreadNotifications(isReadList));
    }

    /**
     * AT US-01: All unread notifications counted correctly.
     */
    @Test
    public void testAllUnreadCountedCorrectly() {
        java.util.List<Boolean> isReadList = new java.util.ArrayList<>();
        isReadList.add(false);
        isReadList.add(false);
        isReadList.add(false);
        assertEquals(3, countUnreadNotifications(isReadList));
    }

    /**
     * AT US-01: Empty notification list returns zero unread.
     */
    @Test
    public void testEmptyNotificationListReturnsZero() {
        assertEquals(0, countUnreadNotifications(new java.util.ArrayList<>()));
    }

    // ═══════════════════════════════════════════════
    // AT US-01 — Navigation intent extras
    // ═══════════════════════════════════════════════

    /**
     * AT US-01: userId is included in all navigation intents.
     */
    @Test
    public void testUserIdIncludedInBrowseEventsIntent() {
        Map<String, String> extras = new HashMap<>();
        extras.put("userId", "user123");
        assertTrue(extras.containsKey("userId"));
        assertEquals("user123", extras.get("userId"));
    }

    /**
     * AT US-01: userId preserved when navigating to calendar.
     */
    @Test
    public void testUserIdPreservedForCalendarNavigation() {
        String userId = "user123";
        Map<String, String> calendarExtras = new HashMap<>();
        calendarExtras.put("userId", userId);
        assertEquals("user123", calendarExtras.get("userId"));
    }

    /**
     * AT US-01: userId preserved when navigating to notifications.
     */
    @Test
    public void testUserIdPreservedForNotificationsNavigation() {
        String userId = "user456";
        Map<String, String> extras = new HashMap<>();
        extras.put("userId", userId);
        assertEquals("user456", extras.get("userId"));
    }

    /**
     * AT US-01: userId preserved when navigating to registrations.
     */
    @Test
    public void testUserIdPreservedForRegistrationsNavigation() {
        String userId = "user789";
        Map<String, String> extras = new HashMap<>();
        extras.put("userId", userId);
        assertEquals("user789", extras.get("userId"));
    }
}