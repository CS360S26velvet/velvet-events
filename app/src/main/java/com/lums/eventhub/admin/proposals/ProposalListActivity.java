package com.lums.eventhub.admin.proposals;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lums.eventhub.R;
import com.lums.eventhub.model.Proposal;
import java.util.ArrayList;
import java.util.List;

public class ProposalListActivity extends AppCompatActivity {

    private RecyclerView rvProposals;
    private ProposalAdapter adapter;
    private List<Proposal> allProposals      = new ArrayList<>();
    private List<Proposal> filteredProposals = new ArrayList<>();
    private FirebaseFirestore db;

    // Canonical status values
    private static final String STATUS_SUBMITTED = "Submitted";
    private static final String STATUS_APPROVED  = "Approved";
    private static final String STATUS_REJECTED  = "Rejected";
    private static final String STATUS_REVISION  = "Revision Requested";
    private static final String STATUS_DRAFT     = "Draft";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proposal_list);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvProposals = findViewById(R.id.rvProposals);
        rvProposals.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProposalAdapter(filteredProposals, proposal -> {
            Intent intent = new Intent(this, ProposalDetailActivity.class);
            intent.putExtra("proposalId", proposal.getId());
            startActivity(intent);
        });
        rvProposals.setAdapter(adapter);

        // Filter buttons — "Pending" filter shows "Submitted" proposals
        findViewById(R.id.btnFilterAll).setOnClickListener(v      -> filterBy("all"));
        findViewById(R.id.btnFilterPending).setOnClickListener(v  -> filterBy(STATUS_SUBMITTED));
        findViewById(R.id.btnFilterApproved).setOnClickListener(v -> filterBy(STATUS_APPROVED));
        findViewById(R.id.btnFilterRejected).setOnClickListener(v -> filterBy(STATUS_REJECTED));

        loadProposals();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProposals();
    }

    private void loadProposals() {
        // Load all non-Draft proposals — Draft is private to the organizer
        db.collection("proposals").get()
                .addOnSuccessListener(query -> {
                    allProposals.clear();
                    int submitted = 0, approved = 0, rejected = 0;

                    for (QueryDocumentSnapshot doc : query) {
                        Proposal p = doc.toObject(Proposal.class);
                        p.setId(doc.getId());

                        // Skip Draft — not visible to admin
                        if (STATUS_DRAFT.equals(p.getStatus())) continue;

                        allProposals.add(p);

                        String status = p.getStatus();
                        if (STATUS_SUBMITTED.equals(status))      submitted++;
                        else if (STATUS_APPROVED.equals(status))  approved++;
                        else if (STATUS_REJECTED.equals(status))  rejected++;
                    }

                    // Update stat cards
                    ((TextView) findViewById(R.id.tvStatPending)).setText(String.valueOf(submitted));
                    ((TextView) findViewById(R.id.tvStatApproved)).setText(String.valueOf(approved));
                    ((TextView) findViewById(R.id.tvStatRejected)).setText(String.valueOf(rejected));
                    ((TextView) findViewById(R.id.tvStatTotal)).setText(String.valueOf(allProposals.size()));

                    filterBy("all");
                });
    }

    private void filterBy(String status) {
        filteredProposals.clear();
        for (Proposal p : allProposals) {
            if ("all".equals(status) || status.equals(p.getStatus())) {
                filteredProposals.add(p);
            }
        }
        adapter.notifyDataSetChanged();
    }
}