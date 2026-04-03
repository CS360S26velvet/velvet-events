package com.lums.eventhub;

/**
 * MainActivity.java
 *
 * Role: Main dashboard for the Organizer interface.
 * Acts as the entry point and navigation hub for all Person C screens.
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

        Button btnFormBuilder = findViewById(R.id.btnFormBuilder);
        Button btnCheckIn = findViewById(R.id.btnCheckIn);
        Button btnPayments = findViewById(R.id.btnPayments);
        Button btnRegistrants = findViewById(R.id.btnRegistrants);
        Button btnAttendeeForm = findViewById(R.id.btnAttendeeForm);
        btnAttendeeForm.setOnClickListener(v ->
                startActivity(new Intent(this, AttendeeRegistrationActivity.class)));

        btnFormBuilder.setOnClickListener(v ->
                startActivity(new Intent(this, CapacitySettingActivity.class)));

        btnCheckIn.setOnClickListener(v ->
                startActivity(new Intent(this, CheckInActivity.class)));

        btnPayments.setOnClickListener(v ->
                Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show());

        btnRegistrants.setOnClickListener(v ->
                Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show());
        btnRegistrants.setOnClickListener(v ->
                startActivity(new Intent(this, RegistrantDashboardActivity.class)));
    }
}