package com.example.project1;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FamilyLogin#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FamilyLogin extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private FirebaseAuth mAuth;

    public FamilyLogin() {
        // Required empty public constructor
    }

    public static FamilyLogin newInstance(String param1, String param2) {
        FamilyLogin fragment = new FamilyLogin();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        View view = inflater.inflate(R.layout.fragment_family_login, container, false);

        EditText emailEditText = view.findViewById(R.id.emailEditText);
        EditText passwordEditText = view.findViewById(R.id.passwordEditText);
        Button signUpButton = view.findViewById(R.id.signUpButton);
        Button loginButton = view.findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getActivity(), "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        signUpButton.setOnClickListener(v -> {
            showSignUpFragment();
        });

        return view;
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getActivity(), "로그인 성공", Toast.LENGTH_SHORT).show();
                        saveDeviceToken(); // 로그인 후 deviceToken 저장

                        // 여기서 Nav_main → MainActivity 로 변경
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        startActivity(intent);
                        requireActivity().finish();

                    } else {
                        Toast.makeText(getActivity(), "로그인 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveDeviceToken() {
        Log.d("FCM saving", "SavedDeviceToken called");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        Log.d("FirestoreuserId", String.valueOf(userId));
        if (userId == null) {
            Log.w("Firestore", "User is not logged in.");
            return;
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String deviceToken = task.getResult();

                    Map<String, Object> data = new HashMap<>();
                    data.put("deviceToken", deviceToken);

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

    private void showSignUpFragment() {
        FamilySignup signUpFragment = new FamilySignup();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();

        transaction.replace(android.R.id.content, signUpFragment)
                .addToBackStack(null)
                .commit();
    }
}
