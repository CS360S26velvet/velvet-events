package com.lums.eventhub.admin.auditorium;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lums.eventhub.R;
import java.util.ArrayList;
import java.util.List;

public class AuditoriumActivity extends AppCompatActivity {
    private ListView lvBookings;
    private List<String> bookingList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auditorium);

        lvBookings = findViewById(R.id.lvBookings);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch approved proposals that have a venue assigned
        db.collection("proposals").whereEqualTo("status", "approved").get()
                .addOnSuccessListener(query -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : query) {
                        String info = doc.getString("title") + " - " + doc.getString("venue");
                        bookingList.add(info);
                    }
                    lvBookings.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bookingList));
                });
    }
}