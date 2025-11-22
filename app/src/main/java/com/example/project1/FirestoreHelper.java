package com.example.project1;

import static android.content.ContentValues.TAG;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirestoreHelper {

    private FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    // 날짜 메타데이터를 FamilyMember 문서에 추가하는 메서드 (username 문서에 필드 추가)
    public void addDateToFamilyMember(
            String username,
            String dateStr,
            StatusCallback callback
    ) {
        if (username == null || dateStr == null) {
            Log.e("FirestoreHelper", "username 또는 dateStr이 null입니다.");
            callback.onStatusUpdateFailed(new IllegalArgumentException("필수 필드가 null입니다."));
            return;
        }

        DocumentReference familyMemberRef = db.collection("FamilyMember")
                .document(username);

        Map<String, Object> dateField = new HashMap<>();
        dateField.put(dateStr, dateStr);

        familyMemberRef.update(dateField)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "날짜 필드가 성공적으로 추가되었습니다: " + dateStr);
                    callback.onStatusUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "날짜 필드 추가에 실패했습니다: " + dateStr, e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    // UID로 user 문서 조회
    public void getUserDocumentByUid(String uid, UserDataCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onUserDataFailed(new IllegalArgumentException("UID must not be null or empty"));
            return;
        }

        db.collection("FamilyMember")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "No user document found for UID: " + uid);
                        callback.onUserDataNotFound();
                    } else {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        Map<String, Object> data = document.getData();
                        Log.d(TAG, "User data retrieved for UID: " + uid);
                        callback.onUserDataReceived(data);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying user document for UID: " + uid, e);
                    callback.onUserDataFailed(e);
                });
    }

    // UID 기반 사용자 데이터 가져오기 (getUserDataByUID)
    public void getUserDataByUID(String uid, UserDataCallback callback) {
        if (uid == null || uid.isEmpty()) {
            Log.e("FirestoreHelper", "UID is null or empty");
            callback.onUserDataFailed(new IllegalArgumentException("UID must not be null or empty"));
            return;
        }

        db.collection("FamilyMember")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        callback.onUserDataReceived(document.getData());
                    } else {
                        callback.onUserDataNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Failed to query user data by UID", e);
                    callback.onUserDataFailed(e);
                });
    }

    // ✅ 수정된 메서드 - currentMedications도 함께 업데이트
    public void updateMedicineIcon(String username, String dateStr, String pillName, int iconResId, StatusCallback callback) {
        if (username == null || username.isEmpty() || dateStr == null || dateStr.isEmpty() || pillName == null || pillName.isEmpty()) {
            Log.e("FirestoreHelper", "Invalid parameters for updateMedicineIcon");
            callback.onStatusUpdateFailed(new IllegalArgumentException("username, dateStr, or pillName is invalid"));
            return;
        }

        DocumentReference pillRef = db.collection("FamilyMember")
                .document(username)
                .collection(dateStr)
                .document(pillName);

        Map<String, Object> updates = new HashMap<>();
        updates.put("iconResId", iconResId);

        pillRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "iconResId updated successfully for: " + pillName);

                    DocumentReference currentRef = db.collection("FamilyMember")
                            .document(username)
                            .collection("currentMedications")
                            .document(pillName);

                    currentRef.update(updates)
                            .addOnSuccessListener(v -> {
                                Log.d("FirestoreHelper", "iconResId synced to currentMedications for: " + pillName);
                                callback.onStatusUpdated();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FirestoreHelper", "Failed to sync iconResId to currentMedications for: " + pillName, e);
                                callback.onStatusUpdated();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Failed to update iconResId for: " + pillName, e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    public void getFamilyMemberData(String username, String dateStr, DataCallback callback) {
        if (username == null || username.isEmpty() || dateStr == null) {
            callback.onDataFetchFailed(new IllegalArgumentException("Username and dateStr must not be null or empty"));
            return;
        }

        DocumentReference pillRef = db.collection("FamilyMember")
                .document(username)
                .collection(dateStr)
                .document("pillName");

        pillRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> pillData = documentSnapshot.getData();
                        callback.onDataReceived(pillData);
                    } else {
                        Log.d("FirestoreHelper", "No such document");
                        callback.onDataNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Error fetching document", e);
                    callback.onDataFetchFailed(e);
                });
    }

    // 약 추가: 날짜별 컬렉션 + currentMedications 둘 다 동기화
    public void addMedicine(
            String username,
            String pillName,
            int pillType,
            boolean alarmEnabled,
            boolean favorite,
            List<String> alarmTimes,
            String notes,
            List<String> daysOfWeek,
            MedicationCallback callback
    ) {
        String dateStr = getCurrentDate();

        if (username == null || username.isEmpty() || pillName == null || pillName.isEmpty()) {
            Log.e("FirestoreHelper", "Username or pillName is null or empty");
            callback.onMedicationAddFailed(new IllegalArgumentException("Username and pillName must not be empty"));
            return;
        }

        DocumentReference pillRef = db.collection("FamilyMember")
                .document(username)
                .collection(dateStr)
                .document(pillName);

        Map<String, Object> medicineData = new HashMap<>();
        medicineData.put("pillName", pillName);
        medicineData.put("pillType", pillType);
        medicineData.put("alarmEnabled", alarmEnabled);
        medicineData.put("favorite", favorite);
        medicineData.put("alarmTimes", alarmTimes);
        medicineData.put("notes", notes);
        medicineData.put("daysOfWeek", daysOfWeek);
        medicineData.put("dateStr", dateStr);

        for (int i = 0; i < alarmTimes.size() && i < 10; i++) {
            String fieldName = "pillIsChecked" + (i + 1);
            medicineData.put(fieldName, 0);
        }

        medicineData.put("pillIsChecked", 0);

        pillRef.set(medicineData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "Medicine added successfully");

                    MedicineData addedMedicine = new MedicineData();
                    addedMedicine.setPillName(pillName);
                    addedMedicine.setPillType(pillType);
                    addedMedicine.setAlarmEnabled(alarmEnabled);
                    addedMedicine.setFavorite(favorite);
                    addedMedicine.setAlarmTimes(alarmTimes);
                    addedMedicine.setNotes(notes);
                    addedMedicine.setDaysOfWeek(daysOfWeek);
                    addedMedicine.setDateStr(dateStr);
                    for (int i = 0; i < alarmTimes.size() && i < 10; i++) {
                        addedMedicine.setPillIsCheckedAt(i, 0);
                    }

                    // currentMedications에도 동기화
                    upsertCurrentMedication(username, addedMedicine, new StatusCallback() {
                        @Override
                        public void onStatusUpdated() {
                            callback.onMedicationAdded(addedMedicine);
                        }

                        @Override
                        public void onStatusUpdateFailed(Exception e) {
                            Log.e("FirestoreHelper", "Failed to sync to currentMedications", e);
                            callback.onMedicationAddFailed(e);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Failed to add medicine", e);
                    callback.onMedicationAddFailed(e);
                });
    }

    public void updateCaution(String username, String dateStr, String pillName, String caution, StatusCallback callback) {
        if (username == null || dateStr == null || pillName == null) {
            callback.onStatusUpdateFailed(new IllegalArgumentException("Required fields are null"));
            return;
        }

        DocumentReference pillRef = db.collection("FamilyMember")
                .document(username)
                .collection(dateStr)
                .document(pillName);

        Map<String, Object> updates = new HashMap<>();
        updates.put("caution", caution);

        pillRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "Caution updated successfully for: " + pillName);

                    // currentMedications에도 동기화
                    DocumentReference currentRef = db.collection("FamilyMember")
                            .document(username)
                            .collection("currentMedications")
                            .document(pillName);

                    currentRef.update(updates)
                            .addOnSuccessListener(v ->
                                    Log.d("FirestoreHelper", "Caution synced to currentMedications for: " + pillName)
                            )
                            .addOnFailureListener(e ->
                                    Log.e("FirestoreHelper", "Failed to sync caution to currentMedications for: " + pillName, e)
                            );

                    callback.onStatusUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Failed to update caution for: " + pillName, e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    public void updateAlarmStatus(String username, String dateStr, String pillName, boolean alarmEnabled, StatusCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("alarmEnabled", alarmEnabled);

        db.collection("FamilyMember")
                .document(username)
                .collection(dateStr)
                .document(pillName)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "AlarmEnabled updated successfully for: " + pillName);
                    callback.onStatusUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Failed to update alarmEnabled for: " + pillName, e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    public void getUserDataByUsername(String username, UserDataCallback callback) {
        if (username == null || username.isEmpty()) {
            callback.onUserDataFailed(new IllegalArgumentException("Username must not be null or empty"));
            return;
        }

        db.collection("FamilyMember")
                .document(username)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        callback.onUserDataReceived(data);
                    } else {
                        callback.onUserDataNotFound();
                    }
                })
                .addOnFailureListener(callback::onUserDataFailed);
    }

    public void updateDisplayName(String username, String newDisplayName, StatusCallback callback) {
        if (username == null || username.isEmpty()) {
            callback.onStatusUpdateFailed(new IllegalArgumentException("Username must not be null or empty"));
            return;
        }

        DocumentReference userRef = db.collection("FamilyMember")
                .document(username);

        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", newDisplayName);

        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "displayName updated: " + newDisplayName);
                    callback.onStatusUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Failed to update displayName", e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    private String getCurrentDateTimeAsString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // "지금 복용 중인 약" 목록 가져오기
    public void getCurrentMedications(
            String username,
            MedicationListCallback callback
    ) {
        if (username == null || username.isEmpty()) {
            callback.onMedicationListFailed(new IllegalArgumentException("Username must not be null or empty"));
            return;
        }

        db.collection("FamilyMember")
                .document(username)
                .collection("currentMedications")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<MedicineData> medicines = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        MedicineData medicine = doc.toObject(MedicineData.class);
                        if (medicine != null) {
                            medicines.add(medicine);
                        }
                    }
                    Log.d("FirestoreHelper", "Fetched " + medicines.size() + " current medications");
                    callback.onMedicationListReceived(medicines);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Failed to get current medications", e);
                    callback.onMedicationListFailed(e);
                });
    }

    // currentMedications upsert (있으면 수정, 없으면 생성)
    public void upsertCurrentMedication(
            String username,
            MedicineData medicine,
            StatusCallback callback
    ) {
        if (username == null || username.isEmpty()
                || medicine == null || medicine.getPillName() == null) {
            callback.onStatusUpdateFailed(
                    new IllegalArgumentException("Invalid arguments for upsertCurrentMedication")
            );
            return;
        }

        DocumentReference currentRef = db.collection("FamilyMember")
                .document(username)
                .collection("currentMedications")
                .document(medicine.getPillName());

        currentRef.set(medicine)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "currentMedications upserted for: " + medicine.getPillName());
                    callback.onStatusUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Failed to upsert currentMedications for: " + medicine.getPillName(), e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    // currentMedications에서만 삭제
    public void deleteCurrentMedication(
            String username,
            String pillName,
            StatusCallback callback
    ) {
        if (username == null || username.isEmpty()
                || pillName == null || pillName.isEmpty()) {
            callback.onStatusUpdateFailed(
                    new IllegalArgumentException("Invalid arguments for deleteCurrentMedication")
            );
            return;
        }

        DocumentReference currentRef = db.collection("FamilyMember")
                .document(username)
                .collection("currentMedications")
                .document(pillName);

        currentRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "currentMedications deleted for: " + pillName);
                    callback.onStatusUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Failed to delete currentMedications for: " + pillName, e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    public void updateFavoriteStatus(
            String username,
            String dateStr,
            String pillName,
            boolean isFavorite,
            StatusCallback callback
    ) {
        if (username == null || dateStr == null || pillName == null) {
            Log.e("FirestoreHelper", "username, dateStr, or pillName is null");
            callback.onStatusUpdateFailed(new IllegalArgumentException("Required fields are null"));
            return;
        }

        DocumentReference pillRef = db.collection("FamilyMember")
                .document(username)
                .collection(dateStr)
                .document(pillName);

        Map<String, Object> updates = new HashMap<>();
        updates.put("favorite", isFavorite);

        pillRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "Favorite status updated successfully for: " + pillName);

                    // currentMedications에도 즐겨찾기 동기화
                    DocumentReference currentRef = db.collection("FamilyMember")
                            .document(username)
                            .collection("currentMedications")
                            .document(pillName);

                    currentRef.update(updates)
                            .addOnSuccessListener(v ->
                                    Log.d("FirestoreHelper", "Favorite synced to currentMedications for: " + pillName)
                            )
                            .addOnFailureListener(e ->
                                    Log.e("FirestoreHelper", "Failed to sync favorite to currentMedications for: " + pillName, e)
                            );

                    callback.onStatusUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Failed to update favorite status for: " + pillName, e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    public void updatePillIsCheckedAt(
            String username,
            String dateStr,
            String pillName,
            int alarmIndex,
            int isChecked,
            StatusCallback callback
    ) {
        if (username == null || dateStr == null || pillName == null) {
            callback.onStatusUpdateFailed(new IllegalArgumentException("Required fields are null"));
            return;
        }

        DocumentReference pillRef = db.collection("FamilyMember")
                .document(username)
                .collection(dateStr)
                .document(pillName);

        String pillIsCheckedField = "pillIsChecked" + (alarmIndex + 1);
        Log.d("FirestoreHelper", "업데이트할 필드: " + pillIsCheckedField);

        Map<String, Object> updates = new HashMap<>();
        updates.put(pillIsCheckedField, isChecked);

        pillRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "pillIsChecked 필드가 성공적으로 업데이트됨: " + pillIsCheckedField);
                    callback.onStatusUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "pillIsChecked 필드 업데이트 실패: " + pillIsCheckedField, e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    public void resetPillIsCheckedAt(
            String username,
            String dateStr,
            String pillName,
            int alarmIndex,
            StatusCallback callback
    ) {
        if (username == null || dateStr == null || pillName == null) {
            callback.onStatusUpdateFailed(new IllegalArgumentException("Required fields are null"));
            return;
        }

        DocumentReference pillRef = db.collection("FamilyMember")
                .document(username)
                .collection(dateStr)
                .document(pillName);

        String pillIsCheckedField = "pillIsChecked" + (alarmIndex + 1);
        Log.d("FirestoreHelper", "업데이트할 필드: " + pillIsCheckedField);

        Map<String, Object> updates = new HashMap<>();
        updates.put(pillIsCheckedField, 0);

        pillRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "pillIsChecked 필드가 성공적으로 업데이트됨: " + pillIsCheckedField);
                    callback.onStatusUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "pillIsChecked 필드 업데이트 실패: " + pillIsCheckedField, e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    // 날짜별 약 리스트 가져오기
    public void getMedicationsForDate(
            String username,
            String dateStr,
            MedicationListCallback callback
    ) {
        db.collection("FamilyMember")
                .document(username)
                .collection(dateStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<MedicineData> medicines = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        MedicineData medicine = doc.toObject(MedicineData.class);
                        if (medicine != null) {
                            List<String> alarmTimes = medicine.getAlarmTimes();
                            if (alarmTimes != null) {
                                for (int i = 0; i < alarmTimes.size() && i < 10; i++) {
                                    String fieldName = "pillIsChecked" + (i + 1);
                                    Long isCheckedLong = doc.getLong(fieldName);
                                    int isChecked = isCheckedLong != null ? isCheckedLong.intValue() : 0;
                                    medicine.setPillIsCheckedAt(i, isChecked);
                                }
                            }
                            medicines.add(medicine);
                        }
                    }
                    Log.d("FirestoreHelper", "Fetched " + medicines.size() + " medicines for dateStr: " + dateStr);
                    callback.onMedicationListReceived(medicines);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Failed to get medications for date: " + dateStr, e);
                    callback.onMedicationListFailed(e);
                });
    }

    // 약 수정
    public void updateMedicineFields(
            String username,
            String dateStr,
            String oldPillName,
            String newPillName,
            int pillType,
            boolean alarmEnabled,
            boolean favorite,
            List<String> alarmTimes,
            String notes,
            StatusCallback callback
    ) {
        if (username == null || dateStr == null || oldPillName == null) {
            callback.onStatusUpdateFailed(new IllegalArgumentException("Required fields are null"));
            return;
        }

        DocumentReference oldPillRef = db.collection("FamilyMember")
                .document(username)
                .collection(dateStr)
                .document(oldPillName);

        DocumentReference newPillRef = oldPillName.equals(newPillName) ?
                oldPillRef :
                db.collection("FamilyMember")
                        .document(username)
                        .collection(dateStr)
                        .document(newPillName);

        oldPillRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> existingData = documentSnapshot.getData();
                Map<String, Object> updates = new HashMap<>();

                if (existingData != null) {
                    updates.putAll(existingData);
                }

                updates.put("pillName", newPillName);
                updates.put("pillType", pillType);
                updates.put("alarmEnabled", alarmEnabled);
                updates.put("favorite", favorite);
                updates.put("alarmTimes", alarmTimes);
                updates.put("notes", notes);
                updates.put("dateStr", dateStr);

                MedicineData updatedMedicine = new MedicineData();
                updatedMedicine.setPillName(newPillName);
                updatedMedicine.setPillType(pillType);
                updatedMedicine.setAlarmEnabled(alarmEnabled);
                updatedMedicine.setFavorite(favorite);
                updatedMedicine.setAlarmTimes(alarmTimes);
                updatedMedicine.setNotes(notes);
                updatedMedicine.setDateStr(dateStr);

                if (alarmTimes != null && !alarmTimes.isEmpty()) {
                    for (int i = 0; i < 10; i++) {
                        String fieldName = "pillIsChecked" + (i + 1);
                        Object existingValue = existingData != null ? existingData.get(fieldName) : null;

                        if (i < alarmTimes.size()) {
                            if (existingValue != null) {
                                Long value = (Long) existingValue;
                                updates.put(fieldName, value);
                                updatedMedicine.setPillIsCheckedAt(i, value.intValue());
                            } else {
                                updates.put(fieldName, 0);
                                updatedMedicine.setPillIsCheckedAt(i, 0);
                            }
                        } else {
                            updates.put(fieldName, 0);
                        }
                    }
                } else {
                    for (int i = 0; i < alarmTimes.size() && i < 10; i++) {
                        String fieldName = "pillIsChecked" + (i + 1);
                        updates.put(fieldName, 0);
                    }
                }

                final MedicineData finalUpdatedMedicine = updatedMedicine;

                newPillRef.set(updates)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("FirestoreHelper", "Medicine updated successfully for: " + newPillName);

                            // currentMedications 동기화
                            upsertCurrentMedication(username, finalUpdatedMedicine, new StatusCallback() {
                                @Override
                                public void onStatusUpdated() {
                                    // 이름이 바뀐 경우, 예전 currentMedications 문서도 삭제
                                    if (!oldPillName.equals(newPillName)) {
                                        DocumentReference oldCurrentRef = db.collection("FamilyMember")
                                                .document(username)
                                                .collection("currentMedications")
                                                .document(oldPillName);
                                        oldCurrentRef.delete()
                                                .addOnSuccessListener(v ->
                                                        Log.d("FirestoreHelper", "Old currentMedications doc deleted: " + oldPillName)
                                                )
                                                .addOnFailureListener(e ->
                                                        Log.e("FirestoreHelper", "Failed to delete old currentMedications doc: " + oldPillName, e)
                                                );
                                    }

                                    if (!oldPillName.equals(newPillName)) {
                                        oldPillRef.delete()
                                                .addOnSuccessListener(v ->
                                                        Log.d("FirestoreHelper", "Old pill doc deleted: " + oldPillName)
                                                )
                                                .addOnFailureListener(e ->
                                                        Log.e("FirestoreHelper", "Failed to delete old pill doc: " + oldPillName, e)
                                                );
                                    }

                                    callback.onStatusUpdated();
                                }

                                @Override
                                public void onStatusUpdateFailed(Exception e) {
                                    Log.e("FirestoreHelper", "Failed to sync to currentMedications", e);
                                    callback.onStatusUpdateFailed(e);
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirestoreHelper", "Failed to update medicine for: " + newPillName, e);
                            callback.onStatusUpdateFailed(e);
                        });
            } else {
                Log.e("FirestoreHelper", "Medicine document does not exist: " + oldPillName);
                callback.onStatusUpdateFailed(new Exception("Medicine not found"));
            }
        }).addOnFailureListener(e -> {
            Log.e("FirestoreHelper", "Failed to fetch medicine document: " + oldPillName, e);
            callback.onStatusUpdateFailed(e);
        });
    }

    // Username 유효성 검사
    private boolean isValidUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        // 영문, 숫자, 언더스코어만 허용 (3-20자)
        return username.matches("^[a-zA-Z0-9_가-힣]{3,20}$");
    }

    // Username 중복 확인
    public void isUsernameAvailable(String username, UsernameCallback callback) {
        if (!isValidUsername(username)) {
            callback.onUsernameUnavailable("유효하지 않은 사용자 이름입니다.");
            return;
        }

        db.collection("FamilyMember")
                .document(username)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onUsernameExists();
                    } else {
                        callback.onUsernameAvailable();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Failed to check username availability", e);
                    callback.onUsernameCheckFailed(e);
                });
    }

    // Username 설정
    public void setUsername(String username, String email, String uid, SetUsernameCallback callback) {
        if (!isValidUsername(username)) {
            callback.onSetUsernameFailed(new Exception("유효하지 않은 사용자 이름입니다: " + username));
            return;
        }

        DocumentReference uidRef = db.collection("FamilyMember").document(uid);
        DocumentReference usernameRef = db.collection("FamilyMember").document(username);

        db.runTransaction(transaction -> {
            DocumentSnapshot usernameSnapshot = transaction.get(usernameRef);
            if (usernameSnapshot.exists()) {
                throw new FirebaseFirestoreException("Username 이미 존재합니다: " + username,
                        FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }

            DocumentSnapshot uidSnapshot = transaction.get(uidRef);

            Map<String, Object> userData;

            if (uidSnapshot.exists()) {
                userData = new HashMap<>(uidSnapshot.getData());
                userData.put("username", username);
                if (email != null) {
                    userData.put("email", email);
                }

                transaction.set(usernameRef, userData);
                transaction.delete(uidRef);
            } else {
                userData = new HashMap<>();
                userData.put("uid", uid);
                userData.put("username", username);
                if (email != null) {
                    userData.put("email", email);
                }

                transaction.set(usernameRef, userData);
            }

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d("FirestoreHelper", "Username이 성공적으로 설정되었습니다: " + username);
            callback.onSetUsernameSuccess();
        }).addOnFailureListener(e -> {
            if (e instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
                if (firestoreException.getCode() == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
                    callback.onSetUsernameFailed(new Exception("이미 사용 중인 사용자 이름입니다."));
                } else {
                    callback.onSetUsernameFailed(e);
                }
            } else {
                callback.onSetUsernameFailed(e);
            }
        });
    }

    // UID 문서 조회 및 username 반환
    public void getUsernameFromUid(String uid, UsernameCallback callback) {
        db.collection("FamilyMember")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        if (username == null || username.isEmpty()) {
                            callback.onUsernameFailed(new Exception("Username field is empty"));
                        } else {
                            callback.onUsernameReceived(username);
                        }
                    } else {
                        callback.onUsernameFailed(new Exception("User document does not exist"));
                    }
                })
                .addOnFailureListener(callback::onUsernameFailed);
    }

    // Username 문서에서 uid 확인
    public void checkUserExists(String uid, CheckUserCallback callback) {
        db.collection("FamilyMember")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String username = doc.getId();
                        callback.onUserExists(username);
                    } else {
                        callback.onUserDoesNotExist();
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    // Username 없이 UID 문서 생성
    public void addNewUserWithoutUsername(FirebaseUser user, StatusCallback callback) {
        String uid = user.getUid();
        String email = user.getEmail();
        String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;
        String rawDisplayName = user.getDisplayName();

        final String displayName = (rawDisplayName == null || rawDisplayName.isEmpty()) ? " " : rawDisplayName;

        DocumentReference uidDocRef = db.collection("FamilyMember").document(uid);

        uidDocRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("FirestoreHelper", "User document already exists for UID: " + uid);
                        callback.onStatusUpdateFailed(new Exception("User document already exists"));
                    } else {
                        String createdAt = getCurrentDateAsString();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("uid", uid);
                        if (email != null) {
                            userData.put("email", email);
                        }
                        userData.put("username", "");
                        userData.put("displayName", displayName);
                        userData.put("photoUrl", photoUrl);
                        userData.put(createdAt, createdAt);

                        uidDocRef.set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("FirestoreHelper", "UID 문서 생성 성공: " + uid);
                                    callback.onStatusUpdated();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FirestoreHelper", "UID 문서 생성 실패: " + uid, e);
                                    callback.onStatusUpdateFailed(e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Error checking existing UID: " + uid, e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    // 사용자 문서 존재 여부 확인 및 없으면 UID 문서 생성
    public void checkAndCreateUserDocument(FirebaseUser user, StatusCallback callback) {
        if (user == null) {
            Log.e(TAG, "checkAndCreateUserDocument: User is null");
            callback.onStatusUpdateFailed(new IllegalArgumentException("User cannot be null"));
            return;
        }

        String uid = user.getUid();
        DocumentReference userRef = db.collection("FamilyMember").document(uid);

        Log.d(TAG, "Checking if user document exists for UID: " + uid);

        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "User document exists for UID: " + uid);
                        callback.onStatusUpdateFailed(new Exception("User document already exists"));
                    } else {
                        Log.d(TAG, "User document does not exist for UID: " + uid + ". Creating document.");
                        addNewUserWithoutUsername(user, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user document for UID: " + uid, e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    // UID 문서 존재 확인 후 생성
    public void ensureUserDocumentExists(FirebaseUser user, StatusCallback callback) {
        String uid = user.getUid();

        DocumentReference uidDocRef = db.collection("FamilyMember").document(uid);

        uidDocRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "UID document already exists: " + uid);
                        callback.onStatusUpdated();
                    } else {
                        Log.d(TAG, "UID document does not exist for UID: " + uid + ". Creating document.");
                        addNewUserWithoutUsername(user, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user document for UID: " + uid, e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    // 현재 날짜 문자열 반환
    public static String getCurrentDateAsString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        Date now = new Date();
        return dateFormat.format(now);
    }

    // 약 삭제
    public void deleteMedicine(String username, String dateStr, String pillName, DeleteCallback callback) {
        DocumentReference pillRef = db.collection("FamilyMember")
                .document(username)
                .collection(dateStr)
                .document(pillName);

        pillRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "Medicine deleted successfully: " + pillName);

                    // currentMedications에서도 삭제
                    deleteCurrentMedication(username, pillName, new StatusCallback() {
                        @Override
                        public void onStatusUpdated() {
                            Log.d("FirestoreHelper", "Deleted from currentMedications: " + pillName);
                        }

                        @Override
                        public void onStatusUpdateFailed(Exception e) {
                            Log.e("FirestoreHelper", "Failed to delete from currentMedications: " + pillName, e);
                        }
                    });

                    callback.onMedicineDeleted();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Error deleting medicine", e);
                    callback.onMedicineDeleteFailed(e);
                });
    }

    // Username 가져오기
    public void getUsername(String uid, GetUsernameCallback callback) {
        if (uid == null || uid.isEmpty()) {
            Log.e("FirestoreHelper", "UID is null or empty");
            callback.onUsernameFailed(new IllegalArgumentException("UID must not be null or empty"));
            return;
        }

        db.collection("FamilyMember").whereEqualTo("uid", uid).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        String username = document.getId();
                        if (username.equals(uid)) {
                            callback.onUsernameFailed(new Exception("Username이 설정되지 않았습니다."));
                        } else {
                            callback.onUsernameReceived(username);
                        }
                    } else {
                        callback.onUsernameFailed(new Exception("User document does not exist"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Error getting username", e);
                    callback.onUsernameFailed(e);
                });
    }

    // UID 문서 존재 확인 후 생성

    // 콜백 인터페이스들
    public interface StatusCallback {
        void onStatusUpdated();
        void onStatusUpdateFailed(Exception e);
    }

    public interface MedicationCallback {
        void onMedicationAdded(MedicineData medicine);
        void onMedicationAddFailed(Exception e);
    }

    public interface MedicationListCallback {
        void onMedicationListReceived(List<MedicineData> medications);
        void onMedicationListFailed(Exception e);
    }

    public interface DataCallback {
        void onDataReceived(Map<String, Object> data);
        void onDataNotFound();
        void onDataFetchFailed(Exception e);
    }

    public interface UsernameCallback {
        void onUsernameAvailable();
        void onUsernameUnavailable(String reason);
        void onUsernameCheckFailed(Exception e);
        void onUsernameReceived(String username);
        void onUsernameFailed(Exception e);
        void onUsernameExists();
    }

    public interface SetUsernameCallback {
        void onSetUsernameSuccess();
        void onSetUsernameFailed(Exception e);
    }

    public interface UserDataCallback {
        void onUserDataReceived(Map<String, Object> data);
        void onUserDataNotFound();
        void onUserDataFailed(Exception e);
    }

    public interface CheckUserCallback {
        void onUserExists(String username);
        void onUserDoesNotExist();
        void onError(Exception e);
    }

    public interface DeleteCallback {
        void onMedicineDeleted();
        void onMedicineDeleteFailed(Exception e);
    }

    public interface GetUsernameCallback {
        void onUsernameReceived(String username);
        void onUsernameFailed(Exception e);
    }
}