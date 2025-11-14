// app/src/main/java/com/example/project_yakkuk/AlarmReceiver.java

package com.example.project1;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID = "medicine_alarm"; // MainActivity와 동일한 채널 ID 사용
    private static final int NOTIFICATION_ID = 1001; // 고유한 알림 ID

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmReceiver", "onReceive called");

        // Intent로부터 필요한 식별자 추출
        String username = intent.getStringExtra("username"); // 수정된 부분
        String dateStr = intent.getStringExtra("dateStr");
        String pillName = intent.getStringExtra("pillName");
        String alarmTime = intent.getStringExtra("alarmTime"); // 추가된 부분

        Log.d("AlarmReceiver", "username: " + username); // 수정된 부분
        Log.d("AlarmReceiver", "dateStr: " + dateStr);
        Log.d("AlarmReceiver", "pillName: " + pillName);
        Log.d("AlarmReceiver", "alarmTime: " + alarmTime); // 추가된 부분

        // 알림 클릭 시 이동할 Intent 설정 (OpenCalendarBottomSheetActivity로 이동)
        Intent openIntent = new Intent(context, OpenCalendarBottomSheetActivity.class);
        openIntent.putExtra("username", username); // 수정된 부분
        openIntent.putExtra("dateStr", dateStr);
        openIntent.putExtra("pillName", pillName);
        openIntent.putExtra("alarmTime", alarmTime);

        NotificationHelper.showNotification(context, username, dateStr, pillName, alarmTime);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(), // 고유한 requestCode 사용
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 알림 생성
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // ic_notification 아이콘을 res/drawable에 추가하세요
                .setContentTitle("약 먹을 시간입니다!")
                .setContentText(pillName + " 약을 드실 시간입니다.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent); // 클릭 시 이동하도록 설정

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());

        Log.d("AlarmReceiver", "Notification sent for pillName: " + pillName + " at alarmTime: " + alarmTime);

        // Firestore 업데이트: pillIsCheckedX 필드를 true로 설정
        if (username != null && dateStr != null && pillName != null && alarmTime != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference pillRef = db.collection("FamilyMember")
                    .document(username)
                    .collection(dateStr)
                    .document(pillName);

            pillRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        List<String> alarmTimes = (List<String>) documentSnapshot.get("alarmTimes");
                        if (alarmTimes != null) {
                            int index = alarmTimes.indexOf(alarmTime);
                            if (index != -1) {
                                String pillIsCheckedField = "pillIsChecked" + (index + 1);
                                pillRef.update(pillIsCheckedField, 1)
                                        .addOnSuccessListener(aVoid -> Log.d("AlarmReceiver", "pillIsChecked updated successfully: " + pillIsCheckedField))
                                        .addOnFailureListener(e -> Log.e("AlarmReceiver", "Failed to update pillIsChecked: " + pillIsCheckedField, e));
                            } else {
                                Log.e("AlarmReceiver", "Alarm time not found in alarmTimes list: " + alarmTime);
                            }
                        } else {
                            Log.e("AlarmReceiver", "alarmTimes field is null");
                        }
                    } else {
                        Log.e("AlarmReceiver", "Medicine document does not exist: " + pillName);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("AlarmReceiver", "Failed to fetch medicine document: " + pillName, e);
                }
            });
        } else {
            Log.e("AlarmReceiver", "Missing required data to update pillIsChecked");
        }
    }
}
