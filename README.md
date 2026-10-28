# 모동숲(닌텐도 게임) 교환 정보 게시판

웹서비스설계 JWT 게시판 과제입니다.

## 작성자와 서비스 소개

- 학번 / 이름: 202446124 / 김민영
- 서비스 목적과 대상 사용자: 닌텐도 스위치 게임 "모여봐요 동물의 숲"을 하는 사용자들이 필요한 아이템을 구하거나 남는 아이템을 나눔하고, 섬 방문과 같은 게임 정보를 주고받을 수 있는 게시판입니다. 아이템을 교환할 때 게임 유저들만의 소통 공간이 있으면 편리하겠다는 생각에서 시작했습니다. 게시글은 로그인하지 않아도 누구나 볼 수 있고, 글 작성은 로그인한 사용자만, 수정과 삭제는 글을 쓴 본인만 할 수 있도록 서버에서 JWT와 작성자를 검사합니다. (화면 제목: 모여봐요 동물의 숲(게임) 정보 교환 게시판)
- 선택한 기술: Vue + Spring Boot
- 주요 기능: 공개 목록과 상세 조회, 로그인과 JWT 발급, 내 정보 조회, 본인 글 작성과 수정, 삭제, 토큰 만료와 로그아웃 처리, SQLite 데이터 유지

## 보고서와 테스트 증거

- [구현 보고서 PDF](docs/report.pdf)
- [테스트 증거](docs/evidence/)

## 프로젝트 구조

```text
.
├── README.md
├── .gitignore
├── frontend-vue/        # Vue 프론트 (src/api.js, src/App.vue)
├── backend-spring/      # Spring Boot 백엔드 (AuthService.java 등)
├── tools/               # 제공된 API 검사 스크립트
└── docs/
    ├── report.pdf
    ├── evidence/        # T01~T17 테스트 캡처
    └── 01~07 과제 안내 문서
```

## 실행 환경

| 항목 | 사용 버전 |
|---|---|
| 운영체제 | Windows 11 |
| 프론트 런타임 Node.js | v24.21.0 (20.19 이상 필요) |
| 백엔드 런타임 | JDK 27 (17 이상 필요) |
| 프레임워크 | Vue 3.5.43, Vite 8.3.4, Spring Boot 3.5.16, JJWT 0.13.0 |
| 데이터 저장 | SQLite (backend-spring/data/board.sqlite) |

## 설치와 실행

### 1 저장소 복제

```sh
git clone https://github.com/Miy329/web_service_hw2.git
cd web_service_hw2
```

### 2 백엔드 준비와 실행

`backend-spring` 폴더에서 실행합니다. Spring은 `.env`를 자동으로 읽지 않으므로, 서버를 실행할 터미널에서 환경변수를 먼저 설정합니다.

- 환경설정 예시 파일: `backend-spring/env.example.txt`
- JWT_SECRET 설정 방법: 아래 명령으로 64자리 16진수 키를 새로 만들어 환경변수로 넣습니다. 실제 키는 저장소에 기록하지 않습니다.

  ```sh
  node -e "console.log(require('node:crypto').randomBytes(32).toString('hex'))"
  ```

- TOKEN_TTL: 기본값 90초입니다. 만료 테스트를 할 때는 3처럼 작은 값으로 설정한 뒤 서버를 다시 실행합니다.
- 백엔드 주소: http://127.0.0.1:8000
- 정상 실행 확인 방법: http://127.0.0.1:8000/api/health 접속 시 `{"status":"ok","stack":"spring"}` 응답

Windows (cmd):

```bat
cd backend-spring
set JWT_SECRET=새로_만든_64자리_16진수_키
set TOKEN_TTL=90
.\mvnw.cmd package
java -jar target\jwt-board-1.0.0.jar
```

Windows (PowerShell):

```powershell
cd backend-spring
$env:JWT_SECRET='새로_만든_64자리_16진수_키'
$env:TOKEN_TTL='90'
.\mvnw.cmd package
java -jar target\jwt-board-1.0.0.jar
```

macOS / Linux:

```sh
cd backend-spring
export JWT_SECRET='새로_만든_64자리_16진수_키'
export TOKEN_TTL=90
chmod +x mvnw
./mvnw package
java -jar target/jwt-board-1.0.0.jar
```

JWT_SECRET이 없거나 64자리 16진수가 아니면 서버가 실행되지 않습니다.

### 3 프론트 빌드와 로컬 실행

백엔드를 켜 둔 상태에서 새 터미널을 열고 실행합니다.

```sh
cd frontend-vue
npm ci
npm run build
npm run preview
```

- 최종 확인 주소: http://127.0.0.1:4173
- 개발 중 실행: `npm run dev`, http://127.0.0.1:5173
- API 연결 방식: 브라우저는 4173번(또는 5173번)의 `/api`로 요청하고, Vite 프록시가 이 요청을 8000번 백엔드로 전달합니다. 백엔드 포트를 바꾸면 `frontend-vue/vite.config.js`의 프록시 주소도 함께 바꿉니다.
- 종료: 프론트와 백엔드를 실행한 각 터미널에서 Ctrl+C
- 코드 변경 후 재실행: 프론트는 `npm run build` 후 `npm run preview`를 다시 실행하고, 백엔드는 기존 서버를 Ctrl+C로 끈 뒤 `mvnw package`와 `java -jar`를 다시 실행합니다. 기존 서버가 켜진 상태에서는 jar 파일을 교체할 수 없어 빌드가 실패할 수 있습니다.

### 4 테스트 계정과 DB

| 사용자 | 이메일 | 실습용 비밀번호 |
|---|---|---|
| Alice | alice@class.test | Classroom!2026 |
| Bob | bob@class.test | Classroom!2026 |

- SQLite 파일 위치와 자동 생성 방식: `backend-spring/data/board.sqlite`이며, 서버를 처음 실행할 때 폴더와 파일, 테이블이 자동으로 생성됩니다.
- DB 초기화 방법: 서버를 종료한 뒤 `backend-spring/data/board.sqlite`를 삭제하고 다시 실행합니다. 기존 게시글이 모두 지워집니다.
- 서버 재시작 후 데이터 유지 확인 결과: 서버를 종료한 뒤 다시 실행해도 게시글과 작성자가 그대로 유지되었습니다(T15).

위 계정은 과제 전용 테스트 계정입니다.

## 구현 완료 범위

| 기능 | 완료 여부 | 주요 파일 또는 함수 |
|---|---|---|
| 공개 목록과 상세 | 완료 | BoardController.java, App.vue (제공 코드) |
| 로그인과 JWT 발급 | 완료 | AuthService.java / issueToken(), api.js / login() |
| JWT 검증과 내 정보 조회 | 완료 | AuthService.java / currentUserId() |
| 글 작성과 작성자 저장 | 완료 | BoardController.java / create(), AuthService.java / currentUserId() |
| 본인 글 수정·삭제 | 완료 | AuthService.java / assertOwner(), api.js / canEdit() |
| 토큰 만료와 로그아웃 | 완료 | api.js / request(), clearSession(), App.vue / logout() |
| 입력 오류와 권한 오류 처리 | 완료 | BoardController.java / fields(), App.vue / errorMessage() |

알려진 문제와 미완성 기능: 로그아웃은 현재 탭의 토큰만 지우기 때문에, 이미 복사된 토큰은 만료 시간 전까지 서버에서 사용할 수 있습니다.

## 테스트 방법과 결과 요약

