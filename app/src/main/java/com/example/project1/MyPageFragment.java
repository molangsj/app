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

    private String username;
    private String email;

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

    // 전역 로딩 헬퍼
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

        imageProfile.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "프로필 사진 변경 기능은 추후 추가 가능합니다.", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void loadUserInfoFromFirestore() {
        if (username == null || username.isEmpty()) return;

        showGlobalLoading(true);

        firestoreHelper.getUserDataByUsername(username, new FirestoreHelper.UserDataCallback() {
            @Override
            public void onUserDataReceived(java.util.Map<String, Object> data) {
                showGlobalLoading(false);

                Object displayNameObj = data.get("displayName");
                if (displayNameObj != null) {
                    String displayName = displayNameObj.toString();
                    if (!displayName.trim().isEmpty()) {
                        textUsername.setText(displayName);
                    }
                }
            }

            @Override
            public void onUserDataNotFound() {
                showGlobalLoading(false);
            }

            @Override
            public void onUserDataFailed(Exception e) {
                showGlobalLoading(false);
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
        showGlobalLoading(true);

        firestoreHelper.updateDisplayName(username, newName, new FirestoreHelper.StatusCallback() {
            @Override
            public void onStatusUpdated() {
                showGlobalLoading(false);
                textUsername.setText(newName);
                Toast.makeText(requireContext(), "닉네임이 변경되었습니다.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStatusUpdateFailed(Exception e) {
                showGlobalLoading(false);
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
