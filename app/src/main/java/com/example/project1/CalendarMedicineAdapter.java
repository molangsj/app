// CalendarMedicineAdapter.java
package com.example.project1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CalendarMedicineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ALARM = 1;

    private Context context;
    private List<MedicineData> medicineList;
    private OnAlarmCheckedChangeListener listener;

    // 알람 체크 콜백
    public interface OnAlarmCheckedChangeListener {
        void onAlarmCheckedChanged(MedicineData medicine, int alarmIndex, int isChecked); // 1/0
    }

    public CalendarMedicineAdapter(Context context,
                                   List<MedicineData> medicineList,
                                   OnAlarmCheckedChangeListener listener) {
        this.context = context;
        this.medicineList = medicineList;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        int count = 0;
        for (MedicineData medicine : medicineList) {
            if (position == count) {
                return TYPE_HEADER;
            }
            count++;
            if (medicine.getAlarmTimes() != null) {
                if (position < count + medicine.getAlarmTimes().size()) {
                    return TYPE_ALARM;
                }
                count += medicine.getAlarmTimes().size();
            }
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        int total = 0;
        for (MedicineData medicine : medicineList) {
            total += 1; // 헤더
            if (medicine.getAlarmTimes() != null) {
                total += medicine.getAlarmTimes().size(); // 알림 시간
            }
        }
        return total;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_medicine_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_medicine_alarm, parent, false);
            return new AlarmViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int count = 0;
        for (MedicineData medicine : medicineList) {
            if (position == count) {
                ((HeaderViewHolder) holder).bind(medicine);
                return;
            }
            count++;
            if (medicine.getAlarmTimes() != null) {
                if (position < count + medicine.getAlarmTimes().size()) {
                    int alarmIndex = position - count;
                    ((AlarmViewHolder) holder).bind(medicine, alarmIndex);
                    return;
                }
                count += medicine.getAlarmTimes().size();
            }
        }
    }

    // 헤더 뷰홀더
    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedicineName;
        ImageView ivMedicineIndicator; // “하나라도 먹은 알람이 있는 약” 표시

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedicineName = itemView.findViewById(R.id.tvMedicineName);
            ivMedicineIndicator = itemView.findViewById(R.id.ivMedicineIndicator);
        }

        void bind(MedicineData medicine) {
            tvMedicineName.setText(medicine.getPillName());

            // 이 약에 대해 pillIsChecked1..N 중 하나라도 1이면 아이콘 표시
            boolean hasTaken = false;
            if (medicine.getAlarmTimes() != null) {
                int alarmCount = medicine.getAlarmTimes().size();
                for (int i = 0; i < alarmCount && i < 10; i++) {
                    Integer v = medicine.getPillIsCheckedAt(i);
                    if (v != null && v == 1) {
                        hasTaken = true;
                        break;
                    }
                }
            }

            ivMedicineIndicator.setVisibility(hasTaken ? View.VISIBLE : View.GONE);
        }
    }

    // 알림 뷰홀더
    class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView tvAlarmTime;
        CheckBox cbAlarm;

        AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAlarmTime = itemView.findViewById(R.id.tvAlarmTime);
            cbAlarm = itemView.findViewById(R.id.cbAlarm);
        }

        void bind(MedicineData medicine, int alarmIndex) {
            String alarmTime = medicine.getAlarmTimes().get(alarmIndex);
            Integer isCheckedInt = medicine.getPillIsCheckedAt(alarmIndex);
            boolean isChecked = (isCheckedInt != null && isCheckedInt == 1);
            tvAlarmTime.setText(alarmTime);

            // 리사이클러 뷰 재사용 방지
            cbAlarm.setOnCheckedChangeListener(null);
            cbAlarm.setChecked(isChecked);

            cbAlarm.setOnCheckedChangeListener((buttonView, isChecked1) -> {
                int newIsChecked = isChecked1 ? 1 : 0; // 체크 시 1, 미체크 시 0
                if (listener != null) {
                    listener.onAlarmCheckedChanged(medicine, alarmIndex, newIsChecked);
                }
            });
        }
    }
}
