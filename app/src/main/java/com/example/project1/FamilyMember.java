package com.example.project1;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FamilyMember {
    private String name;
    private int imageResId;
    private String docId;
    private Map<Integer, Integer> pillTypeCount;

    public FamilyMember(String docId) {
//        this.name = name;
        this.docId = docId;
        this.imageResId = imageResId;
        this.pillTypeCount = new HashMap<>();
    }

    public String getDocId() {
        return docId;
    }

    public int getImageResId() {
        return imageResId;
    }
    public void setDocId(String docId){
        this.docId = docId;
    }
    public Map<Integer, Integer> getPillTypeCount() {
        return pillTypeCount;
    }
    public void setPillTypeCount(Map<Integer, Integer> pillTypeCount) {
        this.pillTypeCount = pillTypeCount;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FamilyMember that = (FamilyMember) o;
        return name != null && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    public void setUsername(String s) {

    }
}
