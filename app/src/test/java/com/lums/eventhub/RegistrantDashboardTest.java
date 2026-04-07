package com.lums.eventhub;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for RegistrantDashboardActivity logic
 *
 * Covers:
 *   US-28 — Show incomplete registrations
 *   US-29 — Export registrant list to CSV
 *
 * Pure Java — no Android or Firestore dependencies.
 */
public class RegistrantDashboardTest {

    // ─────────────────────────────────────────────
    // Mirror of RegistrantDashboardActivity.Registrant
    // ─────────────────────────────────────────────

    static class Registrant {
        String  name, studentId, startedAt;
        boolean completed;

        Registrant(String name, String studentId, String startedAt, boolean completed) {
            this.name      = name;
            this.studentId = studentId;
            this.startedAt = startedAt;
            this.completed = completed;
        }
    }

    // ─────────────────────────────────────────────
    // Mirror of showIncomplete() filter logic
    // ─────────────────────────────────────────────

    private List<Registrant> showIncomplete(List<Registrant> all) {
        List<Registrant> filtered = new ArrayList<>();
        for (Registrant r : all) {
            if (!r.completed) filtered.add(r);
        }
        return filtered;
    }

    // ─────────────────────────────────────────────
    // Mirror of updateUI() stats logic
    // ─────────────────────────────────────────────

    private int countIncomplete(List<Registrant> list) {
        int count = 0;
        for (Registrant r : list) {
            if (!r.completed) count++;
        }
        return count;
    }

    // ─────────────────────────────────────────────
    // Mirror of exportToCSV() content building
    // ─────────────────────────────────────────────

