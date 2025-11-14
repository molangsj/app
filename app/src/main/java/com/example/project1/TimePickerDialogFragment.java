package com.example.project1;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class TimePickerDialogFragment extends DialogFragment {

    public interface OnTimeSelectedListener {
        void onTimeSelected(int hour, int minute);
    }

    private OnTimeSelectedListener listener;

    public void setOnTimeSelectedListener(OnTimeSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.custom_time_picker_dialog, null);

        NumberPicker hourPicker = view.findViewById(R.id.hour_picker);
        NumberPicker minutePicker = view.findViewById(R.id.minute_picker);
        View confirmButton = view.findViewById(R.id.confirm_button);

        // 시간 범위 설정 (24시간 형식)
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);

        // 분 범위 설정 (00 ~ 59)
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);

        // 확인 버튼 클릭 시 시간 반환
        confirmButton.setOnClickListener(v -> {
            int selectedHour = hourPicker.getValue();
            int selectedMinute = minutePicker.getValue();
            if (listener != null) {
                listener.onTimeSelected(selectedHour, selectedMinute);
            }
            dismiss();
        });

        // Dialog 생성
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setView(view);
        return builder.create();
    }
}
