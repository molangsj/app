// app/src/main/java/com/example/project_yakkuk/MedicineAdapter.java

package com.example.project1;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {

    // 인터페이스 정의
    public interface OnAlarmToggleListener {
        void onAlarmToggled(String medicationId, boolean isEnabled);
    }

    public interface OnFavoriteToggleListener {
        void onFavoriteToggled(String medicationId, boolean isFavorite);
    }

    public interface OnItemClickListener {
        void onItemClick(MedicineData medicine);
    }

    private List<MedicineData> medicineList;
    private OnFavoriteToggleListener favoriteToggleListener;
    private OnAlarmToggleListener alarmToggleListener;
    private OnItemClickListener itemClickListener;
    private Context context;
    private String username; // familyMemberId 대신 username 사용
    private String dateStr;

    public MedicineAdapter(Context context, List<MedicineData> medicineList,
                           OnFavoriteToggleListener favoriteToggleListener,
                           OnAlarmToggleListener alarmToggleListener,
                           OnItemClickListener itemClickListener) {
        this.context = context;
        this.medicineList = medicineList;
        this.favoriteToggleListener = favoriteToggleListener;
        this.alarmToggleListener = alarmToggleListener;
        this.itemClickListener = itemClickListener;
        sortMedicines();
    }

    // 즐겨찾기 우선 정렬
    private void sortMedicines() {
        Collections.sort(medicineList, new Comparator<MedicineData>() {
            @Override
            public int compare(MedicineData m1, MedicineData m2) {
                if (m1.isFavorite() && !m2.isFavorite()) {
                    return -1;
                } else if (!m1.isFavorite() && m2.isFavorite()) {
                    return 1;
                }
                return 0;
            }
        });
    }

    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
        MedicineData medicine = medicineList.get(position);

        holder.tvMedicineName.setText(medicine.getPillName());
        holder.imgMedicineIcon.setImageResource(medicine.getIconResId()); // Firestore에서 가져온 iconResId 사용

        // 알람 토글 아이콘 설정
        if (medicine.isAlarmEnabled()) {
            holder.btnAlarmToggle.setImageResource(R.drawable.ic_bell);
        } else {
            holder.btnAlarmToggle.setImageResource(R.drawable.ic_silent);
        }

        holder.btnAlarmToggle.setOnClickListener(v -> {
            boolean newStatus = !medicine.isAlarmEnabled();
            holder.btnAlarmToggle.setImageResource(newStatus ? R.drawable.ic_bell : R.drawable.ic_silent);
            if (alarmToggleListener != null) {
                alarmToggleListener.onAlarmToggled(medicine.getPillName(), newStatus);
            }
            medicine.setAlarmEnabled(newStatus);
            sortMedicines();
            notifyDataSetChanged();
        });

        // 즐겨찾기 토글 아이콘 설정
        if (medicine.isFavorite()) {
            holder.btnFavoriteToggle.setImageResource(R.drawable.ic_favorite);
        } else {
            holder.btnFavoriteToggle.setImageResource(R.drawable.ic_favorite_border);
        }

        holder.btnFavoriteToggle.setOnClickListener(v -> {
            boolean newFavorite = !medicine.isFavorite();
            holder.btnFavoriteToggle.setImageResource(newFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            if (favoriteToggleListener != null) {
                favoriteToggleListener.onFavoriteToggled(medicine.getPillName(), newFavorite);
            }
            medicine.setFavorite(newFavorite);
            sortMedicines();
            notifyDataSetChanged();
        });

        // 아이템 클릭 리스너 설정
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(medicine);
            }
        });
    }

    @Override
    public int getItemCount() {
        return medicineList.size();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < medicineList.size()) {
            medicineList.remove(position);
            notifyItemRemoved(position);
        }
    }

    class MedicineViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedicineName;
        ImageButton btnAlarmToggle;
        ImageButton btnFavoriteToggle;
        ImageView imgMedicineIcon;

        public MedicineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedicineName = itemView.findViewById(R.id.tvMedicineName);
            btnAlarmToggle = itemView.findViewById(R.id.btnAlarmToggle);
            btnFavoriteToggle = itemView.findViewById(R.id.btnFavoriteToggle);
            imgMedicineIcon = itemView.findViewById(R.id.imgMedicineIcon);
        }
    }

    // 삭제 메서드
    public void deleteItem(int position) {
        medicineList.remove(position);
        notifyItemRemoved(position);
    }

    // 수정 메서드 (실제 수정 로직은 프래그먼트에서 처리)
    public void editItem(int position) {
        // Not used in this implementation
        notifyItemChanged(position);
    }

    // 업데이트 메서드
    public void updateMedicine(MedicineData updatedMedicine) {
        for (int i = 0; i < medicineList.size(); i++) {
            if (medicineList.get(i).getAutoId() != null && medicineList.get(i).getAutoId().equals(updatedMedicine.getAutoId())) {
                medicineList.set(i, updatedMedicine);
                notifyItemChanged(i);
                break;
            }
        }
        sortMedicines();
        notifyDataSetChanged();
    }
}
