package com.example.project1;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseUser;

import org.checkerframework.common.subtyping.qual.Bottom;

public class Nav_main extends AppCompatActivity {
    private AuthHelper authHelper;
    private FirestoreHelper firestoreHelper;
    private String familyMemberId;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1;
    private static final int REQUEST_WRITE_STORAGE = 112; // 권한 요청 코드
    private static final String CHANNEL_ID = "medicine_alarm";
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_nav_main);
//        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
//
//        if (savedInstanceState == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.fragment_container, new BlankFragment1())
//                    .commit();
//        }
//        authHelper = new AuthHelper(this);
//        firestoreHelper = new FirestoreHelper();
//
//        // 현재 사용자 확인
//        FirebaseUser currentUser = authHelper.getCurrentUser();
//        if (currentUser == null) {
//            // 로그인되어 있지 않으면 AuthActivity로 이동
//            Intent intent = new Intent(this, ActivityFortest.class);
//            startActivity(intent);
//            finish();
//            return;
//        }
//
//        familyMemberId = currentUser.getUid();
//
//        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
//            @Override
//            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
//                int itemId= item.getItemId();
//                if (itemId == R.id.page_1) {
//                    // MedicineList 프래그먼트에 familyMemberId 전달
//                    MedicineList medicineListFragment = new MedicineList();
//                    Bundle args = new Bundle();
//                    args.putString("familyMemberId", familyMemberId);
//                    args.putString("memberId", familyMemberId); // memberId도 familyMemberId로 설정
//                    medicineListFragment.setArguments(args);
//                    transferTo(medicineListFragment);
//                    return true;
//                }
//                if(itemId == R.id.page_2){
//                    transferTo(BlankFragment1.newInstance("param1", "param2"));
//                    return true;
//                }
//                if(itemId == R.id.page_3){
//                    transferTo(BlankFragment1.newInstance("param1", "param2"));
//                    return true;
//                }
//                if(itemId == R.id.page_4){
//                    // CalendarFragment에 familyMemberId 전달
//                    CalendarFragment calendarFragment = CalendarFragment.newInstance(familyMemberId);
//                    transferTo(calendarFragment);
//                    return true;
//                }
//
//                if(itemId == R.id.page_5){
//                    transferTo(Family_main_sub.newInstance("param1", "param2"));
//                    return true;
//                }
//                return false;
//            }
//        });
//
//        bottomNavigationView.setOnItemReselectedListener(new NavigationBarView.OnItemReselectedListener() {
//            @Override
//            public void onNavigationItemReselected(@NonNull MenuItem item) {
//
//            }
//        });
//        if (savedInstanceState == null) {
//            MedicineList medicineListFragment = new MedicineList();
//            Bundle args = new Bundle();
//            args.putString("familyMemberId", familyMemberId);
//            args.putString("memberId", familyMemberId); // memberId도 familyMemberId로 설정
//            medicineListFragment.setArguments(args);
//            transferTo(medicineListFragment);
//        }
//    }
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_nav_main);
    BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
    authHelper = new AuthHelper(this);
    firestoreHelper = new FirestoreHelper();

    // 현재 사용자 확인
    FirebaseUser currentUser = authHelper.getCurrentUser();
    if (currentUser == null) {
        // 로그인되어 있지 않으면 AuthActivity로 이동
        Intent intent = new Intent(this, com.example.project1.ActivityFortest.class);
        startActivity(intent);
        finish();
        return;
    }

    familyMemberId = currentUser.getUid();

    if (savedInstanceState == null) {
        // 초기 화면으로 MedicineList 설정
        MedicineList medicineListFragment = new MedicineList();
        Bundle args = new Bundle();
        args.putString("familyMemberId", familyMemberId);
        args.putString("memberId", familyMemberId); // memberId도 familyMemberId로 설정
        medicineListFragment.setArguments(args);
        showFragment(medicineListFragment, "MedicineList");

        // 초기 선택 항목 설정
        bottomNavigationView.setSelectedItemId(R.id.page_1);
    }

    bottomNavigationView.setOnItemSelectedListener(item -> {
        Fragment fragment = null;
        String tag = null;

        int itemId = item.getItemId();
        if (itemId == R.id.page_1) {
            tag = "MedicineList";
            fragment = getSupportFragmentManager().findFragmentByTag(tag);
            if (fragment == null) {
                MedicineList medicineListFragment = new MedicineList();
                Bundle args = new Bundle();
                args.putString("familyMemberId", familyMemberId);
                args.putString("memberId", familyMemberId); // memberId도 familyMemberId로 설정
                medicineListFragment.setArguments(args);
                fragment = medicineListFragment;
            }
        } else if (itemId == R.id.page_2) {
            tag = "HistoryFragment";
            fragment = getSupportFragmentManager().findFragmentByTag(tag);
            if (fragment == null) {
                fragment = HistoryFragment.newInstance("param1", "param2");
            }
        }
        else if (itemId == R.id.page_4) {
            tag = "CalendarFragment";
            fragment = getSupportFragmentManager().findFragmentByTag(tag);
            if (fragment == null) {
                fragment = CalendarFragment.newInstance(familyMemberId);
            }
        } else if (itemId == R.id.page_5) {
            tag = "Family_main_sub";
            fragment = getSupportFragmentManager().findFragmentByTag(tag);
            if (fragment == null) {
                fragment = Family_main_sub.newInstance("param1", "param2");
            }
        }

        if (fragment != null && tag != null) {
            showFragment(fragment, tag);
            return true;
        }
        return false;
    });

    bottomNavigationView.setOnItemReselectedListener(item -> {
        // 메뉴가 다시 선택되었을 때의 동작(필요하면 구현)
    });
}

    private void showFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // 모든 프래그먼트를 숨김
        for (Fragment frag : getSupportFragmentManager().getFragments()) {
            transaction.hide(frag);
        }

        // 이미 추가된 프래그먼트면 보여주고, 아니면 추가
        if (fragment.isAdded()) {
            transaction.show(fragment);
        } else {
            transaction.add(R.id.fragment_container, fragment, tag);
        }

        transaction.commit();
    }
}

//    private void transferTo2(Fragment fragment) {
//        getSupportFragmentManager().beginTransaction()
//                .replace(R.id.fragment_container, fragment)
//                .commit();
//    }
//
//    private void transferTo(Fragment fragment){
//        getSupportFragmentManager().beginTransaction()
//                .replace(R.id.fragment_container, fragment)
//                .commit();
//    }
//}