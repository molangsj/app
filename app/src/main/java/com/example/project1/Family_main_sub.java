package com.example.project1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project1.databinding.FamilyMainSubBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import android.content.Context;
import android.content.SharedPreferences;


public class Family_main_sub extends Fragment {

    private FamilyMainSubBinding binding;
    private RecyclerView recyclerView;
    private FamilyAdapter adapter;
    private static final String PREF_NAME = "family_prefs";
    private static final String KEY_DISPLAYED = "displayed_members";


    // Firestore에서 읽어온 전체 유저 목록 (닉네임 검색의 대상)
    private List<FamilyMember> familyMembers;
    // 실제 화면에 보여지는 가족 멤버 목록 (닉네임으로 추가된 멤버들만)
    private List<FamilyMember> displayedMembers;
    // 삭제된 멤버 ID 추적
    private Set<String> removedMemberIds;

    private FamilyViewModel familyViewModel;
    private static FirebaseFirestore db;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private NotificationService notificationService;
    private String memberId3;
    private String uid;
    private String uid2;

    private String mParam1;
    private String mParam2;

    public static Family_main_sub newInstance(String param1, String param2) {
        Family_main_sub fragment = new Family_main_sub();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FamilyMainSubBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        notificationService = RetrofitClient.getInstance().create(NotificationService.class);
        familyViewModel = new ViewModelProvider(requireActivity()).get(FamilyViewModel.class);

        // 여기만 XML에 맞게 수정
        recyclerView = view.findViewById(R.id.family_content);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        familyMembers = new ArrayList<>();
        displayedMembers = new ArrayList<>();
        removedMemberIds = new HashSet<>();

        adapter = new FamilyAdapter(
                displayedMembers,
                position -> handleExtraInfoClick(position),
                this::familydruginfo
        );
        recyclerView.setAdapter(adapter);

        adapter.setOnItemLongClickListener(position -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("삭제 확인")
                    .setMessage("정말로 이 항목을 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> removeMember(position))
                    .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        loadFamilyMembersFromFirestore();

        familyViewModel.getFamilyMembers().observe(
                getViewLifecycleOwner(),
                members -> adapter.updateFamilyMembers(displayedMembers)
        );

        view.findViewById(R.id.add_mem).setOnClickListener(v -> showAddMemberDialog());

        Fragment currentFragment = getParentFragmentManager().findFragmentById(R.id.fragment_container);

        view.findViewById(R.id.family_statistic).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Family_statistic.class);
            getParentFragmentManager().beginTransaction()
                    .hide(currentFragment)
                    .add(R.id.fragment_container, Family_statistic.newInstance("param1", "param2"))
                    .addToBackStack(null)
                    .commit();
        });
    }

    // 현재 displayedMembers 를 SharedPreferences 에 저장
    private void saveDisplayedMembersToPrefs() {
        if (getContext() == null) return;

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // memberId 들만 Set<String> 으로 저장
        Set<String> idSet = new HashSet<>();
        for (FamilyMember m : displayedMembers) {
            idSet.add(m.getDocId());
        }

        prefs.edit().putStringSet(KEY_DISPLAYED, idSet).apply();
    }

    // SharedPreferences 에 저장된 idSet 을 읽어와 displayedMembers 로 복원
    private void restoreDisplayedMembersFromPrefs() {
        if (getContext() == null) return;

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        Set<String> idSet = prefs.getStringSet(KEY_DISPLAYED, null);
        if (idSet == null || idSet.isEmpty()) {
            return;
        }

        displayedMembers.clear();

        // familyMembers(전체 유저 목록) 중에서 idSet 에 있는 것만 골라서 추가
        for (FamilyMember member : familyMembers) {
            if (idSet.contains(member.getDocId())) {
                displayedMembers.add(member);
            }
        }
        adapter.notifyDataSetChanged();
    }



    // ---------------------------
    // 닉네임 입력 다이얼로그
    // ---------------------------
    private void showAddMemberDialog() {
        if (getContext() == null) return;

        final EditText input = new EditText(getContext());
        input.setHint("닉네임을 입력하세요");

        new AlertDialog.Builder(getContext())
                .setTitle("가족 멤버 추가")
                .setView(input)
                .setPositiveButton("검색", (dialog, which) -> {
                    String nickname = input.getText().toString().trim();
                    if (nickname.isEmpty()) {
                        Toast.makeText(getContext(), "닉네임을 입력하세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    searchAndAddMemberByNickname(nickname);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // 이미 화면에 추가된 멤버인지 확인
    private boolean isMemberAlreadyDisplayed(String memberId) {
        for (FamilyMember m : displayedMembers) {
            if (memberId.equals(m.getDocId())) {
                return true;
            }
        }
        return false;
    }

    // 닉네임으로 familyMembers에서 검색해서 displayedMembers에 추가
    private void searchAndAddMemberByNickname(String nickname) {
        if (familyMembers == null || familyMembers.isEmpty()) {
            Toast.makeText(getContext(), "가족 멤버 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 이미 추가된 멤버인지 검사
        if (isMemberAlreadyDisplayed(nickname)) {
            Toast.makeText(getContext(), "이미 추가된 멤버입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        FamilyMember target = null;
        for (FamilyMember member : familyMembers) {
            // 닉네임을 문서 ID(docId)로 사용하고 있으므로 이렇게 비교
            if (nickname.equals(member.getDocId())) {
                target = member;
                break;
            }
        }

        if (target == null) {
            Toast.makeText(getContext(), "해당 닉네임의 사용자를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 제거 목록에 들어있었다면 제거
        if (removedMemberIds != null) {
            removedMemberIds.remove(target.getDocId());
        }

        displayedMembers.add(target);
        adapter.notifyItemInserted(displayedMembers.size() - 1);
        saveDisplayedMembersToPrefs();
    }

    // Firestore에서 전체 FamilyMember 문서 로딩 (닉네임 검색 대상)
    private void loadFamilyMembersFromFirestore() {
        // 현재 Activity가 MainActivity일 때만 로딩 오버레이 사용
        final MainActivity activity =
                (getActivity() instanceof MainActivity) ? (MainActivity) getActivity() : null;

        if (activity != null) {
            activity.showLoading(true);
        }

        db.collection("FamilyMember").get()
                .addOnCompleteListener(task -> {
                    if (activity != null) {
                        activity.showLoading(false);
                    }

                    if (task.isSuccessful()) {
                        familyMembers.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String docId = document.getId(); // 실제 username/문서 ID

                            FamilyMember member = new FamilyMember(docId);

                            // Firestore 문서의 displayName 필드를 화면 표시용 이름으로 사용
                            String displayName = document.getString("displayName");
                            if (displayName != null && !displayName.trim().isEmpty()) {
                                member.setName(displayName);
                            }

                            familyMembers.add(member);
                        }

                        // 저장해 둔 가족 목록 복원
                        restoreDisplayedMembersFromPrefs();
                    } else {
                        Log.w("Firestore", "Error getting data", task.getException());
                    }
                });
    }



    private void sendNotification(String receiverId, String messageText) {
        NotificationRequest request = new NotificationRequest(receiverId, messageText);

        notificationService.sendNotification(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getActivity(), "알림 전송 성공", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("NotificationError", "Response failed. Code: " + response.code() + " Message: " + response.message());
                    Toast.makeText(getActivity(), "알림 전송 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("NotificationError", "Error: " + t.getMessage(), t);
                Toast.makeText(getActivity(), "서버 연결 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Extra Info 버튼 클릭 시 동작
    private void handleExtraInfoClick(int position) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FamilyMember selectedMember = displayedMembers.get(position);
        String memberId = selectedMember.getDocId();
        memberId3 = memberId;

        Family_drug_info familyDrugInfoFragment = Family_drug_info.newInstance(memberId);
        Bundle result = new Bundle();
        result.putString("memberId", memberId);
        getParentFragmentManager().setFragmentResult("request", result);
        Log.d("Firestoreblabla", "messageId Send");

        // uid 조회
        db.collection("FamilyMember").document(memberId3)
                .get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        uid = documentSnapshot.getString("uid");
                        if (uid != null) {
                            Log.d("Firestore", "UID 값: " + uid);
                        } else {
                            Log.d("Firestore", "UID 필드가 존재하지 않습니다.");
                        }
                    } else {
                        Log.d("Firestore", "문서가 존재하지 않습니다.");
                    }
                }).addOnFailureListener(e -> Log.w("Firestore", "데이터 가져오기 실패", e));

        db.collection("FamilyMember").document(memberId3)
                .get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        uid = documentSnapshot.getString("uid");
                        if (uid != null) {
                            Log.d("Firestore", "UID 값: " + uid);

                            uid2 = uid;
                            String messageText = "아직 먹지 않은 약이 있습니다!";

                            db.collection("FamilyMember").document("users")
                                    .collection("users").document(uid2)
                                    .get()
                                    .addOnSuccessListener(innerSnapshot -> {
                                        if (innerSnapshot.exists()) {
                                            String deviceToken = innerSnapshot.getString("deviceToken");
                                            if (deviceToken != null) {
                                                Log.d("Firestore", "Device Token: " + deviceToken);
                                                // sendNotification(deviceToken);
                                            } else {
                                                Log.w("Firestore", "Device token is null for user: " + uid2);
                                            }
                                        } else {
                                            Log.w("Firestore", "Document does not exist for user: " + uid2);
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.w("Firestore", "Error fetching device token", e));

                        } else {
                            Log.d("Firestore", "UID 필드가 존재하지 않습니다.");
                        }
                    } else {
                        Log.d("Firestore", "문서가 존재하지 않습니다.");
                    }
                }).addOnFailureListener(e -> Log.w("Firestore", "데이터 가져오기 실패", e));

        // 현재 RecyclerView에서 해당 position의 itemView 가져오기
        RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
        if (viewHolder == null) {
            return;
        }
        View itemView = viewHolder.itemView;

        String messageText = "아직 먹지 않은 약이 있습니다!";
        String bubble = "abc";

        LinearLayout drugInfo = itemView.findViewById(R.id.drug_info);
        ImageView familySendImageView = itemView.findViewById(R.id.family_send);
        Animation slideDown = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down);
        Animation slideUp = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);

        familySendImageView.setOnClickListener(v -> {
            sendNotification(uid2, messageText);
            Toast.makeText(getContext(), "Family Send clicked", Toast.LENGTH_SHORT).show();
        });

        if (drugInfo.getVisibility() == View.GONE) {
            fetchDrugInfo(itemView);
            checkPillStatusAndUpdateUI(itemView, bubble);
            drugInfo.startAnimation(slideDown);
            drugInfo.setVisibility(View.VISIBLE);
        } else {
            drugInfo.startAnimation(slideUp);
            drugInfo.setVisibility(View.GONE);
        }
    }

    private void fetchDrugInfo(View fragmentView) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("FamilyMember")
                .document(memberId3)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<Integer> dateCollectionNames = new ArrayList<>();
                        for (String key : documentSnapshot.getData().keySet()) {
                            try {
                                dateCollectionNames.add(Integer.parseInt(key));
                            } catch (NumberFormatException e) {
                                Log.d("Firestore", "Non-date field ignored: " + key);
                            }
                        }

                        if (!dateCollectionNames.isEmpty()) {
                            Collections.sort(dateCollectionNames, Collections.reverseOrder());
                            int latestDate = dateCollectionNames.get(0);
                            Log.d("Firestore", "Recent date collection: " + latestDate);

                            // 최신 날짜로 약 체크 상태 확인
                            checkPillStatusAndUpdateUI(fragmentView, String.valueOf(latestDate));
                        } else {
                            Log.d("Firestore", "No date collections found.");
                        }
                    } else {
                        Log.d("Firestore", "Member document doesn't exist.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to fetch Member document", e);
                });
    }


    private void checkPillStatusAndUpdateUI(View itemView, String latestDate) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d("latest date is:", latestDate);
        db.collection("FamilyMember")
                .document(memberId3)
                .collection(latestDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean allChecked = true;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Long pillChecked = doc.getLong("pillIsChecked");
                        if (pillChecked == null || !pillChecked.equals(1L)) {
                            allChecked = false;
                            break;
                        }
                    }

                    ImageView memberPillCheck = itemView.findViewById(R.id.member_pillcheck);

                    memberPillCheck.setImageResource(
                            allChecked ? R.drawable.pill_checked : R.drawable.pill_check
                    );
                })
                .addOnFailureListener(e -> Log.e("Firestore Error", "Failed to fetch Drug_Info data", e));
    }

    // 멤버 레이아웃 삭제 처리
    private void removeMember(int position) {
        FamilyMember removedMember = displayedMembers.get(position);
        removedMemberIds.add(removedMember.getDocId());
        displayedMembers.remove(position);
        adapter.notifyItemRemoved(position);
        saveDisplayedMembersToPrefs();
    }

    private void familydruginfo() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        Fragment currentFragment = fragmentManager.findFragmentById(R.id.recycler_frame);
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }

        transaction.replace(R.id.recycler_frame, new Family_drug_info());
        transaction.addToBackStack(null);
        transaction.commit();

        recyclerView.setVisibility(View.GONE);
    }

    // ---------------- Notification ----------------

    public interface NotificationService {
        @POST("/send-notification")
        Call<Void> sendNotification(@Body NotificationRequest request);
    }

    public class NotificationRequest {
        private String receiverId;
        private String messageText;

        public NotificationRequest(String receiverId, String messageText) {
            this.receiverId = receiverId;
            this.messageText = messageText;
        }

        public String getReceiverId() {
            return receiverId;
        }

        public void setReceiverId(String receiverId) {
            this.receiverId = receiverId;
        }

        public String getMessageText() {
            return messageText;
        }

        public void setMessageText(String messageText) {
            this.messageText = messageText;
        }
    }
}

class RetrofitClient {
    private static Retrofit retrofit;

    public static Retrofit getInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("http://192.168.11.11:5000")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
