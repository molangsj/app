// app/src/main/java/com/example/project_yakkuk/Calendar_Adapter.java

package com.example.project1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Date;

public class Calendar_Adapter extends RecyclerView.Adapter<Calendar_Adapter.CalendarViewHolder> {

    private final Context context;
    private List<Date> days;
    private Set<String> medicineDates; // Set for efficient lookup
    private OnItemClickListener onItemClickListener;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

    public Calendar_Adapter(Context context, List<Date> days){
        this.context = context;
        this.days = days;
        this.medicineDates = new HashSet<>();
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        Date date = days.get(position);
        if (date == null) {
            // 빈 칸 처리 (이전 달 또는 다음 달 날짜)
            holder.dayText.setText("");
            holder.pillIcon.setVisibility(View.GONE);
        } else {
            String dateStr = sdf.format(date);
            holder.dayText.setText(String.valueOf(getDayOfMonth(date)));

            if (medicineDates.contains(dateStr)) {
                holder.pillIcon.setVisibility(View.VISIBLE); // 약 아이콘 표시
            } else {
                holder.pillIcon.setVisibility(View.GONE); // 약 아이콘 숨기기
            }

            holder.itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(date);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    // 날짜가 약을 포함하는지 설정하는 메서드
    public void setMedicineDates(List<Date> medicineDatesList) {
        this.medicineDates.clear();
        for (Date date : medicineDatesList) {
            String dateStr = sdf.format(date);
            this.medicineDates.add(dateStr);
        }
        notifyDataSetChanged();
    }

    private int getDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    // 새로운 달력 날짜 리스트로 업데이트
    public void updateDays(List<Date> newDays) {
        this.days = newDays;
        notifyDataSetChanged();
    }

    // 아이템 클릭 리스너 설정
    public void setOnItemClickListener(OnItemClickListener listener){
        this.onItemClickListener = listener;
    }

    // 아이템 클릭 이벤트 인터페이스
    public interface OnItemClickListener {
        void onItemClick(Date date);
    }

    // 뷰홀더 클래스
    static class CalendarViewHolder extends RecyclerView.ViewHolder{
        TextView dayText;
        ImageView pillIcon;

        public CalendarViewHolder(@NonNull View itemView){
            super(itemView);
            dayText = itemView.findViewById(R.id.tv_day); // item_day.xml에 있는 TextView ID
            pillIcon = itemView.findViewById(R.id.pill_icon); // item_day.xml에 있는 ImageView ID
        }
    }
}
