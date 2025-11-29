// Calendar_BottomSheet.java
package com.example.project1;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project1.databinding.FragmentCalendarBottomSheetBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Calendar_BottomSheet extends BottomSheetDialogFragment
        implements CalendarMedicineAdapter.OnAlarmCheckedChangeListener {

    private static final String ARG_DATE = "selected_date";
    private static final String ARG_FAMILY_MEMBER_ID = "family_member_id";

    private FragmentCalendarBottomSheetBinding binding;
    private Date selectedDate;
    private String familyMemberId;

    private FirestoreHelper firestoreHelper;

    private CalendarMedicineAdapter adapter;
    private List<MedicineData> medicineList;

    public static Calendar_BottomSheet newInstance(Date date, String familyMemberId) {
        Calendar_BottomSheet fragment = new Calendar_BottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_DATE, date.getTime());
        args.putString(ARG_FAMILY_MEMBER_ID, familyMemberId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBottomSheetBinding.inflate(inflater, container, false);
        firestoreHelper = new FirestoreHelper();

        if (getArguments() != null) {
            long dateMillis = getArguments().getLong(ARG_DATE);
            selectedDate = new Date(dateMillis);
            familyMemberId = getArguments().getString(ARG_FAMILY_MEMBER_ID);
            loadMedicineInfo();
        }

        return binding.getRoot();
    }

    private void loadMedicineInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String dateStr = sdf.format(selectedDate);

        binding.tvSelectedDate.setText("선택된 날짜: " + sdf.format(selectedDate));

        firestoreHelper.getMedicationsForDate(familyMemberId, dateStr, new FirestoreHelper.MedicationListCallback() {
            @Override
            public void onMedicationListReceived(List<MedicineData> medications) {
                medicineList = medications;

                if (medications.isEmpty()) {
                    binding.medicineRecyclerView.setVisibility(View.GONE);
                    binding.tvNoMedicine.setVisibility(View.VISIBLE);
                    binding.tvNoMedicine.setText("오늘은 복용할 약이 없습니다!");
                    binding.tvNoMedicine.setTextColor(Color.BLACK);
                } else {
                    binding.medicineRecyclerView.setVisibility(View.VISIBLE);
                    binding.tvNoMedicine.setVisibility(View.GONE);
                }

                adapter = new CalendarMedicineAdapter(getContext(), medications, Calendar_BottomSheet.this);
                binding.medicineRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                binding.medicineRecyclerView.setAdapter(adapter);
            }

            @Override
            public void onMedicationListFailed(Exception e) {
                Log.e("Calendar_BottomSheet", "Error getting medications: ", e);
                binding.medicineRecyclerView.setVisibility(View.GONE);
                binding.tvNoMedicine.setVisibility(View.VISIBLE);
                binding.tvNoMedicine.setText("약 정보를 불러오는데 실패했습니다.");
                binding.tvNoMedicine.setTextColor(Color.RED);
            }
        });
    }

    /**
     * 약물의 알람 체크박스 상태가 변경되었을 때 Firestore에 업데이트
     *
     * @param medicine    변경된 약물 데이터
     * @param alarmIndex  변경된 알람의 인덱스
     * @param isChecked   체크 상태 (1: 복용, 0: 미복용)
     */
    @Override
    public void onAlarmCheckedChanged(MedicineData medicine, int alarmIndex, int isChecked) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String dateStr = sdf.format(selectedDate);

        if (medicine.getAlarmTimes() != null && alarmIndex < medicine.getAlarmTimes().size()) {

            // ① 기존: 개별 알람 인덱스별 pillIsCheckedN 업데이트
            firestoreHelper.updatePillIsCheckedAt(
                    familyMemberId,
                    dateStr,
                    medicine.getPillName(),
                    alarmIndex,
                    isChecked,
                    new FirestoreHelper.StatusCallback() {
                        @Override
                        public void onStatusUpdated() {
                            Log.d("Calendar_BottomSheet", "pillIsCheckedAt updated: "
                                    + medicine.getPillName()
                                    + " index=" + alarmIndex
                                    + " value=" + isChecked);

                            // ② 추가: 전체 pillIsChecked(요약 필드)도 동시에 업데이트
                            firestoreHelper.updatePillIsCheckedOverall(
                                    familyMemberId,
                                    dateStr,
                                    medicine.getPillName(),
                                    isChecked,
                                    new FirestoreHelper.StatusCallback() {
                                        @Override
                                        public void onStatusUpdated() {
                                            Toast.makeText(
                                                    getContext(),
                                                    "복약 여부가 업데이트되었습니다.",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                            Log.d("Calendar_BottomSheet",
                                                    "pillIsCheckedOverall updated: "
                                                            + medicine.getPillName()
                                                            + " = " + isChecked);
                                        }

                                        @Override
                                        public void onStatusUpdateFailed(Exception e) {
                                            Toast.makeText(
                                                    getContext(),
                                                    "복약 여부(요약) 업데이트에 실패했습니다.",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                            Log.e("Calendar_BottomSheet",
                                                    "Failed to update pillIsCheckedOverall", e);
                                        }
                                    }
                            );
                        }

                        @Override
                        public void onStatusUpdateFailed(Exception e) {
                            Toast.makeText(getContext(),
                                    "복약 여부 업데이트에 실패했습니다.",
                                    Toast.LENGTH_SHORT).show();
                            Log.e("Calendar_BottomSheet",
                                    "Failed to update pillIsCheckedAt", e);
                        }
                    }
            );

        } else {
            Log.e("Calendar_BottomSheet", "Invalid alarmIndex: " + alarmIndex
                    + " for medicine: " + medicine.getPillName());
            Toast.makeText(getContext(),
                    "복약 여부 업데이트에 실패했습니다.",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
