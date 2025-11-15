package com.example.project1;

import static android.content.ContentValues.TAG;

import android.util.Log;

import com.example.project1.MedicineData;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
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

    // ë‚ ì§œ ë©”íƒ€ë°ì´í„°ë¥¼ FamilyMember ë¬¸ì„œì— ì¶”ê°€í•˜ëŠ” ë©”ì„œë“œ (username ë¬¸ì„œì— í•„ë“œ ì¶”ê°€)
    public void addDateToFamilyMember(
            String username,
            String dateStr,
            StatusCallback callback
    ) {
        if (username == null || dateStr == null) {
            Log.e("FirestoreHelper", "username ë˜ëŠ” dateStrì´ nullìž…ë‹ˆë‹¤.");
            callback.onStatusUpdateFailed(new IllegalArgumentException("í•„ìˆ˜ í•„ë“œê°€ nullìž…ë‹ˆë‹¤."));
            return;
        }

        DocumentReference familyMemberRef = db.collection("FamilyMember")
                .document(username);

        Map<String, Object> dateField = new HashMap<>();
        dateField.put(dateStr, dateStr);

        familyMemberRef.update(dateField)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "ë‚ ì§œ í•„ë“œê°€ ì„±ê³µì ìœ¼ë¡œ ì¶”ê°€ë˜ì—ˆìŠµë‹ˆë‹¤: " + dateStr);
                    callback.onStatusUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "ë‚ ì§œ í•„ë“œ ì¶”ê°€ì— ì‹¤íŒ¨í–ˆìŠµë‹ˆë‹¤: " + dateStr, e);
                    callback.onStatusUpdateFailed(e);
                });
    }


    // UIDë¡œ user ë¬¸ì„œ ì¡°íšŒ: username ë¬¸ì„œê°€ ì¡´ìž¬í•˜ëŠ”ì§€ í™•ì¸í•˜ê¸° ìœ„í•´ uid í•„ë“œë¡œ ê²€ìƒ‰
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

    // ì´ë©”ì¼ë¡œ ì‚¬ìš©ìž ë¬¸ì„œ ì¡°íšŒ
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

    // UID ê¸°ë°˜ ì‚¬ìš©ìž ë°ì´í„° ê°€ì ¸ì˜¤ê¸°(ë¬¸ì„œ ID=username)
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

    // ê°€ìž… ì‹œ UIDë¥¼ ë¬¸ì„œ IDë¡œ ìž„ì‹œ ìƒì„±
    // ì´ ë¬¸ì„œëŠ” username ì„¤ì • ì „ê¹Œì§€ ìž„ì‹œ ì €ìž¥ìš©
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
                        // createdAtì„ ë¬¸ìžì—´ë¡œ ì €ìž¥
//                        String createdAt = getCurrentDateTimeAsString();
                        String createdAt = getCurrentDateAsString();

                        Map<String, Object> memberData = new HashMap<>();
                        memberData.put("uid", uid);
                        memberData.put("email", email);
                        memberData.put("displayName", displayName);
                        memberData.put("photoUrl", photoUrl);
