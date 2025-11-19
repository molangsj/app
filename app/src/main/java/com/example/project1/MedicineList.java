package com.example.project1;

import android.app.AlertDialog;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project1.databinding.FragmentMedicineListBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MedicineList extends Fragment implements
        MedicineAdapter.OnItemClickListener,
        MedicineAdapter.OnAlarmToggleListener,
        MedicineAdapter.OnFavoriteToggleListener {

    private FragmentMedicineListBinding binding;
    private List<MedicineData> medicineList;
    private MedicineAdapter adapter;

    private String dateStr;
    private String username;

    private FirestoreHelper firestoreHelper;

    public MedicineList() {
        // Required empty public constructor
    }

    public static MedicineList newInstance(String username) {
        MedicineList fragment = new MedicineList();
        Bundle args = new Bundle();
        args.putString("username", username);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMedicineListBinding.inflate(inflater, container, false);
        firestoreHelper = new FirestoreHelper();

        if (getArguments() != null) {
            username = getArguments().getString("username");
            Log.d("MedicineListFragment", "Arguments received: username=" + username);
        } else {
            username = "";
            Log.e("MedicineListFragment", "Arguments are null");
        }

        binding.addMedicineButton.setOnClickListener(v -> {
            navigateToAddMedicine();
        });

        setupRecyclerView();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (username != null && !username.isEmpty()) {
            Log.d("MedicineListFragment", "onResume: username=" + username);
            dateStr = getCurrentDate(); // 알람 설정에 사용할 오늘 날짜
            getMedicineListFromFirestore();  // "현재 복용 약" 목록 가져오기
        } else {
            Log.e("MedicineListFragment", "Invalid username: " + username);
            Toast.makeText(getContext(), "유효하지 않은 사용자 이름입니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        if (medicineList == null) {
            medicineList = new ArrayList<>();
        }
        adapter = new MedicineAdapter(requireContext(), medicineList, this, this, this);
        binding.medicineRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.medicineRecyclerView.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                MedicineData medicine = medicineList.get(position);

                LayoutInflater inflater = LayoutInflater.from(getContext());
                View dialogView = inflater.inflate(R.layout.dialog_edit_delete, null);

                TextView title = dialogView.findViewById(R.id.dialog_title);
                title.setText(medicine.getPillName());

                Button btnEdit = dialogView.findViewById(R.id.btn_edit);
                Button btnDelete = dialogView.findViewById(R.id.btn_delete);

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setView(dialogView)
                        .setCancelable(false);

                AlertDialog dialog = builder.create();

                btnEdit.setOnClickListener(v -> {
                    openEditMedicine(medicine);
                    dialog.dismiss();
                });

                btnDelete.setOnClickListener(v -> {
                    confirmDeleteMedicine(position, medicine);
                    dialog.dismiss();
                });

                dialog.show();
            }

        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(binding.medicineRecyclerView);
    }

    // 🔴 여기서부터: "오늘 날짜"가 아니라 currentMedications 컬렉션을 봄
    private void getMedicineListFromFirestore() {
        if (username == null || username.isEmpty()) {
            Log.e("MedicineListFragment", "Invalid username");
            Toast.makeText(getContext(), "유효하지 않은 사용자 이름입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        firestoreHelper.getCurrentMedications(username, new FirestoreHelper.MedicationListCallback() {
            @Override
            public void onMedicationListReceived(List<MedicineData> medications) {
                Log.d("MedicineListFragment", "Current medications received: " + medications.size());
                for (MedicineData medicine : medications) {
                    Log.d("MedicineListFragment", "Medicine: " + medicine.getPillName());
                }

                medications.sort((m1, m2) -> Boolean.compare(m2.isFavorite(), m1.isFavorite()));

                medicineList.clear();
                medicineList.addAll(medications);
                adapter.notifyDataSetChanged();
                updateUI();
            }

            @Override
            public void onMedicationListFailed(Exception e) {
                Log.e("MedicineListFragment", "Error getting current medications: ", e);
                binding.medicineRecyclerView.setVisibility(View.GONE);
                binding.emptyTextView.setVisibility(View.VISIBLE);
                binding.emptyTextView.setText("복용할 약이 없습니다. 약을 추가해보세요!");
            }
        });
    }
    // 🔴 여기까지 변경

    private void updateUI() {
        if (medicineList.isEmpty()) {
            binding.medicineRecyclerView.setVisibility(View.GONE);
            binding.emptyTextView.setVisibility(View.VISIBLE);
            binding.emptyTextView.setText("복용할 약이 없습니다. 약을 추가해보세요!");
        } else {
            binding.medicineRecyclerView.setVisibility(View.VISIBLE);
            binding.emptyTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAlarmToggled(String medicationId, boolean isEnabled) {
        if (medicationId == null || medicationId.isEmpty()) {
            Log.e("MedicineListFragment", "Invalid medicationId");
            return;
        }
        String dateStr = getCurrentDate();

        firestoreHelper.updateAlarmStatus(username, dateStr, medicationId, isEnabled, new FirestoreHelper.StatusCallback() {
            @Override
            public void onStatusUpdated() {
                Toast.makeText(getContext(), isEnabled ? "알람이 설정되었습니다." : "알람이 해제되었습니다.", Toast.LENGTH_SHORT).show();
                MedicineData updatedMedicine = findMedicineById(medicationId);
                if (updatedMedicine != null) {
                    if (isEnabled) {
                        setAlarm(updatedMedicine);
                    } else {
                        cancelAlarm(updatedMedicine);
                    }
                }
            }

            @Override
            public void onStatusUpdateFailed(Exception e) {
                Toast.makeText(getContext(), "알람 상태 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onFavoriteToggled(String medicationId, boolean isFavorite) {
        String dateStr = getCurrentDate();

        firestoreHelper.updateFavoriteStatus(
                username,
                dateStr,
                medicationId,
                isFavorite,
                new FirestoreHelper.StatusCallback() {
                    @Override
                    public void onStatusUpdated() {
                        getMedicineListFromFirestore();
                    }

                    @Override
                    public void onStatusUpdateFailed(Exception e) {
                        Toast.makeText(getContext(), "즐겨찾기 상태 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public void onItemClick(MedicineData medicine) {
        MedicineDetailDialog dialog = MedicineDetailDialog.newInstance(medicine, username);
        dialog.show(getParentFragmentManager(), "medicine_detail_dialog");
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void navigateToAddMedicine() {
        if (username == null || username.isEmpty()) {
            Toast.makeText(getContext(), "사용자 이름을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
            Log.e("MedicineListFragment", "Username is null or empty");
            return;
        }

        Log.d("MedicineListFragment", "Navigating to AddEditMedicineFragment with username: " + username);

        try {
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, AddEditMedicineFragment.newInstance(null, username));
            transaction.addToBackStack(null);
            transaction.commit();
            Log.d("MedicineListFragment", "Navigated to AddEditMedicineFragment successfully");
        } catch (Exception e) {
            Log.e("MedicineListFragment", "Error navigating to AddEditMedicineFragment", e);
            Toast.makeText(getContext(), "화면 전환 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openEditMedicine(MedicineData medicine) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, AddEditMedicineFragment.newInstance(medicine, username));
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void confirmDeleteMedicine(int position, MedicineData medicine) {
        if (medicine.getPillName() == null) {
            Toast.makeText(getContext(), "삭제할 약의 이름이 없습니다.", Toast.LENGTH_SHORT).show();
            adapter.notifyItemChanged(position);
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("삭제 확인")
                .setMessage(medicine.getPillName() + "을 정말 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    String dateStr = getCurrentDate();

                    firestoreHelper.deleteMedicine(
                            username,
                            dateStr,
                            medicine.getPillName(),
                            new FirestoreHelper.DeleteCallback() {
                                @Override
                                public void onMedicineDeleted() {
                                    Toast.makeText(getContext(), "약이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                    adapter.removeItem(position);
                                    updateUI();
                                    cancelAlarm(medicine);
                                }

                                @Override
                                public void onMedicineDeleteFailed(Exception e) {
                                    Toast.makeText(getContext(), "약 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                    adapter.notifyItemChanged(position);
                                }
                            }
                    );

                })
                .setNegativeButton("취소", (dialog, which) -> {
                    adapter.notifyItemChanged(position);
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private MedicineData findMedicineById(String medicationId) {
        for (MedicineData medicine : medicineList) {
            if (medicine.getPillName().equals(medicationId)) {
                return medicine;
            }
        }
        return null;
    }

    private void setAlarm(MedicineData medicine) {
        try {
            List<String> alarmTimes = medicine.getAlarmTimes();
            if (alarmTimes == null || alarmTimes.isEmpty()) {
                Log.e("MedicineListFragment", "No alarm times available for medicine: " + medicine.getPillName());
                return;
            }

            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e("MedicineListFragment", "AlarmManager is null");
                return;
            }

            // Android 12(S) 이상: 정확한 알람 권한 확인
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w("MedicineListFragment", "Cannot schedule exact alarms (permission not granted)");
                    Toast.makeText(requireContext(),
                            "정확한 알람 권한이 없어 알람을 설정할 수 없습니다.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            for (String alarmTime : alarmTimes) {
                String[] timeParts = alarmTime.split(":");
                if (timeParts.length != 2) {
                    Log.e("MedicineListFragment", "Invalid alarm time format: " + alarmTime);
                    continue;
                }

                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm", Locale.getDefault());
                String dateTimeStr = dateStr + " " + alarmTime;
                Date alarmDate = sdf.parse(dateTimeStr);

                if (alarmDate == null) {
                    Log.e("MedicineListFragment", "Invalid alarm date for: " + dateTimeStr);
                    continue;
                }

                long triggerAtMillis = alarmDate.getTime();
                if (triggerAtMillis <= System.currentTimeMillis()) {
                    triggerAtMillis += AlarmManager.INTERVAL_DAY;
                }

                Intent intent = new Intent(requireContext(), AlarmReceiver.class);
                intent.putExtra("username", username);
                intent.putExtra("dateStr", dateStr);
                intent.putExtra("pillName", medicine.getPillName());
                intent.putExtra("alarmTime", alarmTime);

                int requestCode = (medicine.getPillName() + alarmTime).hashCode();

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        requireContext(),
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                try {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                    );
                    Log.d("MedicineListFragment", "Alarm set for: " + alarmDate
                            + " with pillName: " + medicine.getPillName()
                            + " and alarmTime: " + alarmTime);
                } catch (SecurityException se) {
                    Log.e("MedicineListFragment", "SecurityException while setting exact alarm", se);
                    Toast.makeText(requireContext(),
                            "정확한 알람 권한이 없어 알람을 설정할 수 없습니다.",
                            Toast.LENGTH_SHORT).show();
                    // 정확 알람 권한이 없는 상태이므로 더 이상 반복할 필요 없음
                    break;
                }
            }
        } catch (Exception e) {
            Log.e("MedicineListFragment", "Error setting alarm", e);
        }
    }

    private void cancelAlarm(MedicineData medicine) {
        try {
            List<String> alarmTimes = medicine.getAlarmTimes();
            if (alarmTimes == null || alarmTimes.isEmpty()) {
                Log.e("MedicineListFragment", "No alarm times available to cancel for medicine: " + medicine.getPillName());
                return;
            }

            for (String alarmTime : alarmTimes) {
                Intent intent = new Intent(requireContext(), AlarmReceiver.class);
                intent.putExtra("username", username);
                intent.putExtra("dateStr", dateStr);
                intent.putExtra("pillName", medicine.getPillName());
                intent.putExtra("alarmTime", alarmTime);

                int requestCode = (medicine.getPillName() + alarmTime).hashCode();

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        requireContext(),
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                    Log.d("MedicineListFragment", "Alarm canceled for pill: " + medicine.getPillName() + " at " + alarmTime);
                }
            }
        } catch (Exception e) {
            Log.e("MedicineListFragment", "Error canceling alarm", e);
        }
    }
}
