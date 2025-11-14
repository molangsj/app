package com.example.project1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.project1.databinding.FamilyMemAddBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


public class family_mem_add extends Fragment {
    private FamilyMemAddBinding binding;
    private Animation slideDown, slideUp;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding= FamilyMemAddBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView extraInfo = binding.extraInfo;
        LinearLayout drugInfo = binding.drugInfo;
        slideDown = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down);
        slideUp = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
        binding.extraInfo.setOnClickListener(v -> handleExtraInfoClick());
        view.findViewById(R.id.pill_text).setOnClickListener(v -> navigateToDrugInfo());
        view.findViewById(R.id.sub_pill_text).setOnClickListener(v -> navigateToSubDrugInfo());
    }

    public void handleExtraInfoClick() {
        LinearLayout drugInfo = binding.drugInfo;
        if (drugInfo.getVisibility() == View.GONE) {
            drugInfo.startAnimation(slideDown);
            drugInfo.setVisibility(View.VISIBLE);
        } else {
            drugInfo.startAnimation(slideUp);
            drugInfo.setVisibility(View.GONE);
        }
    }

    public void navigateToDrugInfo() {
        getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.family_content, new Family_drug_info())
                .addToBackStack(null)
                .commit();
    }

    public void navigateToSubDrugInfo(){
        getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.family_content, new Family_Sub_Drug_Info())
                .addToBackStack(null)
                .commit();
    }

}
