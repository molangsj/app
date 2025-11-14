package com.example.project1;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project1.databinding.FragmentChartBinding;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartFragment extends Fragment {
    private FirebaseFirestore firestore;
    CollectionReference historyMedicineRef;
    Map<String, Integer> dataCount;
    private FragmentChartBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();

        historyMedicineRef = firestore.collection("History_Medicine");

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChartBinding.inflate(inflater, container, false);
        fetchDataAndShowChart(1, binding.pieChart, true);

        // 약 차트


// 병원 차트


        return binding.getRoot();
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.change.setOnClickListener(v->{
            showFilterDialog();
        });
        binding.showlist.setOnClickListener(v->{
            showRecyclerViewDialog(dataCount);
        });

    }
    // Firestore 참조


    // 특정 기간을 계산하는 함수
    private String getStartDate(int monthsAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -monthsAgo);

        // 연도, 월, 일을 추출
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        Log.d("month", String.valueOf(month));
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // 날짜 형식을 yyMMdd로 변환
        return String.format("%04d%02d%02d", year, month, day);
    }

    private void fetchDataAndShowChart(int monthsAgo, PieChart pieChart, boolean isMedicineChart) {
        String startDate = getStartDate(monthsAgo);
        String endDate = new SimpleDateFormat("yyyyMMdd").format(new Date());  // 현재 날짜를 "yyMMdd" 형식으로 변환

        historyMedicineRef
                .whereGreaterThanOrEqualTo("date", Integer.parseInt(startDate))
                .whereLessThanOrEqualTo("date", Integer.parseInt(endDate))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                     dataCount = new HashMap<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        // 각 라인을 순회하며 데이터 처리
                        for (int i = 1; i <= 3; i++) {
                            String dataField = isMedicineChart
                                    ? document.getString("medicine_line" + i)
                                    : document.getString("hospital_line" + i);

                            // Null 또는 빈 데이터 건너뛰기
                            if (dataField == null || dataField.trim().isEmpty()) {
                                continue; // 다음 라인으로 이동
                            }

                            // 쉼표로 분리하여 각각 카운트
                            String[] items = dataField.split("\\s*,\\s*");
                            for (String item : items) {
                                item = item.trim(); // 공백 제거
                                if (!item.isEmpty()) {
                                    dataCount.put(item, dataCount.getOrDefault(item, 0) + 1);
                                }
                            }
                        }
                    }
                    // 차트를 업데이트
                    binding.pieChart.setRotationEnabled(true); // 회전 가능
                    binding.pieChart.animateY(1200, Easing.EaseInOutCubic);
                    updatePieChart(dataCount, pieChart);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching data", e);
                });
    }

    private void updatePieChart(Map<String, Integer> dataCount, PieChart pieChart) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        // 데이터 입력
        for (Map.Entry<String, Integer> entry : dataCount.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        // PieDataSet 생성
        PieDataSet dataSet = new PieDataSet(entries, "Chart Data");
        dataSet.setColors(ColorTemplate.PASTEL_COLORS);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);

        // PieChart에 데이터 적용
        pieChart.setData(pieData);
        pieChart.invalidate(); // 차트 새로고침
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter, null);
        builder.setView(dialogView);

        // 뷰의 요소 가져오기
        RadioGroup typeRadioGroup = dialogView.findViewById(R.id.typeRadioGroup);
        RadioGroup durationRadioGroup = dialogView.findViewById(R.id.durationRadioGroup);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);

        AlertDialog dialog = builder.create();

        confirmButton.setOnClickListener(v -> {
            // 선택된 항목 확인
            boolean isMedicineChart = typeRadioGroup.getCheckedRadioButtonId() == R.id.medicineRadioButton;

            int monthsAgo = 1; // 기본값은 1개월
            int checkedRadioButtonId = durationRadioGroup.getCheckedRadioButtonId();
            if (checkedRadioButtonId == R.id.sixMonthRadioButton) {
                monthsAgo = 6;
            } else if (checkedRadioButtonId == R.id.oneYearRadioButton) {
                monthsAgo = 12;
            }
            // fetchDataAndShowChart 호출 (Fragment 내 메서드)
            PieChart pieChart = requireView().findViewById(R.id.pieChart); // Fragment 뷰에서 찾기
            fetchDataAndShowChart(monthsAgo, pieChart, isMedicineChart);

            // 다이얼로그 닫기
            dialog.dismiss();
        });

        dialog.show();
    }


    class MapDialogAdapter extends RecyclerView.Adapter<MapDialogAdapter.ViewHolder> {

        private final List<Map.Entry<String, Integer>> items;

        public MapDialogAdapter(Map<String, Integer> data) {
            this.items = new ArrayList<>(data.entrySet());
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recycler_entry, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map.Entry<String, Integer> entry = items.get(position);
            holder.keyTextView.setText(entry.getKey());
            holder.valueTextView.setText(String.valueOf(entry.getValue()));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView keyTextView;
            TextView valueTextView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                keyTextView = itemView.findViewById(R.id.itemKey);
                valueTextView = itemView.findViewById(R.id.itemValue);
            }
        }
    }
    private void showRecyclerViewDialog(Map<String, Integer> data) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_recycler, null);
        builder.setView(dialogView);

        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerView);
        Button closeButton = dialogView.findViewById(R.id.closeButton);

        // Adapter 설정
        MapDialogAdapter adapter = new MapDialogAdapter(data);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        AlertDialog dialog = builder.create();

        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }



}
