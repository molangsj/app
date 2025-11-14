package com.example.project1;

import java.io.Serializable;
import java.util.List;
import android.util.Log;

public class MedicineData implements Serializable {
    private String autoId;
    private String pillName;
    private int pillType;
    private boolean alarmEnabled;
    private boolean favorite;
    private List<String> daysOfWeek;
    private List<String> alarmTimes;
    private String notes;
    private String sideEffects;
    private String familyMemberId;
    private String caution;
    private String memberId;
    private int iconResId;
    private String dateStr;

    // 개별 pillIsChecked 필드 (1: 복용, 0: 미복용)
    private Integer pillIsChecked;
    private Integer pillIsChecked1;
    private Integer pillIsChecked2;
    private Integer pillIsChecked3;
    private Integer pillIsChecked4;
    private Integer pillIsChecked5;
    private Integer pillIsChecked6;
    private Integer pillIsChecked7;
    private Integer pillIsChecked8;
    private Integer pillIsChecked9;
    private Integer pillIsChecked10;

    public Integer getPillIsChecked() { return pillIsChecked; }
    public void setPillIsChecked(Integer pillIsChecked) { this.pillIsChecked = pillIsChecked; }

    public int calculatePillIsChecked() {
        this.pillIsChecked = (pillIsChecked1 == 0 || pillIsChecked2 == 0 || pillIsChecked3 == 0 ||
                pillIsChecked4 == 0 || pillIsChecked5 == 0 || pillIsChecked6 == 0 ||
                pillIsChecked7 == 0 || pillIsChecked8 == 0 || pillIsChecked9 == 0 ||
                pillIsChecked10 == 0) ? 0 : 1;
        return pillIsChecked;
    }

    // 기본 생성자
    public MedicineData() {}

    // Getters and Setters for existing fields
    public String getAutoId() { return autoId; }
    public void setAutoId(String autoId) { this.autoId = autoId; }

    public String getPillName() { return pillName; }
    public void setPillName(String pillName) { this.pillName = pillName; }

    public int getPillType() { return pillType; }
    public void setPillType(int pillType) { this.pillType = pillType; }

    public boolean isAlarmEnabled() { return alarmEnabled; }
    public void setAlarmEnabled(boolean alarmEnabled) { this.alarmEnabled = alarmEnabled; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    public List<String> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<String> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public List<String> getAlarmTimes() { return alarmTimes; }
    public void setAlarmTimes(List<String> alarmTimes) { this.alarmTimes = alarmTimes; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getSideEffects() { return sideEffects; }
    public void setSideEffects(String sideEffects) { this.sideEffects = sideEffects; }

    public String getFamilyMemberId() { return familyMemberId; }
    public void setFamilyMemberId(String familyMemberId) { this.familyMemberId = familyMemberId; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getCaution() { return caution; }
    public void setCaution(String caution) { this.caution = caution; }

    public String getDateStr() { return dateStr; }
    public void setDateStr(String dateStr) { this.dateStr = dateStr; }

    public Integer getPillIsChecked1() { return pillIsChecked1; }
    public void setPillIsChecked1(Integer pillIsChecked1) { this.pillIsChecked1 = pillIsChecked1; }

    public Integer getPillIsChecked2() { return pillIsChecked2; }
    public void setPillIsChecked2(Integer pillIsChecked2) { this.pillIsChecked2 = pillIsChecked2; }

    public Integer getPillIsChecked3() { return pillIsChecked3; }
    public void setPillIsChecked3(Integer pillIsChecked3) { this.pillIsChecked3 = pillIsChecked3; }

    public Integer getPillIsChecked4() { return pillIsChecked4; }
    public void setPillIsChecked4(Integer pillIsChecked4) { this.pillIsChecked4 = pillIsChecked4; }

    public Integer getPillIsChecked5() { return pillIsChecked5; }
    public void setPillIsChecked5(Integer pillIsChecked5) { this.pillIsChecked5 = pillIsChecked5; }

    public Integer getPillIsChecked6() { return pillIsChecked6; }
    public void setPillIsChecked6(Integer pillIsChecked6) { this.pillIsChecked6 = pillIsChecked6; }

    public Integer getPillIsChecked7() { return pillIsChecked7; }
    public void setPillIsChecked7(Integer pillIsChecked7) { this.pillIsChecked7 = pillIsChecked7; }

    public Integer getPillIsChecked8() { return pillIsChecked8; }
    public void setPillIsChecked8(Integer pillIsChecked8) { this.pillIsChecked8 = pillIsChecked8; }

    public Integer getPillIsChecked9() { return pillIsChecked9; }
    public void setPillIsChecked9(Integer pillIsChecked9) { this.pillIsChecked9 = pillIsChecked9; }

    public Integer getPillIsChecked10() { return pillIsChecked10; }
    public void setPillIsChecked10(Integer pillIsChecked10) { this.pillIsChecked10 = pillIsChecked10; }

    public int getIconResId() {return iconResId;}
    public void setIconResId(int iconResId) {this.iconResId = iconResId;}

    // pillIsChecked 필드를 인덱스로 접근할 수 있는 헬퍼 메서드 (0-based index)
    public Integer getPillIsCheckedAt(int index) {
        switch (index) {
            case 0: return getPillIsChecked1();
            case 1: return getPillIsChecked2();
            case 2: return getPillIsChecked3();
            case 3: return getPillIsChecked4();
            case 4: return getPillIsChecked5();
            case 5: return getPillIsChecked6();
            case 6: return getPillIsChecked7();
            case 7: return getPillIsChecked8();
            case 8: return getPillIsChecked9();
            case 9: return getPillIsChecked10();
            default:
                Log.e("MedicineData", "Invalid pillIsChecked index: " + index);
                return 0; // 기본값: 미복용
        }
    }

    public void setPillIsCheckedAt(int index, Integer value) {
        switch (index) {
            case 0: setPillIsChecked1(value); break;
            case 1: setPillIsChecked2(value); break;
            case 2: setPillIsChecked3(value); break;
            case 3: setPillIsChecked4(value); break;
            case 4: setPillIsChecked5(value); break;
            case 5: setPillIsChecked6(value); break;
            case 6: setPillIsChecked7(value); break;
            case 7: setPillIsChecked8(value); break;
            case 8: setPillIsChecked9(value); break;
            case 9: setPillIsChecked10(value); break;
            default:
                Log.e("MedicineData", "Invalid pillIsChecked index: " + index);
                break;
        }
    }
}