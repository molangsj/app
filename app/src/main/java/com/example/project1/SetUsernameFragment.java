// app/src/main/java/com/example/project_yakkuk/SetUsernameFragment.java

package com.example.project1;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.google.firebase.auth.FirebaseUser;

public class SetUsernameFragment extends Fragment {

    private static final String TAG = "SetUsernameFragment";

    private EditText etUsername;
    private Button btnSetUsername;
    private AuthHelper authHelper;
    private FirestoreHelper firestoreHelper;
    private String email;
    private String uid;
    private OnUsernameSetListener usernameSetListener;

    public interface OnUsernameSetListener {
        void onUsernameSet(String username);
    }

    public static SetUsernameFragment newInstance(String email, String uid) {
        SetUsernameFragment fragment = new SetUsernameFragment();
        Bundle args = new Bundle();
        args.putString("email", email);
        args.putString("uid", uid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnUsernameSetListener) {
            usernameSetListener = (OnUsernameSetListener) context;
        } else if (getTargetFragment() instanceof OnUsernameSetListener) {
            usernameSetListener = (OnUsernameSetListener) getTargetFragment();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_set_username, container, false);

        etUsername = view.findViewById(R.id.etUsername);
        btnSetUsername = view.findViewById(R.id.btnSetUsername);

        authHelper = new AuthHelper(requireActivity());
        firestoreHelper = new FirestoreHelper();

        if (getArguments() != null) {
            email = getArguments().getString("email");
            uid = getArguments().getString("uid");
        }

        Log.d(TAG, "Email: " + email + ", UID: " + uid);

        if (email == null || uid == null) {
            Toast.makeText(getActivity(), "유저 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return view;
        }

        btnSetUsername.setOnClickListener(v -> {
            String usernameInput = etUsername.getText().toString().trim();
            if (TextUtils.isEmpty(usernameInput)) {
                etUsername.setError("사용자 이름을 입력하세요");
                etUsername.requestFocus();
                return;
            }

            Log.d(TAG, "Username 설정 시도: " + usernameInput);
            btnSetUsername.setEnabled(false);

            // username으로 문서 생성
            firestoreHelper.setUsername(usernameInput, email, uid, new FirestoreHelper.SetUsernameCallback() {
                @Override
                public void onSetUsernameSuccess() {
                    Log.d(TAG, "Username 설정 성공!");
                    Toast.makeText(getActivity(), "사용자 이름이 설정되었습니다.", Toast.LENGTH_SHORT).show();

                    if (usernameSetListener != null) {
                        usernameSetListener.onUsernameSet(usernameInput);
                    } else {
                        // Listener가 없으면 로그인 화면으로 이동
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.test_fragment_container, new FamilyLogin2())
                                .commit();
                    }
                }

                @Override
                public void onSetUsernameFailed(Exception e) {
                    Log.e(TAG, "Username 설정 실패", e);
                    btnSetUsername.setEnabled(true);
                    Toast.makeText(getActivity(), "사용자 이름 설정 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        return view;
    }
}