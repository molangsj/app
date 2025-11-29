// FontSpinnerAdapter.java - 새 파일 생성
package com.example.project1;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

public class FontSpinnerAdapter extends ArrayAdapter<String> {
    private Context context;
    private String[] fontNames;

    public FontSpinnerAdapter(@NonNull Context context, String[] fontNames) {
        super(context, android.R.layout.simple_spinner_item, fontNames);
        this.context = context;
        this.fontNames = fontNames;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    private View getCustomView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View row = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        TextView label = row.findViewById(android.R.id.text1);
        label.setText(fontNames[position]);

        // 각 항목에 해당하는 폰트 적용
        Typeface typeface = getFontTypeface(position);
        if (typeface != null) {
            label.setTypeface(typeface);
        }

        return row;
    }

    private Typeface getFontTypeface(int position) {
        switch (position) {
            case 0: // 기본 폰트
                return ResourcesCompat.getFont(context, R.font.paperlogy_4regular);
            case 1: // 바탕체
                return ResourcesCompat.getFont(context, R.font.batang);
            case 2: // 한컴 미래펀
                return ResourcesCompat.getFont(context, R.font.hmfmpyun);
            case 3: // 한컴 흐림
                return ResourcesCompat.getFont(context, R.font.hmhmold);
            default:
                return Typeface.DEFAULT;
        }
    }
}