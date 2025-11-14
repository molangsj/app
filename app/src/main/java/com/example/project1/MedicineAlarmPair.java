// app/src/main/java/com/example/project_yakkuk/MedicineAlarmPair.java

package com.example.project1;

public class MedicineAlarmPair {
    private MedicineData medicine;
    private int alarmIndex;

    public MedicineAlarmPair(MedicineData medicine, int alarmIndex) {
        this.medicine = medicine;
        this.alarmIndex = alarmIndex;
    }

    public MedicineData getMedicine() {
        return medicine;
    }

    public void setMedicine(MedicineData medicine) {
        this.medicine = medicine;
    }

    public int getAlarmIndex() {
        return alarmIndex;
    }

    public void setAlarmIndex(int alarmIndex) {
        this.alarmIndex = alarmIndex;
    }
}
