# 결정 기록

## 2026-11-16 M0
- applicationId 확정: `com.uk2solutions.sudoku` (사용자 확인)
- 위치: Portal 저장소의 `sudoku/` 하위 폴더 (사용자 확인)
- 툴체인: Gradle 8.14.3, AGP 8.7.3, Kotlin 2.0.21(Compose 컴파일러 플러그인), KSP 2.0.21-1.0.28, Hilt 2.52, Compose BOM 2024.12.01
- compileSdk/targetSdk 35. 출시 전 Play의 최신 targetSdk 요구값을 확인해 올릴 것(M5).
- `local.properties`(sdk.dir)는 커밋하지 않는다.

## 2026-11-16 UI 개편 (사용자 제공 참고 스크린샷 반영)
- 색 토큰 값 조정(역할 이름은 SPEC §6.1 유지): background `#F4F6FB`(차가운 톤), line `#D5DAE6`, cellPeer `#ECEEF3`(회색), cellSame `#BFD3FA`, cellSelected `#CFDCFB`(+accent 테두리 2dp). 사용자 입력 숫자(accent) 대비를 4.5:1 부근으로 유지하도록 강조색을 연하게 잡았다.
- 다크 테마 값을 같은 역할 이름으로 추가(`DarkSudokuColors`). 설정에서 시스템/라이트/다크 선택.
- 숫자 패드를 5열 2행에서 **한 줄 9칸**으로 변경(참고 화면). 칸 너비가 약 38dp로 SPEC의 터치 타깃 48dp보다 좁다. 높이는 64dp로 보완. 접근성 점검(M5)에서 재검토.
- 점수는 저장하지 않고 현재 상태로 계산한다(`Scoring`): (맞게 채운 칸×10 + 새로 완성한 줄·박스×50 − 실수×30 − 힌트×10) × 난이도 배수. 지우고 다시 넣어 점수를 불리는 꼼수를 막는다.
- 참고 화면에 있어도 넣지 않은 것: 광고, 알림, 언어, SNS 링크, 자동완료, 라이팅 모드, 애니메이션 스코어링, 스마트 힌트 토글(SPEC 비목표 또는 범위 밖). 필요하면 BACKLOG로.
- 도구 아이콘은 아이콘 라이브러리 대신 문자(↶ ⌫ ✎ 💡)를 사용(앱 용량·단순함). 기기별 글꼴에 따라 모양이 다를 수 있어 M5에서 벡터 아이콘으로 교체 검토.

## 2026-11-16 범위 변경 (사용자 요청)
- **다국어**: SPEC §1 비목표("한국어만")를 변경. 1차 지원 언어: 한국어, English, 日本語, Español, Português(Brasil), Русский, Deutsch, Français. 2차 후보: 중국어 간체·번체, 힌디어, 인도네시아어, 태국어, 베트남어, 튀르키예어. 아랍어(RTL)는 별도.
  - 앱 내 언어 선택은 `AppCompatDelegate.setApplicationLocales` 사용(Android 13+ 시스템 앱별 언어 설정과 연동, `locales_config.xml`). 번역은 초안이므로 출시 전 원어민 검수 필요. 새 문자열은 모든 언어 파일에 추가해야 린트(MissingTranslation)가 통과한다.
  - Play 스토어 등록 문구(제목·설명)는 앱 내 번역과 별도로 언어별 작성 필요.
- **알림**: 오늘의 도전 리마인더(로컬 알림, WorkManager 주기 작업). 서버·푸시 서비스 없음, 분석 SDK 아님. 기본 꺼짐, Android 13+는 켤 때 알림 권한 요청. 오늘 이미 완료했으면 보내지 않음. 정확한 시각은 보장되지 않음(절전 모드 등).
- **점수 애니메이션**: 칸을 채울 때 +N/−N이 칸 위로 떠오름(설정에서 끔).
- **스크린샷**: `ScreenshotTest`(Robolectric)로 설치 없이 `docs/screens/`에 PNG 생성. 릴리스 변형 테스트에서는 제외. (네트워크: Robolectric 네이티브 jar 때문에 `gcp.mirrors.maven.org` 허용 필요)
- **기본 언어를 영어로 변경**: `values/`(기본)=English, 한국어는 `values-ko/`. 번역이 없는 언어를 쓰는 폰은 영어가 나온다(한국어 폰은 그대로 한국어). 새 문자열은 기본(영어)에 먼저 추가하고 모든 언어 파일에 번역을 넣는다.