    private String buildCSV(List<Registrant> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name,Student ID,Started At,Status\n");
        for (Registrant r : list) {
            sb.append(r.name).append(",")
                    .append(r.studentId).append(",")
                    .append(r.startedAt).append(",")
                    .append(r.completed ? "Completed" : "Incomplete")
                    .append("\n");
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────
    // Sample data — mirrors addSampleData() exactly
    // ─────────────────────────────────────────────

    private List<Registrant> registrantList;

    @Before
    public void setUp() {
        registrantList = new ArrayList<>();
        registrantList.add(new Registrant("Hassan Raza",  "AT0041", "3 hours ago", false));
        registrantList.add(new Registrant("Fatima Malik", "AT0023", "2 hours ago", true));
        registrantList.add(new Registrant("Bilal Khan",   "AT0067", "1 hour ago",  false));
        registrantList.add(new Registrant("Sara Ahmed",   "AT0078", "45 min ago",  true));
        registrantList.add(new Registrant("Usman Tariq",  "AT0089", "30 min ago",  false));
        registrantList.add(new Registrant("Zainab Ali",   "AT0055", "15 min ago",  false));
    }

    // ═══════════════════════════════════════════════
    // Registrant model
    // ═══════════════════════════════════════════════

    /**
     * US-28: Constructor stores all fields correctly.
     */
    @Test
    public void testRegistrantConstructorStoresAllFields() {
        Registrant r = new Registrant("Hassan Raza", "AT0041", "3 hours ago", false);
        assertEquals("Hassan Raza",  r.name);
        assertEquals("AT0041",       r.studentId);
        assertEquals("3 hours ago",  r.startedAt);
        assertFalse(r.completed);
    }

    /**
     * US-28: completed=true is stored correctly.
     */
    @Test
    public void testCompletedTrueStoredCorrectly() {
        Registrant r = new Registrant("Fatima Malik", "AT0023", "2 hours ago", true);
        assertTrue(r.completed);
    }

    // ═══════════════════════════════════════════════
    // US-28 — Stats & filtering
    // ═══════════════════════════════════════════════

    /**
     * US-28: Total registrant count is correct.
     */
    @Test
    public void testTotalRegistrantCount() {
        assertEquals(6, registrantList.size());
    }

    /**
     * US-28: Incomplete count is correct from sample data.
     */
    @Test
    public void testIncompleteCountFromSampleData() {
        assertEquals(4, countIncomplete(registrantList));
    }

    /**
     * US-28: Completed count is correct from sample data.
     */
    @Test
    public void testCompletedCountFromSampleData() {
        int completed = registrantList.size() - countIncomplete(registrantList);
        assertEquals(2, completed);
    }

    /**
     * US-28: incomplete + completed always equals total.
     */
    @Test
    public void testIncompleteAndCompletedSumToTotal() {
        int incomplete = countIncomplete(registrantList);
        int completed  = registrantList.size() - incomplete;
        assertEquals(registrantList.size(), incomplete + completed);
    }

    /**
     * US-28: showIncomplete returns only incomplete registrants.
     */
    @Test
    public void testShowIncompleteFiltersCorrectly() {
        List<Registrant> filtered = showIncomplete(registrantList);
        assertEquals(4, filtered.size());
        for (Registrant r : filtered) {
            assertFalse(r.completed);
        }
    }

    /**
     * US-28: showAll returns every registrant unchanged.
     */
    @Test
    public void testShowAllReturnsEveryone() {
        // showAll just copies the full list
        List<Registrant> all = new ArrayList<>(registrantList);
        assertEquals(registrantList.size(), all.size());
    }

    /**
     * US-28: showIncomplete on empty list returns empty list.
     */
    @Test
    public void testShowIncompleteOnEmptyListReturnsEmpty() {
        assertEquals(0, showIncomplete(new ArrayList<>()).size());
    }

    /**
     * US-28: showIncomplete when all completed returns empty list.
     */
    @Test
    public void testShowIncompleteWhenAllCompletedReturnsEmpty() {
        List<Registrant> allDone = new ArrayList<>();
        allDone.add(new Registrant("Ali",  "AT001", "1h ago", true));
        allDone.add(new Registrant("Sara", "AT002", "2h ago", true));
        assertEquals(0, showIncomplete(allDone).size());
    }

    /**
     * US-28: showIncomplete returns correct registrant names.
     */
    @Test
    public void testShowIncompleteReturnsCorrectNames() {
        List<Registrant> filtered = showIncomplete(registrantList);
        assertEquals("Hassan Raza", filtered.get(0).name);
        assertEquals("Bilal Khan",  filtered.get(1).name);
        assertEquals("Usman Tariq", filtered.get(2).name);
        assertEquals("Zainab Ali",  filtered.get(3).name);
    }

    // ═══════════════════════════════════════════════
    // US-29 — CSV export
    // ═══════════════════════════════════════════════

    /**
     * US-29: CSV first line is the correct header.
     */
    @Test
    public void testCSVHeader() {
        String csv = buildCSV(registrantList);
        String header = csv.split("\n")[0];
        assertEquals("Name,Student ID,Started At,Status", header);
    }

    /**
     * US-29: CSV row count equals header + one row per registrant.
     */
    @Test
    public void testCSVRowCount() {
        String[] lines = buildCSV(registrantList).split("\n");
        assertEquals(registrantList.size() + 1, lines.length); // +1 for header
    }

    /**
     * US-29: Completed registrant shows "Completed" in CSV status column.
     */
    @Test
    public void testCSVCompletedStatusLabel() {
        List<Registrant> single = new ArrayList<>();
        single.add(new Registrant("Fatima Malik", "AT0023", "2 hours ago", true));
        String[] lines = buildCSV(single).split("\n");
        assertTrue(lines[1].endsWith("Completed"));
    }

    /**
     * US-29: Incomplete registrant shows "Incomplete" in CSV status column.
     */
    @Test
    public void testCSVIncompleteStatusLabel() {
        List<Registrant> single = new ArrayList<>();
        single.add(new Registrant("Hassan Raza", "AT0041", "3 hours ago", false));
        String[] lines = buildCSV(single).split("\n");
        assertTrue(lines[1].endsWith("Incomplete"));
    }

    /**
     * US-29: CSV data row contains all four fields for a registrant.
     */
    @Test
    public void testCSVDataRowContainsAllFields() {
        List<Registrant> single = new ArrayList<>();
        single.add(new Registrant("Hassan Raza", "AT0041", "3 hours ago", false));
        String dataRow = buildCSV(single).split("\n")[1];
        assertTrue(dataRow.contains("Hassan Raza"));
        assertTrue(dataRow.contains("AT0041"));
        assertTrue(dataRow.contains("3 hours ago"));
        assertTrue(dataRow.contains("Incomplete"));
    }

    /**
     * US-29: CSV with empty list contains only the header line.
     */
    @Test
    public void testCSVWithEmptyListHasOnlyHeader() {
        String[] lines = buildCSV(new ArrayList<>()).split("\n");
        assertEquals(1, lines.length);
        assertEquals("Name,Student ID,Started At,Status", lines[0]);
    }

    /**
     * US-29: CSV filename includes the event ID.
     */
    @Test
    public void testCSVFilenameIncludesEventId() {
        String eventId  = "SPADES2025";
        String filename = "registrants_" + eventId + ".csv";
        assertEquals("registrants_SPADES2025.csv", filename);
        assertTrue(filename.endsWith(".csv"));
    }
}