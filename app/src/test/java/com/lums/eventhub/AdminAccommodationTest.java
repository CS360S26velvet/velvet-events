package com.lums.eventhub;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminAccommodationTest.java
 *
 * Unit tests for AdminAccommodationBySocietyActivity logic:
 *   - Society grouping of events
 *   - CSV generation correctness
 *   - Filtering: wantsAccommodation == "Yes" AND paymentStatus == "Approved"
 *   - Empty results handling
 *   - CSV safe encoding
 */
public class AdminAccommodationTest {

    // ── Mirror models ─────────────────────────────────────────────────────────

    static class SocietyItem {
        String orgUsername, societyName;
        List<EventItem> events;

        SocietyItem(String orgUsername, String societyName) {
            this.orgUsername = orgUsername;
            this.societyName = societyName;
            this.events      = new ArrayList<>();
        }
    }

    static class EventItem {
        String id, title;
        EventItem(String id, String title) { this.id = id; this.title = title; }
    }

    static class RegistrationDoc {
        String studentName, studentId, accommodationAmount;
        String wantsAccommodation, paymentStatus, eventId;

        RegistrationDoc(String studentName, String studentId,
                        String wantsAccommodation, String paymentStatus, String eventId) {
            this.studentName          = studentName;
            this.studentId            = studentId;
            this.wantsAccommodation   = wantsAccommodation;
            this.paymentStatus        = paymentStatus;
            this.eventId              = eventId;
            this.accommodationAmount  = "PKR 1500";
        }
    }

    // ── Mirror logic helpers ──────────────────────────────────────────────────

    private List<RegistrationDoc> filterAccommodation(
            List<RegistrationDoc> regs, String eventId) {
        List<RegistrationDoc> result = new ArrayList<>();
        for (RegistrationDoc r : regs) {
            if (eventId.equals(r.eventId)
                    && "Yes".equals(r.wantsAccommodation)
                    && "Approved".equals(r.paymentStatus)) {
                result.add(r);
            }
        }
        return result;
    }

