// app/src/main/java/com/example/project_yakkuk/MedicineIconManager.java

package com.example.project1;

import android.content.Context;
import android.content.SharedPreferences;

public class MedicineIconManager {

    private static final String PREFS_NAME = "MedicineIconsPrefs";
    private SharedPreferences sharedPreferences;

    public MedicineIconManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // 아이콘 저장
    public void saveMedicineIcon(String pillName, int iconResId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(pillName, iconResId);
        editor.apply();
    }

    // 아이콘 불러오기
    public int getMedicineIcon(String pillName, int defaultIconResId) {
        return sharedPreferences.getInt(pillName, defaultIconResId);
    }

    // 아이콘 삭제
    public void removeMedicineIcon(String pillName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(pillName);
        editor.apply();
    }
}
