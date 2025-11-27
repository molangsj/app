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

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
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

    // ⭐ 어댑터가 파괴될 때 모든 리스너 해제
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        detachAllListeners();
    }

    // 모든 리스너 해제
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
            drugInfoLayout = itemView.findViewById(R.id.family_specific_info);
            pillText = itemView.findViewById(R.id.pill_text);
            profileImage = itemView.findViewById(R.id.top_mem1);
        }

        public void bind(FamilyMember member, int position, OnExtraInfoClickListener listener, OnDrugInfoClickListener drugInfoListener, OnItemLongClickListener longClickListener) {
            String docId = member.getDocId();
            currentDocId = docId;
            nameText.setText(docId);

            // ⭐ 실시간 리스너로 프로필 이미지 로드
            attachRealtimeListener(docId);

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

        /**
         * ⭐ 실시간 리스너 추가 - Firestore 데이터가 변경되면 자동으로 업데이트!
         */
        private void attachRealtimeListener(String docId) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 기존 리스너가 있으면 제거
            ListenerRegistration oldListener = listenerMap.get(docId);
            if (oldListener != null) {
                oldListener.remove();
            }

            // 실시간 리스너 등록
            ListenerRegistration listener = db.collection("FamilyMember")
                    .document(docId)
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null) {
                            Log.e("FamilyAdapter", "Listen failed for: " + docId, error);
                            profileImage.setImageResource(R.drawable.user1);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                // Glide로 프로필 이미지 로드
                                Glide.with(itemView.getContext())
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.user1)
                                        .circleCrop()
                                        .into(profileImage);
                                Log.d("FamilyAdapter", "Profile image updated for: " + docId);
                            } else {
                                // 프로필 이미지 URL이 없으면 기본 이미지
                                profileImage.setImageResource(R.drawable.user1);
                                Log.d("FamilyAdapter", "No profile image URL for: " + docId);
                            }

                            // displayName이 있으면 닉네임으로 표시
                            String displayName = documentSnapshot.getString("displayName");
                            if (displayName != null && !displayName.trim().isEmpty()) {
                                nameText.setText(displayName);
                            }
                        } else {
                            // 문서를 찾지 못하면 기본 이미지
                            profileImage.setImageResource(R.drawable.user1);
                            Log.w("FamilyAdapter", "Document not found: " + docId);
                        }
                    });

            // 리스너를 Map에 저장 (나중에 해제하기 위해)
            listenerMap.put(docId, listener);
        }
    }
}