# 개인정보 처리방침 (초안)

> 초안입니다. `[ ]` 표시를 채우고, 웹에 게시한 URL을 Play Console에 입력하세요. 법적 검토는 직접 받으시길 권합니다.
> 시행일: [YYYY-MM-DD] · 운영자: UK2 Solutions · 문의: [문의 이메일]

## 1. 요약
스도쿠(이하 "앱")는 **계정이 없고, 자체 서버가 없으며, 광고·분석·크래시 수집 도구를 쓰지 않습니다.** 게임 기록과 설정은 모두 사용자의 기기에만 저장되고, 운영자는 사용자의 개인정보를 수집하거나 전송받지 않습니다.

## 2. 기기에만 저장되는 정보
- 진행 중인 게임, 완료 기록(시간·실수·점수), 오늘의 도전 기록과 연속 일수
- 설정(테마, 언어, 소리, 진동, 알림 등), 하루 힌트 사용 횟수
- 프리미엄 구매 여부와 마지막 확인 시각(캐시)

이 정보는 앱을 삭제하거나 기기의 "저장소 지우기"를 하면 삭제됩니다. 운영자는 이 정보에 접근할 수 없습니다.

## 3. 결제 (Google Play 결제)
프리미엄은 Google Play 결제로 판매됩니다. 결제 수단·결제 내역은 Google이 처리하며 운영자에게 전달되지 않습니다. 앱은 구매 여부(권한 부여 여부)만 기기에 저장합니다. 자세한 내용은 Google의 개인정보처리방침을 따릅니다.

## 4. 초청 코드
초청 코드를 입력하면 앱은 코드를 **저장하거나 전송하지 않고**, Google Play의 코드 사용 화면으로 연결만 합니다. 코드의 유효성은 Google Play가 확인합니다.

## 5. 알림
"오늘의 도전" 알림은 **기기 안에서만** 예약·표시되는 로컬 알림이며, 사용자가 설정에서 켠 경우에만 동작합니다. Android 13 이상에서는 알림 권한을 요청합니다. 알림 때문에 외부로 전송되는 정보는 없습니다.

## 6. 도전장 공유
"도전장 보내기"는 사용자가 직접 선택한 앱(메신저 등)으로 **퍼즐 코드와 내 기록이 담긴 문구**를 보냅니다. 이 문구는 사용자가 고른 앱과 받는 사람에게 전달되며, 운영자의 서버를 거치지 않습니다. "도전장 받기"의 붙여넣기 버튼은 사용자가 누를 때만 클립보드를 읽습니다.

## 7. 제3자 서비스·권한
- 제3자 SDK: Google Play 결제 라이브러리(결제 처리). 광고·분석·크래시 수집 SDK는 없습니다.
- 사용하는 권한: 알림(선택, 오늘의 도전 알림), 인터넷·네트워크 상태(Google Play 구매 확인용), 결제(Google Play 결제), 부팅 완료 수신·절전 방지(재부팅 후 알림 예약을 되살리는 Android WorkManager가 사용). 위치·연락처·카메라·마이크·저장소 권한은 사용하지 않습니다.

## 8. 아동
앱은 만 13세 미만 아동을 주 대상으로 하지 않으며, 아동의 개인정보를 별도로 수집하지 않습니다. [대상 연령 설정에 맞춰 수정]

## 9. 변경과 문의
방침이 바뀌면 이 페이지와 앱 정보에서 알립니다. 문의: [문의 이메일]

---

# Privacy Policy (draft)

> Draft. Fill in the `[ ]` fields and publish it at a public URL for the Play Console.
> Effective date: [YYYY-MM-DD] · Operator: UK2 Solutions · Contact: [contact email]

**Summary.** The Sudoku app has no accounts, no servers of its own, no ads, and no analytics or crash-reporting tools. Game progress and settings stay on your device. The operator does not collect or receive your personal information.

**Stored only on your device:** games in progress, completed-game records (time, mistakes, score), daily-challenge history and streak, settings (theme, language, sound, vibration, notifications), the daily hint counter, and a cached flag showing whether Premium is unlocked. Uninstalling the app or clearing its storage deletes this data.

**Payments.** Premium is sold through Google Play Billing. Google processes payment details; they are not shared with the operator. The app only stores whether Premium is unlocked.

**Invitation (promo) codes.** The app does not store or transmit the code. It opens Google Play's redeem screen; Google Play validates the code.

**Notifications.** The optional daily-challenge reminder is a local notification scheduled and shown on the device. Nothing is sent externally.

**Challenge sharing.** "Send challenge" hands a text containing a puzzle code and your result to the app you choose (for example a messenger). It does not pass through any server of the operator. The paste button reads the clipboard only when you tap it.

**Third parties and permissions.** Google Play Billing Library only. No advertising, analytics or crash-reporting SDKs. Permissions used: notifications (optional, daily-challenge reminder), internet and network state (Google Play purchase checks), billing, and boot-completed/wake-lock (used by Android WorkManager to restore the reminder schedule after a reboot). No location, contacts, camera, microphone or storage permissions.

**Children.** The app is not primarily directed at children under 13 and does not knowingly collect their personal information. [Adjust to your target-audience setting.]

**Changes and contact.** Changes will be posted on this page. Contact: [contact email]
