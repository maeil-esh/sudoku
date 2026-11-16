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
