package com.example.project1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {
    private static final String CHANNEL_ID = "medicine_alarm_channel";

    public static void showNotification(Context context, String username, String dateStr, String pillName, String alarmTime) {
        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // 알림 아이콘 설정
                .setContentTitle("약 복용 시간 알림")
                .setContentText(pillName + " 복용 시간입니다: " + alarmTime)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // 알림 ID는 고유해야 합니다.
        int notificationId = (username + pillName + alarmTime).hashCode();

        // Context를 사용하여 권한 체크
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없을 경우 알림을 표시하지 않습니다.
            // 권한 요청은 Activity나 Fragment에서 별도로 처리해야 합니다.
            Log.e("NotificationHelper", "POST_NOTIFICATIONS 권한이 없습니다. 알림을 표시할 수 없습니다.");
            return;
        }

        notificationManager.notify(notificationId, builder.build());
        Log.d("NotificationHelper", "Notification shown for pill: " + pillName + " at " + alarmTime);
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Medicine Alarm Channel";
            String description = "Channel for medicine alarm notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
