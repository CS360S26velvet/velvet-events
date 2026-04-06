package com.lums.eventhub.organizer;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * OrganizerDashboardActivity.java (stub in admin's codebase)
 *
 * This is a placeholder — the full implementation lives in Person B/C's codebase.
 * When merged, this file should be replaced by Person B/C's full OrganizerDashboardActivity.
 *
 * Receives from LoginActivity:
 *   "organizerUsername" — the #ORG_xxx username (same as organizerId)
 *   "societyName"       — e.g. "SPADES Society"
 *
 * The full implementation should:
 *   - Display societyName in the header
 *   - Query proposals/ where organizerUsername == organizerUsername
 *   - Show status badges: Approved (green, no edit), Revision Requested (orange, edit),
 *     Submitted (yellow, no edit), Draft (grey, edit), Rejected (red, no edit)
 */
public class OrganizerDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String organizerUsername = getIntent().getStringExtra("organizerUsername");
        String societyName       = getIntent().getStringExtra("societyName");

        // TODO: replace this stub with Person B/C's full OrganizerDashboardActivity on merge
        Toast.makeText(this,
                "Welcome, " + societyName + " (" + organizerUsername + ")",
                Toast.LENGTH_LONG).show();
        finish();
    }
}