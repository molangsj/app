package com.example.project1;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class FamilyMember {

    // Firestore 문서에 있는 displayName (화면에 보여줄 이름)
    private String name;

    // 프로필 이미지 리소스 (필요 없다면 항상 0으로 둬도 됨)
    private int imageResId;

    // Firestore 문서 ID = username (내부 식별자)
    private String docId;

    private Map<Integer, Integer> pillTypeCount;

    public FamilyMember(String docId) {
        this.docId = docId;
        this.name = docId;   // 기본값: 이름 없으면 ID랑 같게
        this.pillTypeCount = new HashMap<>();
    }

    // 화면 표시용 이름
    public String getName() {
        return name != null ? name : docId;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Firestore 문서 ID (username)
    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }

    public Map<Integer, Integer> getPillTypeCount() {
        return pillTypeCount;
    }

    public void setPillTypeCount(Map<Integer, Integer> pillTypeCount) {
        this.pillTypeCount = pillTypeCount;
    }

    // equals: 내부 식별자인 docId 기준으로 비교 (중복 제거/삭제 시 안정적)
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof FamilyMember)) return false;
        FamilyMember that = (FamilyMember) o;
        return docId != null && docId.equals(that.docId);
    }

    @Override
    public int hashCode() {
        return docId != null ? docId.hashCode() : 0;
    }

}
