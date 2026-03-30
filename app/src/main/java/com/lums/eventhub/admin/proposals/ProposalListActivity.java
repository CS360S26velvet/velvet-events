package com.lums.eventhub.admin.proposals;

import android.content.Intent;
import android.os.Bundle;
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
    private FirebaseFirestore db;
    private List<Proposal> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proposal_list);

        db = FirebaseFirestore.getInstance();
        db.collection("proposals").get().addOnSuccessListener(query -> {
            for (QueryDocumentSnapshot doc : query) {
                Proposal p = doc.toObject(Proposal.class);
                p.setId(doc.getId());
                list.add(p);
            }
            // You'll need an Adapter here later to show these in the list!
        });
    }
}