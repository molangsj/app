package com.example.project1;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

public class IconSelectionDialog {

    private Context context;
    private IconSelectionListener listener;

    // 아이콘 목록 (drawable 리소스 ID 배열)
    private int[] iconIds = {
            R.drawable.img_1, R.drawable.img_2, R.drawable.img_3, R.drawable.img_4,
            R.drawable.img_5, R.drawable.img_6, R.drawable.img_7, R.drawable.img_8,
            R.drawable.img_9, R.drawable.img_10, R.drawable.img_11, R.drawable.img_12,
            R.drawable.img_13, R.drawable.img_14, R.drawable.img_15, R.drawable.img_16
    };

    // iconNames 배열 제거

    public interface IconSelectionListener {
        void onIconSelected(int iconResId);
    }

    public IconSelectionDialog(Context context, IconSelectionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("아이콘 선택");

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_icon_selection, null);
        GridView gridView = view.findViewById(R.id.gridViewIcons);

        IconAdapter adapter = new IconAdapter(context, iconIds); // 생성자 수정
        gridView.setAdapter(adapter);

        // AlertDialog 객체 생성 및 참조 저장
        AlertDialog dialog = builder.setView(view)
                .setNegativeButton("취소", null)
                .create();

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View gridViewView, int position, long id) {
                int selectedIconResId = iconIds[position];
                Log.d("IconSelectionDialog", "Selected Icon ID: " + selectedIconResId);
                listener.onIconSelected(selectedIconResId);
                dialog.dismiss(); // 다이얼로그 닫기
            }
        });

        dialog.show();
    }
}
