package com.lums.eventhub;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for AttendeeRegistrationActivity logic
 *
 * Role: Organizer selects an event to build its registration form.
 * Shows event list loaded from Firestore. If empty → shows "No events yet" message.
 * Each event row has a "Build Reg Form" button → opens FormBuilderActivity.
 *
 * Covers: Org US-18 (Build Registration Form — event selection screen)
 *
 * Pure Java — no Android or Firestore dependencies.
 */
public class AttendeeRegistrationTest {

    // ─────────────────────────────────────────────
    // Mirror of AttendeeRegistrationActivity.EventItem
    // ─────────────────────────────────────────────

    static class EventItem {
        String id;
        String title;

        EventItem(String id, String title) {
            this.id    = id;
            this.title = title;
        }
    }

    // ─────────────────────────────────────────────
    // Mirror of showOrHideEmpty() logic
    // ─────────────────────────────────────────────

    private boolean shouldShowEmpty(List<EventItem> eventList) {
        return eventList.isEmpty();
    }

    // ─────────────────────────────────────────────
    // Mirror of loadEvents() title fallback logic
    // ─────────────────────────────────────────────

    private String resolveTitle(String title, String docId) {
        if (title == null || title.isEmpty()) return docId;
        return title;
    }

    // ─────────────────────────────────────────────
    // Mirror of organizerUsername fallback
    // ─────────────────────────────────────────────

    private String resolveOrganizerUsername(String fromIntent) {
        return (fromIntent == null) ? "ORG0012" : fromIntent;
    }

    // ─────────────────────────────────────────────
    // Sample data
    // ─────────────────────────────────────────────

    private List<EventItem> eventList;

    @Before
    public void setUp() {
        eventList = new ArrayList<>();
        eventList.add(new EventItem("evt001", "Tech Summit 2025"));
        eventList.add(new EventItem("evt002", "Annual Gala Night"));
        eventList.add(new EventItem("evt003", "Sports Day"));
    }

    // ═══════════════════════════════════════════════
    // EventItem model
    // ═══════════════════════════════════════════════

    /**
     * US-18: EventItem constructor stores id and title correctly.
     */
    @Test
    public void testEventItemConstructorStoresFields() {
        EventItem item = new EventItem("evt001", "Tech Summit 2025");
        assertEquals("evt001",           item.id);
        assertEquals("Tech Summit 2025", item.title);
    }

    /**
     * US-18: EventItem id is stored correctly.
     */
    @Test
    public void testEventItemIdStoredCorrectly() {
        EventItem item = new EventItem("SPADES2025", "SPADES Event");
        assertEquals("SPADES2025", item.id);
    }

    /**
     * US-18: EventItem title is stored correctly.
     */
    @Test
    public void testEventItemTitleStoredCorrectly() {
        EventItem item = new EventItem("id1", "Annual Gala Night");
        assertEquals("Annual Gala Night", item.title);
    }

    // ═══════════════════════════════════════════════
    // US-18 — Empty state logic (showOrHideEmpty)
    // ═══════════════════════════════════════════════

    /**
     * US-18: Empty list triggers the "No events yet" message.
     */
    @Test
    public void testEmptyListShowsEmptyState() {
        assertTrue(shouldShowEmpty(new ArrayList<>()));
    }

    /**
     * US-18: Non-empty list hides the "No events yet" message.
     */
    @Test
    public void testNonEmptyListHidesEmptyState() {
        assertFalse(shouldShowEmpty(eventList));
    }

    /**
     * US-18: List with exactly one event hides empty state.
     */
    @Test
    public void testSingleEventHidesEmptyState() {
        List<EventItem> single = new ArrayList<>();
        single.add(new EventItem("evt001", "Tech Summit"));
        assertFalse(shouldShowEmpty(single));
    }

    /**
     * US-18: After clearing a non-empty list, empty state is shown.
     */
    @Test
    public void testClearingListShowsEmptyState() {
        assertFalse(shouldShowEmpty(eventList));
        eventList.clear();
        assertTrue(shouldShowEmpty(eventList));
    }

    // ═══════════════════════════════════════════════
    // US-18 — Event list content
    // ═══════════════════════════════════════════════

    /**
     * US-18: Sample event list has correct size.
     */
    @Test
    public void testEventListSize() {
        assertEquals(3, eventList.size());
    }

    /**
     * US-18: Events are stored in order they were added.
     */
    @Test
    public void testEventListOrder() {
        assertEquals("Tech Summit 2025",  eventList.get(0).title);
        assertEquals("Annual Gala Night", eventList.get(1).title);
        assertEquals("Sports Day",        eventList.get(2).title);
    }

    /**
     * US-18: Each event has a non-null, non-empty id.
     */
    @Test
    public void testAllEventsHaveNonEmptyId() {
        for (EventItem item : eventList) {
            assertNotNull(item.id);
            assertFalse(item.id.isEmpty());
        }
    }

    /**
     * US-18: Each event has a non-null, non-empty title.
     */
    @Test
    public void testAllEventsHaveNonEmptyTitle() {
        for (EventItem item : eventList) {
            assertNotNull(item.title);
            assertFalse(item.title.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════
    // US-18 — Title fallback logic (from loadEvents)
    // ═══════════════════════════════════════════════

    /**
     * US-18: Null Firestore title falls back to docId.
     */
    @Test
    public void testNullTitleFallsBackToDocId() {
        assertEquals("evt001", resolveTitle(null, "evt001"));
    }

    /**
     * US-18: Empty Firestore title falls back to docId.
     */
    @Test
    public void testEmptyTitleFallsBackToDocId() {
        assertEquals("evt002", resolveTitle("", "evt002"));
    }

    /**
     * US-18: Valid Firestore title is used as-is.
     */
    @Test
    public void testValidTitleIsUsedDirectly() {
        assertEquals("Tech Summit 2025", resolveTitle("Tech Summit 2025", "evt001"));
    }

    // ═══════════════════════════════════════════════
    // US-18 — organizerUsername fallback
    // ═══════════════════════════════════════════════

    /**
     * US-18: Null intent extra falls back to default "ORG0012".
     */
    @Test
    public void testNullOrganizerUsernameFallsBackToDefault() {
        assertEquals("ORG0012", resolveOrganizerUsername(null));
    }

    /**
     * US-18: Valid intent extra is used directly.
     */
    @Test
    public void testValidOrganizerUsernameUsedDirectly() {
        assertEquals("ORG0099", resolveOrganizerUsername("ORG0099"));
    }
}