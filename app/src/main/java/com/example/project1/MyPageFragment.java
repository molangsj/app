package com.example.project1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.app.AlertDialog;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class MyPageFragment extends Fragment {

    private static final String ARG_USERNAME = "username";
    private static final String ARG_EMAIL = "email";
    private static final int PICK_IMAGE_REQUEST = 100;

    private String username;
    private String email;


    private TextView textUsername;
    private TextView textEmail;
    private ImageView imageEditUsername;
    private ImageView imageProfile;
    private Switch switchDarkMode;
    private Button buttonLogout;
    private TextView textMedicationCount;
    private TextView textTodayAlarms;
    private Spinner fontSpinner;

    private FirestoreHelper firestoreHelper;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public static MyPageFragment newInstance(String username, String email) {
        MyPageFragment fragment = new MyPageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    public MyPageFragment() { }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestoreHelper = new FirestoreHelper();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            username = getArguments().getString(ARG_USERNAME);
            email = getArguments().getString(ARG_EMAIL);
        }
    }

    private void showGlobalLoading(boolean show) {
        if (!isAdded()) return;
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showLoading(show);
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_my_page, container, false);

        textUsername = view.findViewById(R.id.text_username);
        textEmail = view.findViewById(R.id.text_email);
        imageEditUsername = view.findViewById(R.id.image_edit_username);
        imageProfile = view.findViewById(R.id.image_profile);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        buttonLogout = view.findViewById(R.id.button_logout);
        textMedicationCount = view.findViewById(R.id.text_medication_count);
        textTodayAlarms = view.findViewById(R.id.text_today_alarms);
        fontSpinner = view.findViewById(R.id.font_spinner);

        textUsername.setText(username != null ? username : "-");
        textEmail.setText(email != null ? email : "-");

        loadUserInfoFromFirestore();
        loadMedicationStats();

        imageEditUsername.setOnClickListener(v -> showEditNicknameDialog());

        initDarkModeToggle();
        initFontSelector();

        buttonLogout.setOnClickListener(v -> {
            AuthHelper authHelper = new AuthHelper(requireContext());
            authHelper.logout(requireContext());

            Intent intent = new Intent(requireActivity(), ActivityFortest.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        imageProfile.setOnClickListener(v -> openImagePicker());

        return view;
    }

    private void loadUserInfoFromFirestore() {
        // username이 이미 있다면 바로 username으로 조회
        if (username != null && !username.isEmpty()) {
            showGlobalLoading(true);

            db.collection("FamilyMember")
                    .document(username)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        showGlobalLoading(false);

                        if (documentSnapshot.exists()) {
                            // displayName 로드
                            Object displayNameObj = documentSnapshot.get("displayName");
                            if (displayNameObj != null) {
                                String displayName = displayNameObj.toString();
                                if (!displayName.trim().isEmpty()) {
                                    textUsername.setText(displayName);
                                }
                            }

                            // 프로필 이미지 로드
                            Object profileImageUrlObj = documentSnapshot.get("profileImageUrl");
                            if (profileImageUrlObj != null) {
                                String profileImageUrl = profileImageUrlObj.toString();
                                if (!profileImageUrl.trim().isEmpty()) {
                                    loadProfileImage(profileImageUrl);
                                }
                            }
                        } else {
                            Log.e("MyPageFragment", "User document not found for username: " + username);
                        }
                    })
                    .addOnFailureListener(e -> {
                        showGlobalLoading(false);
                        Log.e("MyPageFragment", "Failed to load user info", e);
                    });
        }
    }

    private void loadMedicationStats() {
        if (username == null || username.isEmpty()) {
            return;
        }

        db.collection("FamilyMember")
                .document(username)
                .collection("currentMedications")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        int medicationCount = queryDocumentSnapshots.size();
                        int totalAlarmsToday = 0;

                        // 각 약의 알림 횟수 계산
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            // alarmTimes 배열 가져오기
                            Object alarmTimesObj = doc.get("alarmTimes");
                            if (alarmTimesObj instanceof java.util.List) {
                                java.util.List<?> alarmTimes = (java.util.List<?>) alarmTimesObj;
                                totalAlarmsToday += alarmTimes.size();
                            }
                        }

                        // UI 업데이트
                        textMedicationCount.setText("등록된 약: " + medicationCount + "개");
                        textTodayAlarms.setText("오늘 알림 예정: " + totalAlarmsToday + "회");
                    } else {
                        textMedicationCount.setText("등록된 약: 0개");
                        textTodayAlarms.setText("오늘 알림 예정: 0회");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MyPageFragment", "Failed to load medication stats", e);
                    textMedicationCount.setText("등록된 약: -");
                    textTodayAlarms.setText("오늘 알림 예정: -");
                });
    }

    private void loadProfileImage(@Nullable String imageUrl) {
        if (!isAdded()) return;

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            imageProfile.setBackground(null);
            imageProfile.setPadding(0, 0, 0, 0);
            imageProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);

            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .circleCrop()
                    .into(imageProfile);
        } else {
            imageProfile.setBackground(
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_profile_circle)
            );
            imageProfile.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageProfile.setImageResource(R.drawable.baseline_person_24);

            // dp 8을 px로 변환해서 패딩 다시 줌
            int padding = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    8,
                    getResources().getDisplayMetrics()
            );
            imageProfile.setPadding(padding, padding, padding, padding);
        }
    }


    private void openImagePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            Log.e("MyPageFragment", "Error opening image picker", e);
            Toast.makeText(getContext(), "이미지 선택기를 열 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                uploadImageToFirebase(imageUri);
            }
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (username == null || username.isEmpty()) {
            Toast.makeText(getContext(), "사용자 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        showGlobalLoading(true);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showGlobalLoading(false);
            Toast.makeText(getContext(), "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        // Firebase Storage 참조 생성 (UID 사용 - 고유해야 하니까)
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference profileImageRef = storageRef.child("profile_images/" + uid + ".jpg");

        // 이미지 업로드
        profileImageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // 업로드 성공 - 다운로드 URL 가져오기
                    profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        Log.d("MyPageFragment", "Image uploaded successfully: " + downloadUrl);

                        // Firestore에 URL 저장 (username을 문서 ID로 사용)
                        saveProfileImageUrlToFirestore(downloadUrl);
                    }).addOnFailureListener(e -> {
                        showGlobalLoading(false);
                        Toast.makeText(getContext(), "다운로드 URL 가져오기 실패", Toast.LENGTH_SHORT).show();
                        Log.e("MyPageFragment", "Failed to get download URL", e);
                    });
                })
                .addOnFailureListener(e -> {
                    showGlobalLoading(false);
                    Toast.makeText(getContext(), "이미지 업로드 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e("MyPageFragment", "Failed to upload image", e);
                });
    }

    private void saveProfileImageUrlToFirestore(String imageUrl) {
        // ⭐ username(닉네임)을 문서 ID로 사용
        Map<String, Object> data = new HashMap<>();
        data.put("profileImageUrl", imageUrl);

        db.collection("FamilyMember")
                .document(username)  // username을 문서 ID로 사용!
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    showGlobalLoading(false);
                    Toast.makeText(getContext(), "프로필 이미지가 업데이트되었습니다.",
                            Toast.LENGTH_SHORT).show();

                    // UI 업데이트
                    loadProfileImage(imageUrl);
                })
                .addOnFailureListener(e -> {
                    showGlobalLoading(false);
                    Toast.makeText(getContext(), "이미지 저장 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e("MyPageFragment", "Failed to save image URL to Firestore", e);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 이 프래그먼트 떠날 때는 무조건 로딩 끄기
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showLoading(false);
        }
    }


    private void showEditNicknameDialog() {
        if (username == null || username.isEmpty()) {
            Toast.makeText(requireContext(), "사용자 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText editText = new EditText(requireContext());
        editText.setSingleLine(true);
        editText.setText(textUsername.getText().toString());

        new AlertDialog.Builder(requireContext())
                .setTitle("닉네임 수정")
                .setMessage("닉네임을 수정하시겠습니까?")
                .setView(editText)
                .setNegativeButton("취소", null)
                .setPositiveButton("확인", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(requireContext(), "닉네임을 비워둘 수 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateNickname(newName);
                })
                .show();
    }

    private void updateNickname(String newName) {
        showGlobalLoading(true);

        Map<String, Object> data = new HashMap<>();
        data.put("displayName", newName);

        db.collection("FamilyMember")
                .document(username)  // username을 문서 ID로 사용!
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    showGlobalLoading(false);
                    textUsername.setText(newName);
                    Toast.makeText(requireContext(), "닉네임이 변경되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    showGlobalLoading(false);
                    Toast.makeText(requireContext(), "닉네임 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    Log.e("MyPageFragment", "Failed to update nickname", e);
                });
    }

    private void initDarkModeToggle() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(isDark);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            requireActivity().recreate();
        });
    }


    private void initFontSelector() {
        // ⭐ 커스텀 어댑터 사용
        String[] fontOptions = getResources().getStringArray(R.array.font_options);
        FontSpinnerAdapter adapter = new FontSpinnerAdapter(requireContext(), fontOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fontSpinner.setAdapter(adapter);

        // 저장된 폰트 불러오기
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        int savedFont = prefs.getInt("selected_font", 0);
        fontSpinner.setSelection(savedFont);

        // 폰트 선택 리스너
        fontSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean isInitialLoad = true; // 첫 로드 시 무시용

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 첫 로드 시에는 무시 (이미 적용된 폰트)
                if (isInitialLoad) {
                    isInitialLoad = false;
                    return;
                }

                int currentFont = prefs.getInt("selected_font", 0);

                // 실제로 폰트가 변경되었을 때만 적용
                if (currentFont != position) {
                    // 폰트 저장
                    prefs.edit().putInt("selected_font", position).apply();

                    // Activity 재생성 (모든 Fragment에 폰트 적용됨)
                    requireActivity().recreate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
}