package com.lums.eventhub.admin.proposals;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.lums.eventhub.R;
import com.lums.eventhub.model.Proposal;
import java.util.List;

/**
 * ProposalAdapter.java
 *
 * Role: RecyclerView Adapter for displaying a list of Proposal objects
 * in ProposalListActivity. Each row shows the event title, society name,
 * date, venue, status badge, and a Review button.
 *
 * Status colour coding:
 *   Submitted          → pink  (#E8637A) — pending CCA review
 *   Approved           → green (#27AE60)
 *   Rejected           → red   (#E74C3C)
 *   Revision Requested → orange (#F39C12)
 *   default            → grey  (#9B7B86)
 *
 * Implements: Admin US-02
 */

public class ProposalAdapter extends RecyclerView.Adapter<ProposalAdapter.ViewHolder> {

    private final List<Proposal> proposalList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Proposal proposal);
    }

    public ProposalAdapter(List<Proposal> proposalList, OnItemClickListener listener) {
        this.proposalList = proposalList;
        this.listener     = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_proposal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Proposal p = proposalList.get(position);

        // Event Name
        holder.tvTitle.setText(p.getTitle() != null ? p.getTitle() : "Untitled");

        // Society name — prefer societyName field, fallback to extracting from organizerUsername
        String society = p.getSocietyName();
        if (society == null || society.isEmpty()) {
            society = p.getOrganizerUsername();
            if (society != null && society.contains("_")) {
                society = society.substring(society.indexOf("_") + 1).toUpperCase();
            }
        }
        holder.tvOrganizer.setText("👤 " + (society != null ? society : "—"));

        // Date — use date field, fallback to eventDate for legacy docs
        String date = p.getEventDate();
        holder.tvDate.setText("📅 " + (date != null ? date : "—"));

        // Venue
        holder.tvVenue.setText("📍 " + (p.getVenue() != null ? p.getVenue() : "—"));

        // Status badge with canonical colour coding
        String status = p.getStatus() != null ? p.getStatus() : "Unknown";
        holder.tvStatus.setText(status.toUpperCase());
        switch (status) {
            case "Submitted":
                holder.tvStatus.setTextColor(Color.parseColor("#E8637A"));  // pink = pending review
                break;
            case "Approved":
                holder.tvStatus.setTextColor(Color.parseColor("#27AE60"));  // green
                break;
            case "Rejected":
                holder.tvStatus.setTextColor(Color.parseColor("#E74C3C"));  // red
                break;
            case "Revision Requested":
                holder.tvStatus.setTextColor(Color.parseColor("#F39C12"));  // orange
                break;
            default:
                holder.tvStatus.setTextColor(Color.parseColor("#9B7B86"));  // grey
        }

        holder.tvReview.setOnClickListener(v -> listener.onItemClick(p));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(p));
    }

    @Override
    public int getItemCount() { return proposalList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStatus, tvOrganizer, tvDate, tvVenue, tvReview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle     = itemView.findViewById(R.id.tvItemTitle);
            tvStatus    = itemView.findViewById(R.id.tvItemStatus);
            tvOrganizer = itemView.findViewById(R.id.tvItemOrganizer);
            tvDate      = itemView.findViewById(R.id.tvItemDate);
            tvVenue     = itemView.findViewById(R.id.tvItemVenue);
            tvReview    = itemView.findViewById(R.id.tvItemReview);
        }
    }
}