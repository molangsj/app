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
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.app.AlertDialog;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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

        textUsername.setText(username != null ? username : "-");
        textEmail.setText(email != null ? email : "-");

        loadUserInfoFromFirestore();

        imageEditUsername.setOnClickListener(v -> showEditNicknameDialog());

        initDarkModeToggle();

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

    private void loadProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty() && isAdded()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.baseline_person_24)
                    .circleCrop()
                    .into(imageProfile);
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
}