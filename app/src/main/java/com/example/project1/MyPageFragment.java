package com.example.project1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

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

public class MyPageFragment extends Fragment {

    private static final String ARG_USERNAME = "username";
    private static final String ARG_EMAIL = "email";

    private String username;   // 문서 ID (로그인 시 받은 username)
    private String email;      // 로그인 이메일

    private TextView textUsername;
    private TextView textEmail;
    private ImageView imageEditUsername;
    private ImageView imageProfile;
    private Switch switchDarkMode;
    private Button buttonLogout;

    private FirestoreHelper firestoreHelper;

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

        if (getArguments() != null) {
            username = getArguments().getString(ARG_USERNAME);
            email = getArguments().getString(ARG_EMAIL);
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

        // 초기 표시
        textUsername.setText(username != null ? username : "-");
        textEmail.setText(email != null ? email : "-");

        // Firestore에서 displayName / photoUrl 등을 가져와서 반영 (있으면)
        loadUserInfoFromFirestore();

        // 닉네임 수정 아이콘 클릭
        imageEditUsername.setOnClickListener(v -> showEditNicknameDialog());

        // 다크 모드 토글 초기 상태 설정 + 리스너
        initDarkModeToggle();

        // 로그아웃 버튼
        buttonLogout.setOnClickListener(v -> {
            AuthHelper authHelper = new AuthHelper(requireContext());
            authHelper.logout(requireContext());

            Intent intent = new Intent(requireActivity(), ActivityFortest.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        // 프로필 이미지 클릭 시 (나중에 사진 업로드 붙이고 싶으면 여기서 처리)
        imageProfile.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "프로필 사진 변경 기능은 추후 추가 가능합니다.", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void loadUserInfoFromFirestore() {
        if (username == null || username.isEmpty()) return;

        firestoreHelper.getUserDataByUsername(username, new FirestoreHelper.UserDataCallback() {
            @Override
            public void onUserDataReceived(java.util.Map<String, Object> data) {
                // displayName이 있으면 그걸 닉네임으로 보여주기
                Object displayNameObj = data.get("displayName");
                if (displayNameObj != null) {
                    String displayName = displayNameObj.toString();
                    if (!displayName.trim().isEmpty()) {
                        textUsername.setText(displayName);
                    }
                }
                // photoUrl이 있으면, Glide 같은 걸로 로딩 가능 (지금은 생략)
            }

            @Override
            public void onUserDataNotFound() {
                // 무시
            }

            @Override
            public void onUserDataFailed(Exception e) {
                // 무시 또는 로그
            }
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
        // 여기서는 username(문서 ID)은 바꾸지 않고, displayName 필드만 수정
        firestoreHelper.updateDisplayName(username, newName, new FirestoreHelper.StatusCallback() {
            @Override
            public void onStatusUpdated() {
                textUsername.setText(newName);
                Toast.makeText(requireContext(), "닉네임이 변경되었습니다.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStatusUpdateFailed(Exception e) {
                Toast.makeText(requireContext(), "닉네임 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
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
