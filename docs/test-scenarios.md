# Test Scenarios — leetCodeHelper
Generated: 2026-04-19T13:29:13Z

## Passing scenarios (1/1)

### should_loadContext_when_applicationStarts
**Given** Spring Boot 애플리케이션 컨텍스트  
**When**  애플리케이션 시작  
**Then**  모든 Bean이 정상 로드됨

---

## Failing scenarios
(없음 — 전체 통과)

---

## 리팩토링 내역 (이번 실행)~~~~
- `leetcode/UnSolved/{Easy|Med|Hard}/{id}-{slug}/` 구조로 경로 변경
- `leetcode/Solved/{Easy|Med|Hard}/{id}-{slug}/` 구조로 Solved 이동 경로 변경
- `LeetCodeProperties`에 `leetcodeDir` 필드 추가
- `MarkdownService` — 난이도 디렉터리 포함 저장 (`Medium` → `Med`)
- `GitService.moveProblemToSolved()` — `difficultyDir` 파라미터 추가
