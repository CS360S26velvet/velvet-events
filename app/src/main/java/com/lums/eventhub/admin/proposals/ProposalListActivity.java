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
    private List<Proposal> allProposals = new ArrayList<>();
    private List<Proposal> filteredProposals = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proposal_list);

        db = FirebaseFirestore.getInstance();

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // RecyclerView setup
        rvProposals = findViewById(R.id.rvProposals);
        rvProposals.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProposalAdapter(filteredProposals, proposal -> {
            Intent intent = new Intent(this, ProposalDetailActivity.class);
            intent.putExtra("proposalId", proposal.getId());
            startActivity(intent);
        });
        rvProposals.setAdapter(adapter);

        // Filter buttons
        findViewById(R.id.btnFilterAll).setOnClickListener(v -> filterBy("all"));
        findViewById(R.id.btnFilterPending).setOnClickListener(v -> filterBy("pending"));
        findViewById(R.id.btnFilterApproved).setOnClickListener(v -> filterBy("approved"));
        findViewById(R.id.btnFilterRejected).setOnClickListener(v -> filterBy("rejected"));

        loadProposals();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProposals(); // Refresh after returning from detail
    }

    private void loadProposals() {
        db.collection("proposals").get()
                .addOnSuccessListener(query -> {
                    allProposals.clear();
                    int pending = 0, approved = 0, rejected = 0;

                    for (QueryDocumentSnapshot doc : query) {
                        Proposal p = doc.toObject(Proposal.class);
                        p.setId(doc.getId());
                        allProposals.add(p);

                        String status = p.getStatus();
                        if ("pending".equals(status)) pending++;
                        else if ("approved".equals(status)) approved++;
                        else if ("rejected".equals(status)) rejected++;
                    }

                    // Update stat cards
                    ((TextView) findViewById(R.id.tvStatPending)).setText(String.valueOf(pending));
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