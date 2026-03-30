package com.lums.eventhub.admin.accommodation;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.lums.eventhub.R;

public class AccommodationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accommodation);

        Button btnDownload = findViewById(R.id.btnDownload);
        btnDownload.setOnClickListener(v -> {
            // For the demo, just show a toast.
            // In reality, this would create a CSV file.
            Toast.makeText(this, "Exporting to Residence Office (CSV)...", Toast.LENGTH_LONG).show();
        });
    }
}