    private String csvSafe(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private String buildCsvRow(RegistrationDoc r, String eventTitle, String societyName) {
        return csvSafe(r.studentName) + "," +
                csvSafe(r.studentId) + "," +
                csvSafe(r.accommodationAmount) + "," +
                csvSafe(eventTitle) + "," +
                csvSafe(societyName);
    }

    private Map<String, SocietyItem> groupBySociety(
            List<String[]> orgSocietyPairs, List<String[]> approvedEvents) {
        Map<String, String> orgToSociety = new LinkedHashMap<>();
        for (String[] pair : orgSocietyPairs) orgToSociety.put(pair[0], pair[1]);

        Map<String, SocietyItem> map = new LinkedHashMap<>();
        for (String[] event : approvedEvents) {
            String orgUser    = event[0];
            String eventId    = event[1];
            String eventTitle = event[2];
            String society    = orgToSociety.containsKey(orgUser)
                    ? orgToSociety.get(orgUser) : orgUser;
            if (!map.containsKey(orgUser)) {
                map.put(orgUser, new SocietyItem(orgUser, society));
            }
            map.get(orgUser).events.add(new EventItem(eventId, eventTitle));
        }
        return map;
    }

    // ── Sample data ───────────────────────────────────────────────────────────

    private List<RegistrationDoc> registrations;

    @Before
    public void setUp() {
        registrations = new ArrayList<>();
        registrations.add(new RegistrationDoc("Ayesha", "24L-001", "Yes", "Approved", "event1"));
        registrations.add(new RegistrationDoc("Ahmed",  "24L-002", "Yes", "Pending",  "event1"));
        registrations.add(new RegistrationDoc("Sara",   "24L-003", "No",  "Approved", "event1"));
        registrations.add(new RegistrationDoc("Usman",  "24L-004", "Yes", "Approved", "event2"));
        registrations.add(new RegistrationDoc("Zara",   "24L-005", "Yes", "Approved", "event1"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Filter logic
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    public void testFilterReturnsOnlyApprovedWithAccommodation() {
        List<RegistrationDoc> result = filterAccommodation(registrations, "event1");
        assertEquals(2, result.size()); // Ayesha and Zara
    }

    @Test
    public void testFilterExcludesPendingPayment() {
        List<RegistrationDoc> result = filterAccommodation(registrations, "event1");
        for (RegistrationDoc r : result) assertEquals("Approved", r.paymentStatus);
    }

    @Test
    public void testFilterExcludesNoAccommodation() {
        List<RegistrationDoc> result = filterAccommodation(registrations, "event1");
        for (RegistrationDoc r : result) assertEquals("Yes", r.wantsAccommodation);
    }

    @Test
    public void testFilterByEventIdIsolatesCorrectEvent() {
        List<RegistrationDoc> result = filterAccommodation(registrations, "event2");
        assertEquals(1, result.size());
        assertEquals("Usman", result.get(0).studentName);
    }

    @Test
    public void testFilterReturnsEmptyWhenNoMatchingEvent() {
        List<RegistrationDoc> result = filterAccommodation(registrations, "event99");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilterReturnsEmptyWhenNoneApproved() {
        List<RegistrationDoc> regs = new ArrayList<>();
        regs.add(new RegistrationDoc("X", "001", "Yes", "Pending", "event1"));
        assertTrue(filterAccommodation(regs, "event1").isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CSV generation
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    public void testCsvRowContainsStudentName() {
        List<RegistrationDoc> result = filterAccommodation(registrations, "event1");
        String row = buildCsvRow(result.get(0), "FINAL AMNAS TRY", "Spades");
        assertTrue(row.contains("Ayesha"));
    }

    @Test
    public void testCsvRowContainsStudentId() {
        List<RegistrationDoc> result = filterAccommodation(registrations, "event1");
        String row = buildCsvRow(result.get(0), "FINAL AMNAS TRY", "Spades");
        assertTrue(row.contains("24L-001"));
    }

    @Test
    public void testCsvRowContainsEventTitle() {
        List<RegistrationDoc> result = filterAccommodation(registrations, "event1");
        String row = buildCsvRow(result.get(0), "FINAL AMNAS TRY", "Spades");
        assertTrue(row.contains("FINAL AMNAS TRY"));
    }

    @Test
    public void testCsvRowContainsSocietyName() {
        List<RegistrationDoc> result = filterAccommodation(registrations, "event1");
        String row = buildCsvRow(result.get(0), "Event", "Spades");
        assertTrue(row.contains("Spades"));
    }

    @Test
    public void testCsvSafeHandlesNull() {
        assertEquals("", csvSafe(null));
    }

    @Test
    public void testCsvSafeHandlesQuotesInName() {
        String result = csvSafe("O\"Brien");
        assertEquals("\"O\"\"Brien\"", result);
    }

    @Test
    public void testCsvSafeWrapsInQuotes() {
        String result = csvSafe("Ayesha");
        assertTrue(result.startsWith("\""));
        assertTrue(result.endsWith("\""));
    }

    @Test
    public void testCsvRowHasFiveColumns() {
        List<RegistrationDoc> result = filterAccommodation(registrations, "event1");
        String row = buildCsvRow(result.get(0), "Event", "Society");
        String[] cols = row.split(",");
        assertEquals(5, cols.length);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Society grouping
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    public void testSocietyGroupingCorrect() {
        List<String[]> orgs = new ArrayList<>();
        orgs.add(new String[]{"#ORG_spades", "Spades"});
        orgs.add(new String[]{"#ORG_ieee",   "IEEE"});

        List<String[]> events = new ArrayList<>();
        events.add(new String[]{"#ORG_spades", "event1", "PYPT"});
        events.add(new String[]{"#ORG_spades", "event2", "Science Convention"});
        events.add(new String[]{"#ORG_ieee",   "event3", "Tech Summit"});

        Map<String, SocietyItem> grouped = groupBySociety(orgs, events);
        assertEquals(2, grouped.size());
        assertEquals(2, grouped.get("#ORG_spades").events.size());
        assertEquals(1, grouped.get("#ORG_ieee").events.size());
    }

    @Test
    public void testSocietyNameResolvedFromUsername() {
        List<String[]> orgs = new ArrayList<>();
        orgs.add(new String[]{"#ORG_spades", "Spades Society"});

        List<String[]> events = new ArrayList<>();
        events.add(new String[]{"#ORG_spades", "e1", "Event 1"});

        Map<String, SocietyItem> grouped = groupBySociety(orgs, events);
        assertEquals("Spades Society", grouped.get("#ORG_spades").societyName);
    }

    @Test
    public void testEmptyEventsProducesEmptyGrouping() {
        Map<String, SocietyItem> grouped = groupBySociety(new ArrayList<>(), new ArrayList<>());
        assertTrue(grouped.isEmpty());
    }
}