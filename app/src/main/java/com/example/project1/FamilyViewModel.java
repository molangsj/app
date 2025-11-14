package com.example.project1;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class FamilyViewModel extends ViewModel {
    private final MutableLiveData<List<FamilyMember>> familyMembers = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<FamilyMember>> getFamilyMembers() {
        return familyMembers;
    }

    public void addFamilyMember(FamilyMember member) {
        List<FamilyMember> currentList = familyMembers.getValue();
        if (currentList != null) {
            currentList.add(member);
            familyMembers.setValue(currentList);
        }
    }
}
