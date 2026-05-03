package com.lums.eventhub.attendee.registration;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lums.eventhub.R;
import com.lums.eventhub.organizer.registration.RegistrationFeeSetupActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * AttendeeRegistrationActivity.java  (UPDATED)
 *
 * Role: Shows all events belonging to this society.
 * Each event row has an "Edit Reg Form" button.
 *
 * CHANGE FROM ORIGINAL:
 *   Clicking "Edit Reg Form" now first opens RegistrationFeeSetupActivity
 *   (which asks about reg fee / accommodation).
 *   RegistrationFeeSetupActivity then launches FormBuilderActivity
 *   with the fee data as extras.
 *
 * Firestore reads: events/ where organizerUsername == organizerUsername
 */
public class AttendeeRegistrationActivity extends AppCompatActivity {

    private String organizerUsername;

    private RecyclerView     recyclerViewEvents;
    private TextView         tvNoEvents;
    private EventAdapter     adapter;
    private List<EventItem>  eventList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendee_registration);

        db = FirebaseFirestore.getInstance();

        organizerUsername = getIntent().getStringExtra("organizerUsername");
        if (organizerUsername == null) organizerUsername = "ORG0012";

        recyclerViewEvents = findViewById(R.id.recyclerViewEvents);
        tvNoEvents         = findViewById(R.id.tvNoEvents);

        adapter = new EventAdapter(eventList);
        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewEvents.setAdapter(adapter);

        loadEvents();
    }

    private void loadEvents() {
        db.collection("events")
                .whereEqualTo("organizerUsername", organizerUsername)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String title = doc.getString("title");
                        if (title == null || title.isEmpty()) title = doc.getId();
                        eventList.add(new EventItem(doc.getId(), title));
                    }
                    showOrHideEmpty();
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> showOrHideEmpty());
    }

    private void showOrHideEmpty() {
        if (eventList.isEmpty()) {
            tvNoEvents.setVisibility(View.VISIBLE);
            recyclerViewEvents.setVisibility(View.GONE);
        } else {
            tvNoEvents.setVisibility(View.GONE);
            recyclerViewEvents.setVisibility(View.VISIBLE);
        }
    }

    // -------------------------------------------------------------------------
    // Model
    // -------------------------------------------------------------------------

    static class EventItem {
        String id;
        String title;
        EventItem(String id, String title) {
            this.id    = id;
            this.title = title;
        }
    }

    // -------------------------------------------------------------------------
    // Adapter
    // -------------------------------------------------------------------------

    class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

        private final List<EventItem> list;

        EventAdapter(List<EventItem> list) { this.list = list; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_event_reg, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            EventItem event = list.get(position);
            holder.tvEventName.setText(event.title);

            // UPDATED: opens RegistrationFeeSetupActivity first, not FormBuilderActivity directly
            holder.btnBuildForm.setOnClickListener(v -> {
                Intent intent = new Intent(
                        AttendeeRegistrationActivity.this,
                        RegistrationFeeSetupActivity.class);
                intent.putExtra("eventId",   event.id);
                intent.putExtra("eventName", event.title);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEventName;
            Button   btnBuildForm;
            ViewHolder(View v) {
                super(v);
                tvEventName  = v.findViewById(R.id.tvEventName);
                btnBuildForm = v.findViewById(R.id.btnBuildForm);
            }
        }
    }
}