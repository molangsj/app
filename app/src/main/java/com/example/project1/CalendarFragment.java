// app/src/main/java/com/example/project_yakkuk/CalendarFragment.java

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

import com.example.project1.Calendar_BottomSheet;
import com.example.project1.FirestoreHelper;
import com.example.project1.MedicineData;
import com.example.project1.databinding.FragmentCalendarBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
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

    // 현재 표시 중인 연도와 월을 추적하기 위한 변수
    private Calendar currentCalendar;

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
        firestoreHelper = new FirestoreHelper(); // FirestoreHelper 객체 초기화
        currentCalendar = Calendar.getInstance(); // 현재 날짜로 초기화
        return binding.getRoot();
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
                // 선택된 날짜에 해당하는 약이 있는지 확인 후 처리
                String dateStr = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date);
                firestoreHelper.getMedicationsForDate(familyMemberId, dateStr, new FirestoreHelper.MedicationListCallback() {
                    @Override
                    public void onMedicationListReceived(List<MedicineData> medications) {
                        if (medications.isEmpty()) {
                            // 약이 없는 경우, 메시지 표시
                            showNoMedicineMessage();
                        } else {
                            // 약이 있는 경우, BottomSheet 표시
                            Calendar_BottomSheet bottomSheetFragment = Calendar_BottomSheet.newInstance(date, familyMemberId);
                            bottomSheetFragment.show(getParentFragmentManager(), "BottomSheet");
                        }
                    }

                    @Override
                    public void onMedicationListFailed(Exception e) {
                        Log.e("CalendarFragment", "Error getting medications: ", e);
                        // 오류 메시지 표시
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

        // 현재 달의 첫 날로 설정
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        // 첫 번째 요일을 가져옵니다 (일요일=1, 월요일=2, ...)
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0 기반 인덱스

        // 현재 달의 총 일수
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 이전 달의 일수를 채우기 위해 null 추가
        for (int i = 0; i < firstDayOfWeek; i++) {
            daysList.add(null);
        }

        // 현재 달의 날짜 추가
        for (int day = 1; day <= daysInMonth; day++) {
            calendar.set(Calendar.DAY_OF_MONTH, day);
            daysList.add(calendar.getTime());
        }

        // 총 날짜가 7의 배수가 되도록 다음 달의 일수 null로 채우기
        while (daysList.size() % 7 != 0) {
            daysList.add(null);
        }

        return daysList;
    }

    private void fetchMedicineDates() {
        // 기존 달의 약 날짜들을 초기화
        medicineDates = new ArrayList<>();

        // 현재 달의 날짜 목록을 생성
        List<Date> currentMonthDays = generateDays();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        for (Date date : currentMonthDays) {
            if (date == null) continue; // 빈 칸은 건너뜀
            String dateStr = sdf.format(date);
            firestoreHelper.getMedicationsForDate(familyMemberId, dateStr, new FirestoreHelper.MedicationListCallback() {
                @Override
                public void onMedicationListReceived(List<MedicineData> medications) {
                    if (!medications.isEmpty() && !medicineDates.contains(date)) {
                        medicineDates.add(date);
                        calendarAdapter.setMedicineDates(medicineDates);
                    }
                }

                @Override
                public void onMedicationListFailed(Exception e) {
                    Log.e("CalendarFragment", "Error fetching medications for date: " + dateStr, e);
                }
            });
        }
    }

    private void showNoMedicineMessage() {
        // 현재 Fragment에 Toast 메시지로 "오늘은 복용할 약이 없습니다!" 표시
        Toast.makeText(getContext(), "오늘은 복용할 약이 없습니다!", Toast.LENGTH_SHORT).show();
    }

    private void showErrorMessage() {
        // Firestore 접근 실패 시 Toast 메시지 표시
        Toast.makeText(getContext(), "약 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
    }
}
