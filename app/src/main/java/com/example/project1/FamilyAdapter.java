package com.example.project1;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FamilyAdapter extends RecyclerView.Adapter<FamilyAdapter.FamilyViewHolder> {

    private List<FamilyMember> familyMembers;
    private OnExtraInfoClickListener extraInfoListener;
    private OnDrugInfoClickListener drugInfoListener;
    private OnItemLongClickListener longClickListener;

    // 실시간 리스너들을 저장할 Map
    private Map<String, ListenerRegistration> listenerMap = new HashMap<>();

    public interface OnExtraInfoClickListener {
        void onExtraInfoClick(int position);
    }

    public interface OnDrugInfoClickListener {
        void onDrugInfoClick(String memberDocId);
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

    public FamilyAdapter(List<FamilyMember> familyMembers,
                         OnExtraInfoClickListener extraInfoListener,
                         OnDrugInfoClickListener drugInfoListener) {
        this.familyMembers = familyMembers;
        this.extraInfoListener = extraInfoListener;
        this.drugInfoListener = drugInfoListener;
    }

    @NonNull
    @Override
    public FamilyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.family_mem_add, parent, false);
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

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        detachAllListeners();
    }

    public void detachAllListeners() {
        for (ListenerRegistration listener : listenerMap.values()) {
            if (listener != null) {
                listener.remove();
            }
        }
        listenerMap.clear();
        Log.d("FamilyAdapter", "All listeners detached");
    }

    class FamilyViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private ImageView extraInfoButton;
        private TextView pillText;
        private LinearLayout drugInfoLayout;
        private ImageView profileImage;
        private String currentDocId;

        public FamilyViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tep1_mem_name);
            extraInfoButton = itemView.findViewById(R.id.extra_info);
            // 여기 수정: family_specific_info → drug_info
            drugInfoLayout = itemView.findViewById(R.id.drug_info);
            pillText = itemView.findViewById(R.id.pill_text);
            profileImage = itemView.findViewById(R.id.top_mem1);
        }

        public void bind(FamilyMember member,
                         int position,
                         OnExtraInfoClickListener listener,
                         OnDrugInfoClickListener drugInfoListener,
                         OnItemLongClickListener longClickListener) {

            String docId = member.getDocId();
            currentDocId = docId;
            nameText.setText(docId);

            attachRealtimeListener(docId);

            extraInfoButton.setOnClickListener(v -> {
                if (FamilyAdapter.this.extraInfoListener != null) {
                    FamilyAdapter.this.extraInfoListener.onExtraInfoClick(position);
                }
            });

            pillText.setOnClickListener(v -> {
                if (drugInfoListener != null) {
                    drugInfoListener.onDrugInfoClick(docId);
                }
            });

            // NPE 방지 + drug_info 에 클릭 걸기
            if (drugInfoLayout != null) {
                drugInfoLayout.setOnClickListener(v -> {
                    if (drugInfoListener != null) {
                        drugInfoListener.onDrugInfoClick(docId);
                    }
                });
            }

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(position);
                    return true;
                }
                return false;
            });
        }

        private void attachRealtimeListener(String docId) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            ListenerRegistration oldListener = listenerMap.get(docId);
            if (oldListener != null) {
                oldListener.remove();
            }

            ListenerRegistration listener = db.collection("FamilyMember")
                    .document(docId)
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null) {
                            Log.e("FamilyAdapter", "Listen failed for: " + docId, error);
                            profileImage.setImageResource(R.drawable.user1);
                            return;
                        }

                        // 뷰가 이미 화면에서 떨어졌으면 그만
                        if (!itemView.isAttachedToWindow()) {
                            return;
                        }

                        // 액티비티가 destroy 되어 있으면 Glide 호출하지 않음
                        Context context = itemView.getContext();
                        if (context instanceof Activity) {
                            Activity activity = (Activity) context;
                            if (activity.isFinishing() || activity.isDestroyed()) {
                                return;
                            }
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                // View 기반으로 Glide 시작 (액티비티 라이프사이클 묶임)
                                Glide.with(itemView)
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.user1)
                                        .circleCrop()
                                        .into(profileImage);
                                Log.d("FamilyAdapter", "Profile image updated for: " + docId);
                            } else {
                                profileImage.setImageResource(R.drawable.user1);
                                Log.d("FamilyAdapter", "No profile image URL for: " + docId);
                            }

                            String displayName = documentSnapshot.getString("displayName");
                            if (displayName != null && !displayName.trim().isEmpty()) {
                                nameText.setText(displayName);
                            }
                        } else {
                            profileImage.setImageResource(R.drawable.user1);
                            Log.w("FamilyAdapter", "Document not found: " + docId);
                        }
                    });

            listenerMap.put(docId, listener);
        }

    }
}
