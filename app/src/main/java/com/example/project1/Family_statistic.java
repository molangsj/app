package com.example.project1;

import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Family_statistic#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Family_statistic extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private PieChart pieChart;
    public Family_statistic() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Family_statistic.
     */
    // TODO: Rename and change types and number of parameters
    public static Family_statistic newInstance(String param1, String param2) {
        Family_statistic fragment = new Family_statistic();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_family_statistic, container, false);

        pieChart = view.findViewById(R.id.pieChart);
        Map<Integer, Integer> pillTypeCount = new HashMap<>();
        fetchPillTypeDataFromAllMembers(pillTypeCount);
        configurePieChart();
        return view;
    }

    private void fetchAllDateCollectionNames(OnSuccessListener<List<String>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // FamilyMember 컬렉션의 모든 문서 가져오기
        db.collection("FamilyMember")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> allDateCollectionNames = new ArrayList<>();

                    // 각 문서 순회
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        if (document.exists()) {
                            Map<String, Object> fields = document.getData(); // 문서의 모든 필드 가져오기
                            if (fields != null) {
                                for (String field : fields.keySet()) {
                                    // 날짜 필드만 추가 (필요 시 필터링 로직 추가 가능)
                                    if (isDateField(field)) {
                                        allDateCollectionNames.add(field);
                                    }
                                }
                            }
                        }
                    }

                    if (allDateCollectionNames.isEmpty()) {
                        Log.d("Firestore", "No date fields found in any document.");
                    } else {
                        Log.d("Firestore", "Date fields found: " + allDateCollectionNames);
                    }

                    // 결과를 콜백으로 반환
                    callback.onSuccess(allDateCollectionNames);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to fetch FamilyMember collection", e);
                    callback.onSuccess(new ArrayList<>()); // 빈 리스트 반환
                });
    }
    private boolean isDateField(String fieldName) {
        try {
            // 필드 이름을 정수로 변환 가능하면 날짜로 간주
            Integer.parseInt(fieldName);
            return true;
        } catch (NumberFormatException e) {
            return false; // 숫자가 아닌 필드는 제외
        }
    }


    private void fetchPillTypeDataFromAllMembers(Map<Integer, Integer> pillTypeCount) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AtomicInteger completedMembers = new AtomicInteger(0);

        // FamilyMember 컬렉션의 모든 문서를 순회
        db.collection("FamilyMember")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        int totalMembers = querySnapshot.size();
                        for (DocumentSnapshot memberDocument : querySnapshot.getDocuments()) {
                            String memberId = memberDocument.getId();
                            Log.d("Firestore", "PillTypeCount map: " + pillTypeCount);
                            fetchPillTypeDataFromMember(memberId, pillTypeCount, completedMembers, totalMembers);
                        }
                    } else {
                        Log.d("Firestore", "No members found in FamilyMember collection.");
                        Log.d("Firestore", "PillTypeCount map: " + pillTypeCount);

                        updateGraph(pillTypeCount); // 빈 데이터를 업데이트
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to fetch FamilyMember collection", e);
                    Log.d("Firestore", "PillTypeCount map: " + pillTypeCount);

                    updateGraph(pillTypeCount); // 오류 상황에서도 그래프 업데이트 시도
                });
    }


    private void fetchPillTypeDataFromMember(String memberId, Map<Integer, Integer> pillTypeCount,
                                             AtomicInteger completedMembers, int totalMembers) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d("FirestoreDebug", "Fetching date collections for memberId: " + memberId);

        // 각 멤버의 날짜 컬렉션 순회
        db.collection("FamilyMember")
                .document(memberId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> fields = documentSnapshot.getData();
                        if (fields != null) {
                            List<String> dateCollectionNames = new ArrayList<>(fields.keySet());
                            Log.d("FirestoreDebug", "Date collections for memberId: " + memberId + ": " + dateCollectionNames);
                            AtomicInteger completedDates = new AtomicInteger(0);

                            for (String date : dateCollectionNames) {
                                fetchPillTypeDataFromDate(memberId, date, pillTypeCount, completedDates, dateCollectionNames.size(), completedMembers, totalMembers);
                            }
                        } else {
                            Log.d("FirestoreDebug", "No date collections for memberId: " + memberId);
                            checkIfAllMembersCompleted(completedMembers, totalMembers, pillTypeCount);
                        }
                    } else {
                        Log.d("Debug", "Document for member " + memberId + " does not exist.");
                        checkIfAllMembersCompleted(completedMembers, totalMembers, pillTypeCount);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to fetch data for member: " + memberId, e);
                    checkIfAllMembersCompleted(completedMembers, totalMembers, pillTypeCount);
                });
    }


    private void fetchPillTypeDataFromDate(String memberId, String date, Map<Integer, Integer> pillTypeCount,
                                           AtomicInteger completedDates, int totalDates,
                                           AtomicInteger completedMembers, int totalMembers) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d("FirestoreDebug", "Fetching pill data for memberId: " + memberId + ", date: " + date);
        // 날짜 컬렉션 내 모든 문서 조회
        db.collection("FamilyMember")
                .document(memberId)
                .collection(date)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot drugDocument : querySnapshot.getDocuments()) {
                        Log.d("FirestoreDebug", "Fetched documents for date: " + date + ", Count: " + querySnapshot.size());
                        Object pillTypeObj = drugDocument.get("pillType");

                        // 약 정보 누적
                        if (pillTypeObj instanceof Number) {
                            int pillType = ((Number) pillTypeObj).intValue();
                            pillTypeCount.put(pillType, pillTypeCount.getOrDefault(pillType, 0) + 1);
                            Log.d("Firestore", "Pill type: " + pillType + ", Count: " + pillTypeCount.get(pillType));
                        }
                    }

                    if (completedDates.incrementAndGet() == totalDates) {
                        Log.d("FirestoreDebug", "All dates processed for memberId: " + memberId);
                        checkIfAllMembersCompleted(completedMembers, totalMembers, pillTypeCount);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to fetch data for date: " + date, e);
                    if (completedDates.incrementAndGet() == totalDates) {
                        Log.d("FirestoreDebug", "All dates processed for memberId: " + memberId + " (with errors)");
                        checkIfAllMembersCompleted(completedMembers, totalMembers, pillTypeCount);
                    }
                });
    }


    private void checkIfAllMembersCompleted(AtomicInteger completedMembers, int totalMembers, Map<Integer, Integer> pillTypeCount) {
        int currentCompletedMembers = completedMembers.incrementAndGet();
        Log.d("FirestoreDebug", "Completed members: " + currentCompletedMembers + "/" + totalMembers);
        Log.d("FirestoreDebug", "Current pillTypeCount: " + pillTypeCount);

        // 모든 멤버 데이터 처리가 끝난 경우 확인
        if (currentCompletedMembers == totalMembers) {
            Log.d("PieChart", "Final pillTypeCount: " + pillTypeCount);
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
                case 101: label = "감기약"; break;
                case 102: label = "해열제"; break;
                case 103: label = "심장약"; break;
                case 104: label = "위장약"; break;
                case 105: label = "진통제"; break;
                case 106: label = "항생제"; break;
                case 107: label = "피임약"; break;
                case 108: label = "항우울제"; break;
                case 109: label = "항암제"; break;
                case 110: label = "정신과약"; break;
                case 111: label = "당뇨병약"; break;
                case 112: label = "고혈압약"; break;
                case 113: label = "호흡기약"; break;
                case 201: label = "비타민"; break;
                case 202: label = "유산균"; break;
                case 203: label = "단백질보충제"; break;
                case 204: label = "홍삼"; break;
                case 205: label = "소화제"; break;
                case 206: label = "오메가-3"; break;
                case 207: label = "콜라겐"; break;
                case 208: label = "철분제"; break;
                case 209: label = "기타보조제"; break;
                default: label = "기타약";
            }

            entries.add(new PieEntry(count, label));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        // 퍼센트를 슬라이스 내부에만 표시
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);

        // PieData 생성 및 설정
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        pieChart.setData(data);

        // 약 이름을 슬라이스에서 제거 (범례에만 표시)
        pieChart.setDrawEntryLabels(false);

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
        //pieChart.setEntryLabelTextSize(12f);


        Legend legend = pieChart.getLegend();
        legend.setTextSize(11f); // 범례 텍스트 크기 설정
        legend.setTextColor(Color.BLACK); // 범례 텍스트 색상
        legend.setForm(Legend.LegendForm.CIRCLE); // 범례 모양 (원형)
        legend.setXEntrySpace(10f); // 항목 간 간격 (좌우)
        legend.setYEntrySpace(5f); // 항목 간 간격 (상하)
        legend.setOrientation(Legend.LegendOrientation.VERTICAL); // 가로 정렬
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
    }

}