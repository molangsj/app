// FamilyLogin.java

package com.example.project1;

import android.app.Activity;
import android.content.Intent;
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

import com.google.android.gms.common.SignInButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class FamilyLogin2 extends Fragment implements AuthHelper.GoogleSignInCallback, AuthHelper.AuthCallback {

    private static final int RC_SIGN_IN = 9001; // Google Sign-In 요청 코드

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoToSignUp;
    private SignInButton btnGoogleSignIn;
    private AuthHelper authHelper;
    private OnLoginSuccessListener listener;

    public interface OnLoginSuccessListener {
        void onLoginSuccess(String userId);
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof OnLoginSuccessListener) {
            listener = (OnLoginSuccessListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnLoginSuccessListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_family_login2, container, false);

        etEmail = view.findViewById(R.id.emailEditText);
        etPassword = view.findViewById(R.id.passwordEditText);
        btnLogin = view.findViewById(R.id.loginButton);
        btnGoToSignUp = view.findViewById(R.id.signUpButton);
        btnGoogleSignIn = view.findViewById(R.id.btnGoogleSignIn);

        authHelper = new AuthHelper(requireActivity());

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (!validateInput(email, password)) {
                return;
            }

            btnLogin.setEnabled(false);
            authHelper.login(email, password, this);
        });

        btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE);
        btnGoogleSignIn.setOnClickListener(v -> {
            Activity activity = getActivity();
            if (activity != null) {
                authHelper.signInWithGoogle(activity, RC_SIGN_IN, this);
            }
        });

        btnGoToSignUp.setOnClickListener(v -> {
            // FamilySignup 프래그먼트로 교체
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.test_fragment_container, new FamilySignup2())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private boolean validateInput(String email, String password) {
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

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            authHelper.handleSignInResult(data, this);
        }
    }

    @Override
    public void onAuthSuccess(FirebaseUser user) {
        btnLogin.setEnabled(true);

        // username이 설정되었는지 확인
        FirestoreHelper firestoreHelper = new FirestoreHelper();
        firestoreHelper.getUserDocumentByUid(user.getUid(), new FirestoreHelper.UserDataCallback() {
            @Override
            public void onUserDataReceived(Map<String, Object> data) {
                String username = (String) data.get("username");

                if (username == null || username.isEmpty()) {
                    // username이 없으면 SetUsernameFragment로 이동
                    Toast.makeText(getActivity(), "사용자 이름을 설정해주세요", Toast.LENGTH_SHORT).show();
                    SetUsernameFragment fragment = SetUsernameFragment.newInstance(user.getEmail(), user.getUid());
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.test_fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                } else {
                    // username이 있으면 정상 로그인 진행
                    saveDeviceToken();
                    Toast.makeText(getActivity(), "로그인 성공!", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onLoginSuccess(user.getUid());
                    }
                }
            }

            @Override
            public void onUserDataNotFound() {
                // 사용자 데이터가 없으면 SetUsernameFragment로 이동
                Toast.makeText(getActivity(), "사용자 이름을 설정해주세요", Toast.LENGTH_SHORT).show();
                SetUsernameFragment fragment = SetUsernameFragment.newInstance(user.getEmail(), user.getUid());
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.test_fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onUserDataFailed(Exception e) {
                Toast.makeText(getActivity(), "사용자 정보 확인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAuthFailed(Exception e) {
        btnLogin.setEnabled(true);

        if (e instanceof FirebaseAuthInvalidUserException) {
            Toast.makeText(getActivity(), "해당 계정이 없습니다. 회원가입 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.test_fragment_container, new FamilySignup())
                    .addToBackStack(null)
                    .commit();
        } else {
            Toast.makeText(getActivity(), "로그인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onGoogleSignInSuccess(FirebaseUser user, String username) {
        Toast.makeText(getActivity(), "Google 로그인 성공!", Toast.LENGTH_SHORT).show();
        if (listener != null) {
            listener.onLoginSuccess(user.getUid());
        }
    }

    @Override
    public void onGoogleSignInFailed(Exception e) {
        Toast.makeText(getActivity(), "Google 로그인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNeedUsername(FirebaseUser user) {
        FirestoreHelper firestoreHelper = new FirestoreHelper();

        // UID 문서가 없으면 생성하는 메서드 호출
        firestoreHelper.checkAndCreateUserDocument(user, new FirestoreHelper.StatusCallback() {
            @Override
            public void onStatusUpdated() {
                // UID 문서가 생성 완료되면 이제 SetUsernameFragment로 이동 가능
                SetUsernameFragment fragment = SetUsernameFragment.newInstance(user.getEmail(), user.getUid());
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.test_fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onStatusUpdateFailed(Exception e) {
                Toast.makeText(getActivity(), "UID 문서 생성 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
