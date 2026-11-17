# 스토어 등록 문구 초안

> 앱 이름은 가칭입니다(SPEC §11-2). 실제 이름을 정하면 아래 제목을 바꾸세요. 제목 30자, 간단한 설명 80자, 자세한 설명 4000자 이내.
> 그래픽: `docs/store/icon-512.png`(앱 아이콘 512×512), `docs/store/feature-graphic-ko-1024x500.png` / `-en-`(그래픽 이미지), 스크린샷은 `docs/screens/`를 참고해 실기기에서 다시 찍으세요(비율은 가로:세로 최대 1:2).

## 한국어 (기본)
**제목:** 스도쿠 – 오프라인 숫자 퍼즐

**간단한 설명:** 광고 없이 깔끔하게. 오프라인으로 즐기는 스도쿠, 오늘의 도전, 친구와 점수 대결.

**자세한 설명:**
스도쿠를 가장 깔끔하게 즐기세요. 인터넷이 없어도, 광고 없이, 끊김 없이 동작합니다.

■ 이런 점이 좋아요
• 트레이닝: 규칙부터 X-윙·Y-윙·스워드피시까지 12개 레슨 — 패턴을 보드에 표시하며 배우고, 연습 문제로 확인해요
• 쉬움부터 전문가까지 5단계 난이도 중 전문가까지 무료! 난이도마다 필요한 기술이 달라요 — 쉬움은 "후보가 하나뿐인 칸"만 찾으면 끝까지 풀려요
• 오늘의 도전: 하루 한 판, 모든 사용자가 같은 퍼즐. 연속 일수 기록과 알림(선택)
• 도전장: 푼 결과를 친구에게 보내고, 친구가 같은 퍼즐을 풀어 점수·시간을 비교해요(서버·가입 필요 없음)
• 틀린 숫자는 바로 알려 줘요. 지우고 다시 도전해 보세요
• 메모, 되돌리기, 단계별 힌트(하루 3회 무료) — 정답만 알려 주지 않고 왜 그 숫자인지 보드에 표시하며 설명해요
• 완성 효과와 점수 애니메이션, 진동·소리 설정
• 라이트·다크 모드, 15개 언어, 글자 크기 확대 대응
• 기록: 난이도별 완료 수·최고·평균, 연속 일수

■ 프리미엄 (한 번 결제로 영구)
• 악마 난이도, 중급·고수 레슨 6개(숨은 쌍부터 X-윙·Y-윙·스워드피시까지), 힌트 무제한, 상세 통계, 추가 컬러 테마 3종

개인정보 걱정 없이: 계정도 서버도 없고, 모든 기록은 내 기기에만 저장돼요.

## English
**Title:** Sudoku – Offline Number Puzzle

**Short description:** Clean, ad-free Sudoku you can play offline. Daily challenge and score battles with friends.

**Full description:**
Enjoy Sudoku the clean way — no ads, no internet needed, no account.

■ Highlights
• Training: 12 lessons from the rules to X-Wing, Y-Wing and Swordfish — learn with the pattern shown on the board, then check with practice questions
• Five levels, Easy to Expert, with Expert free to play — each level needs different techniques; Easy only needs "cells with a single candidate"
• Daily Challenge: one puzzle a day, the same for everyone, with streaks and an optional reminder
• Challenges: send your result to a friend; they solve the same puzzle and compare score and time — no server or sign-up
• Wrong numbers are flagged instantly; clear them and try again
• Notes, undo, step-by-step hints (3 free per day) that show why a number fits, highlights for matching numbers, rows, columns and boxes
• Completion effects, score animations, haptics and sound options
• Light and dark mode, 15 languages, large-text support
• Statistics: solved count, best and average time per level, streaks

■ Premium (one-time purchase)
• Devil level, 6 intermediate and expert lessons (hidden pairs up to X-Wing, Y-Wing and Swordfish), unlimited hints, detailed statistics and 3 extra color themes

Privacy-friendly: no account, no servers, everything stays on your device.

## 다른 언어
앱 안은 15개 언어를 지원합니다. 스토어 문구는 Console의 "기본 스토어 등록정보 → 번역 추가"에서 언어별로 올려야 하며, 위 한국어·영어를 바탕으로 번역하면 됩니다(원어민 검수 권장).

## 출시 노트(첫 버전)
• 첫 출시: 5단계 난이도(전문가·악마는 프리미엄), 오늘의 도전, 도전장, 다크 모드, 15개 언어

## 홍보 이미지 (Play 스크린샷 슬롯, 1080×1920)
- `docs/store/promo-ko/01~07.png`, `docs/store/promo-en/01~07.png` — 앱 실제 화면 + 한 줄 후킹 문구. 순서가 곧 업로드 순서(1번이 목록에서 가장 먼저 보임).
- 재생성: `./gradlew testDebugUnitTest --tests '*ScreenshotTest'` 후 `python3 docs/store/tools/make_promo.py` (영어 원본은 `EnglishScreenshotTest`가 `docs/store/src-en`에 저장).
- 현재 한/영/일/스/포(브라질) 5개 언어 완료. 추가 언어는 `TEXT`에 문구를 넣고 `XxxScreenshotTest`(로케일만 다른 하위 클래스)를 추가하면 된다.
- 7번 프리미엄 화면의 가격은 4,900원 기준 국가별 가격 제안(미국 5달러, 한국 4,900원, 일본 500엔처럼 끝자리가 깔끔한 가격)(₩4,900 / $5 / ¥500 / 4,99 € / R$ 14,90). 실제 가격은 Play Console에서 상품 `premium_lifetime` 기본가 4,900원 설정 후 국가별 가격을 확인·조정하고, 이미지의 숫자를 맞춰 다시 뽑는다(`*ScreenshotTest`의 `price`).
