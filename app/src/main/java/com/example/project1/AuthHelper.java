package com.example.project1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;

public class AuthHelper {

    private static final String TAG = "AuthHelper";

    private FirebaseAuth mAuth;
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private FirestoreHelper firestoreHelper;
    private GoogleSignInClient mGoogleSignInClient;
    private Context context;

    public AuthHelper(Context context) {
        mAuth = FirebaseAuth.getInstance();
        this.context = context;

        oneTapClient = Identity.getSignInClient(context);
        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(context.getString(R.string.default_web_client_id))
                                .setFilterByAuthorizedAccounts(false)
                                .build())
                .setAutoSelectEnabled(false)
                .build();

        firestoreHelper = new FirestoreHelper(); // FirestoreHelper 초기화
        configureGoogleSignIn();
    }

    // username 없이 회원가입
    public void signUpWithoutUsername(String email, String password, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // UID 문서 생성
                            firestoreHelper.addNewUserWithoutUsername(user, new FirestoreHelper.StatusCallback() {
                                @Override
                                public void onStatusUpdated() {
                                    // UID 문서 생성 완료
                                    callback.onAuthSuccess(user);
                                }

                                @Override
                                public void onStatusUpdateFailed(Exception e) {
                                    callback.onAuthFailed(e);
                                }
                            });
                        } else {
                            callback.onAuthFailed(new Exception("회원가입은 성공했으나 FirebaseUser가 null입니다."));
                        }
                    } else {
                        callback.onAuthFailed(task.getException());
                    }
                });
    }

    // SetUsernameFragment에서 username 설정 시 호출
    public void setUsername(String username, String email, String uid, FirestoreHelper.SetUsernameCallback callback) {
        firestoreHelper.setUsername(username, email, uid, callback);
    }

    // Google Sign-In 옵션 설정
    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    // GoogleSignInCallback 인터페이스 정의
    public interface GoogleSignInCallback {
        void onGoogleSignInSuccess(FirebaseUser user, String username);
        void onGoogleSignInFailed(Exception e);
        void onNeedUsername(FirebaseUser user);
    }

    // AuthCallback 인터페이스 정의
    public interface AuthCallback {
        void onAuthSuccess(FirebaseUser user);
        void onAuthFailed(Exception e);
    }

    // 이메일/비밀번호 회원가입 (username 미포함)
    public void signUp(String email, String password, AuthCallback callback) {
        if (email == null || password == null) {
            Log.e(TAG, "Email or password is null");
            callback.onAuthFailed(new IllegalArgumentException("Email and password must not be null"));
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // username 없이 가입한 경우 username 필드를 빈 문자열로 설정
                            firestoreHelper.setUsername("", email, user.getUid(), new FirestoreHelper.SetUsernameCallback() {
                                @Override
                                public void onSetUsernameSuccess() {
                                    callback.onAuthSuccess(user);
                                }

                                @Override
                                public void onSetUsernameFailed(Exception e) {
                                    callback.onAuthFailed(e);
                                }
                            });
                        } else {
                            callback.onAuthFailed(new Exception("User is null after registration"));
                        }
                    } else {
                        Exception exception = task.getException();
                        callback.onAuthFailed(exception != null ? exception : new Exception("Unknown error"));
                    }
                });
    }

    // 이메일/비밀번호 회원가입 (username 포함)
    public void signUp(String email, String password, String username, AuthCallback callback) {
        if (email == null || password == null || username == null) {
            Log.e(TAG, "Email, password, or username is null");
            callback.onAuthFailed(new IllegalArgumentException("Email, password, and username must not be null"));
            return;
        }

        firestoreHelper.isUsernameAvailable(username, new FirestoreHelper.UsernameCallback() {
            @Override
            public void onUsernameAvailable() {
                // username 사용 가능
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    firestoreHelper.setUsername(username, email, user.getUid(), new FirestoreHelper.SetUsernameCallback() {
                                        @Override
                                        public void onSetUsernameSuccess() {
                                            callback.onAuthSuccess(user);
                                        }

                                        @Override
                                        public void onSetUsernameFailed(Exception e) {
                                            callback.onAuthFailed(e);
                                        }
                                    });
                                }
                            }
                        });
            }

            @Override
            public void onUsernameExists() {
                // username 이미 존재
                callback.onAuthFailed(new Exception("이미 사용 중인 사용자 이름입니다."));
            }

            @Override
            public void onUsernameUnavailable(String reason) {
                // username 사용 불가
                callback.onAuthFailed(new Exception(reason));
            }

            @Override
            public void onUsernameCheckFailed(Exception e) {
                // 확인 실패
                callback.onAuthFailed(e);
            }

            @Override
            public void onUsernameReceived(String username) {
                // 사용되지 않음
            }

            @Override
            public void onUsernameFailed(Exception e) {
                // 사용되지 않음
            }
        });
    }

    // 로그인
    public void login(String email, String password, AuthCallback callback) {
        if (email == null || password == null) {
            callback.onAuthFailed(new IllegalArgumentException("Email and password must not be null"));
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            firestoreHelper.checkUserExists(uid, new FirestoreHelper.CheckUserCallback() {
                                @Override
                                public void onUserExists(String username) {
                                    callback.onAuthSuccess(user);
                                }

                                @Override
                                public void onUserDoesNotExist() {
                                    callback.onAuthFailed(new Exception("Username이 설정되지 않았습니다."));
                                }

                                @Override
                                public void onError(Exception e) {
                                    callback.onAuthFailed(e);
                                }
                            });
                        } else {
                            callback.onAuthFailed(new Exception("User is null after sign-in"));
                        }
                    } else {
                        Exception exception = task.getException();
                        callback.onAuthFailed(exception != null ? exception : new Exception("Unknown error"));
                    }
                });
    }

    // 구글 로그인 시작
    public void signInWithGoogle(Activity activity, int requestCode, GoogleSignInCallback callback) {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, requestCode);
    }

    // 구글 로그인 결과 처리
    public void handleSignInResult(Intent data, GoogleSignInCallback callback) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount acct = task.getResult(ApiException.class);
            if (acct != null) {
                firebaseAuthWithGoogle(acct, callback);
            } else {
                callback.onGoogleSignInFailed(new Exception("GoogleSignInAccount is null"));
            }
        } catch (ApiException e) {
            callback.onGoogleSignInFailed(e);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct, GoogleSignInCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            firestoreHelper.checkUserExists(uid, new FirestoreHelper.CheckUserCallback() {
                                @Override
                                public void onUserExists(String username) {
                                    callback.onGoogleSignInSuccess(user, username);
                                }

                                @Override
                                public void onUserDoesNotExist() {
                                    callback.onNeedUsername(user);
                                }

                                @Override
                                public void onError(Exception e) {
                                    callback.onGoogleSignInFailed(e);
                                }
                            });
                        } else {
                            callback.onGoogleSignInFailed(new Exception("Firebase user is null after sign-in"));
                        }
                    } else {
                        Exception exception = task.getException();
                        callback.onGoogleSignInFailed(exception != null ? exception : new Exception("Unknown error"));
                    }
                });
    }

    // 로그아웃
    public void logout(Context context) {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "User signed out successfully");
            } else {
                Log.w(TAG, "User sign out failed", task.getException());
            }
        });
    }

    // 현재 사용자 가져오기
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }
}
