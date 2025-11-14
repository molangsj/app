// app/src/main/java/com/example/project_yakkuk/OpenCalendarBottomSheetActivity.java

package com.example.project1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.project1.Calendar_BottomSheet;

import java.util.Date;

public class OpenCalendarBottomSheetActivity extends AppCompatActivity {

    private static final String TAG = "OpenCalendarBSActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Intent로부터 데이터 추출
        Intent intent = getIntent();
        String username = intent.getStringExtra("username"); // 수정된 부분
        String dateStr = intent.getStringExtra("dateStr");
        String pillName = intent.getStringExtra("pillName");
        String alarmTime = intent.getStringExtra("alarmTime"); // 추가된 부분

        Log.d(TAG, "Received data - username: " + username + ", dateStr: " + dateStr + ", pillName: " + pillName + ", alarmTime: " + alarmTime); // 수정된 부분

        // 알림 클릭 시 오늘 날짜로 Calendar_BottomSheet 열기
        Date selectedDate = new Date(); // 오늘 날짜
        Calendar_BottomSheet bottomSheet = Calendar_BottomSheet.newInstance(selectedDate, username); // 수정된 부분

        bottomSheet.show(getSupportFragmentManager(), "CalendarBottomSheet");

        // 액티비티 종료
        finish();
    }
}