- 실행한 검사 방법과 실행 위치: 4173번 빌드 화면에서 개발자 도구 Network로 확인하고, 별도 터미널에서 curl -i로 서버 응답을 직접 확인했습니다. 일반 테스트는 TOKEN_TTL=600, 만료 테스트는 TOKEN_TTL=3으로 진행했습니다.
- Alice/Bob 권한 구분 확인 결과: Bob의 정상 토큰으로 Alice 글을 수정, 삭제하면 403 FORBIDDEN이 반환되고 글은 그대로 유지되었습니다. Bob 본인 글의 등록, 수정, 삭제는 201, 200, 204로 성공했습니다.
- HTTP 상태와 오류 code 확인: 200, 201, 204 / 400 BAD_INPUT, 401(INVALID_CREDENTIALS, TOKEN_REQUIRED, INVALID_TOKEN, TOKEN_EXPIRED), 403 FORBIDDEN, 404 NOT_FOUND를 모두 확인했습니다. 자세한 기록은 보고서 4장에 있습니다.
- 응답 헤더와 본문 확인: 글 등록 201 응답의 Location: /api/posts/{id}, 보호 API 401 응답의 WWW-Authenticate: Bearer, 삭제 204 응답의 빈 본문을 확인했습니다.
- 토큰 없음, 변조, 만료 확인 결과: 각각 401 TOKEN_REQUIRED, 401 INVALID_TOKEN, 401 TOKEN_EXPIRED가 반환되었고, 화면에서는 로그인 상태가 해제되었습니다.
- 전체 T01~T17 결과와 증거: 모두 PASS이며, 결과 표는 [보고서](docs/report.pdf) 4장, 캡처는 [docs/evidence](docs/evidence/)에 있습니다.

## 핵심 구현 설명

1. 로그인: `POST /api/auth/login`에서 계정을 확인한 뒤 `AuthService.issueToken()`이 사용자 id, 발급자, 사용 대상, 만료 시간을 담은 JWT를 HS256으로 서명해 발급합니다.
2. 토큰 보관과 전송: `api.js`의 `login()`이 토큰을 메모리(session 변수)에만 저장하고, `request()`가 요청마다 `Authorization: Bearer` 헤더를 붙입니다. 401을 받으면 `clearSession()`으로 토큰을 지웁니다.
3. 서버 검증: 보호 API는 `AuthService.currentUserId()`에서 서명, 만료, 발급자, 사용 대상, 사용자 존재 여부를 확인한 뒤 사용자 id를 넘깁니다.
4. 작성자 권한 검사: 글 작성자는 토큰에서 확인한 사용자 id로 저장하고, 수정과 삭제 전에는 `AuthService.assertOwner()`가 요청자와 작성자를 비교해 다르면 403을 반환합니다.

상세 코드 설명은 보고서 3장에 있습니다.

## AI 활용 요약과 참고 자료

- AI 사용 여부: 사용
- 도구와 사용 목적: Claude Opus 5.5를 과제 이해, 프론트엔드와 백엔드, DB의 연결 구조와 JWT 전달 흐름 학습, 코드 초안 작성과 함께 분석하기, 빌드와 실행 오류 원인 확인, T01~T17 테스트 절차 정리, 보고서 오탈자 수정과 문장 정리, README 작성 방법 확인에 사용했습니다.
- 반영 위치와 직접 수정, 검증한 내용: AuthService.java, api.js, App.vue와 보고서 전반에 반영했습니다. AI 초안은 그대로 쓰지 않고 강의자료, 과제 안내 문서와 비교하며 직접 수정했고, 코드마다 동작을 이해한 내용을 주석으로 남겼습니다. 모든 기능은 T01~T17 테스트로 응답 코드를 직접 확인했습니다.
- 상세 활용 기록: [보고서](docs/report.pdf) 5장
- 참고한 수업 자료: 웹서비스설계 3주차 강의자료 「게시판 서비스 설계와 구현」 27쪽(상태 코드), 7주차 강의자료 「JWT 접근 제어와 Swagger」 53쪽(토큰 검증), 64, 65, 68쪽(작성자 권한), 과제 안내 문서(docs/01~07). 강의자료는 LMS에 있어 URL은 따로 없습니다.

## 제출 확인

- [x] README의 안내 문구를 실제 내용으로 교체했다.
- [x] 새 폴더에 설치하여 README의 명령으로 환경설정, 빌드, 로컬 실행을 확인했다.
- [x] 4173번 빌드 화면에서 API 연결과 전체 필수 기능을 확인했다.
- [x] GitHub에서 보고서와 증거 링크가 열린다.
- [x] 교수자가 저장소를 열람, 복제할 수 있다. (공개 저장소)
- [x] 비밀 키, 실제 토큰, DB, node_modules, target을 커밋하지 않았다.
- [x] 최종 push 후 저장소 URL과 제출 기준 커밋 SHA를 LMS에 제출했다.
