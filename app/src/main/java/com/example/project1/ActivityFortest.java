package com.example.project1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.project1.FamilyLogin;
import com.example.project1.FamilyLogin2;
import com.example.project1.FamilySignup2;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

public class ActivityFortest extends AppCompatActivity implements
        FamilyLogin2.OnLoginSuccessListener,
        FamilySignup2.OnSignUpSuccessListener,
        SetUsernameFragment.OnUsernameSetListener {

    private FirestoreHelper firestoreHelper;
    private AuthHelper authHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_for_test);

        firestoreHelper = new FirestoreHelper();
        authHelper = new AuthHelper(this);

        // 초기 프래그먼트 설정: FamilyLogin
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.test_fragment_container, new FamilyLogin2())
                    .commit();
        }
    }

    @Override
    public void onLoginSuccess(String userId) {
        // userId는 UID
        firestoreHelper.getUserDataByUID(userId, new FirestoreHelper.UserDataCallback() {
            @Override
            public void onUserDataReceived(Map<String, Object> data) {
                if (data.containsKey("username") && data.get("username") != null && !data.get("username").equals("")) {
                    // username이 있으면 MedicineList로 이동
                    navigateToMainActivity();
                } else {
                    // username이 없으면 SetUsernameFragment로 이동
                    navigateToSetUsernameFragment(userId);
                }
            }

            @Override
            public void onUserDataNotFound() {
                // 문서가 없으면 username 설정 필요
                navigateToSetUsernameFragment(userId);
            }

            @Override
            public void onUserDataFailed(Exception e) {
                Toast.makeText(ActivityFortest.this, "사용자 데이터 불러오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(ActivityFortest.this, MainActivity.class);
        startActivity(intent);
        finish(); // ActivityFortest 종료
    }

    private void navigateToMedicineList(String username) {
        MedicineList fragment = MedicineList.newInstance(username);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.test_fragment_container, fragment)
                .commit();
    }

    private void navigateToSetUsernameFragment(String userId) {
        // currentUser로부터 email 가져옴
        FirebaseUser currentUser = authHelper.getCurrentUser();
        String email = (currentUser != null) ? currentUser.getEmail() : "";
        SetUsernameFragment fragment = SetUsernameFragment.newInstance(email, userId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.test_fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onSignUpSuccess() {
        // 회원가입 성공 시 이미 username=""으로 문서 생성 후 SetUsernameFragment 이동 필요
        FirebaseUser currentUser = authHelper.getCurrentUser();
        if (currentUser != null) {
            SetUsernameFragment fragment = SetUsernameFragment.newInstance(currentUser.getEmail(), currentUser.getUid());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.test_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            Toast.makeText(this, "회원가입 후 사용자 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUsernameSet(String username) {
        // username 설정 완료 후 MedicineList로 이동
//        navigateToMedicineList(username);
        Intent intent = new Intent(ActivityFortest.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.test_fragment_container);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }
}
