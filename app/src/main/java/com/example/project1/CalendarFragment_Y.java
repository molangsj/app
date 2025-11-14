package com.example.project1;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.project1.databinding.FragmentCalendarBinding;
import com.example.project1.databinding.FragmentCalendarYBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CalendarFragment_Y#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CalendarFragment_Y extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private BottomSheetDialog dialog;
    private @NonNull FragmentCalendarYBinding binding;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public CalendarFragment_Y() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CalendarFragment_Y.
     */
    // TODO: Rename and change types and number of parameters
    public static CalendarFragment_Y newInstance(String param1, String param2) {
        CalendarFragment_Y fragment = new CalendarFragment_Y();
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
//        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        binding= FragmentCalendarYBinding.inflate(inflater, container, false);
        // Inflate the layout for this fragment
        binding.calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                if(selected){
                    String selectedDate="";
                    int month =date.getMonth();
                    String Month;
                    if(month<10) Month = "0"+date.getMonth();
                    else Month = ""+date.getMonth();

                    if(date.getDay()<10) {
                        selectedDate =  + date.getYear() + Month + 0 + date.getDay();
                    }
                    else{
                        selectedDate =  + date.getYear() + Month + date.getDay();
                    }
                    showBottomSheet(selectedDate);
                }
            }
        });


        return binding.getRoot();
    }
    @Override
    public void onDestroyView() {

        super.onDestroyView();
        binding = null; // ViewBinding 메모리 누수 방지
    }

    private void showBottomSheet(String date) {
        BottomSheetDialogFragment bottomSheet = MyBottomSheet.newInstance(date);
        bottomSheet.show(getChildFragmentManager(), "BottomSheetDialog");
    }
}