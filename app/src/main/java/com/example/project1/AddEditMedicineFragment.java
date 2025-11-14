// AddEditMedicineFragment.java
package com.example.project1;

import androidx.core.content.ContextCompat;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.annotation.SuppressLint;
import android.os.Build;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.project1.databinding.FragmentAddEditMedicineBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddEditMedicineFragment extends Fragment {

    private static final String ARG_MEDICINE = "medicine";
    private static final String ARG_USERNAME = "username";

    private FragmentAddEditMedicineBinding binding;
    private MedicineData medicine;
    private String username;
    private boolean alarmEnabled;
    private static int uniqueRequestCode = 0;
    private String dateStr;
    private boolean favorite;
    private int pillType; // 101~199: 처방 의약품, 201~299: 일반 의약품
    private int selectedIconResId = R.drawable.pill_default; // 기본 아이콘 설정
    private List<String> selectedDays;
    private ImageView addMedicineImageView;
    private MedicineIconManager iconManager;

    private FirestoreHelper firestoreHelper;

    private List<String> alarmTimes;

    // 카테고리 목록 정의
    private final String[] prescriptionCategories = {
            "감기약 (Cold Medicine)",              // 101
            "해열제 (Antipyretics)",               // 102
            "심장 약 (Heart Medication)",          // 103
            "위장 약 (Gastrointestinal Medication)",// 104
            "진통제 (Pain Relievers)",             // 105
            "항생제 (Antibiotics)",                // 106
            "피임약 (Birth Control)",              // 107
            "항우울제 (Antidepressants)",          // 108
            "항암제 (Chemotherapy)",               // 109
            "정신과 약 (Psychiatric Medication)",   // 110
            "당뇨병 약 (Diabetes Medication)",      // 111
            "고혈압 약 (Hypertension Medication)",  // 112
            "호흡기 약 (Respiratory Medication)"    // 113
    };

    private final int[] prescriptionTypeValues = {
            101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113
    };

    private final String[] nonPrescriptionCategories = {
            "비타민 (Vitamins)",                   // 201
            "유산균 (Probiotics)",                 // 202
            "단백질 보충제 (Protein Supplements)",  // 203
            "홍삼 (Red Ginseng)",                   // 204
            "소화제 (Digestives)",                  // 205
            "오메가-3 (Omega-3)",                   // 206
            "콜라겐 (Collagen)",                    // 207
            "철분제 (Iron Supplements)",            // 208
            "기타 (Others)"                         // 209
    };

    private final int[] nonPrescriptionTypeValues = {
            201, 202, 203, 204, 205, 206, 207, 208, 209
    };

    public AddEditMedicineFragment() {
        // 생성자
    }

    public static AddEditMedicineFragment newInstance(MedicineData medicine, String username) {
        AddEditMedicineFragment fragment = new AddEditMedicineFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MEDICINE, medicine);
        args.putString(ARG_USERNAME, username);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d("AddEditMedicineFragment", "onCreateView 시작");
        try {
            binding = FragmentAddEditMedicineBinding.inflate(inflater, container, false);
            firestoreHelper = new FirestoreHelper();
            Log.d("AddEditMedicineFragment", "Binding 및 FirestoreHelper 초기화 완료");

            addMedicineImageView = binding.imgSelectedIcon;
            iconManager = new MedicineIconManager(requireContext());

            if (getArguments() != null) {
                medicine = (MedicineData) getArguments().getSerializable(ARG_MEDICINE);
                username = getArguments().getString(ARG_USERNAME);
                Log.d("AddEditMedicineFragment", "Arguments received: username=" + username);

                if (username == null || username.isEmpty()) {
                    Log.e("AddEditMedicineFragment", "Username is null or empty");
                    Toast.makeText(getContext(), "유효하지 않은 사용자 이름입니다.", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                    return binding.getRoot();
                }

                if (medicine != null && (medicine.getDateStr() == null || medicine.getDateStr().isEmpty())) {
                    String currentDateStr = getCurrentDate();
                    medicine.setDateStr(currentDateStr);
                    Log.d("AddEditMedicineFragment", "dateStr이 누락되어 현재 날짜로 설정됨: " + currentDateStr);
                }
            } else {
                Log.e("AddEditMedicineFragment", "Arguments are null");
                Toast.makeText(getContext(), "필수 정보가 누락되었습니다.", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
                return binding.getRoot();
            }

            setupUI();
            binding.btnSelectAlarm.setOnClickListener(v -> handleInitialAlarmButtonClick());
            binding.addNotificationButton.setOnClickListener(v -> addAlarmTime());
            addMedicineImageView.setOnClickListener(v -> openIconSelectionDialog());

            Log.d("AddEditMedicineFragment", "onCreateView 종료");
            return binding.getRoot();
        } catch (Exception e) {
            Log.e("AddEditMedicineFragment", "onCreateView 중 오류 발생", e);
            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }

    private void setupUI() {
        initializeCategorySpinners();
        if (medicine != null) {
            // 수정 모드
            binding.addMedicineName.setText(medicine.getPillName());
            binding.addMedicineMemo.setText(medicine.getNotes());
            binding.addMedicationCaution.setText(medicine.getCaution());

            // Firestore에서 불러온 iconResId 사용 (iconManager 호출 제거)
            binding.imgSelectedIcon.setImageResource(medicine.getIconResId());
            selectedIconResId = medicine.getIconResId();  // Firestore 반영값 사용

            alarmTimes = (medicine.getAlarmTimes() != null) ? medicine.getAlarmTimes() : new ArrayList<>();
            setDaysOfWeekToggle(medicine.getDaysOfWeek());

            alarmEnabled = medicine.isAlarmEnabled();
            favorite = medicine.isFavorite();

            pillType = medicine.getPillType();
            if (pillType >= 101 && pillType <= 113) {
                selectPrescriptionButton();
                int index = getCategoryIndex(prescriptionTypeValues, pillType);
                if (index != -1) {
                    binding.spinnerPrescriptionCategories.setSelection(index, false);
                }
                binding.spinnerPrescriptionCategories.setVisibility(View.VISIBLE);
                binding.spinnerNonPrescriptionCategories.setVisibility(View.GONE);
            } else if (pillType >= 201 && pillType <= 209) {
                selectNonPrescriptionButton();
                int index = getCategoryIndex(nonPrescriptionTypeValues, pillType);
                if (index != -1) {
                    binding.spinnerNonPrescriptionCategories.setSelection(index, false);
                }
                binding.spinnerNonPrescriptionCategories.setVisibility(View.VISIBLE);
                binding.spinnerPrescriptionCategories.setVisibility(View.GONE);
            }

            for (String time : alarmTimes) {
                addAlarmButton(time, false);
            }

            // 수정 모드에서는 힌트 버튼 숨기기
            binding.btnSelectAlarm.setVisibility(View.GONE);
        } else {
            // 추가 모드
            alarmTimes = new ArrayList<>();
            setAllDaysOfWeekToggle(true);
            // 추가 모드는 Firestore 값 없음, selectedIconResId=기본값 그대로
            binding.imgSelectedIcon.setImageResource(selectedIconResId);
        }

        binding.addMedicineBtnPrescription.setOnClickListener(v -> {
            selectPrescriptionButton();
            binding.spinnerPrescriptionCategories.setVisibility(View.VISIBLE);
            binding.spinnerNonPrescriptionCategories.setVisibility(View.GONE);
            if (prescriptionTypeValues.length > 0) {
                binding.spinnerPrescriptionCategories.setSelection(0, false);
                pillType = prescriptionTypeValues[0];
            }
        });

        binding.addMedicineBtnNonPrescription.setOnClickListener(v -> {
            selectNonPrescriptionButton();
            binding.spinnerPrescriptionCategories.setVisibility(View.GONE);
            binding.spinnerNonPrescriptionCategories.setVisibility(View.VISIBLE);
            if (nonPrescriptionTypeValues.length > 0) {
                int othersIndex = nonPrescriptionTypeValues.length - 1;
                binding.spinnerNonPrescriptionCategories.setSelection(othersIndex, false);
                pillType = nonPrescriptionTypeValues[othersIndex];
            }
        });

        binding.imgSelectedIcon.setOnClickListener(v -> {
            IconSelectionDialog dialog = new IconSelectionDialog(getContext(), iconResId -> {
                binding.imgSelectedIcon.setImageResource(iconResId);
                selectedIconResId = iconResId;
            });
            dialog.show();
        });

        binding.addMedicineAdd.setOnClickListener(v -> saveMedicine());
    }


    private void openIconSelectionDialog() {
        IconSelectionDialog dialog = new IconSelectionDialog(getContext(), new IconSelectionDialog.IconSelectionListener() {
            @Override
            public void onIconSelected(int iconResId) {
                selectedIconResId = iconResId;
                addMedicineImageView.setImageResource(selectedIconResId);
                Log.d("AddEditMedicineFragment", "아이콘이 선택됨: " + iconResId);
            }
        });
        dialog.show();
    }

    private void initializeCategorySpinners() {
        // 처방 의약품 Spinner 설정
        ArrayAdapter<String> prescriptionAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                prescriptionCategories
        );
        prescriptionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPrescriptionCategories.setAdapter(prescriptionAdapter);
        binding.spinnerPrescriptionCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pillType = prescriptionTypeValues[position];
                Log.d("AddEditMedicineFragment", "Selected prescription category: " + prescriptionCategories[position] + " with pillType: " + pillType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 기본값 설정
                if (prescriptionTypeValues.length > 0) {
                    pillType = prescriptionTypeValues[0];
                }
                Log.d("AddEditMedicineFragment", "Nothing selected in prescription spinner");
            }
        });

        // 일반 의약품 Spinner 설정
        ArrayAdapter<String> nonPrescriptionAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                nonPrescriptionCategories
        );
        nonPrescriptionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerNonPrescriptionCategories.setAdapter(nonPrescriptionAdapter);
        binding.spinnerNonPrescriptionCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pillType = nonPrescriptionTypeValues[position];
                Log.d("AddEditMedicineFragment", "Selected non-prescription category: " + nonPrescriptionCategories[position] + " with pillType: " + pillType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 기본값 설정
                if (nonPrescriptionTypeValues.length > 0) {
                    pillType = nonPrescriptionTypeValues[0];
                }
                Log.d("AddEditMedicineFragment", "Nothing selected in non-prescription spinner");
            }
        });
    }

    // 카테고리 목록에서 선택된 pillType 인덱스 반환
    private int getCategoryIndex(int[] types, int selectedType) {
        for (int i = 0; i < types.length; i++) {
            if (types[i] == selectedType) {
                return i;
            }
        }
        return -1;
    }

    // 초기 힌트 버튼 클릭 시 처리
    private void handleInitialAlarmButtonClick() {
        addAlarmTime(true);
    }

    private void addAlarmTime(boolean isInitial) {
        TimePickerDialogFragment dialog = new TimePickerDialogFragment();
        dialog.setOnTimeSelectedListener((selectedHour, selectedMinute) -> {
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
            if (alarmTimes.contains(formattedTime)) {
                Toast.makeText(getContext(), "이미 추가된 시간입니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            alarmTimes.add(formattedTime);
            Toast.makeText(getContext(), "알람 시간 추가됨: " + formattedTime, Toast.LENGTH_SHORT).show();
            addAlarmButton(formattedTime, isInitial);

            if (medicine != null) {
                setAlarm(username, medicine.getDateStr(), pillNameInput(), formattedTime);
            } else {
                String currentDateStr = getCurrentDate();
                setAlarm(username, currentDateStr, pillNameInput(), formattedTime);
            }

            if (isInitial) {
                binding.btnSelectAlarm.setVisibility(View.GONE);
            }
        });
        dialog.show(getParentFragmentManager(), "time_picker_dialog");
    }


    private void addAlarmTime() {
        addAlarmTime(false);
    }

    private void addAlarmButton(String time, boolean isInitial) {
        try {
            Log.d("AddEditMedicineFragment", "Adding alarm button: " + time);

            LinearLayout alarmLayout = new LinearLayout(requireContext());
            alarmLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams alarmLayoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            alarmLayoutParams.setMargins(0, 8, 0, 8);
            alarmLayout.setLayoutParams(alarmLayoutParams);

            Button timeButton = new Button(requireContext());
            LinearLayout.LayoutParams timeButtonParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            timeButton.setLayoutParams(timeButtonParams);
            timeButton.setText(time);
            timeButton.setBackgroundResource(R.drawable.radius_button_90);
            timeButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
            timeButton.setAllCaps(false);

            Button deleteButton = new Button(requireContext());
            LinearLayout.LayoutParams deleteButtonParams = new LinearLayout.LayoutParams(
                    (int) getResources().getDimension(R.dimen.delete_button_size),
                    (int) getResources().getDimension(R.dimen.delete_button_size)
            );
            deleteButton.setLayoutParams(deleteButtonParams);
            deleteButton.setBackgroundResource(R.drawable.ic_delete);
            deleteButton.setText("");
            deleteButton.setContentDescription("삭제 버튼");

            deleteButton.setOnClickListener(v -> {
                int index = alarmTimes.indexOf(time);
                if (index != -1) {
                    if (medicine != null) {
                        // 수정 모드: Firestore에서 알람 삭제
                        firestoreHelper.removePillIsChecked(username, medicine.getDateStr(), pillNameInput(), index, new FirestoreHelper.StatusCallback() {
                            @Override
                            public void onStatusUpdated() {
                                // 알람 시간과 pillIsChecked 필드 삭제
                                alarmTimes.remove(index);
                                binding.timeSettingContainer.removeView(alarmLayout);
                                Log.d("AddEditMedicineFragment", "Alarm removed: " + time);
                                Toast.makeText(getContext(), "알람 시간 삭제됨: " + time, Toast.LENGTH_SHORT).show();

                                if (alarmTimes.isEmpty()) {
                                    binding.btnSelectAlarm.setVisibility(View.VISIBLE);
                                }

                                // pillIsChecked 필드 업데이트 (필요 시)
                                updatePillIsCheckedFields();
                            }

                            @Override
                            public void onStatusUpdateFailed(Exception e) {
                                Toast.makeText(getContext(), "알람 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                Log.e("AddEditMedicineFragment", "Failed to remove pillIsChecked", e);
                            }
                        });
                    } else {
                        // 추가 모드: Firestore에 저장되지 않은 알람이므로 로컬에서만 삭제
                        alarmTimes.remove(index);
                        binding.timeSettingContainer.removeView(alarmLayout);
                        Log.d("AddEditMedicineFragment", "Alarm removed locally: " + time);
                        Toast.makeText(getContext(), "알람 시간 삭제됨: " + time, Toast.LENGTH_SHORT).show();

                        if (alarmTimes.isEmpty()) {
                            binding.btnSelectAlarm.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    Toast.makeText(getContext(), "알람 시간을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    Log.e("AddEditMedicineFragment", "Alarm time not found in list: " + time);
                }
            });

            // 시간 수정 기능 (수정 모드에서만 동작)
            timeButton.setOnClickListener(v -> {
                TimePickerDialogFragment dialog = new TimePickerDialogFragment();
                dialog.setOnTimeSelectedListener((selectedHour, selectedMinute) -> {
                    String newTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    if (alarmTimes.contains(newTime)) {
                        Toast.makeText(getContext(), "이미 추가된 시간입니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int currentIndex = alarmTimes.indexOf(time);
                    if (currentIndex != -1 && currentIndex < 30) { // 최대 알람 수 정의
                        String oldTime = alarmTimes.get(currentIndex);
                        alarmTimes.set(currentIndex, newTime);
                        timeButton.setText(newTime);
                        Log.d("AddEditMedicineFragment", "Alarm time updated from " + oldTime + " to " + newTime);

                        // 기존 알람 취소
                        cancelAlarm(username, medicine.getDateStr(), oldTime);

                        // 새로운 알람 설정
                        setAlarm(username, medicine.getDateStr(), pillNameInput(), newTime);

                        // pillIsChecked 필드 업데이트
                        firestoreHelper.updatePillIsCheckedAt(username, medicine.getDateStr(), pillNameInput(), currentIndex, 0, new FirestoreHelper.StatusCallback() {
                            @Override
                            public void onStatusUpdated() {
                                firestoreHelper.updatePillIsCheckedAt(username, medicine.getDateStr(), pillNameInput(), currentIndex, 1, new FirestoreHelper.StatusCallback() {
                                    @Override
                                    public void onStatusUpdated() {
                                        Log.d("AddEditMedicineFragment", "pillIsChecked updated for new alarm time: " + newTime);
                                        Toast.makeText(getContext(), "알람 시간이 성공적으로 업데이트되었습니다.", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onStatusUpdateFailed(Exception e) {
                                        Log.e("AddEditMedicineFragment", "Failed to update pillIsChecked for new alarm time: " + newTime, e);
                                        Toast.makeText(getContext(), "알람 시간 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onStatusUpdateFailed(Exception e) {
                                Log.e("AddEditMedicineFragment", "Failed to update pillIsChecked for old alarm time: " + oldTime, e);
                            }
                        });
                    }
                });
                dialog.show(getParentFragmentManager(), "time_picker_dialog");
            });

            alarmLayout.addView(timeButton);
            alarmLayout.addView(deleteButton);

            int addButtonIndex = binding.timeSettingContainer.indexOfChild(binding.addNotificationButton);
            binding.timeSettingContainer.addView(alarmLayout, addButtonIndex);
        } catch (Exception e) {
            Log.e("AddEditMedicineFragment", "Error adding alarm button", e);
        }
    }

    private synchronized int getUniqueRequestCode() {
        return uniqueRequestCode++;
    }

    private void saveMedicine() {
        String pillNameInput = binding.addMedicineName.getText().toString().trim();
        String notesInput = binding.addMedicineMemo.getText().toString().trim();
        String cautionInput = binding.addMedicationCaution.getText().toString().trim();

        if (medicine == null) {
            alarmEnabled = true;
            favorite = false;
        } else {
            alarmEnabled = medicine.isAlarmEnabled();
            favorite = medicine.isFavorite();
        }

        List<String> missingFields = new ArrayList<>();
        if (TextUtils.isEmpty(pillNameInput)) missingFields.add("약 이름");
        if (pillType == 0) missingFields.add("약 종류");
        if (alarmTimes.isEmpty()) missingFields.add("알람 시간");

        if (!missingFields.isEmpty()) {
            String message = "필수 정보가 누락되었습니다: " + TextUtils.join(", ", missingFields);
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selectedDays = getSelectedDaysOfWeek();

        if (medicine == null) {
            // 추가 모드
            firestoreHelper.addMedicine(
                    username,
                    pillNameInput,
                    pillType,
                    alarmEnabled,
                    favorite,
                    alarmTimes,
                    notesInput,  // notes
                    selectedDays,
                    new FirestoreHelper.MedicationCallback() {
                        @Override
                        public void onMedicationAdded(MedicineData addedMedicine) {
                            firestoreHelper.addDateToFamilyMember(username, addedMedicine.getDateStr(), new FirestoreHelper.StatusCallback() {
                                @Override
                                public void onStatusUpdated() {
                                    // 날짜 필드 추가 성공 후 알람 설정
                                    for (String alarmTime : alarmTimes) {
                                        setAlarm(username, addedMedicine.getDateStr(), pillNameInput, alarmTime);
                                    }
                                    requireActivity().getSupportFragmentManager();
                                }

                                @Override
                                public void onStatusUpdateFailed(Exception e) {
                                    Toast.makeText(getContext(), "날짜 추가에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                    Log.e("AddEditMedicineFragment", "Failed to add date metadata: " + addedMedicine.getDateStr(), e);
                                }
                            });
                            // caution 업데이트
                            firestoreHelper.updateCaution(username, addedMedicine.getDateStr(), pillNameInput, cautionInput, new FirestoreHelper.StatusCallback() {
                                @Override
                                public void onStatusUpdated() {
                                    // icon 업데이트
                                    firestoreHelper.updateMedicineIcon(username, addedMedicine.getDateStr(), pillNameInput, selectedIconResId, new FirestoreHelper.StatusCallback() {
                                        @Override
                                        public void onStatusUpdated() {
                                            requireActivity().getSupportFragmentManager().popBackStack();
                                        }

                                        @Override
                                        public void onStatusUpdateFailed(Exception e) {
                                            requireActivity().getSupportFragmentManager().popBackStack();
                                        }
                                    });
                                }

                                @Override
                                public void onStatusUpdateFailed(Exception e) {
                                    requireActivity().getSupportFragmentManager().popBackStack();
                                }
                            });
                        }

                        @Override
                        public void onMedicationAddFailed(Exception e) {
                            Toast.makeText(getContext(), "약 추가 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        } else {
            // 수정 모드
            String dateStr = medicine.getDateStr();
            String oldPillName = medicine.getPillName();

            firestoreHelper.updateMedicineFields(
                    username,
                    dateStr,
                    oldPillName,
                    pillNameInput,
                    pillType,
                    alarmEnabled,
                    favorite,
                    alarmTimes,
                    notesInput,
                    new FirestoreHelper.StatusCallback() {
                        @Override
                        public void onStatusUpdated() {
                            // caution 업데이트 후 icon 업데이트
                            firestoreHelper.updateCaution(username, dateStr, pillNameInput, cautionInput, new FirestoreHelper.StatusCallback() {
                                @Override
                                public void onStatusUpdated() {
                                    firestoreHelper.updateMedicineIcon(username, dateStr, pillNameInput, selectedIconResId, new FirestoreHelper.StatusCallback() {
                                        @Override
                                        public void onStatusUpdated() {
                                            requireActivity().getSupportFragmentManager().popBackStack();
                                        }

                                        @Override
                                        public void onStatusUpdateFailed(Exception e) {
                                            requireActivity().getSupportFragmentManager().popBackStack();
                                        }
                                    });
                                }

                                @Override
                                public void onStatusUpdateFailed(Exception e) {
                                    requireActivity().getSupportFragmentManager().popBackStack();
                                }
                            });
                        }

                        @Override
                        public void onStatusUpdateFailed(Exception e) {
                            Toast.makeText(getContext(), "약 수정 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }
    }


    private void updatePillIsCheckedFields() {
        // 현재 alarmTimes 리스트에 따라 pillIsCheckedX 필드를 재설정
        for (int i = 0; i < alarmTimes.size() && i < 10; i++) {
            // 루프 변수 i의 복사본을 final로 선언
            final int index = i;
            String alarmTime = alarmTimes.get(index);
            String fieldName = "pillIsChecked" + (index + 1);

            // pillIsCheckedX 필드를 false로 초기화
            firestoreHelper.updatePillIsCheckedAt(username, medicine.getDateStr(), pillNameInput(), index, 0, new FirestoreHelper.StatusCallback() {
                @Override
                public void onStatusUpdated() {
                    Log.d("AddEditMedicineFragment", "pillIsChecked field updated: " + fieldName);

                    // pillIsChecked 값을 계산
                    int pillIsChecked = medicine.calculatePillIsChecked();

                    // 다시 업데이트 시도 (index 값을 포함하여 전달)
                    firestoreHelper.updatePillIsCheckedAt(username, medicine.getDateStr(), pillNameInput(), index, pillIsChecked, new FirestoreHelper.StatusCallback() {
                        @Override
                        public void onStatusUpdated() {
                            Log.d("AddEditMedicineFragment", "pillIsChecked updated: " + pillIsChecked);
                        }

                        @Override
                        public void onStatusUpdateFailed(Exception e) {
                            Log.e("AddEditMedicineFragment", "Failed to update pillIsChecked", e);
                        }
                    });
                }

                @Override
                public void onStatusUpdateFailed(Exception e) {
                    Log.e("AddEditMedicineFragment", "Failed to update pillIsChecked field: " + fieldName, e);
                }
            });
        }
    }


    private String pillNameInput() {
        return binding.addMedicineName.getText().toString().trim();
    }

    private void setAlarm(String username, String dateStr, String pillName, String alarmTime) { // username 기반
        try {
            String[] timeParts = alarmTime.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            Intent intent = new Intent(requireContext(), AlarmReceiver.class);
            intent.putExtra("username", username);
            intent.putExtra("dateStr", dateStr);
            intent.putExtra("pillName", pillName);
            intent.putExtra("alarmTime", alarmTime);



            int requestCode = getUniqueRequestCode();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
                Log.d("AddEditMedicineFragment", "Alarm set for: " + calendar.getTime() + " with pillName: " + pillName + " and alarmTime: " + alarmTime + " requestCode: " + requestCode);
            } else {
                Log.e("AddEditMedicineFragment", "AlarmManager is null");
            }
        } catch (Exception e) {
            Log.e("AddEditMedicineFragment", "Error setting alarm", e);
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private List<String> getSelectedDaysOfWeek() {
        selectedDays = new ArrayList<>();
        if (binding.toggleMon.isChecked()) selectedDays.add("Mon");
        if (binding.toggleTue.isChecked()) selectedDays.add("Tue");
        if (binding.toggleWed.isChecked()) selectedDays.add("Wed");
        if (binding.toggleThu.isChecked()) selectedDays.add("Thu");
        if (binding.toggleFri.isChecked()) selectedDays.add("Fri");
        if (binding.toggleSat.isChecked()) selectedDays.add("Sat");
        if (binding.toggleSun.isChecked()) selectedDays.add("Sun");
        return selectedDays;
    }

    private void setDaysOfWeekToggle(List<String> selectedDays) {
        if (selectedDays == null) {
            Log.e("AddEditMedicineFragment", "selectedDays is null");
        } else {
            Log.d("AddEditMedicineFragment", "Selected Days: " + selectedDays.toString());
        }
        binding.toggleMon.setChecked(selectedDays != null && selectedDays.contains("Mon"));
        binding.toggleTue.setChecked(selectedDays != null && selectedDays.contains("Tue"));
        binding.toggleWed.setChecked(selectedDays != null && selectedDays.contains("Wed"));
        binding.toggleThu.setChecked(selectedDays != null && selectedDays.contains("Thu"));
        binding.toggleFri.setChecked(selectedDays != null && selectedDays.contains("Fri"));
        binding.toggleSat.setChecked(selectedDays != null && selectedDays.contains("Sat"));
        binding.toggleSun.setChecked(selectedDays != null && selectedDays.contains("Sun"));
    }

    private void setAllDaysOfWeekToggle(boolean isChecked) {
        binding.toggleMon.setChecked(isChecked);
        binding.toggleTue.setChecked(isChecked);
        binding.toggleWed.setChecked(isChecked);
        binding.toggleThu.setChecked(isChecked);
        binding.toggleFri.setChecked(isChecked);
        binding.toggleSat.setChecked(isChecked);
        binding.toggleSun.setChecked(isChecked);
    }

    // 처방 의약품 버튼 선택 시 UI 업데이트
    private void selectPrescriptionButton() {
        binding.addMedicineBtnPrescription.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorSecondary));
        binding.addMedicineBtnPrescription.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        binding.addMedicineBtnNonPrescription.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        binding.addMedicineBtnNonPrescription.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorSecondaryVariant));

        binding.spinnerPrescriptionCategories.setVisibility(View.VISIBLE);
        binding.spinnerNonPrescriptionCategories.setVisibility(View.GONE);
    }

    // 일반 의약품 버튼 선택 시 UI 업데이트
    private void selectNonPrescriptionButton() {
        binding.addMedicineBtnNonPrescription.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorSecondary));
        binding.addMedicineBtnNonPrescription.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        binding.addMedicineBtnPrescription.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        binding.addMedicineBtnPrescription.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorSecondaryVariant));

        binding.spinnerPrescriptionCategories.setVisibility(View.GONE);
        binding.spinnerNonPrescriptionCategories.setVisibility(View.VISIBLE);
    }

    // Alarm을 취소하는 메서드 (수정 시 기존 알람 제거)
    private void cancelAlarm(String username, String dateStr, String alarmTime) {
        try {
            Intent intent = new Intent(requireContext(), AlarmReceiver.class);
            intent.putExtra("username", username);
            intent.putExtra("dateStr", dateStr);
            intent.putExtra("alarmTime", alarmTime);

            int requestCode = (username + alarmTime).hashCode();


            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                Log.d("AddEditMedicineFragment", "Alarm canceled for time: " + alarmTime);
            }
        } catch (Exception e) {
            Log.e("AddEditMedicineFragment", "Error canceling alarm", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
