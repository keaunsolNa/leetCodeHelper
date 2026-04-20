# LeetCode Helper

LeetCode 문제 자동 수신, 코드 제출, Git 정리를 자동화하는 Spring Boot 로컬 서버입니다.

## 주요 기능

| 기능 | 설명 |
|------|------|
| 문제 자동 수신 | 스케줄러가 LeetCode 오늘의 문제를 지정 시각에 자동으로 가져와 저장 |
| 전체 문제 동기화 | 로컬에 없는 모든 문제를 일괄 수신 |
| SQL 문제 지원 | Database 태그 문제를 자동 감지하여 `.sql` 파일로 저장, 별도 언어 설정 적용 |
| 코드 제출 | IntelliJ External Tool로 현재 열린 Solution 파일을 LeetCode에 제출 |
| Git 자동화 | Accepted 시 UnSolved → Solved 이동 후 자동 commit & push |
| AI 코드 리뷰 | Accepted 시 Groq(llama-3.3-70b) 기반 한국어 코드 리뷰를 `analysis.md`에 저장 |

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

Accepted 시 생성되는 파일:
```
Solved/Easy/0001-two-sum/
├── problem.md      # 문제 설명
├── Solution.java   # 제출한 코드
└── analysis.md     # 성능 지표 + AI 코드 리뷰
```

## 환경 변수 설정 (IntelliJ Edit Configurations)

`Run → Edit Configurations → Environment Variables`에 아래 값을 입력합니다.

| 환경 변수 | 설명 | 기본값 |
|-----------|------|--------|
| `LEETCODE_SESSION` | 브라우저 쿠키 `LEETCODE_SESSION` 값 | (필수) |
| `LEETCODE_CSRF_TOKEN` | 브라우저 쿠키 `csrftoken` 값 | (필수) |
| `LEETCODE_REPO_PATH` | Coding_Test 레포 로컬 경로 | (필수) |
| `LEETCODE_GIT_USERNAME` | Git 커밋 username | (필수) |
| `LEETCODE_GIT_EMAIL` | Git 커밋 email | (필수) |
| `GROQ_API_KEY` | Groq API 키 ([console.groq.com](https://console.groq.com) 발급, 무료) | (필수) |
| `LEETCODE_ALG_LANGUAGE` | 알고리즘 문제 제출 언어 | `java` |
| `LEETCODE_SQL_LANGUAGE` | SQL 문제 제출 언어 | `mysql` |
| `LEETCODE_SCHEDULE_CRON` | 오늘의 문제 수신 cron | `0 0 9 * * MON-FRI` |
| `LEETCODE_ALL_FETCH_CRON` | 전체 문제 동기화 cron | `0 0 6 * * MON` |

> 쿠키 값은 leetcode.com 로그인 후 개발자도구 → Application → Cookies에서 복사합니다. 약 2주마다 갱신 필요.

> SQL 언어 옵션: `mysql`, `mssql`, `oraclesql`

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
