package com.example.project1;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Family_drug_statistic extends AppCompatActivity {
    private PieChart pieChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_drug_statistic);

        pieChart = findViewById(R.id.pieChart);
        fetchDateCollectionNames(dateCollectionNames -> {
            Map<Integer, Integer> pillTypeCount = new HashMap<>();
            fetchDrugDataFromAllDates(dateCollectionNames, pillTypeCount);
        });
        configurePieChart();
    }
    private void fetchDateCollectionNames(OnSuccessListener<List<String>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // FamilyMember > Member1 문서 참조
        db.collection("FamilyMember")
                .document("Member1")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> fields = documentSnapshot.getData(); // 문서 필드 전체 가져오기
                        if (fields != null && !fields.isEmpty()) {
                            // 필드 이름(날짜 값)을 리스트로 변환
                            List<String> dateCollectionNames = new ArrayList<>(fields.keySet());
                            callback.onSuccess(dateCollectionNames); // 콜백 호출
                        } else {
                            Log.e("Firestore", "No date fields found in Member1 document.");
                            callback.onSuccess(new ArrayList<>()); // 빈 리스트 반환
                        }
                    } else {
                        Log.e("Firestore", "Member1 document does not exist.");
                        callback.onSuccess(new ArrayList<>()); // 빈 리스트 반환
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to fetch Member1 document", e);
                    callback.onSuccess(new ArrayList<>()); // 빈 리스트 반환
                });
    }
    private void fetchDrugDataFromAllDates(List<String> dateCollectionNames, Map<Integer, Integer> pillTypeCount) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AtomicInteger completedDates = new AtomicInteger(0);

        // 날짜별로 데이터를 가져오도록 반복
        for (String date : dateCollectionNames) {
            Log.d("Firestore", "Processing date collection: " + date);

            // 각 날짜에 대해 서브컬렉션 데이터 처리
            fetchSubCollectionData(date, pillTypeCount, completedDates, dateCollectionNames.size());
        }
    }

    private void fetchSubCollectionData(String date, Map<Integer, Integer> pillTypeCount,
                                        AtomicInteger completedDates, int totalDates) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AtomicInteger completedSubCollections = new AtomicInteger(0);

        // 서브컬렉션명 (1부터 10까지)
        for (int i = 1; i <= 10; i++) {
            String subCollectionName = "drug_info_" + i;
            Log.d("Firestore", "Processing sub-collection: " + subCollectionName);

            db.collection("FamilyMember")
                    .document("Member1")
                    .collection(date)
                    .document("Drug_Info")
                    .collection(subCollectionName)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        Log.d("Firestore", "Sub-collection fetched: " + subCollectionName + ", Documents: " + querySnapshot.size());
                        if (!querySnapshot.isEmpty()) {
                            for (DocumentSnapshot drugSnapshot : querySnapshot.getDocuments()) {
                                Object pillTypeObj = drugSnapshot.get("pilltype");

                                // 약 정보 누적
                                if (pillTypeObj instanceof Number) {
                                    int pillType = ((Number) pillTypeObj).intValue();
                                    pillTypeCount.put(pillType, pillTypeCount.getOrDefault(pillType, 0) + 1);
                                    Log.d("Firestore", "Pill type: " + pillType + ", Count: " + pillTypeCount.get(pillType));
                                } else {
                                    Log.e("Firestore Warning", "Invalid pilltype value: " + pillTypeObj);
                                }
                            }
                        }

                        // 모든 서브컬렉션 처리 완료 체크
                        if (completedSubCollections.incrementAndGet() == 10) {
                            // 서브컬렉션 모두 완료 후 날짜 처리 완료 확인
                            checkIfAllDatesCompleted(totalDates, completedDates, pillTypeCount);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Failed to fetch data from sub-collection: " + subCollectionName, e);
                        if (completedSubCollections.incrementAndGet() == 10) {
                            checkIfAllDatesCompleted(totalDates, completedDates, pillTypeCount);
                        }
                    });
        }
    }


    private void checkIfAllDatesCompleted(int totalDates, AtomicInteger completedDates, Map<Integer, Integer> pillTypeCount) {
        // 모든 날짜 데이터 처리가 끝난 경우 확인
        if (completedDates.incrementAndGet() == totalDates) {
            Log.d("PieChart", "pillTypeCount contents: " + pillTypeCount);
            updateGraph(pillTypeCount);
        }
    }

    private void updateGraph(Map<Integer, Integer> pillTypeCount) {
        pieChart.clear();
        List<PieEntry> entries = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : pillTypeCount.entrySet()) {
            int pillType = entry.getKey();
            int count = entry.getValue();

            String label;
            switch (pillType) {
                case 1:
                    label = "염증약";
                    break;
                case 2:
                    label = "감기약";
                    break;
                case 3:
                    label = "피부약";
                    break;
                default:
                    label = "기타약";
            }

            entries.add(new PieEntry(count, label));
            Log.d("PieChart", "PieEntry added: label = " + label + ", count = " + count);
        }

        // PieDataSet 생성
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS); // 색상 설정
        dataSet.setValueTextSize(16f); // 비율 글씨 크기
        dataSet.setValueTextColor(Color.BLACK); // 비율 글씨 색상

        // 약 이름 설정
        pieChart.setEntryLabelColor(Color.BLACK); // 약 이름 색상
        pieChart.setEntryLabelTextSize(18f);// 약 이름 글씨 크기

        // 비율 값을 슬라이스 내부에 배치
        dataSet.setValueLinePart1OffsetPercentage(150f); // 슬라이스와 값 사이의 거리
        dataSet.setValueLinePart1Length(0.2f); // 첫 번째 선 길이
        dataSet.setValueLinePart2Length(0.4f); // 두 번째 선 길이
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE); // 비율 위치 (슬라이스 내부)

        // PieData 생성 및 설정
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart)); // 비율 포맷
        pieChart.setData(data);

        // 차트 갱신
        pieChart.invalidate();
    }

    private void configurePieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setHoleRadius(40f);
        pieChart.setRotationEnabled(true);
        pieChart.setEntryLabelTextSize(12f);


        Legend legend = pieChart.getLegend();
        legend.setTextSize(20f); // 범례 텍스트 크기 설정
        legend.setTextColor(Color.BLACK); // 범례 텍스트 색상
        legend.setForm(Legend.LegendForm.CIRCLE); // 범례 모양 (원형)
        legend.setXEntrySpace(20f); // 항목 간 간격 (좌우)
        legend.setYEntrySpace(10f); // 항목 간 간격 (상하)
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL); // 가로 정렬
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER); // 가로 중앙 정렬
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM); // 아래쪽 정렬
    }
}