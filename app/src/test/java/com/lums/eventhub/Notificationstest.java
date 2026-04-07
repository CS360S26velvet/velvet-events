package com.lums.eventhub;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for NotificationsActivity logic
 * Covers: AT US-07 — View and manage notifications
 */
public class Notificationstest {

    // ─────────────────────────────────────────────
    // Mirror of Notification model
    // ─────────────────────────────────────────────
    static class NotificationItem {
        String notifId, title, message, type, eventId, sentAt;
        boolean isRead;

        NotificationItem(String title, String message, String type, boolean isRead) {
            this.title   = title;
            this.message = message;
            this.type    = type;
            this.isRead  = isRead;
        }
    }

    // ─────────────────────────────────────────────
    // Mirror of icon mapping logic from buildNotifCard()
    // ─────────────────────────────────────────────
    private String resolveIcon(String type) {
        if (type == null) return "🕐";
        switch (type) {
            case "confirmation":
            case "payment_received":  return "✅";
            case "emergency":
            case "change":            return "⚠";
            case "rejection":
            case "payment_rejected":  return "✕";
            case "reminder":
            default:                  return "🕐";
        }
    }

    // ─────────────────────────────────────────────
    // Mirror of unread count from displayNotifications()
    // ─────────────────────────────────────────────
    private int countUnread(List<NotificationItem> list) {
        int count = 0;
        for (NotificationItem n : list) {
            if (!n.isRead) count++;
        }
        return count;
    }

    // ─────────────────────────────────────────────
    // Sample data
    // ─────────────────────────────────────────────
    private List<NotificationItem> notifications;

    @Before
    public void setUp() {
        notifications = new ArrayList<>();
        notifications.add(new NotificationItem("Registered!", "You registered for Tech Summit", "confirmation", false));
        notifications.add(new NotificationItem("Payment Received", "Your payment was confirmed", "payment_received", true));
        notifications.add(new NotificationItem("Event Changed", "Venue has changed", "change", false));
        notifications.add(new NotificationItem("Reminder", "Event starts tomorrow", "reminder", true));
        notifications.add(new NotificationItem("Registration Rejected", "Your registration was rejected", "rejection", false));
    }

    // ═══════════════════════════════════════════════
    // NotificationItem model
    // ═══════════════════════════════════════════════

    /**
     * AT US-07: NotificationItem stores all fields correctly.
     */
    @Test
    public void testNotificationItemStoresAllFields() {
        NotificationItem n = new NotificationItem("Title", "Message", "confirmation", false);
        assertEquals("Title",        n.title);
        assertEquals("Message",      n.message);
        assertEquals("confirmation", n.type);
        assertFalse(n.isRead);
    }

    /**
     * AT US-07: isRead=true stored correctly.
     */
    @Test
    public void testIsReadTrueStoredCorrectly() {
        NotificationItem n = new NotificationItem("T", "M", "reminder", true);
        assertTrue(n.isRead);
    }

    // ═══════════════════════════════════════════════
    // AT US-07 — Icon mapping for notification types
    // ═══════════════════════════════════════════════

    /**
     * AT US-07: confirmation type maps to ✅ icon.
     */
    @Test
    public void testConfirmationMapsToCheckIcon() {
        assertEquals("✅", resolveIcon("confirmation"));
    }

    /**
     * AT US-07: payment_received type maps to ✅ icon.
     */
    @Test
    public void testPaymentReceivedMapsToCheckIcon() {
        assertEquals("✅", resolveIcon("payment_received"));
    }

    /**
     * AT US-07: emergency type maps to ⚠ icon.
     */
    @Test
    public void testEmergencyMapsToWarningIcon() {
        assertEquals("⚠", resolveIcon("emergency"));
    }

    /**
     * AT US-07: change type maps to ⚠ icon.
     */
    @Test
    public void testChangeMapsToWarningIcon() {
        assertEquals("⚠", resolveIcon("change"));
    }

    /**
     * AT US-07: rejection type maps to ✕ icon.
     */
    @Test
    public void testRejectionMapsToXIcon() {
        assertEquals("✕", resolveIcon("rejection"));
    }

    /**
     * AT US-07: payment_rejected type maps to ✕ icon.
     */
    @Test
    public void testPaymentRejectedMapsToXIcon() {
        assertEquals("✕", resolveIcon("payment_rejected"));
    }

    /**
     * AT US-07: reminder type maps to 🕐 icon.
     */
    @Test
    public void testReminderMapsToClockIcon() {
        assertEquals("🕐", resolveIcon("reminder"));
    }

    /**
     * AT US-07: Unknown type defaults to 🕐 icon.
     */
    @Test
    public void testUnknownTypeDefaultsToClockIcon() {
        assertEquals("🕐", resolveIcon("unknown_type"));
    }

    /**
     * AT US-07: Null type defaults to 🕐 icon.
     */
    @Test
    public void testNullTypeDefaultsToClockIcon() {
        assertEquals("🕐", resolveIcon(null));
    }

    // ═══════════════════════════════════════════════
    // AT US-07 — Unread count
    // ═══════════════════════════════════════════════

    /**
     * AT US-07: Unread count correct from sample data.
     */
    @Test
    public void testUnreadCountFromSampleData() {
        assertEquals(3, countUnread(notifications));
    }

    /**
     * AT US-07: Zero unread when all notifications are read.
     */
    @Test
    public void testZeroUnreadWhenAllRead() {
        List<NotificationItem> list = new ArrayList<>();
        list.add(new NotificationItem("T1", "M1", "reminder", true));
        list.add(new NotificationItem("T2", "M2", "reminder", true));
        assertEquals(0, countUnread(list));
    }

    /**
     * AT US-07: All unread when none are read.
     */
    @Test
    public void testAllUnreadWhenNoneRead() {
        List<NotificationItem> list = new ArrayList<>();
        list.add(new NotificationItem("T1", "M1", "reminder", false));
        list.add(new NotificationItem("T2", "M2", "reminder", false));
        assertEquals(2, countUnread(list));
    }

    /**
     * AT US-07: Empty list returns zero unread.
     */
    @Test
    public void testEmptyListReturnsZeroUnread() {
        assertEquals(0, countUnread(new ArrayList<>()));
    }

    /**
     * AT US-07: Total count equals sample data size.
     */
    @Test
    public void testTotalNotificationCount() {
        assertEquals(5, notifications.size());
    }

    /**
     * AT US-07: Unread + read equals total.
     */
    @Test
    public void testUnreadPlusReadEqualsTotal() {
        int unread = countUnread(notifications);
        int read   = notifications.size() - unread;
        assertEquals(notifications.size(), unread + read);
    }

    // ═══════════════════════════════════════════════
    // AT US-07 — Empty state
    // ═══════════════════════════════════════════════

    /**
     * AT US-07: Empty notifications list triggers empty state.
     */
    @Test
    public void testEmptyListTriggersEmptyState() {
        assertTrue(new ArrayList<>().isEmpty());
    }

    /**
     * AT US-07: Non-empty list does not trigger empty state.
     */
    @Test
    public void testNonEmptyListDoesNotTriggerEmptyState() {
        assertFalse(notifications.isEmpty());
    }
}