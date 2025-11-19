package com.example.project1;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project1.databinding.FragmentCalendarBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar; // java.util.Calendar 사용
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private Calendar_Adapter calendarAdapter;
    private List<Date> days;
    private List<Date> medicineDates;

    private String familyMemberId;

    private FirestoreHelper firestoreHelper;

    // 현재 표시 중인 연도와 월
    private Calendar currentCalendar;

    // 한 달치 요청 카운트
    private int pendingMedicineRequests = 0;

    public static CalendarFragment newInstance(String familyMemberId) {
        CalendarFragment fragment = new CalendarFragment();
        Bundle args = new Bundle();
        args.putString("familyMemberId", familyMemberId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        firestoreHelper = new FirestoreHelper();
        currentCalendar = Calendar.getInstance();
        return binding.getRoot();
    }

    // 전역 로딩 헬퍼
    private void showGlobalLoading(boolean show) {
        if (!isAdded()) return;
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showLoading(show);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            familyMemberId = getArguments().getString("familyMemberId");
        }

        setupRecyclerView();
        setupMonthNavigation();
        fetchMedicineDates();
    }

    private void setupRecyclerView() {
        days = generateDays();
        calendarAdapter = new Calendar_Adapter(getContext(), days);
        RecyclerView recyclerView = binding.recyclerViewCalendar;
        recyclerView.setAdapter(calendarAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 7));

        calendarAdapter.setOnItemClickListener((date) -> {
            if (date != null) {
                String dateStr = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date);

                // 날짜 클릭 시 로딩 시작
                showGlobalLoading(true);

                firestoreHelper.getMedicationsForDate(familyMemberId, dateStr, new FirestoreHelper.MedicationListCallback() {
                    @Override
                    public void onMedicationListReceived(List<MedicineData> medications) {
                        showGlobalLoading(false);

                        if (medications.isEmpty()) {
                            showNoMedicineMessage();
                        } else {
                            Calendar_BottomSheet bottomSheetFragment = Calendar_BottomSheet.newInstance(date, familyMemberId);
                            bottomSheetFragment.show(getParentFragmentManager(), "BottomSheet");
                        }
                    }

                    @Override
                    public void onMedicationListFailed(Exception e) {
                        showGlobalLoading(false);
                        Log.e("CalendarFragment", "Error getting medications: ", e);
                        showErrorMessage();
                    }
                });
            }
        });
    }

    private void setupMonthNavigation() {
        updateMonthYearText();

        binding.buttonPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        binding.buttonNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendar();
        });
    }

    private void updateMonthYearText() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String monthYear = sdf.format(currentCalendar.getTime());
        binding.textViewMonthYear.setText(monthYear);
    }

    private void updateCalendar() {
        days = generateDays();
        calendarAdapter.updateDays(days);
        fetchMedicineDates();
        updateMonthYearText();
    }

    private List<Date> generateDays() {
        List<Date> daysList = new ArrayList<>();
        Calendar calendar = (Calendar) currentCalendar.clone();

        // 현재 달의 첫날
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0 기반
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 앞쪽 빈칸(null) 채우기
        for (int i = 0; i < firstDayOfWeek; i++) {
            daysList.add(null);
        }

        // 현재 달 날짜
        for (int day = 1; day <= daysInMonth; day++) {
            calendar.set(Calendar.DAY_OF_MONTH, day);
            daysList.add(calendar.getTime());
        }

        // 7의 배수 되도록 뒤쪽 null 채우기
        while (daysList.size() % 7 != 0) {
            daysList.add(null);
        }

        return daysList;
    }

    private void fetchMedicineDates() {
        medicineDates = new ArrayList<>();

        List<Date> currentMonthDays = generateDays();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        pendingMedicineRequests = 0;
        for (Date date : currentMonthDays) {
            if (date != null) {
                pendingMedicineRequests++;
            }
        }

        if (pendingMedicineRequests == 0) {
            return;
        }

        // 한 달치 로딩 시작
        showGlobalLoading(true);

        for (Date date : currentMonthDays) {
            if (date == null) continue;
            String dateStr = sdf.format(date);

            firestoreHelper.getMedicationsForDate(familyMemberId, dateStr, new FirestoreHelper.MedicationListCallback() {
                @Override
                public void onMedicationListReceived(List<MedicineData> medications) {
                    if (!medications.isEmpty() && !medicineDates.contains(date)) {
                        medicineDates.add(date);
                        calendarAdapter.setMedicineDates(medicineDates);
                    }
                    onOneMedicineRequestFinished();
                }

                @Override
                public void onMedicationListFailed(Exception e) {
                    Log.e("CalendarFragment", "Error fetching medications for date: " + dateStr, e);
                    onOneMedicineRequestFinished();
                }
            });
        }
    }

    private void onOneMedicineRequestFinished() {
        pendingMedicineRequests--;
        if (pendingMedicineRequests <= 0) {
            showGlobalLoading(false);
        }
    }

    private void showNoMedicineMessage() {
        Toast.makeText(getContext(), "오늘은 복용할 약이 없습니다!", Toast.LENGTH_SHORT).show();
    }

    private void showErrorMessage() {
        Toast.makeText(getContext(), "약 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
    }
}
