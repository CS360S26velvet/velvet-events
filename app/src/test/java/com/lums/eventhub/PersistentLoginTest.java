package com.lums.eventhub;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

/**
 * PersistentLoginTest.java
 *
 * Unit tests for persistent login/logout logic in LoginActivity:
 *   - Session saved correctly per role (admin, organizer, attendee)
 *   - Session cleared on logout
 *   - Correct dashboard routed from saved session
 *   - Null/empty session handled gracefully
 *   - Role detection from saved prefs
 */
public class PersistentLoginTest {

    // ── Mirror SharedPreferences as a simple Map ──────────────────────────────

    static final String KEY_USER    = "saved_username";
    static final String KEY_ROLE    = "saved_role";
    static final String KEY_SOCIETY = "saved_society";
    static final String KEY_USERID  = "saved_userId";

    private Map<String, String> prefs;

    // ── Mirror logic helpers ──────────────────────────────────────────────────

    private void saveSession(String username, String role,
                             String society, String userId) {
        prefs.put(KEY_USER, username);
        prefs.put(KEY_ROLE, role);
        if (society != null) prefs.put(KEY_SOCIETY, society);
        if (userId  != null) prefs.put(KEY_USERID,  userId);
    }

    private void clearSession() {
        prefs.clear();
    }

    private boolean hasSession() {
        return prefs.containsKey(KEY_USER) && prefs.containsKey(KEY_ROLE);
    }

    private String getDashboardForRole(String role) {
        if (role == null) return "Login";
        switch (role) {
            case "admin":     return "AdminDashboard";
            case "organizer": return "OrganizerDashboard";
            case "attendee":  return "AttendeeDashboard";
            default:          return "Login";
        }
    }

    @Before
    public void setUp() {
        prefs = new HashMap<>();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Session save
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    public void testAdminSessionSavedCorrectly() {
        saveSession("#AD_admin", "admin", null, null);
        assertEquals("#AD_admin", prefs.get(KEY_USER));
        assertEquals("admin",     prefs.get(KEY_ROLE));
    }

    @Test
    public void testOrganizerSessionSavedWithSociety() {
        saveSession("#ORG_spades", "organizer", "Spades Society", null);
        assertEquals("#ORG_spades",   prefs.get(KEY_USER));
        assertEquals("organizer",     prefs.get(KEY_ROLE));
        assertEquals("Spades Society", prefs.get(KEY_SOCIETY));
    }

    @Test
    public void testAttendeeSessionSavedWithUserId() {
        saveSession("#AT_sara", "attendee", null, "firestoreDocId123");
        assertEquals("#AT_sara",        prefs.get(KEY_USER));
        assertEquals("attendee",        prefs.get(KEY_ROLE));
        assertEquals("firestoreDocId123", prefs.get(KEY_USERID));
    }

    @Test
    public void testSessionExistsAfterSave() {
        saveSession("#AD_admin", "admin", null, null);
        assertTrue(hasSession());
    }

    @Test
    public void testNoSessionBeforeSave() {
        assertFalse(hasSession());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Session clear (logout)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    public void testSessionClearedOnLogout() {
        saveSession("#AD_admin", "admin", null, null);
        clearSession();
        assertFalse(hasSession());
    }

    @Test
    public void testPrefsEmptyAfterClear() {
        saveSession("#ORG_spades", "organizer", "Spades", null);
        clearSession();
        assertTrue(prefs.isEmpty());
    }

    @Test
    public void testClearSessionOnEmptyPrefsDoesNotThrow() {
        try {
            clearSession();
            assertTrue(true);
        } catch (Exception e) {
            fail("clearSession should not throw on empty prefs");
        }
    }

    @Test
    public void testHasSessionReturnsFalseAfterClear() {
        saveSession("#AT_user", "attendee", null, "uid1");
        clearSession();
        assertFalse(hasSession());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Dashboard routing from saved session
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    public void testAdminRoleRoutesToAdminDashboard() {
        saveSession("#AD_admin", "admin", null, null);
        String dashboard = getDashboardForRole(prefs.get(KEY_ROLE));
        assertEquals("AdminDashboard", dashboard);
    }

    @Test
    public void testOrganizerRoleRoutesToOrganizerDashboard() {
        saveSession("#ORG_spades", "organizer", "Spades", null);
        String dashboard = getDashboardForRole(prefs.get(KEY_ROLE));
        assertEquals("OrganizerDashboard", dashboard);
    }

    @Test
    public void testAttendeeRoleRoutesToAttendeeDashboard() {
        saveSession("#AT_sara", "attendee", null, "uid1");
        String dashboard = getDashboardForRole(prefs.get(KEY_ROLE));
        assertEquals("AttendeeDashboard", dashboard);
    }

    @Test
    public void testNullRoleRoutesToLogin() {
        assertEquals("Login", getDashboardForRole(null));
    }

    @Test
    public void testUnknownRoleRoutesToLogin() {
        assertEquals("Login", getDashboardForRole("unknown_role"));
    }

    @Test
    public void testNoSessionMeansShowLoginScreen() {
        assertFalse(hasSession());
        // Without session, app should show login — simulated by checking hasSession()
        String destination = hasSession()
                ? getDashboardForRole(prefs.get(KEY_ROLE))
                : "Login";
        assertEquals("Login", destination);
    }

    @Test
    public void testWithSessionSkipsLoginScreen() {
        saveSession("#AD_admin", "admin", null, null);
        String destination = hasSession()
                ? getDashboardForRole(prefs.get(KEY_ROLE))
                : "Login";
        assertEquals("AdminDashboard", destination);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Edge cases
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    public void testSessionOverwrittenOnNewLogin() {
        saveSession("#AD_admin", "admin", null, null);
        saveSession("#ORG_spades", "organizer", "Spades", null);
        assertEquals("organizer", prefs.get(KEY_ROLE));
        assertEquals("#ORG_spades", prefs.get(KEY_USER));
    }

    @Test
    public void testSocietyNotStoredForAdmin() {
        saveSession("#AD_admin", "admin", null, null);
        assertNull(prefs.get(KEY_SOCIETY));
    }

    @Test
    public void testUserIdNotStoredForOrganizer() {
        saveSession("#ORG_spades", "organizer", "Spades", null);
        assertNull(prefs.get(KEY_USERID));
    }
}