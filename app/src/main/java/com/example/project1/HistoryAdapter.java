package com.example.project1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project1.databinding.FragmentContainerBinding;
import com.example.project1.databinding.FragmentHistoryBinding;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private static final int TYPE_CALENDAR = 0;
    private static final int TYPE_CHART = 1;

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FragmentContainerBinding binding = FragmentContainerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Fragment fragment = (position == TYPE_CALENDAR) ? new CalendarFragment_Y() : new ChartFragment();
        FragmentActivity activity = (FragmentActivity) holder.itemView.getContext();//fragmentmanager가 activity에서 가져올 수 있어서 이렇게 activity 거쳐서 한다.
        FragmentManager manager = activity.getSupportFragmentManager();

        manager.beginTransaction()
                .replace(holder.fragmentContainerId, fragment)
                .commit();

    }

    @Override
    public int getItemCount() {
        return 2; // CalendarFragment와 ChartFragment 두 개 항목
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        int fragmentContainerId;

        public HistoryViewHolder(@NonNull FragmentContainerBinding binding) {
            super(binding.getRoot());

            // FragmentContainerView의 ID 설정
            fragmentContainerId = View.generateViewId();
            itemView.setId(fragmentContainerId);
        }
    }
}
