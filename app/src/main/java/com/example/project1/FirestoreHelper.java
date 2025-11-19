package com.example.project1;

import static android.content.ContentValues.TAG;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

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

    // 이메일로 사용자 문서 조회
    public void getUserDocumentByEmail(String email, UserDataCallback callback) {
        if (email == null || email.isEmpty()) {
            callback.onUserDataFailed(new IllegalArgumentException("Email must not be null or empty"));
            return;
        }

        db.collection("FamilyMember")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "No user document found for email: " + email);
                        callback.onUserDataNotFound();
                    } else {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        Map<String, Object> data = document.getData();
                        Log.d(TAG, "User data retrieved for email: " + email);
                        callback.onUserDataReceived(data);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying user document for email: " + email, e);
                    callback.onUserDataFailed(e);
                });
    }

    public void getUserDataByUsername(String username, UserDataCallback callback) {
        if (username == null || username.isEmpty()) {
            callback.onUserDataFailed(new IllegalArgumentException("Username must not be null or empty"));
            return;
        }

        DocumentReference userRef = db.collection("FamilyMember").document(username);
        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        callback.onUserDataReceived(data);
                    } else {
                        callback.onUserDataNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user data for username: " + username, e);
                    callback.onUserDataFailed(e);
                });
    }

    // UID 기반 사용자 데이터 가져오기(문서 ID=username)
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

    // 가입 시 UID를 문서 ID로 임시 생성
    public void addNewUserWithoutUsername(FirebaseUser user, StatusCallback callback) {
        if (user == null) {
            callback.onStatusUpdateFailed(new IllegalArgumentException("User cannot be null"));
            return;
        }

        String uid = user.getUid();
        String email = user.getEmail();
        String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;
        String rawDisplayName = user.getDisplayName();

        final String displayName = (rawDisplayName == null || rawDisplayName.isEmpty()) ? " " : rawDisplayName;

        DocumentReference memberRef = db.collection("FamilyMember").document(uid);

        memberRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("FirestoreHelper", "User document already exists for UID: " + uid);
                        callback.onStatusUpdateFailed(new Exception("User document already exists"));
                    } else {
                        String createdAt = getCurrentDateAsString();

                        Map<String, Object> memberData = new HashMap<>();
                        memberData.put("uid", uid);
                        memberData.put("email", email);
                        memberData.put("displayName", displayName);
                        memberData.put("photoUrl", photoUrl);
                        memberData.put(createdAt, createdAt);

                        memberRef.set(memberData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("FirestoreHelper", "New user added successfully with UID as document ID");
                                    callback.onStatusUpdated();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FirestoreHelper", "Failed to add new user to FamilyMember with UID as document ID", e);
                                    callback.onStatusUpdateFailed(e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Error checking existing UID: " + uid, e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    public static String getCurrentDateAsString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date now = new Date();
        return dateFormat.format(now);
    }

    // 사용자 존재 여부(UID 기반)
    public void checkUserExists(String uid, CheckUserCallback callback) {
        db.collection("FamilyMember")
                .whereEqualTo("uid", uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshots = task.getResult();
                        if (snapshots != null && !snapshots.isEmpty()) {
                            DocumentSnapshot doc = snapshots.getDocuments().get(0);
                            String username = doc.getId();
                            callback.onUserExists(username);
                        } else {
                            callback.onUserDoesNotExist();
                        }
                    } else {
                        Log.e(TAG, "Error checking user existence", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    // username 사용 가능 여부 확인
    public void isUsernameAvailable(String username, UsernameCallback callback) {
        if (username == null || username.isEmpty()) {
            callback.onUsernameCheckFailed(new IllegalArgumentException("Username must not be null or empty"));
            return;
        }

        DocumentReference userDocRef = db.collection("FamilyMember").document(username);
        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    callback.onUsernameExists();
                } else {
                    callback.onUsernameAvailable();
                }
            } else {
                Log.e(TAG, "Error checking username availability", task.getException());
                callback.onUsernameCheckFailed(task.getException());
            }
        });
    }

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
                    Log.e("FirestoreHelper", "Username 이미 존재: " + username, e);
                    callback.onSetUsernameFailed(new Exception("Username이 이미 존재합니다: " + username));
                } else {
                    Log.e("FirestoreHelper", "Username 설정 실패: " + username, e);
                    callback.onSetUsernameFailed(new Exception("Username 설정 실패: " + e.getMessage()));
                }
            } else {
                Log.e("FirestoreHelper", "Username 설정 실패: " + username, e);
                callback.onSetUsernameFailed(new Exception("Username 설정 실패: " + e.getMessage()));
            }
        });
    }

    private boolean isValidUsername(String username) {
        if (username == null || username.isEmpty() || username.length() > 1500) {
            return false;
        }
        String invalidChars = "/.#$[]";
        for (char c : invalidChars.toCharArray()) {
            if (username.indexOf(c) >= 0) {
                return false;
            }
        }
        return true;
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
                    callback.onStatusUpdated();
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
                .document("pillName"); // 실제 pillName으로 대체 필요

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

    // ---- 약 추가: 날짜별 컬렉션 + currentMedications 둘 다 동기화 ----
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
                    Log.d("FirestoreHelper", "Medicine added successfully (per-date)");

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

    // 약 삭제: 날짜별 컬렉션 + currentMedications 같이 정리
    public void deleteMedicine(String username, String dateStr, String pillName, DeleteCallback callback) {
        DocumentReference pillRef = db.collection("FamilyMember")
                .document(username)
                .collection(dateStr)
                .document(pillName);

        pillRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "Medicine deleted successfully: " + pillName);

                    // currentMedications에서도 삭제 (실패해도 앱 흐름 깨지지 않게 로그만)
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
                .addOnFailureListener(callback::onUsernameFailed);
    }

    // 즐겨찾기: 날짜별 + currentMedications 동기화
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
        DocumentReference pillRef = db.collection("FamilyMember")
                .document(username)
                .collection(dateStr)
                .document(pillName);

        String pillIsCheckedField = "pillIsChecked" + (alarmIndex + 1);

        Map<String, Object> updates = new HashMap<>();
        updates.put(pillIsCheckedField, isChecked);

        pillRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "pillIsChecked updated successfully for: " + pillName + " at index: " + (alarmIndex + 1));
                    callback.onStatusUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Failed to update pillIsChecked", e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    public void removePillIsChecked(
            String username,
            String dateStr,
            String pillName,
            int alarmIndex,
            StatusCallback callback
    ) {
        Log.d("FirestoreHelper", "removePillIsChecked 호출됨: username=" + username + ", dateStr=" + dateStr + ", pillName=" + pillName + ", alarmIndex=" + alarmIndex);

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

    // 날짜별 약 리스트 가져오기 (Calendar 등에서 계속 사용)
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

    // 약 수정: 날짜별 문서 + currentMedications 동기화
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

        DocumentReference newPillRef = oldPillName.equals(newPillName) ? oldPillRef :
                db.collection("FamilyMember")
                        .document(username)
                        .collection(dateStr)
                        .document(newPillName);

        oldPillRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    // 기존 데이터 기반으로 MedicineData 생성 (daysOfWeek 등 유지)
                    MedicineData updatedMedicine = documentSnapshot.toObject(MedicineData.class);
                    if (updatedMedicine == null) {
                        updatedMedicine = new MedicineData();
                    }
                    updatedMedicine.setPillName(newPillName);
                    updatedMedicine.setPillType(pillType);
                    updatedMedicine.setAlarmEnabled(alarmEnabled);
                    updatedMedicine.setFavorite(favorite);
                    updatedMedicine.setAlarmTimes(alarmTimes);
                    updatedMedicine.setNotes(notes);
                    updatedMedicine.setDateStr(dateStr);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("pillName", newPillName);
                    updates.put("pillType", pillType);
                    updates.put("alarmEnabled", alarmEnabled);
                    updates.put("favorite", favorite);
                    updates.put("alarmTimes", alarmTimes);
                    updates.put("notes", notes);
                    updates.put("dateStr", dateStr);
                    if (updatedMedicine.getDaysOfWeek() != null) {
                        updates.put("daysOfWeek", updatedMedicine.getDaysOfWeek());
                    }

                    for (int i = 0; i < alarmTimes.size() && i < 10; i++) {
                        String fieldName = "pillIsChecked" + (i + 1);
                        updates.put(fieldName, 0);
                    }

                    if (documentSnapshot.exists()) {
                        List<String> existingAlarmTimes = (List<String>) documentSnapshot.get("alarmTimes");
                        if (existingAlarmTimes != null && existingAlarmTimes.size() > alarmTimes.size()) {
                            for (int i = alarmTimes.size(); i < existingAlarmTimes.size() && i < 10; i++) {
                                String fieldName = "pillIsChecked" + (i + 1);
                                updates.put(fieldName, 0);
                            }
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
                                                    .addOnSuccessListener(aVoid1 -> {
                                                        Log.d("FirestoreHelper", "Old medicine document deleted successfully: " + oldPillName);
                                                        callback.onStatusUpdated();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("FirestoreHelper", "Failed to delete old medicine document: " + oldPillName, e);
                                                        callback.onStatusUpdateFailed(e);
                                                    });
                                        } else {
                                            callback.onStatusUpdated();
                                        }
                                    }

                                    @Override
                                    public void onStatusUpdateFailed(Exception e) {
                                        Log.e("FirestoreHelper", "Failed to sync updated medicine to currentMedications", e);
                                        callback.onStatusUpdateFailed(e);
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FirestoreHelper", "Failed to update medicine: " + oldPillName, e);
                                callback.onStatusUpdateFailed(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Failed to get existing medicine for: " + oldPillName, e);
                    callback.onStatusUpdateFailed(e);
                });
    }

    // 현재 날짜 "yyyyMMdd_HHmmss"
    private String getCurrentDateTimeAsString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // 현재 날짜 "yyyyMMdd"
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // --------- "지금 복용 중인 약" : currentMedications 컬렉션 ---------

    // 목록 화면에서 사용할: 현재 복용 약 전체 가져오기
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

    // currentMedications에 upsert
    public void upsertCurrentMedication(
            String username,
            MedicineData medicine,
            StatusCallback callback
    ) {
        if (username == null || username.isEmpty() || medicine == null || medicine.getPillName() == null) {
            callback.onStatusUpdateFailed(new IllegalArgumentException("Invalid arguments for upsertCurrentMedication"));
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
        if (username == null || username.isEmpty() || pillName == null || pillName.isEmpty()) {
            callback.onStatusUpdateFailed(new IllegalArgumentException("Invalid arguments for deleteCurrentMedication"));
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

    // --------- Callback 인터페이스 ---------

    public interface MedicationListCallback {
        void onMedicationListReceived(List<MedicineData> medications);
        void onMedicationListFailed(Exception e);
    }

    public interface MedicationCallback {
        void onMedicationAdded(MedicineData medicine);
        void onMedicationAddFailed(Exception e);
    }

    public interface DeleteCallback {
        void onMedicineDeleted();
        void onMedicineDeleteFailed(Exception e);
    }

    public interface StatusCallback {
        void onStatusUpdated();
        void onStatusUpdateFailed(Exception e);
    }

    public interface UserDataCallback {
        void onUserDataReceived(Map<String, Object> data);
        void onUserDataNotFound();
        void onUserDataFailed(Exception e);
    }

    public interface UidDeleteCallback {
        void onUidDeleteSuccess();
        void onUidDeleteFailed(Exception e);
    }

    public interface UsernameCallback {
        void onUsernameAvailable();
        void onUsernameExists();
        void onUsernameCheckFailed(Exception e);
    }

    public interface SetUsernameCallback {
        void onSetUsernameSuccess();
        void onSetUsernameFailed(Exception e);
    }

    public interface GetUsernameCallback {
        void onUsernameReceived(String username);
        void onUsernameFailed(Exception e);
    }

    public interface DataCallback {
        void onDataReceived(Map<String, Object> data);
        void onDataNotFound();
        void onDataFetchFailed(Exception e);
    }

    public interface CheckUserCallback {
        void onUserExists(String username);
        void onUserDoesNotExist();
        void onError(Exception e);
    }
}
