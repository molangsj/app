package com.example.project1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import com.example.project1.databinding.DialogMedicineDetailBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.List;

public class MedicineDetailDialog extends BottomSheetDialogFragment {

    private static final String ARG_MEDICINE = "medicine";
    private static final String ARG_USERNAME = "username";

    private MedicineData medicine;
    private String username;
    private @NonNull DialogMedicineDetailBinding binding;

    private UpdateListener updateListener;

    public interface UpdateListener {
        void onMedicineUpdated(MedicineData updatedMedicine);
    }

    public void setUpdateListener(UpdateListener listener) {
        this.updateListener = listener;
    }

    // MedicineData와 username을 함께 전달
    public static MedicineDetailDialog newInstance(MedicineData medicine, String username) {
        MedicineDetailDialog dialog = new MedicineDetailDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MEDICINE, medicine);
        args.putString(ARG_USERNAME, username);
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogMedicineDetailBinding.inflate(inflater, container, false);

        if (getArguments() != null) {
            medicine = (MedicineData) getArguments().getSerializable(ARG_MEDICINE);
            username = getArguments().getString(ARG_USERNAME);
        }

        if (medicine != null) {
            binding.tvWeekDayCount.setText("주 " + getWeekDayCount()+" 회");

            // 알람 시간 목록 설정
            setupAlarmTimes(medicine.getAlarmTimes());

            // 메모와 주의사항 설정
            binding.etNotes.setText(medicine.getNotes());
            // 여기서 기존에 etSideEffects -> etCaution 으로 변경하고 getSideEffects() -> getCaution() 사용
            binding.etCaution.setText(medicine.getCaution());
        }

        // "편집" 버튼: AddEditMedicineFragment로 이동
        binding.editMedicine.setOnClickListener(v -> {
            dismiss();
            openEditMedicine(medicine);
        });

        // 저장 버튼
        binding.btnSaveDetails.setOnClickListener(v -> {
            saveDetails();
        });

        return binding.getRoot();
    }

    private int getWeekDayCount() {
        if (medicine != null && medicine.getDaysOfWeek() != null) {
            return medicine.getDaysOfWeek().size();
        }
        return 0;
    }

    // 알람 시간 목록 설정
    private void setupAlarmTimes(List<String> alarmTimes) {
        binding.layoutAlarmTimes.removeAllViews();
        if (alarmTimes != null) {
            for (String time : alarmTimes) {
                View alarmTimeView = LayoutInflater.from(getContext()).inflate(R.layout.item_alarm_time, binding.layoutAlarmTimes, false);
                TextView tvAlarmTime = alarmTimeView.findViewById(R.id.tvAlarmTime);
                tvAlarmTime.setText(time);
                binding.layoutAlarmTimes.addView(alarmTimeView);
            }
        }
    }

    // 상세 정보 저장 메서드
    private void saveDetails() {
        String updatedNotes = binding.etNotes.getText().toString().trim();
        String updatedCaution = binding.etCaution.getText().toString().trim(); // etSideEffects -> etCaution

        medicine.setNotes(updatedNotes);
        medicine.setCaution(updatedCaution); // setSideEffects -> setCaution

        if (updateListener != null) {
            updateListener.onMedicineUpdated(medicine);
        }

        dismiss();
    }

    // AddEditMedicineFragment로 이동
    private void openEditMedicine(MedicineData medicine) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, AddEditMedicineFragment.newInstance(medicine, username));
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
