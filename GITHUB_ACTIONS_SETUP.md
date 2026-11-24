# GitHub Actions로 APK 자동 빌드하기 — 설정 가이드

이 문서는 `.github/workflows/build-apk.yml`을 실제로 작동시키기 위해 **사용자가 직접** 해야 하는
단계를 정리한 것입니다. (Claude가 이 PC에 연결되어 있지만, 셸 명령 실행과 GitHub 저장소 생성/연결은
이 환경에서 직접 할 수 없어서 — 아래 단계는 전부 사용자가 직접 진행해야 합니다.)

## 왜 Claude가 직접 못 하는지

- 이 세션의 GitHub 연동은 "이미 지정된 특정 저장소"에만 접근할 수 있게 묶여 있고, 새 저장소를
  만들거나 임의의 저장소에 접근하는 건 차단되어 있습니다 (`sessions are bound to their configured
  repositories`).
- 이 PC 연결에는 원격 셸(`device_bash`)이 없어서 `git init`, `git push` 같은 명령을 대신 실행할 수
  없습니다. 파일을 읽고 쓰는 것만 가능합니다.

그래서 **저장소 생성 + 최초 push**는 사용자가 직접 해야 하고, 그 이후부터는 GitHub Actions가
알아서 매 push마다 APK를 만들어줍니다.

## 1. GitHub에 새 저장소 만들기

1. https://github.com/new 접속 (fildslabs 계정으로 로그인된 상태여야 함)
2. Repository name: `EuroJackpotAI` (원하는 이름으로 바꿔도 됩니다)
3. Private 선택 (개인 프로젝트이므로 추천)
4. README, .gitignore, license는 전부 체크 해제 (이미 로컬에 있음) → **Create repository**

## 2. 로컬 프로젝트를 이 저장소에 연결하고 push하기

가장 쉬운 방법은 **Android Studio 안에서** 하는 것입니다 (터미널 타이핑 불필요):

1. Android Studio에서 이 프로젝트 열기
2. 상단 메뉴 **VCS > Share Project on GitHub** (또는 **Git > GitHub > Share Project on GitHub**)
3. 이미 1번에서 만든 저장소가 있으니, 대신 **VCS > Enable Version Control Integration** → Git 선택
   → 그다음 **Git > Manage Remotes**에서 1번에서 만든 저장소의 URL
   (`https://github.com/fildslabs/EuroJackpotAI.git`)을 `origin`으로 추가
4. 전체 파일을 커밋(Commit) → **VCS > Git > Push**로 push

또는 터미널(PowerShell/cmd)을 직접 쓸 수 있다면 프로젝트 폴더에서:

```
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/fildslabs/EuroJackpotAI.git
git push -u origin main
```

(GitHub 로그인 창이 뜨면 브라우저로 로그인하면 됩니다.)

## 3. 확인하기

push가 끝나면 GitHub 저장소 페이지의 **Actions** 탭에서 "Build Debug APK" 워크플로가 자동으로
시작됩니다 (`.github/workflows/build-apk.yml`이 이미 포함되어 있으므로). 몇 분 뒤 그 실행 결과
페이지 맨 아래 **Artifacts** 섹션에서 `EuroJackpotAI-debug-apk`를 다운로드하면 안에 `.apk` 파일이
들어있습니다. 그걸 폰에 옮겨서 설치하면 됩니다.

이후부터는 (Claude가 이 코드를 수정해서 PC에 반영해준 뒤) 사용자가 그냥 평소처럼 커밋 + push만
하면, 매번 자동으로 새 APK가 만들어집니다. 수동으로 바로 돌리고 싶으면 Actions 탭에서
**Run workflow** 버튼을 누르면 됩니다.

## 참고

- 디버그 APK라서 서명 키 설정이 필요 없습니다 (AGP가 자동으로 디버그 키로 서명). 나중에 Play
  스토어용 정식 서명 APK가 필요해지면 그때 keystore를 만들어서 GitHub Secrets에 등록하고
  워크플로에 서명 스텝을 추가하면 됩니다.
- `LOTTO_API_KEY`는 등록 안 해도 빌드는 항상 성공합니다 (앱이 샘플 데이터 모드로 동작). 실제 키를
  CI 빌드에도 쓰고 싶으면 저장소 **Settings > Secrets and variables > Actions > New repository
  secret**에서 이름 `LOTTO_API_KEY`, 값은 `local.properties`에 있는 실제 키로 등록하면 됩니다.
