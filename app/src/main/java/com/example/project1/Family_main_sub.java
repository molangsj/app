package com.example.project1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project1.databinding.ActivityFamilyDrugInfoBinding;
import com.example.project1.databinding.FamilyMainSubBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class Family_main_sub extends Fragment {
    private FamilyMainSubBinding binding;
    private RecyclerView recyclerView;
    private FamilyAdapter adapter;
    private List<FamilyMember> familyMembers;
    private List<FamilyMember> displayedMembers;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FamilyMainSubBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        notificationService = RetrofitClient.getInstance().create(NotificationService.class);
        familyViewModel = new ViewModelProvider(requireActivity()).get(FamilyViewModel.class);

        // RecyclerView 초기화
        recyclerView = view.findViewById(R.id.family_content);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 데이터 리스트 초기화
        familyMembers = new ArrayList<>();
        displayedMembers = new ArrayList<>();
        removedMemberIds = new HashSet<>(); // 삭제된 멤버 ID를 추적

        // 어댑터 설정
        adapter = new FamilyAdapter(displayedMembers, position -> handleExtraInfoClick(position), this::familydruginfo);
        recyclerView.setAdapter(adapter);


        adapter.setOnItemLongClickListener(position -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("삭제 확인")
                    .setMessage("정말로 이 항목을 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        // 삭제 확인 시 removeMember 호출
                        removeMember(position);
                    })
                    .setNegativeButton("취소", (dialog, which) -> {
                        // 삭제 취소 시 아무 작업도 하지 않음
                        dialog.dismiss();
                    })
                    .show();
//            removeMember(position);
        });

        loadFamilyMembersFromFirestore();
        familyViewModel.getFamilyMembers().observe(getViewLifecycleOwner(), familyMembers -> {
            adapter.updateFamilyMembers(displayedMembers);
        });

        // add_mem 버튼 클릭 시 아이템 추가
        view.findViewById(R.id.add_mem).setOnClickListener(v -> {
            addMemberFromFirestore();
        });
        Fragment currentFragment = getParentFragmentManager().findFragmentById(R.id.fragment_container);

        view.findViewById(R.id.family_statistic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), Family_statistic.class);
                getParentFragmentManager().beginTransaction()
                        .hide(currentFragment)
                        .add(R.id.fragment_container, Family_statistic.newInstance("param1", "param2"))
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void loadFamilyMembersFromFirestore() {
        db.collection("FamilyMember").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        familyMembers.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String docId = document.getId(); // Firestore 문서 ID
                            familyMembers.add(new FamilyMember(docId));
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.w("Firestore", "Error getting data", task.getException());
                    }
                });
    }

    private void addMemberFromFirestore() {
        for (FamilyMember member : familyMembers) {
            String memberId = member.getDocId();

            if (removedMemberIds.contains(memberId) || !isMemberDisplayedOrRemoved(memberId)) {
//                removedMemberIds.remove(memberId);
                displayedMembers.add(member);
                removedMemberIds.remove(memberId);
                adapter.notifyItemInserted(displayedMembers.size() - 1);

                View newView = LayoutInflater.from(getContext()).inflate(R.layout.family_mem_add, null);
                ImageView familySendImageView = newView.findViewById(R.id.family_send);
                familySendImageView.setOnClickListener(v->{
                    Toast.makeText(getContext(), "Family Send clicked", Toast.LENGTH_SHORT).show();
                });
                break;
            }
        }

        if (displayedMembers.size() == familyMembers.size()) {
            Toast.makeText(getContext(), "모든 멤버가 추가되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isMemberDisplayedOrRemoved(String memberId) {
        for (FamilyMember displayedMember : displayedMembers) {
            if (displayedMember.getDocId().equals(memberId)) {
                return true;
            }
        }
        return false;
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
        memberId3=memberId;
        Family_drug_info familyDrugInfoFragment = Family_drug_info.newInstance(memberId);
        Bundle result=new Bundle();
        result.putString("memberId", memberId);
        getParentFragmentManager().setFragmentResult("request", result);
        Log.d("Firestoreblabla", "meesageId Send");

        db.collection("FamilyMember").document(memberId3)
                .get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // "uid" 필드 값 가져오기
                        uid = documentSnapshot.getString("uid");
                        if (uid != null) {
                            Log.d("Firestore", "UID 값: " + uid);
                        } else {
                            Log.d("Firestore", "UID 필드가 존재하지 않습니다.");
                        }
                    } else {
                        Log.d("Firestore", "문서가 존재하지 않습니다.");
                    }
                }).addOnFailureListener(e -> {
                    Log.w("Firestore", "데이터 가져오기 실패", e);
                });

//        String uid2=uid;

//        String bUserId = "VL3AQ8338XY7AQA7AzzkQD38Iyz2"; // b 사용자의 UID
//        String bUserId = memberId;

