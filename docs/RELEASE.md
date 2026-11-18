# 릴리스 가이드 (사용자가 직접 하는 단계 포함)

## 0. 현재 상태(2026-11-16)
- targetSdk **36** / compileSdk 36 / minSdk 26, AGP 8.13.2, Gradle 8.14.3. 2026-08-31부터 신규 앱은 targetSdk 36 이상이어야 함(공식 문서 확인).
- 네이티브 라이브러리 16KB 페이지 정렬 확인(`p_align` 0x4000).
- R8 축소 릴리스 빌드 통과(AAB ≈ 4.9MB). `./gradlew test lint bundleRelease` 통과.

## 1. 업로드 키 만들기 (한 번만, 안전한 곳에 백업)
```bash
keytool -genkeypair -v -keystore upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```
저장소 루트에 `keystore.properties`를 만들고(**커밋 금지**, `.gitignore`에 이미 등록됨):
```properties
storeFile=upload-keystore.jks
storePassword=비밀번호
keyAlias=upload
keyPassword=비밀번호
```
Play Console에서 **Play 앱 서명**을 켜고, 이 키는 "업로드 키"로만 씁니다.

## 2. 빌드
```bash
./gradlew test lint          # 테스트·린트
./gradlew bundleRelease      # app/build/outputs/bundle/release/app-release.aab (서명됨)
```
`keystore.properties`가 없으면 서명되지 않은 번들이 나옵니다. R8 때문에 생길 수 있는 문제를 미리 보려면 `./gradlew assembleRelease`로 만든 APK를 서명해 실기기에서 써 보세요(전달드린 `sudoku-release-test.apk`가 같은 용도, 디버그 키 서명이라 **Play 업로드 불가**).

## 3. Play Console 체크리스트 (SPEC §10)
- [ ] 개발자 계정 요건 확인(개인/조직별 인증·테스트 요건이 다름 — Console의 현재 안내를 따르세요)
- [ ] 앱 만들기(패키지 `com.uk2solutions.sudoku`는 출시 후 변경 불가), 앱 서명 설정
- [ ] 인앱 상품 `premium_lifetime` 등록·**활성화**·가격 설정
- [ ] 개인정보 처리방침 URL 게시(`docs/PRIVACY_POLICY.md`), 데이터 보안 양식(`docs/DATA_SAFETY.md`), 콘텐츠 등급, 타깃 연령
- [ ] 스토어 등록정보(`docs/STORE_LISTING.md`, 그래픽 `docs/store/`, 실기기 스크린샷)
- [ ] 라이선스 테스터 등록 → 내부 테스트 트랙에 AAB 업로드 → 결제·구매 복원·프로모 코드 상환 실기기 확인
- [ ] 비공개/공개 테스트 요건 확인 후 프로덕션 출시

## 4. 프로모 코드 (초청 사용자 최대 500명)
- 프로모션 생성: 상품 `premium_lifetime`, 일회용 코드 **500개** → CSV 보관(저장소 금지). **생성 후 수 변경 불가, 분기 내 미사용분은 소멸**(공식 문서). 앱이 완성돼 배포 직전인 분기 시작에 발급하는 것이 안전합니다.
- 받는 사람은 Play 스토어의 "코드 사용" 또는 앱의 초청 코드 화면에서 상환합니다.

## 5. 출시 전 알려진 공백
- 전문가·악마 난이도 미구현(프리미엄 혜택 항목) — `BACKLOG.md`
- 번역 원어민 검수, 스토어 문구의 언어별 번역
- 실제 결제·상환·도전장 공유·알림은 실기기/테스트 트랙에서만 검증 가능

## 개인용 전체 기능 빌드 (`personal`)
- 목적: 소유자 본인이 프리미엄 구매 없이 모든 기능(악마 난이도, 전체 레슨, 무제한 힌트, 테마, 상세 통계)을 쓰는 복제본.
- 빌드: `./gradlew assemblePersonal` → `app/build/outputs/apk/personal/app-personal.apk` (복사본: `dist/sudoku7979-full.apk`).
- 패키지 `com.uk2solutions.sudoku.full`(정식 앱과 함께 설치 가능), 앱 이름 `sudoku7979 Full`, 디버그 키 서명. `BuildConfig.ALL_UNLOCKED=true`일 때만 `PremiumManager.isPremium`이 항상 true.
- **스토어에 올리지 않는다.** 스토어용은 `release`(`ALL_UNLOCKED=false`)만 사용. `bundleRelease` 결과에는 영향 없음.
