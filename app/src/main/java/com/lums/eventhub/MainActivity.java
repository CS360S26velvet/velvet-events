package com.lums.eventhub;

/**
 * MainActivity.java
 *
 * Role: Main dashboard / navigation hub for the organizer.
 * Entry point for all Person C screens.
 *
 * Note: organizerId == societyId — same concept everywhere in this project.
 *
 * User Stories: Entry point for US-23, US-25, US-28, US-29, US-30
 */

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Form Settings (CapacitySettingActivity)
        Button btnFormBuilder = findViewById(R.id.btnFormBuilder);
        btnFormBuilder.setOnClickListener(v ->
                startActivity(new Intent(this, CapacitySettingActivity.class)));

        // Live Check-In
        Button btnCheckIn = findViewById(R.id.btnCheckIn);
        btnCheckIn.setOnClickListener(v ->
                startActivity(new Intent(this, CheckInActivity.class)));

        // Payment Verification (placeholder)
        Button btnPayments = findViewById(R.id.btnPayments);
        btnPayments.setOnClickListener(v ->
                Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show());

        // Registrant Dashboard
        Button btnRegistrants = findViewById(R.id.btnRegistrants);
        btnRegistrants.setOnClickListener(v ->
                startActivity(new Intent(this, RegistrantDashboardActivity.class)));

        // Attendee Registration → event list → form builder
        Button btnAttendeeForm = findViewById(R.id.btnAttendeeForm);
        btnAttendeeForm.setOnClickListener(v ->
                startActivity(new Intent(this, AttendeeRegistrationActivity.class)));

        // Organizer Dashboard
        Button btnOrgDashboard = findViewById(R.id.btnOrgDashboard);
        btnOrgDashboard.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerDashboardActivity.class)));

        // Event Proposal Form
        Button btnProposalForm = findViewById(R.id.btnProposalForm);
        btnProposalForm.setOnClickListener(v ->
                startActivity(new Intent(this, ProposalFormActivity.class)));
    }
}