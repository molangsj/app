package com.example.project1;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.project1.databinding.ActivityFamilyDrugInfoBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Family_Sub_Drug_Info extends Fragment {

    private static final String ARG_MEMBER_ID = "memberId";
    private String memberId2;
    private ActivityFamilyDrugInfoBinding binding;

    public static Family_Sub_Drug_Info newInstance(String memberId) {
        Family_Sub_Drug_Info fragment = new Family_Sub_Drug_Info();
        Bundle args = new Bundle();
        args.putString(ARG_MEMBER_ID, memberId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // arguments 로 넘어온 값 우선
        if (getArguments() != null) {
            memberId2 = getArguments().getString(ARG_MEMBER_ID);
        }

        // FragmentResultListener 로도 받을 수 있게 유지
        getParentFragmentManager().setFragmentResultListener(
                "request",
                this,
                (requestKey, result) -> {
                    if (!"request".equals(requestKey)) return;

                    String memberId = result.getString("memberId");
                    if (memberId != null) {
                        Log.d("Family_Sub_Drug_Info", "Received memberId via result: " + memberId);
                        memberId2 = memberId;
                        if (binding != null) {
                            fetchDrugInfo(binding.getRoot());
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = ActivityFamilyDrugInfoBinding.inflate(inflater, container, false);

        if (memberId2 != null) {
            fetchDrugInfo(binding.getRoot());
        } else {
            Log.w("Family_Sub_Drug_Info", "memberId2 is null in onCreateView");
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // 뒤로가기 처리
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        getParentFragmentManager().popBackStack();
                        View recyclerView = requireActivity().findViewById(R.id.family_content);
                        if (recyclerView != null) {
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );
    }

    private void showGlobalLoading(boolean show) {
        if (!isAdded()) return;
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showLoading(show);
        }
    }

    private void fetchDrugInfo(View fragmentView) {
        if (memberId2 == null || memberId2.isEmpty()) {
            Log.e("Family_Sub_Drug_Info", "fetchDrugInfo: memberId2 is null/empty");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        showGlobalLoading(true);

        db.collection("FamilyMember")
                .document(memberId2)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;
                    showGlobalLoading(false);

                    if (documentSnapshot.exists()) {

                        // 1) 상단 닉네임 세팅 (displayName 우선, 없으면 문서 ID)
                        TextView memberNameView = fragmentView.findViewById(R.id.memberId);
                        String displayName = documentSnapshot.getString("displayName");
                        if (displayName != null && !displayName.trim().isEmpty()) {
                            memberNameView.setText(displayName);
                        } else {
                            memberNameView.setText(memberId2);
                        }

                        // 2) 프로필 이미지 세팅 (profileImageUrl 사용)
                        ImageView profileImageView = fragmentView.findViewById(R.id.top_mem1);
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        if (profileImageUrl != null && !profileImageUrl.trim().isEmpty()) {
                            Glide.with(fragmentView.getContext())
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.user1)
                                    .circleCrop()
                                    .into(profileImageView);
                        } else {
                            profileImageView.setImageResource(R.drawable.user1);
                        }

                        // 3) 날짜 필드 모으기
                        List<Integer> dateCollectionNames = new ArrayList<>();
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            for (String key : data.keySet()) {
                                try {
                                    dateCollectionNames.add(Integer.parseInt(key));
                                } catch (NumberFormatException e) {
                                    Log.d("Firestore", "Non-date field ignored: " + key);
                                }
                            }
                        }

                        if (!dateCollectionNames.isEmpty()) {
                            Collections.sort(dateCollectionNames, Collections.reverseOrder());
                            int latestDate = dateCollectionNames.get(0);
                            Log.d("Firestore", "Recent date collection: " + latestDate);

                            fetchDrugInfoFromLatestDate(fragmentView, String.valueOf(latestDate));
                        } else {
                            Log.d("Firestore", "No date collections found.");
                            // 약 정보 없을 때 UI 비우기
                            LinearLayout scrollbarContent = fragmentView.findViewById(R.id.scrollbar_content);
                            scrollbarContent.removeAllViews();
                        }
                    } else {
                        Log.d("Firestore", "Member document doesn't exist: " + memberId2);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showGlobalLoading(false);
                    Log.e("Firestore", "Failed to fetch member document", e);
                });
    }



    private void fetchDrugInfoFromLatestDate(View fragmentView, String latestDate) {
        if (memberId2 == null || memberId2.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        showGlobalLoading(true);

        db.collection("FamilyMember")
                .document(memberId2)
                .collection(latestDate)
                // .whereGreaterThanOrEqualTo("pillType", 200L)  // ← 일단 주석
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    showGlobalLoading(false);

                    LinearLayout scrollbarContent = fragmentView.findViewById(R.id.scrollbar_content);
                    scrollbarContent.removeAllViews();

                    Log.d("Firestore", "Docs in " + latestDate + ": " + querySnapshot.size());

                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                            String drugName = documentSnapshot.getId();
                            Log.d("Firestore", "Drug name fetched: " + drugName);

                            Map<String, Object> drugData = documentSnapshot.getData();
                            if (drugData != null) {
                                Long pillType = null;
                                Object pillTypeObj = drugData.get("pillType");
                                if (pillTypeObj instanceof Number) {
                                    pillType = ((Number) pillTypeObj).longValue();
                                }

                                Object isCheckedObj = drugData.get("pillIsChecked");
                                boolean isChecked = false;
                                if (isCheckedObj instanceof Boolean) {
                                    isChecked = (Boolean) isCheckedObj;
                                } else if (isCheckedObj instanceof Number) {
                                    isChecked = ((Number) isCheckedObj).intValue() != 0;
                                }

                                addDrugInfoToLayout(fragmentView, drugName, pillType, isChecked);
                            }
                        }
                    } else {
                        Log.d("Firestore", latestDate + " 컬렉션에 문서가 없습니다.");
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showGlobalLoading(false);
                    Log.e("Firestore", latestDate + " 컬렉션 데이터 가져오기 실패", e);
                });
    }


    private void addDrugInfoToLayout(
            View fragmentView,
            String drugName,
            @Nullable Long pillType,
            boolean isChecked
    ) {
        LinearLayout scrollbarContent = fragmentView.findViewById(R.id.scrollbar_content);

        // ===== 최상위 pillLayout =====
        LinearLayout pillLayout = new LinearLayout(fragmentView.getContext());
        pillLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams pillLayoutParams = new LinearLayout.LayoutParams(
                dpToPx(fragmentView.getContext(), 260),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        pillLayoutParams.gravity = Gravity.CENTER;
        pillLayoutParams.setMargins(
                0,
                0,
                0,
                dpToPx(fragmentView.getContext(), 15)
        );
        pillLayout.setLayoutParams(pillLayoutParams);
        pillLayout.setGravity(Gravity.CENTER);

        // ===== 약 아이콘 컨테이너 =====
        LinearLayout pillIconContainer = new LinearLayout(fragmentView.getContext());
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                dpToPx(fragmentView.getContext(), 50),
                dpToPx(fragmentView.getContext(), 50)
        );
        containerParams.setMargins(
                0,
                dpToPx(fragmentView.getContext(), 5),
                0,
                dpToPx(fragmentView.getContext(), 5)
        );
        pillIconContainer.setLayoutParams(containerParams);
        pillIconContainer.setGravity(Gravity.CENTER);
        pillIconContainer.setBackgroundResource(R.drawable.circle);

        ImageView medicineIcon = new ImageView(fragmentView.getContext());
        LinearLayout.LayoutParams medicineIconParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        medicineIcon.setLayoutParams(medicineIconParams);
        medicineIcon.setImageResource(R.drawable.ic_medicine_icon);
        pillIconContainer.addView(medicineIcon);
        pillLayout.addView(pillIconContainer);

        // ===== 정보 영역 =====
        RelativeLayout pillInfoLayout = new RelativeLayout(fragmentView.getContext());
        RelativeLayout.LayoutParams pillInfoLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        pillInfoLayout.setLayoutParams(pillInfoLayoutParams);

        // 약 이름
        TextView pillNameTextView = new TextView(fragmentView.getContext());
        RelativeLayout.LayoutParams pillNameParams = new RelativeLayout.LayoutParams(
                dpToPx(fragmentView.getContext(), 160),
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        pillNameParams.setMargins(
                dpToPx(fragmentView.getContext(), 10),
                0,
                0,
                0
        );
        pillNameTextView.setLayoutParams(pillNameParams);
        pillNameTextView.setText(drugName);
        pillNameTextView.setTextSize(15);
        pillNameTextView.setPadding(0, 0, 0, dpToPx(fragmentView.getContext(), 5));
        pillNameTextView.setBackgroundResource(R.drawable.border_setting_below);
        pillInfoLayout.addView(pillNameTextView);

        // 체크 아이콘
        ImageView pillCheckIcon = new ImageView(fragmentView.getContext());
        RelativeLayout.LayoutParams pillCheckIconParams = new RelativeLayout.LayoutParams(
                dpToPx(fragmentView.getContext(), 18),
                dpToPx(fragmentView.getContext(), 18)
        );
        pillCheckIconParams.addRule(RelativeLayout.CENTER_VERTICAL);
        pillCheckIconParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        pillCheckIconParams.setMargins(
                0,
                0,
                dpToPx(fragmentView.getContext(), 7),
                0
        );
        pillCheckIcon.setLayoutParams(pillCheckIconParams);

        if (isChecked) {
            pillCheckIcon.setImageResource(R.drawable.pill_checked);
        } else {
            pillCheckIcon.setImageResource(R.drawable.pill_check);
        }

        pillInfoLayout.addView(pillCheckIcon);
        pillLayout.addView(pillInfoLayout);

        scrollbarContent.addView(pillLayout);
    }

    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