//        String messageText = "아직 먹지 않은 약이 있습니다!";
//        db.collection("FamilyMember").document("users")
//                .collection("users").document(uid2)
//                .get()
//                .addOnSuccessListener(documentSnapshot -> {
//                    if (documentSnapshot.exists()) {
//                        // deviceToken 필드 가져오기
//                        String deviceToken = documentSnapshot.getString("deviceToken");
//                        if (deviceToken != null) {
//                            // FCM을 사용해 알림 전송
//                            Log.d("Firestore", deviceToken);
////                            sendNotification(deviceToken);
//                        } else {
//                            Log.w("Firestore", "Device token is null for user: " + uid2);
//                        }
//                    } else {
//                        Log.w("Firestore", "Document does not exist for user: " + uid2);
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.w("Firestore", "Error fetching device token", e);
//                });

        db.collection("FamilyMember").document(memberId3)
                .get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        uid = documentSnapshot.getString("uid");
                        if (uid != null) {
                            Log.d("Firestore", "UID 값: " + uid);

                            // Firestore 결과가 도착한 후 uid2에 값을 할당하고 사용
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
                                    .addOnFailureListener(e -> {
                                        Log.w("Firestore", "Error fetching device token", e);
                                    });

                        } else {
                            Log.d("Firestore", "UID 필드가 존재하지 않습니다.");
                        }
                    } else {
                        Log.d("Firestore", "문서가 존재하지 않습니다.");
                    }
                }).addOnFailureListener(e -> {
                    Log.w("Firestore", "데이터 가져오기 실패", e);
                });

        RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
        if (viewHolder == null) {
            return;
        }
//        View view = recyclerView.findViewHolderForAdapterPosition(position).itemView;
        String messageText = "아직 먹지 않은 약이 있습니다!";
        View view = viewHolder.itemView;

        String bubble="abc";
        LinearLayout drugInfo = view.findViewById(R.id.drug_info);
        ImageView familySendImageView = view.findViewById(R.id.family_send);
        Animation slideDown = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down);
        Animation slideUp = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);

        familySendImageView.setOnClickListener(v->{
            sendNotification(uid2, messageText);
            Toast.makeText(getContext(), "Family Send clicked", Toast.LENGTH_SHORT).show();
        });

        if (drugInfo.getVisibility() == View.GONE) {
            fetchDrugInfo(view);
            checkPillStatusAndUpdateUI(view, bubble);
            drugInfo.startAnimation(slideDown);
            drugInfo.setVisibility(View.VISIBLE);
        } else {
            drugInfo.startAnimation(slideUp);
            drugInfo.setVisibility(View.GONE);
        }
    }

    private void fetchDrugInfo(View fragmentView) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Step 1: FamilyMember/Member1 문서에서 날짜 필드들을 가져오기
        db.collection("FamilyMember")
                .document(memberId3)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
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
                            checkPillStatusAndUpdateUI(fragmentView, String.valueOf(latestDate));
                        } else {
                            Log.d("Firestore", "No date collections found.");
                        }
                    } else {
                        Log.d("Firestore", "Member1 document doesn't exist.");
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to fetch Member1 document", e));
    }

    private void checkPillStatusAndUpdateUI(View itemView, String latestDate) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d("latest date is:", latestDate);
        db.collection("FamilyMember")
                .document(memberId3)
                .collection(latestDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean allChecked = true; // 모든 pillchecked가 1인지 확인

                    // 각 문서를 순회하면서 pillchecked 값 확인
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Long pillChecked = doc.getLong("pillIsChecked");

                        // pillChecked가 null이거나 1이 아니면 allChecked를 false로 설정
                        if (pillChecked == null || !pillChecked.equals(1L)) {
                            allChecked = false;
                            break;
                        }
                    }
                    // 이미지 업데이트
                    ImageView memberPillCheck = itemView.findViewById(R.id.member_pillcheck);
                    if (allChecked) {
                        memberPillCheck.setImageResource(R.drawable.pill_checked);
                    } else {
                        memberPillCheck.setImageResource(R.drawable.pill_check);
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore Error", "Failed to fetch Drug_Info data", e));
    }


    // 멤버 레이아웃 삭제 처리
    private void removeMember(int position) {
        FamilyMember removedMember = displayedMembers.get(position);
        removedMemberIds.add(removedMember.getDocId());
        displayedMembers.remove(position);
        adapter.notifyItemRemoved(position);
    }

    private void familydruginfo() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        Fragment currentFragment = fragmentManager.findFragmentById(R.id.recycler_frame);
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }

        transaction.replace(R.id.recycler_frame, new Family_drug_info());
        transaction.addToBackStack(null); // 뒤로 가기 지원
        transaction.commit();

        RecyclerView recyclerView = requireView().findViewById(R.id.family_content);
        recyclerView.setVisibility(View.GONE);
    }

    public interface NotificationService {
        @POST("/send-notification")
        Call<Void> sendNotification(@Body NotificationRequest request);
    }

    public class NotificationRequest {
        private String receiverId;
        private String messageText;

        // 생성자
        public NotificationRequest(String receiverId, String messageText) {
            this.receiverId = receiverId;
            this.messageText = messageText;
        }

        // Getter/Setter
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
                    .baseUrl("http://192.168.11.11:5000") // 서버 주소
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}