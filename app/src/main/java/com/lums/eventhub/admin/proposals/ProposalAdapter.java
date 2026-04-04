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

public class ProposalAdapter extends RecyclerView.Adapter<ProposalAdapter.ViewHolder> {

    private List<Proposal> proposalList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Proposal proposal);
    }

    public ProposalAdapter(List<Proposal> proposalList, OnItemClickListener listener) {
        this.proposalList = proposalList;
        this.listener = listener;
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

        holder.tvTitle.setText(p.getTitle());

        // Organizer display
        String org = p.getOrganizerUsername() != null ? p.getOrganizerUsername() : "—";
        if (org.contains("_")) org = org.substring(org.indexOf("_") + 1).toUpperCase();
        holder.tvOrganizer.setText("👤 " + org);

        holder.tvDate.setText("📅 " + (p.getEventDate() != null ? p.getEventDate() : "—"));
        holder.tvVenue.setText("📍 " + (p.getVenue() != null ? p.getVenue() : "—"));

        // Status badge with color
        String status = p.getStatus() != null ? p.getStatus() : "unknown";
        holder.tvStatus.setText(status.toUpperCase());
        switch (status) {
            case "pending":
                holder.tvStatus.setTextColor(Color.parseColor("#E8637A"));
                break;
            case "approved":
                holder.tvStatus.setTextColor(Color.parseColor("#27AE60"));
                break;
            case "rejected":
                holder.tvStatus.setTextColor(Color.parseColor("#E74C3C"));
                break;
            default:
                holder.tvStatus.setTextColor(Color.parseColor("#9B7B86"));
        }

        // Review button and row click
        holder.tvReview.setOnClickListener(v -> listener.onItemClick(p));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(p));
    }

    @Override
    public int getItemCount() {
        return proposalList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStatus, tvOrganizer, tvDate, tvVenue, tvReview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tvItemTitle);
            tvStatus   = itemView.findViewById(R.id.tvItemStatus);
            tvOrganizer = itemView.findViewById(R.id.tvItemOrganizer);
            tvDate     = itemView.findViewById(R.id.tvItemDate);
            tvVenue    = itemView.findViewById(R.id.tvItemVenue);
            tvReview   = itemView.findViewById(R.id.tvItemReview);
        }
    }
}