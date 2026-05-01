package com.lums.eventhub;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * VendorDirectoryTest.java
 *
 * Unit tests for VendorDirectoryActivity and AddVendorActivity logic:
 *   - Category filter
 *   - Search by vendor name
 *   - Star rating display
 *   - Favourite toggle
 *   - usedByCount increment
 *   - Vendor model fields
 *   - Vendor name validation (required)
 *   - nvl helper
 */
public class VendorDirectoryTest {

    // ── Mirror models ─────────────────────────────────────────────────────────

    static class Vendor {
        String id, name, category, about, phone, email, address;
        double rating;
        int    usedByCount;
        List<String> favouritedBy = new ArrayList<>();

        Vendor(String id, String name, String category, double rating, int usedByCount) {
            this.id = id; this.name = name; this.category = category;
            this.rating = rating; this.usedByCount = usedByCount;
        }
    }

    // ── Mirrored logic helpers ────────────────────────────────────────────────

    private List<Vendor> filterByCategory(List<Vendor> all, String filter) {
        if ("All".equals(filter)) return new ArrayList<>(all);
        List<Vendor> result = new ArrayList<>();
        for (Vendor v : all) if (filter.equals(v.category)) result.add(v);
        return result;
    }

    private List<Vendor> searchVendors(List<Vendor> list, String query) {
        if (query == null || query.isEmpty()) return new ArrayList<>(list);
        List<Vendor> result = new ArrayList<>();
        for (Vendor v : list)
            if (v.name.toLowerCase().contains(query.toLowerCase())) result.add(v);
        return result;
    }

