# ✨ SoulPal — 나만의 AI 친구

> 완전 오프라인 AI 동반자 앱. 인터넷 없이도 작동하며 모든 대화가 내 기기에만 저장됩니다.

---

## 📥 설치 (Android)

### 최신 버전 다운로드

| 기기 | 다운로드 |
|------|----------|
| 최신 안드로이드 폰 (권장) | [⬇️ SoulPal-arm64.apk](https://github.com/KROOKIMWOOBIN/SoulPal/releases/latest/download/SoulPal-arm64.apk) |
| 구형 안드로이드 폰 | [⬇️ SoulPal-arm32.apk](https://github.com/KROOKIMWOOBIN/SoulPal/releases/latest/download/SoulPal-arm32.apk) |

> 어떤 걸 받을지 모르겠다면 **arm64** 를 받으세요.

### 설치 방법

1. 위 링크에서 APK 다운로드
2. 안드로이드 설정 → **보안** → **출처를 알 수 없는 앱 설치 허용**
3. 다운로드한 파일 탭 → 설치

> **첫 실행 시 AI 모델 약 800MB를 다운로드합니다.**
> Wi-Fi 환경에서 실행을 권장해요.

---

## ✨ 주요 기능

- 🤖 **완전 오프라인 AI** — 인터넷 없이도 대화 가능 (llama.cpp 기반)
- 🔒 **프라이버시 보호** — 모든 대화가 기기에만 저장, 서버 전송 없음
- 🎨 **캐릭터 커스터마이징** — 관계·성격·말투·관심사·분위기 6단계 설정
- 🌙 **다크 모드** 완벽 지원 (WCAG AA 색상 대비 기준)
- 🔍 **대화 검색** — 이전 메시지 키워드 검색
- ⭐ **즐겨찾기** — 자주 대화하는 친구 상단 고정
- 💬 **AI 응답 재생성** — 마음에 안 들면 다시 생성
- 📤 **대화 공유/복사** — 대화 내용 내보내기
- 🌐 **한국어 / 영어** 지원

---

## 📋 요구사항

- Android 6.0 (API 23) 이상
- 여유 저장공간 1.5GB 이상 (앱 + AI 모델)
- RAM 3GB 이상 권장

---

## 🛠️ 개발자용 빌드

```bash
git clone https://github.com/KROOKIMWOOBIN/SoulPal.git
cd SoulPal
bash setup.sh
flutter run
```

### CI/CD

- GitHub Actions에서 `arm64` / `arm32` APK를 자동 빌드
- 릴리즈 APK는 일관된 키스토어로 서명 (GitHub Secrets 기반)
- 키스토어 최초 생성: Actions → **🔑 키스토어 생성** 워크플로우 수동 실행 후 Secrets 등록

---

## 🐛 알려진 수정 이력

| 버전 | 수정 내용 |
|------|-----------|
| 최신 | 채팅 화면 첫 렌더링 회색 화면 근본 수정 (`didChangeDependencies` 이동) |
| 최신 | 채팅 재진입 시 검색 상태 미초기화 버그 수정 |
| 최신 | `Navigator.pop()` 이후 deactivated context 사용으로 인한 crash 수정 |
| 최신 | `loadCharacters()` JSON 파싱 예외처리 누락으로 인한 앱 크래시 수정 |
| 최신 | APK 업데이트 시 서명 불일치(`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) 수정 |
| 최신 | 다크 모드 색상 대비 부족 전반 수정 |
