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

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.project1.databinding.ActivityFamilyDrugInfoBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Family_drug_info extends Fragment {
    private static final String ARG_MEMBER_ID = "memberId";
    private String memberId2;
    private ActivityFamilyDrugInfoBinding binding;

    public static Family_drug_info newInstance(String memberId) {
        Family_drug_info fragment = new Family_drug_info();
        Bundle args = new Bundle();
        args.putString(ARG_MEMBER_ID, memberId);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FragmentResultListener 설정
        getParentFragmentManager().setFragmentResultListener("request", this, (requestKey, result) -> {
            if ("request".equals(requestKey)) {
                String memberId = result.getString("memberId");
                if (memberId != null) {
                    Log.d("Family_drug_info", "Received memberId: " + memberId);
                    memberId2 = memberId; // 전달받은 memberId로 데이터 처리

                    // memberId를 수신한 후 fetchDrugInfo 호출
                    if (binding != null) { // View가 이미 생성된 상태인지 확인
                        fetchDrugInfo(binding.getRoot());
                    }
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivityFamilyDrugInfoBinding.inflate(inflater, container, false);
        if (memberId2 != null) {
            fetchDrugInfo(binding.getRoot());
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 뒤로 가기 눌렀을 때 RecyclerView 다시 표시
                getParentFragmentManager().popBackStack();
                View recyclerView = requireActivity().findViewById(R.id.family_content);
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void fetchDrugInfo(View fragmentView) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("FamilyMember")
                .document(memberId2)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 날짜 필드를 추출하여 리스트로 저장
                        List<Integer> dateCollectionNames = new ArrayList<>();
                        for (String key : documentSnapshot.getData().keySet()) {
                            try {
                                // 날짜 문자열을 정수로 변환하여 리스트에 추가
                                dateCollectionNames.add(Integer.parseInt(key));
                            } catch (NumberFormatException e) {
                                // 날짜가 아닌 필드는 무시
                                Log.d("Firestore", "Non-date field ignored: " + key);
                            }
                        }

                        if (!dateCollectionNames.isEmpty()) {
                            // 날짜 리스트 정렬 (내림차순: 최신 날짜가 첫 번째)
                            Collections.sort(dateCollectionNames, Collections.reverseOrder());

                            // 최신 날짜 컬렉션을 선택
                            int latestDate = dateCollectionNames.get(0);
                            Log.d("Firestore", "Recent date collection: " + latestDate);

                            // Step 2: 최신 날짜 컬렉션에서 Drug_Info 데이터 가져오기
                            fetchDrugInfoFromLatestDate(fragmentView, String.valueOf(latestDate));
                        } else {
                            Log.d("Firestore", "No date collections found.");
                        }
                    } else {
                        Log.d("Firestore", "Member1 document doesn't exist.");
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to fetch Member1 document", e));
    }



    private void fetchDrugInfoFromLatestDate(View fragmentView, String latestDate) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // 최신 날짜 컬렉션의 모든 문서 가져오기
        db.collection("FamilyMember")
                .document(memberId2)
                .collection(latestDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                            // 약 이름은 문서 이름
                            String drugName = documentSnapshot.getId();
                            Log.d("Firestore", "Drug name fetched: " + drugName);
                            // 약 정보는 문서 데이터
                            Map<String, Object> drugData = documentSnapshot.getData();
                            if (drugData != null) {
                                // pilltype 및 pillischecked 필드 가져오기
                                Long pillIsChecked = (Long) drugData.getOrDefault("pillIsChecked", 0L);
                                Long pillType = (Long) drugData.getOrDefault("pillType", 0L);

                                // UI 업데이트
                                addDrugInfoToLayout(fragmentView, drugName, pillType, pillIsChecked);
                            }
                        }
                    } else {
                        Log.d("Firestore", latestDate + " 컬렉션에 문서가 없습니다.");
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", latestDate + " 컬렉션 데이터 가져오기 실패", e));
    }


    private void addDrugInfoToLayout(View fragmentView, String drugName, Long pillType, Long pillIsChecked) {
        LinearLayout scrollbarContent = fragmentView.findViewById(R.id.scrollbar_content);
        TextView membername=fragmentView.findViewById(R.id.memberId);
        membername.setText(memberId2);
        // 약 이름 (drugName)와 약 유형 (pillType)을 포함하여 약 이름 표시
        String pillName = drugName;

        // 최상위 LinearLayout 생성
        LinearLayout pillLayout = new LinearLayout(fragmentView.getContext());
        pillLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams pillLayoutParams = new LinearLayout.LayoutParams(
                dpToPx(fragmentView.getContext(), 260),  // 260dp
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        pillLayoutParams.gravity = Gravity.CENTER;
        pillLayoutParams.setMargins(0, 0, 0, dpToPx(fragmentView.getContext(), 15)); // marginBottom 15dp
        pillLayout.setLayoutParams(pillLayoutParams);
        pillLayout.setGravity(Gravity.CENTER);

        // ImageView (약 아이콘)
        LinearLayout pillIconContainer = new LinearLayout(fragmentView.getContext());
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                dpToPx(fragmentView.getContext(), 50),
                dpToPx(fragmentView.getContext(), 50)
        );
        containerParams.setMargins(0, dpToPx(fragmentView.getContext(), 5), 0, dpToPx(fragmentView.getContext(), 5));
        pillIconContainer.setLayoutParams(containerParams);
        pillIconContainer.setGravity(Gravity.CENTER);
        pillIconContainer.setBackgroundResource(R.drawable.circle);

        // ImageView (약 이미지)
        ImageView medicineIcon = new ImageView(fragmentView.getContext());
        LinearLayout.LayoutParams medicineIconParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        medicineIcon.setLayoutParams(medicineIconParams);
        medicineIcon.setImageResource(R.drawable.ic_medicine_icon);
        pillIconContainer.addView(medicineIcon);
        pillLayout.addView(pillIconContainer);

        // RelativeLayout (pill 정보 포함 영역)
        RelativeLayout pillInfoLayout = new RelativeLayout(fragmentView.getContext());
        RelativeLayout.LayoutParams pillInfoLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        pillInfoLayout.setLayoutParams(pillInfoLayoutParams);

        // TextView (약 이름 표시)
        TextView pillNameTextView = new TextView(fragmentView.getContext());
        RelativeLayout.LayoutParams pillNameParams = new RelativeLayout.LayoutParams(
                dpToPx(fragmentView.getContext(), 160), // 160dp
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        pillNameParams.setMargins(dpToPx(fragmentView.getContext(), 10), 0, 0, 0);
        pillNameTextView.setLayoutParams(pillNameParams);
        pillNameTextView.setText(pillName);  // 약 이름 + 타입 표시
        pillNameTextView.setTextSize(15);
        pillNameTextView.setPadding(0, 0, 0, dpToPx(fragmentView.getContext(), 5));
        pillNameTextView.setBackgroundResource(R.drawable.border_setting_below);
        pillInfoLayout.addView(pillNameTextView);

        // ImageView (체크 아이콘)
        ImageView pillCheckIcon = new ImageView(fragmentView.getContext());
        RelativeLayout.LayoutParams pillCheckIconParams = new RelativeLayout.LayoutParams(
                dpToPx(fragmentView.getContext(), 18),
                dpToPx(fragmentView.getContext(), 18)
        );
        pillCheckIconParams.addRule(RelativeLayout.CENTER_VERTICAL);
        pillCheckIconParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        pillCheckIconParams.setMargins(0, 0, dpToPx(fragmentView.getContext(), 7), 0);
        pillCheckIcon.setLayoutParams(pillCheckIconParams);

        // pillischecked 값을 기반으로 아이콘 설정
        if (pillIsChecked != null && pillIsChecked.equals(1L)) {  // Long 값을 비교할 때는 equals() 사용
            pillCheckIcon.setImageResource(R.drawable.pill_checked);
        } else {
            pillCheckIcon.setImageResource(R.drawable.pill_check);
        }
        pillInfoLayout.addView(pillCheckIcon);
        pillLayout.addView(pillInfoLayout);

        // 최종 레이아웃 추가
        scrollbarContent.addView(pillLayout);
    }

    // dp 값을 px로 변환하는 헬퍼 메서드
    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}