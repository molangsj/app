# 복약 관리 앱 (Medicine Management App)

가족 구성원의 약 복용을 관리하고 알림을 제공하는 Android 애플리케이션입니다.

## 주요 기능

- 📱 **약 복용 알림**: 설정한 시간에 자동으로 알림
- 👨‍👩‍👧‍👦 **가족 관리**: 여러 가족 구성원의 약 관리
- 📊 **복용 통계**: 복용 기록 및 통계 시각화
- 📅 **캘린더 뷰**: 날짜별 복용 내역 확인
- 📷 **OCR 처방전 인식**: 카메라로 처방전 촬영 시 텍스트 자동 인식
- 🔐 **사용자 인증**: 이메일/비밀번호, Google 로그인 지원

## 기술 스택

- **언어**: Java
- **플랫폼**: Android
- **인증**: Firebase Authentication
- **데이터베이스**: Firebase Firestore
- **푸시 알림**: Firebase Cloud Messaging (FCM)
- **이미지 처리**: ML Kit Text Recognition
- **UI**: Material Design Components

## Firebase 설정 방법

이 앱을 실행하려면 Firebase 프로젝트 설정이 필요합니다:

### 1. Firebase 프로젝트 생성
1. [Firebase Console](https://console.firebase.google.com/)에 접속
2. "프로젝트 추가" 클릭
3. 프로젝트 이름 입력 및 생성

### 2. Android 앱 등록
1. Firebase 프로젝트에서 Android 아이콘 클릭
2. **패키지 이름**: `com.example.project1`
3. 앱 닉네임 입력 (선택사항)
4. **SHA-1 인증서** 지문 입력 (Google 로그인용)

### 3. SHA-1 인증서 얻기
디버그 키스토어의 SHA-1:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

릴리즈 키스토어의 SHA-1:
```bash
keytool -list -v -keystore [your-keystore-path] -alias [your-alias]
```

### 4. google-services.json 다운로드
1. Firebase 콘솔에서 `google-services.json` 파일 다운로드
2. 프로젝트의 `app/` 디렉토리에 배치

### 5. Firebase 서비스 활성화

#### Authentication
- 이메일/비밀번호 로그인 활성화
- Google 로그인 제공업체 추가

#### Firestore Database
- 데이터베이스 생성 (테스트 모드 또는 프로덕션 모드)
- 보안 규칙 설정:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users 컬렉션
    match /Users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // usernames 컬렉션
    match /usernames/{username} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

#### Cloud Messaging
- FCM 자동 활성화됨
- 필요시 서버 키 확인

## 빌드 및 실행

### 필수 조건
- Android Studio Arctic Fox 이상
- JDK 11 이상
- Android SDK (minSdkVersion 24 이상 권장)

### 빌드 방법

1. **프로젝트 클론**
```bash
git clone [repository-url]
cd PJ1215
```

2. **Android Studio에서 열기**
   - Android Studio 실행
   - "Open an Existing Project" 선택
   - 프로젝트 폴더 선택

3. **Gradle 동기화**
   - Android Studio가 자동으로 Gradle 동기화 수행
   - 필요한 의존성 자동 다운로드

4. **빌드**
```bash
./gradlew assembleDebug
```

5. **실행**
   - 에뮬레이터 또는 실제 기기 연결
   - Run 버튼 클릭

## 프로젝트 구조

```
PJ1215/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/project1/
│   │       │   ├── MainActivity.java
│   │       │   ├── FirestoreHelper.java
│   │       │   ├── AuthHelper.java
│   │       │   ├── MedicineList.java
│   │       │   ├── AddEditMedicineFragment.java
│   │       │   └── ... (기타 Java 파일들)
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   ├── build.gradle
│   └── google-services.json (직접 추가 필요)
├── build.gradle
├── settings.gradle
└── README.md
```

## 주요 클래스 설명

- **MainActivity**: 앱의 메인 액티비티
- **FirestoreHelper**: Firestore 데이터베이스 작업 헬퍼
- **AuthHelper**: Firebase 인증 관련 헬퍼
- **MedicineList**: 약 목록 표시 및 관리
- **AddEditMedicineFragment**: 약 추가/수정 화면
- **AlarmReceiver**: 알림 수신 및 처리
- **MyFirebaseMessagingService**: FCM 푸시 알림 처리

## 보안 주의사항

⚠️ **중요**: 다음 파일들은 GitHub에 업로드하지 마세요:
- `google-services.json` (Firebase 설정)
- `*.jks`, `*.keystore` (서명 키)
- `local.properties` (로컬 SDK 경로)

이 파일들은 `.gitignore`에 포함되어 있습니다.

## 문제 해결

### google-services.json 파일이 없다는 오류
- Firebase 콘솔에서 파일을 다운로드하여 `app/` 디렉토리에 배치하세요.

### Google 로그인이 작동하지 않음
- Firebase 콘솔에서 SHA-1 인증서가 등록되어 있는지 확인하세요.
- `google-services.json` 파일이 최신 버전인지 확인하세요.

### 알림이 작동하지 않음
- 앱에 알림 권한이 부여되어 있는지 확인하세요.
- Android 13 이상에서는 런타임 알림 권한이 필요합니다.

## 라이선스

[라이선스 정보 추가 예정]

## 기여

이슈 및 풀 리퀘스트를 환영합니다!

## 연락처

[연락처 정보 추가 예정]