    private String starsFor(double rating) {
        int full = (int) rating;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < full ? "★" : "☆");
        return sb.toString();
    }

    private void toggleFavourite(Vendor v, String organizerUsername) {
        if (v.favouritedBy.contains(organizerUsername))
            v.favouritedBy.remove(organizerUsername);
        else
            v.favouritedBy.add(organizerUsername);
    }

    private void markVendorUsed(Vendor v, String eventTitle) {
        v.usedByCount++;
    }

    private boolean isVendorNameValid(String name) {
        return name != null && !name.trim().isEmpty();
    }

    private String nvl(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }

    // ── Sample data ───────────────────────────────────────────────────────────

    private List<Vendor> vendors;

    @Before
    public void setUp() {
        vendors = new ArrayList<>();
        vendors.add(new Vendor("v1", "AV Masters",      "AV & Tech",   4.5, 12));
        vendors.add(new Vendor("v2", "Karachi Caterers", "Catering",   4.8, 20));
        vendors.add(new Vendor("v3", "PrintZone",        "Printing",   4.0, 8));
        vendors.add(new Vendor("v4", "DecorPro",         "Decor",      4.2, 5));
        vendors.add(new Vendor("v5", "FastWheels",       "Transport",  3.9, 3));
        vendors.add(new Vendor("v6", "Sound Systems",    "AV & Tech",  4.7, 15));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Category Filter
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testFilterAllReturnsAllVendors() {
        assertEquals(6, filterByCategory(vendors, "All").size());
    }

    @Test public void testFilterAVTechReturnsTwoVendors() {
        assertEquals(2, filterByCategory(vendors, "AV & Tech").size());
    }

    @Test public void testFilterCateringReturnsOneVendor() {
        assertEquals(1, filterByCategory(vendors, "Catering").size());
        assertEquals("Karachi Caterers", filterByCategory(vendors, "Catering").get(0).name);
    }

    @Test public void testFilterPrintingReturnsOneVendor() {
        assertEquals(1, filterByCategory(vendors, "Printing").size());
    }

    @Test public void testFilterDecorReturnsOneVendor() {
        assertEquals(1, filterByCategory(vendors, "Decor").size());
    }

    @Test public void testFilterTransportReturnsOneVendor() {
        assertEquals(1, filterByCategory(vendors, "Transport").size());
    }

    @Test public void testFilterNonExistentCategoryReturnsEmpty() {
        assertEquals(0, filterByCategory(vendors, "Photography").size());
    }

    @Test public void testFilteredVendorsHaveCorrectCategory() {
        for (Vendor v : filterByCategory(vendors, "AV & Tech"))
            assertEquals("AV & Tech", v.category);
    }

    @Test public void testFilterEmptyListReturnsEmpty() {
        assertEquals(0, filterByCategory(new ArrayList<>(), "Catering").size());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Search
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testEmptySearchReturnsAll() {
        assertEquals(vendors.size(), searchVendors(vendors, "").size());
    }

    @Test public void testSearchByExactName() {
        List<Vendor> result = searchVendors(vendors, "PrintZone");
        assertEquals(1, result.size());
        assertEquals("PrintZone", result.get(0).name);
    }

    @Test public void testSearchByPartialName() {
        List<Vendor> result = searchVendors(vendors, "AV");
        assertEquals(1, result.size()); // Only "AV Masters"
    }

    @Test public void testSearchIsCaseInsensitive() {
        assertEquals(searchVendors(vendors, "av masters").size(),
                searchVendors(vendors, "AV MASTERS").size());
    }

    @Test public void testSearchNoMatchReturnsEmpty() {
        assertEquals(0, searchVendors(vendors, "xyz_no_match_9999").size());
    }

    @Test public void testSearchMatchingMultipleVendors() {
        // "s" appears in AV Masters, Karachi Caterers, Sound Systems, FastWheels
        List<Vendor> result = searchVendors(vendors, "s");
        assertTrue(result.size() > 1);
    }

    @Test public void testSearchNullReturnsAll() {
        assertEquals(vendors.size(), searchVendors(vendors, null).size());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Star Rating Display
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testStarsForZeroRating() {
        assertEquals("☆☆☆☆☆", starsFor(0.0));
    }

    @Test public void testStarsForFiveRating() {
        assertEquals("★★★★★", starsFor(5.0));
    }

    @Test public void testStarsForFourRating() {
        assertEquals("★★★★☆", starsFor(4.0));
    }

    @Test public void testStarsForThreeRating() {
        assertEquals("★★★☆☆", starsFor(3.0));
    }

    @Test public void testStarsLengthIsAlwaysFive() {
        assertEquals(5, starsFor(3.7).length());
        assertEquals(5, starsFor(0.0).length());
        assertEquals(5, starsFor(5.0).length());
    }

    @Test public void testStarsForDecimalRatingUsesFloor() {
        // 4.8 → floor = 4 → 4 full stars
        assertEquals("★★★★☆", starsFor(4.8));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Favourite Toggle
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testAddToFavourites() {
        Vendor v = vendors.get(0);
        assertFalse(v.favouritedBy.contains("ORG0012"));
        toggleFavourite(v, "ORG0012");
        assertTrue(v.favouritedBy.contains("ORG0012"));
    }

    @Test public void testRemoveFromFavourites() {
        Vendor v = vendors.get(0);
        v.favouritedBy.add("ORG0012");
        toggleFavourite(v, "ORG0012");
        assertFalse(v.favouritedBy.contains("ORG0012"));
    }

    @Test public void testToggleFavouriteDoesNotAffectOtherOrganisers() {
        Vendor v = vendors.get(0);
        v.favouritedBy.add("ORG0099");
        toggleFavourite(v, "ORG0012");
        assertTrue(v.favouritedBy.contains("ORG0099"));
        assertTrue(v.favouritedBy.contains("ORG0012"));
    }

    @Test public void testToggleFavouriteTwiceRestoresOriginalState() {
        Vendor v = vendors.get(0);
        toggleFavourite(v, "ORG0012");
        toggleFavourite(v, "ORG0012");
        assertFalse(v.favouritedBy.contains("ORG0012"));
    }

    @Test public void testMultipleOrganisersFavouritesSameVendor() {
        Vendor v = vendors.get(0);
        toggleFavourite(v, "ORG0001");
        toggleFavourite(v, "ORG0002");
        toggleFavourite(v, "ORG0003");
        assertEquals(3, v.favouritedBy.size());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Mark as Used
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testMarkAsUsedIncrementsCount() {
        Vendor v = vendors.get(0);
        int before = v.usedByCount;
        markVendorUsed(v, "SPADES 2025");
        assertEquals(before + 1, v.usedByCount);
    }

    @Test public void testMarkAsUsedMultipleTimes() {
        Vendor v = vendors.get(0);
        int before = v.usedByCount;
        markVendorUsed(v, "Event A");
        markVendorUsed(v, "Event B");
        assertEquals(before + 2, v.usedByCount);
    }

    @Test public void testInitialUsedByCountIsCorrect() {
        assertEquals(12, vendors.get(0).usedByCount);
        assertEquals(20, vendors.get(1).usedByCount);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Vendor Name Validation (AddVendorActivity)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testVendorNameEmptyIsInvalid() {
        assertFalse(isVendorNameValid(""));
    }

    @Test public void testVendorNameNullIsInvalid() {
        assertFalse(isVendorNameValid(null));
    }

    @Test public void testVendorNameWhitespaceOnlyIsInvalid() {
        assertFalse(isVendorNameValid("   "));
    }

    @Test public void testVendorNameValidWithText() {
        assertTrue(isVendorNameValid("AV Masters"));
    }

    @Test public void testVendorNameValidWithSingleChar() {
        assertTrue(isVendorNameValid("X"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Vendor Model Fields
    // ═══════════════════════════════════════════════════════════════════════════

    @Test public void testVendorFieldsStoredCorrectly() {
        Vendor v = vendors.get(0);
        assertEquals("v1",        v.id);
        assertEquals("AV Masters", v.name);
        assertEquals("AV & Tech",  v.category);
        assertEquals(4.5, v.rating, 0.001);
        assertEquals(12,  v.usedByCount);
    }

    @Test public void testNvlReturnsFallbackForNull() {
        assertEquals("Other", nvl(null, "Other"));
    }

    @Test public void testNvlReturnsFallbackForEmpty() {
        assertEquals("Other", nvl("", "Other"));
    }

    @Test public void testNvlReturnsValueWhenPresent() {
        assertEquals("Catering", nvl("Catering", "Other"));
    }
}