//                        memberData.put("createdAt", createdAt);
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
        // SimpleDateFormatì„ ì‚¬ìš©í•´ ë‚ ì§œ í˜•ì‹ì„ ì§€ì •
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date now = new Date(); // í˜„ìž¬ ë‚ ì§œì™€ ì‹œê°„ ê°€ì ¸ì˜¤ê¸°
        return dateFormat.format(now); // í˜•ì‹ì— ë§žê²Œ ë³€í™˜ëœ ë‚ ì§œ ë°˜í™˜
    }

    // ì‚¬ìš©ìž ì¡´ìž¬ ì—¬ë¶€ í™•ì¸ (UID ê¸°ë°˜)
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

    // username ì‚¬ìš© ê°€ëŠ¥ ì—¬ë¶€ í™•ì¸ (ë¬¸ì„œ IDë¡œ ë°”ë¡œ í™•ì¸)
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
                    // ì´ë¯¸ í•´ë‹¹ username ë¬¸ì„œê°€ ì¡´ìž¬í•¨
                    callback.onUsernameExists();
                } else {
                    // username ë¬¸ì„œê°€ ì—†ìœ¼ë¯€ë¡œ ì‚¬ìš© ê°€ëŠ¥
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
            // 먼저 username 문서가 이미 존재하는지 확인
            DocumentSnapshot usernameSnapshot = transaction.get(usernameRef);
            if (usernameSnapshot.exists()) {
                throw new FirebaseFirestoreException("Username 이미 존재합니다: " + username,
                        FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }

            // UID 문서 확인
            DocumentSnapshot uidSnapshot = transaction.get(uidRef);

            Map<String, Object> userData;

            if (uidSnapshot.exists()) {
                // UID 문서가 있으면 해당 데이터를 복사
                userData = new HashMap<>(uidSnapshot.getData());
                userData.put("username", username);
                if (email != null) {
                    userData.put("email", email);
                }

                // username 문서 생성 및 UID 문서 삭제
                transaction.set(usernameRef, userData);
                transaction.delete(uidRef);
            } else {
                // UID 문서가 없으면 새로 생성 (로그인 후 처음 username 설정하는 경우)
                userData = new HashMap<>();
                userData.put("uid", uid);
                userData.put("username", username);
                if (email != null) {
                    userData.put("email", email);
                }

                // username 문서만 생성
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

    // `username`ì˜ ìœ íš¨ì„± ê²€ì‚¬
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

    // Firestoreì— ì‚¬ìš©ìž ë¬¸ì„œê°€ ìžˆëŠ”ì§€ í™•ì¸í•˜ê³  ì—†ìœ¼ë©´ uid ë¬¸ì„œë¡œ ìƒì„±
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
                .document("pillName"); // ì‹¤ì œ pillNameìœ¼ë¡œ ëŒ€ì²´ í•„ìš”

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

    // ì•½ ì¶”ê°€ ë©”ì„œë“œ
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
        String dateStr = getCurrentDate(); // í˜„ìž¬ ë‚ ì§œ "yyyyMMdd"

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
            medicineData.put(fieldName, 0); // 0: ë¯¸ë³µìš©
        }

        // pillIsChecked í•„ë“œ ì¶”ê°€ (í•˜ë‚˜ë¼ë„ 0ì´ë©´ 0, ëª¨ë‘ ë³µìš©ì´ë©´ 1)
        medicineData.put("pillIsChecked", 0); // ê¸°ë³¸ê°’ì„ 0ìœ¼ë¡œ ì„¤ì •

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
                    callback.onMedicationAdded(addedMedicine);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Failed to add medicine", e);
                    callback.onMedicationAddFailed(e);
                });
    }

    // ì£¼ì˜ì‚¬í•­(caution)ë§Œ ì—…ë°ì´íŠ¸í•˜ëŠ” ìƒˆë¡œìš´ í•¨ìˆ˜
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

    public void deleteMedicine(String username, String dateStr, String pillName, DeleteCallback callback) {
        DocumentReference pillRef = db.collection("FamilyMember")
                .document(username)
                .collection(dateStr)
                .document(pillName);

        pillRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "Medicine deleted successfully: " + pillName);
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
                        // ë‹¨ì¼ ë¬¸ì„œ ê°€ì •
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        String username = document.getId();
                        if (username.equals(uid)) {
                            callback.onUsernameFailed(new Exception("Usernameì´ ì„¤ì •ë˜ì§€ ì•Šì•˜ìŠµë‹ˆë‹¤."));
                        } else {
                            callback.onUsernameReceived(username);
                        }
                    } else {
                        callback.onUsernameFailed(new Exception("User document does not exist"));
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onUsernameFailed(e);
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
        Log.d("FirestoreHelper", "removePillIsChecked í˜¸ì¶œë¨: username=" + username + ", dateStr=" + dateStr + ", pillName=" + pillName + ", alarmIndex=" + alarmIndex);

        DocumentReference pillRef = db.collection("FamilyMember")
                .document(username)
                .collection(dateStr)
                .document(pillName);

        String pillIsCheckedField = "pillIsChecked" + (alarmIndex + 1);
        Log.d("FirestoreHelper", "ì—…ë°ì´íŠ¸í•  í•„ë“œ: " + pillIsCheckedField);

        Map<String, Object> updates = new HashMap<>();
        updates.put(pillIsCheckedField, 0); // ë¯¸ë³µìš©ìœ¼ë¡œ ì´ˆê¸°í™”

        pillRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "pillIsChecked í•„ë“œê°€ ì„±ê³µì ìœ¼ë¡œ ì—…ë°ì´íŠ¸ë¨: " + pillIsCheckedField);
                    callback.onStatusUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "pillIsChecked í•„ë“œ ì—…ë°ì´íŠ¸ ì‹¤íŒ¨: " + pillIsCheckedField, e);
                    callback.onStatusUpdateFailed(e);
                });
    }


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

        Map<String, Object> updates = new HashMap<>();
        updates.put("pillName", newPillName);
        updates.put("pillType", pillType);
        updates.put("alarmEnabled", alarmEnabled);
        updates.put("favorite", favorite);
        updates.put("alarmTimes", alarmTimes);
        updates.put("notes", notes);

        for (int i = 0; i < alarmTimes.size() && i < 10; i++) {
            String fieldName = "pillIsChecked" + (i + 1);
            updates.put(fieldName, 0);
        }

        oldPillRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> existingAlarmTimes = (List<String>) documentSnapshot.get("alarmTimes");
                        if (existingAlarmTimes != null && existingAlarmTimes.size() > alarmTimes.size()) {
                            for (int i = alarmTimes.size(); i < existingAlarmTimes.size() && i < 10; i++) {
                                String fieldName = "pillIsChecked" + (i + 1);
                                updates.put(fieldName, 0);
                            }
                        }
                    }
                    newPillRef.set(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("FirestoreHelper", "Medicine updated successfully for: " + newPillName);
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

    // í˜„ìž¬ ë‚ ì§œ ì‹œê°„ ë¬¸ìžì—´ ë°˜í™˜
    private String getCurrentDateTimeAsString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // í˜„ìž¬ ë‚ ì§œ "yyyyMMdd" í˜•ì‹ìœ¼ë¡œ ë°˜í™˜
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Callback ì¸í„°íŽ˜ì´ìŠ¤ë“¤
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
