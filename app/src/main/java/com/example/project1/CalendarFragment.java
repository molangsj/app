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

public class CalendarFragment extends Fragment
        implements Calendar_BottomSheet.OnDateTakenChangedListener {

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

                showGlobalLoading(true);

                firestoreHelper.getMedicationsForDate(familyMemberId, dateStr,
                        new FirestoreHelper.MedicationListCallback() {
                            @Override
                            public void onMedicationListReceived(List<MedicineData> medications) {
                                showGlobalLoading(false);

                                if (medications.isEmpty()) {
                                    showNoMedicineMessage();
                                } else {
                                    Calendar_BottomSheet bottomSheetFragment =
                                            Calendar_BottomSheet.newInstance(date, familyMemberId);

                                    // 콜백 연결
                                    bottomSheetFragment.setOnDateTakenChangedListener(CalendarFragment.this);

                                    bottomSheetFragment.show(
                                            getParentFragmentManager(),
                                            "BottomSheet"
                                    );
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

    @Override
    public void onDateTakenChanged(Date date, boolean hasTaken) {
        if (date == null) return;

        // 현재 보고 있는 달이 아니면 굳이 갱신 안 해도 됨
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        if (cal.get(Calendar.YEAR) != currentCalendar.get(Calendar.YEAR) ||
                cal.get(Calendar.MONTH) != currentCalendar.get(Calendar.MONTH)) {
            return;
        }

        if (medicineDates == null) {
            medicineDates = new ArrayList<>();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String target = sdf.format(date);

        // 같은 날짜 기존 항목 제거
        for (int i = medicineDates.size() - 1; i >= 0; i--) {
            Date d = medicineDates.get(i);
            if (d != null && sdf.format(d).equals(target)) {
                medicineDates.remove(i);
            }
        }

        // hasTaken == true 이면 다시 추가 (아이콘 표시)
        if (hasTaken) {
            medicineDates.add(date);
        }

        // 어댑터에 반영
        calendarAdapter.setMedicineDates(medicineDates);
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
        // 현재 캘린더에서 연/월 가져오기
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH) + 1; // Calendar.MONTH는 0~11

        // 이번 달 기준으로 '먹은 날'만 가져옴
        showGlobalLoading(true);

        firestoreHelper.getTakenDatesForMonth(
                familyMemberId,
                year,
                month,
                new FirestoreHelper.DatesListCallback() {
                    @Override
                    public void onDatesReceived(List<String> dateStrList) {
                        showGlobalLoading(false);

                        if (dateStrList == null) {
                            dateStrList = new ArrayList<>();
                        }

                        SimpleDateFormat sdf =
                                new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                        List<Date> takenDateList = new ArrayList<>();

                        for (String ds : dateStrList) {
                            try {
                                Date d = sdf.parse(ds);
                                if (d != null) {
                                    takenDateList.add(d);
                                }
                            } catch (Exception e) {
                                Log.e("CalendarFragment", "날짜 파싱 실패: " + ds, e);
                            }
                        }

                        // '먹은 날'만 아이콘 표시용 리스트로 사용
                        medicineDates = takenDateList;
                        calendarAdapter.setMedicineDates(medicineDates);
                    }

                    @Override
                    public void onDatesFailed(Exception e) {
                        showGlobalLoading(false);
                        Log.e("CalendarFragment",
                                "getTakenDatesForMonth 실패: " + e.getMessage(), e);
                        // 실패해도 화면 자체는 보여줘야 하니 토스트 정도만
                        Toast.makeText(
                                getContext(),
                                "복용한 날짜를 불러오지 못했습니다.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void showNoMedicineMessage() {
        Toast.makeText(getContext(), "복용할 약이 없습니다!", Toast.LENGTH_SHORT).show();
    }

    private void showErrorMessage() {
        Toast.makeText(getContext(), "약 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 이 프래그먼트 떠날 때는 무조건 로딩 끄기
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showLoading(false);
        }
    }

}
