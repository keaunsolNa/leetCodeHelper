# LeetCode Helper

LeetCode 문제 자동 수신, 코드 제출, Git 정리를 자동화하는 Spring Boot 로컬 서버입니다.

## 주요 기능

| 기능 | 설명 |
|------|------|
| 문제 자동 수신 | 스케줄러가 LeetCode 오늘의 문제를 지정 시각에 자동으로 가져와 저장 |
| 전체 문제 동기화 | 로컬에 없는 모든 문제를 일괄 수신 |
| 코드 제출 | IntelliJ External Tool로 현재 열린 Solution 파일을 LeetCode에 제출 |
| Git 자동화 | Accepted 시 UnSolved → Solved 이동 후 자동 commit & push |

## 디렉터리 구조 (Coding_Test 레포)

```
Coding_Test/
└── leetcode/
    ├── UnSolved/
    │   ├── Easy/
    │   ├── Med/
    │   └── Hard/
    └── Solved/
        ├── Easy/
        ├── Med/
        └── Hard/
```

## 환경 변수 설정 (IntelliJ Edit Configurations)

`Run → Edit Configurations → Environment Variables`에 아래 값을 입력합니다.

| 환경 변수 | 설명 | 예시 |
|-----------|------|------|
| `LEETCODE_SESSION` | 브라우저 쿠키 `LEETCODE_SESSION` 값 | `eyJhbGci...` |
| `LEETCODE_CSRF_TOKEN` | 브라우저 쿠키 `csrftoken` 값 | `TN7Whg...` |
| `LEETCODE_REPO_PATH` | Coding_Test 레포 로컬 경로 | `C:/Users/USER/IdeaProjects/Coding_Test` |
| `LEETCODE_GIT_USERNAME` | Git 커밋 username | `keaunsolNa` |
| `LEETCODE_GIT_EMAIL` | Git 커밋 email | `knsol1992@naver.com` |
| `LEETCODE_LANGUAGE` | 제출 언어 (기본값: `java`) | `java`, `python3`, `cpp` |
| `LEETCODE_SCHEDULE_CRON` | 오늘의 문제 수신 cron (기본값: 평일 오전 9시) | `0 0 9 * * MON-FRI` |
| `LEETCODE_ALL_FETCH_CRON` | 전체 문제 동기화 cron (기본값: 매주 월 오전 6시) | `0 0 6 * * MON` |

> 쿠키 값은 leetcode.com 로그인 후 개발자도구 → Application → Cookies에서 복사합니다. 약 2주마다 갱신 필요.

## API 엔드포인트

| Method | URL | 설명 |
|--------|-----|------|
| `POST` | `/api/problem/daily` | 오늘의 문제 즉시 수신 |
| `POST` | `/api/problem/all` | 로컬에 없는 전체 문제 일괄 수신 |
| `POST` | `/api/problem/{slug}` | 특정 문제 수신 (예: `two-sum`) |
| `POST` | `/api/submit` | Solution 파일 제출 |

## IntelliJ External Tool 설정 (코드 제출)

`File → Settings → Tools → External Tools → +`

| 항목 | 값 |
|------|-----|
| Name | `LeetCode Submit` |
| Program | `powershell.exe` |
| Arguments | `-Command "Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/submit' -ContentType 'application/json' -Body (ConvertTo-Json @{filePath='$FilePath$'})"` |
| Working directory | `$ProjectFileDir$` |

Solution 파일 우클릭 → `External Tools → LeetCode Submit`

## 실행

```bash
./gradlew bootRun
```

서버는 `http://localhost:8080`에서 실행됩니다.
