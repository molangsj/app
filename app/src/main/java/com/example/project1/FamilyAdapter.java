package com.example.project1;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FamilyAdapter extends RecyclerView.Adapter<FamilyAdapter.FamilyViewHolder> {

    private List<FamilyMember> familyMembers;
    private OnExtraInfoClickListener extraInfoListener;
    private OnDrugInfoClickListener drugInfoListener;
    private OnItemLongClickListener longClickListener;

    public interface OnExtraInfoClickListener {
        void onExtraInfoClick(int position);
    }
    public interface OnDrugInfoClickListener {
        void onDrugInfoClick();
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void updateFamilyMembers(List<FamilyMember> familyMembers) {
        this.familyMembers = familyMembers;
        notifyDataSetChanged();
    }

    public FamilyAdapter(List<FamilyMember> familyMembers, OnExtraInfoClickListener extraInfoListener, OnDrugInfoClickListener drugInfoListener) {
        this.familyMembers = familyMembers;
        this.extraInfoListener = extraInfoListener;
        this.drugInfoListener = drugInfoListener;
    }

    @NonNull
    @Override
    public FamilyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.family_mem_add, parent, false);
            return new FamilyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FamilyViewHolder holder, int position) {
            FamilyMember member = familyMembers.get(position);
            holder.bind(member, position, extraInfoListener, drugInfoListener, longClickListener);
    }

    @Override
    public int getItemCount() {
        return familyMembers.size();
    }
    class FamilyViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private ImageView extraInfoButton;
        private TextView pillText;
        private LinearLayout drugInfoLayout;

        public FamilyViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tep1_mem_name);
            extraInfoButton = itemView.findViewById(R.id.extra_info);
            drugInfoLayout = itemView.findViewById(R.id.family_specific_info);
            pillText = itemView.findViewById(R.id.pill_text);
        }

        public void bind(FamilyMember member, int position, OnExtraInfoClickListener listener, OnDrugInfoClickListener drugInfoListener, OnItemLongClickListener longClickListener) {
            nameText.setText(member.getName());
            extraInfoButton.setOnClickListener(v -> {
                if (FamilyAdapter.this.extraInfoListener != null) {
                    FamilyAdapter.this.extraInfoListener.onExtraInfoClick(position);
                }
            });

            pillText.setOnClickListener(v -> {
                if (drugInfoListener != null) {
                    drugInfoListener.onDrugInfoClick();
                }
            });

            itemView.setOnLongClickListener(v->{
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(position);
                    return true;
                }
                return false;
            });
        }
    }
}