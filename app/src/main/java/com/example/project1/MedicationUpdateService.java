// app/src/main/java/com/example/project_yakkuk/MedicationUpdateService.java

package com.example.project1;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MedicationUpdateService extends Service {
    private static final String TAG = "MedicationUpdateService";
    private static final String CHANNEL_ID = "medicine_alarm";
    private Executor executor = Executors.newSingleThreadExecutor();

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MedicationUpdateService onCreate");

        // 포그라운드 서비스로 전환하여 시스템이 서비스를 종료하지 않도록 함
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("약 복용 업데이트 중")
                    .setContentText("약 복용 상태를 업데이트하고 있습니다.")
                    .setSmallIcon(R.drawable.ic_notification) // ic_notification 아이콘을 res/drawable에 추가하세요
                    .build();
            startForeground(1, notification);
            Log.d(TAG, "Foreground service started");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MedicationUpdateService onStartCommand called");

        String username = intent.getStringExtra("username");
        String dateStr = intent.getStringExtra("dateStr");
        String pillName = intent.getStringExtra("pillName");

        Log.d(TAG, "Received data - username: " + username + ", dateStr: " + dateStr + ", pillName: " + pillName);

        if (username != null && !username.isEmpty() &&
                dateStr != null && !dateStr.isEmpty() &&
                pillName != null && !pillName.isEmpty()) {

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference pillRef = db.collection("Users") // 컬렉션 이름을 "FamilyMember"에서 "Users"로 변경
                    .document(username) // username 사용
                    .collection("Medications") // 날짜 대신 Medications 컬렉션 사용 (필요 시 조정)
                    .document(pillName); // pillName을 문서 ID로 사용

            executor.execute(() -> {
                try {
                    // Firestore 문서 가져오기
                    DocumentSnapshot docSnapshot = Tasks.await(pillRef.get());
                    if (docSnapshot.exists()) {
                        // 'pillischecked' 필드를 1로 업데이트
                        pillRef.update("pillischecked", 1);
                        Log.d(TAG, "복용 여부가 업데이트되었습니다.");
                    } else {
                        Log.w(TAG, "Medication document does not exist for pillName: " + pillName);
                    }
                } catch (Exception e) {
                    // 예외 발생 시 로그에 기록
                    Log.e(TAG, "Error processing medication update for pillName: " + pillName, e);
                } finally {
                    // 작업 완료 후 서비스 종료
                    stopSelf();
                    Log.d(TAG, "MedicationUpdateService stopped");
                }
            });
        } else {
            Log.e(TAG, "Invalid input data: username, dateStr, and pillName must be provided.");
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // 바인딩을 제공하지 않음
        return null;
    }
}
