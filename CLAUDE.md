# CLAUDE.md — 스도쿠 (Android, Google Play 배포용)

> 이 파일은 매 세션 로드되는 **핵심 규칙 요약**입니다. 상세 명세는 `docs/SPEC.md`, 화면 디자인은 `docs/mockup/`(캔버스 스크린샷) 또는 SPEC.md의 §6을 따릅니다.
> 소유자: 정민혁 (UK2 Solutions). 사용자와의 대화·주석·커밋 메시지·문서는 **한국어**, 코드 식별자는 영어.

## 1. 한 줄 요약
오프라인 우선 스도쿠 앱. 일회성 인앱 상품 `premium_lifetime` 하나로 프리미엄을 판매하고, **Google Play 프로모 코드(일회용 코드, 분기당 최대 500개)**로 초청 사용자에게 무료 지급한다. **자체 서버 없음.**

## 2. 확정된 결정 (임의 변경 금지, 바꾸려면 먼저 사용자에게 질문)
- 플랫폼: Android 전용, minSdk 26, targetSdk는 Play 요구 최신값
- 언어/UI: **Kotlin + Jetpack Compose (Material 3)**, 단일 모듈로 시작 (`:app`)
- 아키텍처: MVVM + 단방향 데이터 흐름, Hilt, Coroutines/Flow
- 저장: Room(진행 중 게임, 기록), DataStore(설정)
- 결제: **Play Billing Library 최신 안정판**, 일회성 상품 `premium_lifetime`. 구독 사용 안 함 (프로모 코드가 구독에서는 체험만 제공되므로)
- 초청코드 = Google Play **일회용 프로모 코드** (상품: `premium_lifetime`). 앱은 코드를 직접 검증하지 않는다. 앱은 Play에서 발생한 구매를 감지해 프리미엄을 연다.
- 퍼즐 생성: **온디바이스** 생성기 (서버·네트워크 불필요). 유일해 보장
- 광고: **1차 릴리스 제외**. 광고 SDK/동의 UI는 넣지 않는다.
- 분석/크래시 수집: 1차 릴리스 제외 (넣을 경우 개인정보 처리방침·데이터 보안 양식 영향 → 사전 협의)

## 3. 작업 원칙
1. 마일스톤(SPEC.md §9) 순서대로 진행하고, **각 마일스톤 끝에 빌드·테스트를 실행해 통과를 확인**한 뒤 커밋한다.
2. 도메인 로직(보드, 솔버, 생성기, 프리미엄 판정)은 **Android 의존성 없는 순수 Kotlin 모듈/패키지**로 두고 단위 테스트를 작성한다.
3. 결제는 `BillingGateway` 인터페이스 뒤에 숨기고, 테스트에서는 Fake 구현을 쓴다.
4. 하드코딩 금지: 문자열은 `strings.xml`(한국어 기본), 색/타이포는 디자인 토큰(SPEC.md §6.1)을 `Theme`에 매핑.
5. 접근성: 터치 타깃 ≥ 48dp, 텍스트 대비 4.5:1, 아이콘 버튼에 contentDescription, 색만으로 의미 전달 금지(오류는 색 + 아이콘/굵기).
6. 모르는 Play 정책/API는 추측하지 말고 **공식 문서(developer.android.com, support.google.com/googleplay)를 확인**한 뒤 구현한다. 확인한 근거는 코드 주석이나 `docs/DECISIONS.md`에 남긴다.
7. 비밀 정보(키스토어, 서명 키, 서비스 계정 JSON)는 **절대 저장소에 커밋하지 않는다.** `.gitignore`에 포함하고, 필요하면 사용자에게 요청한다.
8. 범위 밖 기능을 임의로 추가하지 않는다. 아이디어는 `docs/BACKLOG.md`에 적는다.

## 4. 자주 쓰는 명령 (프로젝트 생성 후 실제 값으로 갱신할 것)
- 빌드: `./gradlew assembleDebug`
- 단위 테스트: `./gradlew test`
- 린트: `./gradlew lint`
- 릴리스 번들: `./gradlew bundleRelease` (서명은 사용자 로컬에서, `docs/RELEASE.md` 참고)
- 화면 스크린샷(설치 불필요): `./gradlew testDebugUnitTest --tests '*ScreenshotTest'` → `docs/screens/`
- 접근성 검사: `--tests '*ContrastTest'`, `--tests '*SemanticsTest'`

## 5. 완료 정의 (Definition of Done)
- 해당 마일스톤의 수용 기준(SPEC.md §9) 전부 충족
- `./gradlew test lint` 통과
- 새 도메인 로직마다 단위 테스트 존재
- 사용자에게 "무엇을 만들었고, 무엇이 남았고, 사용자가 직접 해야 할 일(SPEC.md §10)"을 요약 보고

## 6. 사람(사용자)이 직접 해야 하는 일 — Claude Code가 대신할 수 없음
Play Console 개발자 계정/앱 생성, 인앱 상품 `premium_lifetime` 등록·가격 설정, 프로모 코드 500개 발급, 서명 키 생성·Play 앱 서명 설정, 개인정보 처리방침 URL 게시, 데이터 보안 양식, 테스트 트랙 배포. 상세는 SPEC.md §10.
