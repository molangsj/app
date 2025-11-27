// FamilySignup.java

package com.example.project1;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class FamilySignup2 extends Fragment {

    private EditText etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp, btnGoToLogin;
    private AuthHelper authHelper;

    public interface OnSignUpSuccessListener {
        void onSignUpSuccess();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_family_signup2, container, false);

        etEmail = view.findViewById(R.id.etEmailSignUp);
        etPassword = view.findViewById(R.id.etPasswordSignUp);
        etConfirmPassword = view.findViewById(R.id.etConfirmPasswordSignUp);
        btnSignUp = view.findViewById(R.id.btnSignUp);
        btnGoToLogin = view.findViewById(R.id.btnGoToLogin);

        authHelper = new AuthHelper(requireContext());

        btnSignUp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (!validateInput(email, password, confirmPassword)) {
                return;
            }

            btnSignUp.setEnabled(false);
            // username 없이 회원가입
            authHelper.signUpWithoutUsername(email, password, new AuthHelper.AuthCallback() {
                @Override
                public void onAuthSuccess(FirebaseUser user) {
                    btnSignUp.setEnabled(true);
                    saveDeviceToken();
                    Toast.makeText(getActivity(), "회원가입 성공! username을 설정해주세요.", Toast.LENGTH_SHORT).show();

                    // UID 문서 생성(이미 AuthHelper 내부 로직에서 addNewUserWithoutUsername 호출했을 것)
                    // 이제 SetUsernameFragment로 이동하여 username 입력받기
                    SetUsernameFragment fragment = SetUsernameFragment.newInstance(email, user.getUid());
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.test_fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                }


                @Override
                public void onAuthFailed(Exception e) {
                    btnSignUp.setEnabled(true);
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(getActivity(), "이미 사용 중인 이메일입니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "회원가입 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        btnGoToLogin.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.test_fragment_container, new FamilyLogin2())
                    .addToBackStack(null)
                    .commit();
        });


        btnGoToLogin.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.test_fragment_container, new FamilyLogin())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }


    private boolean validateInput(String email, String password, String confirmPassword) {
        if (email.isEmpty()) {
            etEmail.setError("이메일을 입력하세요");
            etEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("유효한 이메일을 입력하세요");
            etEmail.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            etPassword.setError("비밀번호를 입력하세요");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("비밀번호는 최소 6자 이상이어야 합니다");
            etPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("비밀번호가 일치하지 않습니다");
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void saveDeviceToken() {
        Log.d("FCM saving", "SavedDeviceToken called");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        // 현재 로그인한 사용자의 UID 가져오기
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Log.w("Firestore", "User is not logged in.");
            return;
        }

        // FCM 토큰 가져오기
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // FCM 토큰
                    String deviceToken = task.getResult();

                    // Firestore에 저장
                    Map<String, Object> data = new HashMap<>();
                    data.put("deviceToken", deviceToken);

                    // 문서가 없으면 생성, 있으면 업데이트
                    db.collection("FamilyMember").document("users")
                            .collection("users").document(userId)
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Firestore", deviceToken);
                                Log.d("Firestore", "FCM Token saved successfully");
                            })
                            .addOnFailureListener(e -> {
                                Log.w("Firestore", "Error saving token", e);
                            });
                });
    }

}
