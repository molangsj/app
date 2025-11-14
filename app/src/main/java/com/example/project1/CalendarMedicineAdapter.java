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

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Date;

public class CalendarMedicineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ALARM = 1;

    private Context context;
    private List<MedicineData> medicineList;
    private OnAlarmCheckedChangeListener listener;
    private Set<String> medicineDates; // "yyyyMMdd" 형식의 날짜 문자열 저장

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

    // CalendarMedicineAdapter.java 내부
    public interface OnAlarmCheckedChangeListener {
        void onAlarmCheckedChanged(MedicineData medicine, int alarmIndex, int isChecked); // boolean -> int로 변경
    }


    public CalendarMedicineAdapter(Context context, List<MedicineData> medicineList, OnAlarmCheckedChangeListener listener) {
        this.context = context;
        this.medicineList = medicineList;
        this.listener = listener;
        this.medicineDates = new HashSet<>();
    }

    // setMedicineDates 메서드 추가
    public void setMedicineDates(List<Date> dates) {
        medicineDates.clear();
        for (Date date : dates) {
            if (date != null) {
                String formattedDate = sdf.format(date);
                medicineDates.add(formattedDate);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        // 각 약의 알림 시간 리스트를 순회하여 헤더와 알림 아이템을 반환
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
            View view = LayoutInflater.from(context).inflate(R.layout.item_medicine_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_medicine_alarm, parent, false);
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
        ImageView ivMedicineIndicator; // 약이 있는 날짜 표시를 위한 아이콘

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedicineName = itemView.findViewById(R.id.tvMedicineName);
            ivMedicineIndicator = itemView.findViewById(R.id.ivMedicineIndicator); // 레이아웃에 추가 필요
        }

        void bind(MedicineData medicine) {
            tvMedicineName.setText(medicine.getPillName());

            // 현재 날짜의 약 복용 여부에 따라 아이콘 표시
            String currentDateStr = sdf.format(new Date()); // 현재 날짜
            if (medicineDates.contains(currentDateStr)) {
                ivMedicineIndicator.setVisibility(View.VISIBLE);
            } else {
                ivMedicineIndicator.setVisibility(View.GONE);
            }
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

            // 리사이클러뷰 재사용 시 기존 리스너 제거
            cbAlarm.setOnCheckedChangeListener(null);
            cbAlarm.setChecked(isChecked);

            cbAlarm.setOnCheckedChangeListener((buttonView, isChecked1) -> {
                int newIsChecked = isChecked1 ? 1 : 0; // 체크 시 1, 미체크 시 0
                listener.onAlarmCheckedChanged(medicine, alarmIndex, newIsChecked);
            });
        }
    }
}
