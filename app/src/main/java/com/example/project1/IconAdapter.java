package com.example.project1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class IconAdapter extends BaseAdapter {

    private Context context;
    private int[] iconIds;

    public IconAdapter(Context context, int[] iconIds) {
        this.context = context;
        this.iconIds = iconIds;
    }

    @Override
    public int getCount() {
        return iconIds.length;
    }

    @Override
    public Object getItem(int position) {
        return iconIds[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // getView 메서드 수정
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_icon, parent, false);
            holder = new ViewHolder();
            holder.imgIcon = convertView.findViewById(R.id.imgIcon);
            // TextView 제거
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.imgIcon.setImageResource(iconIds[position]);
        // TextView 관련 코드 제거

        return convertView;
    }

    static class ViewHolder {
        ImageView imgIcon;
        // TextView 제거
    }
}
