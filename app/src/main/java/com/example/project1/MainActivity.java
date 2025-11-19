// app/src/main/java/com/example/project_yakkuk/MainActivity.java

package com.example.project1;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.util.Calendar;
import android.view.View;


public class MainActivity extends AppCompatActivity implements
        FamilyLogin2.OnLoginSuccessListener,
        SetUsernameFragment.OnUsernameSetListener {

    private static final String TAG = "MainActivity";
    private AuthHelper authHelper;
    private FirestoreHelper firestoreHelper;
    private String username;
    private String userEmail;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1;
    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final String CHANNEL_ID = "medicine_alarm";
    private static final int REQUEST_CODE_SCHEDULE_EXACT_ALARM = 2;
    private String familyMemberId;
    private FirebaseUser curUser;

    private View globalLoadingOverlay;
    private int loadingCount = 0;

    private ActivityResultLauncher<Intent> exactAlarmPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);

        authHelper = new AuthHelper(this);
        firestoreHelper = new FirestoreHelper();

        // 정확한 알람 권한 요청 런처 초기화
        exactAlarmPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (getAlarmPermission()) {
                        Toast.makeText(this, "정확한 알람 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
                        // 권한이 허용되었으므로 테스트 알람 설정을 다시 시도할 수 있습니다.
                    } else {
                        Toast.makeText(this, "정확한 알람 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 현재 사용자 확인
        FirebaseUser currentUser = authHelper.getCurrentUser();

        if (currentUser == null) {
            // 로그인되어 있지 않으면 AuthActivity로 이동
            Intent intent = new Intent(MainActivity.this, ActivityFortest.class);
            startActivity(intent);
            finish();
            return;
        }



        String email = currentUser.getEmail();
        String uid = currentUser.getUid();

        if (email == null) {
            Toast.makeText(this, "이메일 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            authHelper.logout(this);
            Intent intent = new Intent(MainActivity.this, ActivityFortest.class);
            startActivity(intent);
            finish();
            return;
        }

        userEmail = email;

        setContentView(R.layout.activity_nav_main);

        // 전역 로딩 오버레이 뷰 연결
        globalLoadingOverlay = findViewById(R.id.global_loading_overlay);

        createNotificationChannel();
        requestNotificationPermission();
        handleRecentsDirectory();

        // Firestore에서 사용자 존재 여부 확인
        showLoading(true);

        firestoreHelper.checkUserExists(uid, new FirestoreHelper.CheckUserCallback() {
            @Override
            public void onUserExists(String fetchedUsername) {
                showLoading(false);

                username = fetchedUsername;
                setupBottomNavigation();
                // 초기 프래그먼트 설정
                if (savedInstanceState == null) {
                    MedicineList medicineListFragment = MedicineList.newInstance(username);
                    transferTo(medicineListFragment);
                }
            }

            @Override
            public void onUserDoesNotExist() {
                showLoading(false);

                Toast.makeText(MainActivity.this, "사용자 이름을 설정해주세요.", Toast.LENGTH_SHORT).show();
                navigateToSetUsernameFragment(email, uid);
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);

                Toast.makeText(MainActivity.this, "사용자 확인 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "Error checking user existence", e);
            }
        });

    }

    /**
     * 정확한 알람 권한이 있는지 확인합니다.
     */
    private boolean getAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        return true; // Android 12 미만은 권한이 필요 없음
    }

    /**
     * 사용자를 알람 권한 설정 페이지로 안내합니다.
     */
    private void requestExactAlarm_permission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("정확한 알람 권한 필요")
                .setMessage("정확한 알람을 사용하기 위해 권한을 허용해야 합니다. 설정으로 이동하시겠습니까?")
                .setPositiveButton("설정으로 이동", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    exactAlarmPermissionLauncher.launch(intent);
                })
                .setNegativeButton("취소", (dialog, which) -> {
                    Toast.makeText(this, "알람 권한이 없어 정확한 알람을 설정할 수 없습니다.", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false) // 사용자가 대화 상자를 닫을 수 없도록 설정
                .show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SCHEDULE_EXACT_ALARM) {
            if (getAlarmPermission()) {
                Toast.makeText(this, "정확한 알람 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "정확한 알람 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupBottomNavigation() {
        authHelper = new AuthHelper(this);
        firestoreHelper = new FirestoreHelper();
        FirebaseUser curUser = authHelper.getCurrentUser();
        if (curUser == null) {
            // 로그인되어 있지 않으면 AuthActivity로 이동
            Intent intent = new Intent(MainActivity.this, ActivityFortest.class);
            startActivity(intent);
            finish();
            return;
        }
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        familyMemberId = curUser.getUid();
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.page_1) {
                    MedicineList medicineListFragment = MedicineList.newInstance(username);
                    transferTo(medicineListFragment);
                    return true;
                }

                if (itemId == R.id.page_4) {
                    CalendarFragment calendarFragment = CalendarFragment.newInstance(username);
                    transferTo(calendarFragment);
                    return true;
                }

// History 자리에 Family 배치
                if (itemId == R.id.page_2) {
                    transferTo(Family_main_sub.newInstance("param1", "param2"));
                    return true;
                }

// 네 번째 탭: My Page
                if (itemId == R.id.page_5) {
                    transferTo(MyPageFragment.newInstance(username, userEmail));
                    return true;
                }


                return false;
            }
        });

        bottomNavigationView.setOnItemReselectedListener(new NavigationBarView.OnItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem item) {
                // No action needed
            }
        });


    }

    private void handleRecentsDirectory() {
        File recentsDir = new File(getFilesDir(), "recents");
        if (!recentsDir.exists()) {
            boolean created = recentsDir.mkdirs();
            if (!created) {
                Log.e("FileAccess", "Failed to create recents directory");
            } else {
                Log.d("FileAccess", "Recents directory created at: " + recentsDir.getAbsolutePath());
            }
        }
    }

    private void transferTo(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void showLoading(boolean show) {
        if (globalLoadingOverlay == null) return;

        if (show) {
            loadingCount++;
        } else {
            loadingCount = Math.max(loadingCount - 1, 0);
        }

        globalLoadingOverlay.setVisibility(
                loadingCount > 0 ? View.VISIBLE : View.GONE
        );
    }


    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS);
            } else {
                Toast.makeText(this, "알림 권한이 이미 허용되었습니다.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "알림 권한이 필요하지 않습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Medicine Alarm Channel";
            String description = "Channel for medicine alarm notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // **FamilyLogin.OnLoginSuccessListener 메서드 구현**
    @Override
    public void onLoginSuccess(String userId) {
        FirebaseUser currentUser = authHelper.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            authHelper.logout(this);
            Intent intent = new Intent(MainActivity.this, ActivityFortest.class);
            startActivity(intent);
            finish();
            return;
        }

        String email = currentUser.getEmail();
        String uid = currentUser.getUid();

        if (email == null) {
            Toast.makeText(this, "이메일 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            authHelper.logout(this);
            Intent intent = new Intent(MainActivity.this, ActivityFortest.class);
            startActivity(intent);
            finish();
            return;
        }

        showLoading(true);

        // Firestore에서 사용자 존재 여부 확인
        firestoreHelper.checkUserExists(uid, new FirestoreHelper.CheckUserCallback() {
            @Override
            public void onUserExists(String fetchedUsername) {
                showLoading(false);
                username = fetchedUsername;
                setupBottomNavigation();
                // 초기 프래그먼트 설정
                if (getSupportFragmentManager().getFragments().isEmpty()) {
                    MedicineList medicineListFragment = MedicineList.newInstance(username);
                    transferTo(medicineListFragment);
                }
            }

            @Override
            public void onUserDoesNotExist() {
                showLoading(false);
                Toast.makeText(MainActivity.this, "사용자 이름을 설정해주세요.", Toast.LENGTH_SHORT).show();
                navigateToSetUsernameFragment(email, uid);
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(MainActivity.this, "사용자 확인 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "Error checking user existence", e);
            }
        });
    }

    // **SetUsernameFragment.OnUsernameSetListener 메서드 구현**
    @Override
    public void onUsernameSet(String username) {
        // Username 설정 후 로그인 성공 처리
        this.username = username;
        setupBottomNavigation();
        // Initialize the main fragment
        MedicineList medicineListFragment = MedicineList.newInstance(username);
        transferTo(medicineListFragment);
    }

    // **SetUsernameFragment로 이동하는 메서드**
    private void navigateToSetUsernameFragment(String email, String uid) {
        Bundle args = new Bundle();
        args.putString("email", email);
        args.putString("uid", uid);

        SetUsernameFragment fragment = SetUsernameFragment.newInstance(email, uid);
        fragment.setArguments(args);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // **MedicineList로 이동하는 메서드**
    private void navigateToMedicineList() {
        MedicineList medicineListFragment = MedicineList.newInstance(username);
        transferTo(medicineListFragment);
    }
}
