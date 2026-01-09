# OCPP 1.6J Practice Server

OCPP(Open Charge Point Protocol) 1.6J 프로토콜 학습을 위한 연습용 서버 프로젝트.

**Spring Boot 없이 순수 Java로 구현**하여 OCPP 프로토콜의 핵심 구조를 이해하는 것이 목표.

## 프로젝트 소개

### OCPP란?

OCPP는 전기차 충전기(Charge Point)와 중앙 관리 시스템(Central System) 간의 통신을 위한 개방형 프로토콜입니다.

```
┌─────────────────┐         WebSocket          ┌─────────────────┐
│   Charge Point  │ ◄─────────────────────────►│  Central System │
│    (충전기)      │         OCPP 1.6J          │    (이 서버)     │
└─────────────────┘                            └─────────────────┘
```

### 왜 Spring Boot 없이?

| Spring Boot 사용 시 | 순수 Java 사용 시 |
|-------------------|-----------------|
| 프레임워크 설정이 먼저 보임 | OCPP 프로토콜 구조가 보임 |
| DI가 핵심인지 헷갈림 | 메시지 흐름이 명확함 |
| "이게 Spring인지 OCPP인지" 모호 | 직접 만들어서 이해 |

> **OCPP 연습은 "프레임워크 제거"가 아니라 "프로토콜 노출"이 목적.**

---

## Getting Started

### 요구사항

- **Java 17+**
- **Gradle 8+** (Wrapper 포함)

### 설치 및 실행

```bash
# 1. 프로젝트 클론
git clone <repository-url>
cd ocpp_basic

# 2. 빌드
./gradlew build

# 3. 서버 실행
./gradlew run
```

**서버 실행 시 출력:**
```
===========================================
OCPP WebSocket Server started on port 8081
Waiting for charge point connections...
===========================================
Press Ctrl+C to stop the server
```

### 테스트 클라이언트 실행

서버가 실행 중인 상태에서 테스트 클라이언트 실행:

```bash
# Node.js
cd test_client/nodejs
npm install
npm start

# Python
cd test_client/python
pip install -r requirements.txt
python test_client.py

# Java
cd test_client/java
./gradlew run
```

### 단위 테스트

```bash
./gradlew test
```

---

## 프로젝트 구조

```
ocpp_basic/
├── build.gradle
├── README.md
├── docs/                              # 문서
│   ├── 01-OCPP-OVERVIEW.md           # OCPP 개요
│   ├── 02-MESSAGE-FORMAT.md          # 메시지 포맷
│   ├── 03-PROJECT-STRUCTURE.md       # 프로젝트 구조
│   ├── 04-IMPLEMENTATION-GUIDE.md    # 구현 가이드
│   └── 05-STATE-MACHINE.md           # 상태 머신
│
├── src/main/java/com/clnewze/lab/www/
│   ├── Main.java                      # 진입점
│   ├── protocol/                      # OCPP 프로토콜 정의
│   │   ├── MessageType.java          # 메시지 타입 (2, 3, 4)
│   │   ├── OcppMessage.java          # 메시지 인터페이스
│   │   ├── Call.java                 # 요청 메시지 [2]
│   │   ├── CallResult.java           # 성공 응답 [3]
│   │   ├── CallError.java            # 에러 응답 [4]
│   │   └── ErrorCode.java            # 에러 코드 enum
│   ├── action/                        # Action 핸들러
│   │   ├── ActionHandler.java        # 핸들러 인터페이스
│   │   ├── BootNotificationHandler.java
│   │   ├── HeartbeatHandler.java
│   │   ├── StatusNotificationHandler.java
│   │   ├── AuthorizeHandler.java
│   │   ├── StartTransactionHandler.java
│   │   ├── StopTransactionHandler.java
│   │   └── MeterValuesHandler.java
│   ├── session/                       # 세션 관리
│   │   ├── ChargePointSession.java   # 충전기 세션
│   │   └── SessionManager.java       # 세션 관리자
│   ├── state/                         # 상태 머신
│   │   └── ChargePointState.java     # 충전기 상태 enum
│   ├── router/                        # 메시지 라우팅
│   │   └── MessageRouter.java        # Action → Handler
│   └── transport/websocket/           # WebSocket 서버
│       └── OcppWebSocketServer.java
│
├── src/test/java/                     # 단위 테스트
│
└── test_client/                       # 테스트 클라이언트
    ├── README.md
    ├── python/
    ├── nodejs/
    └── java/
```

---

## 구현된 Action

### CP → CS (충전기 → 서버)

| Action | 설명 | 구현 |
|--------|------|:----:|
| BootNotification | 충전기 부팅 알림 | ✅ |
| Heartbeat | 연결 유지 확인 | ✅ |
| StatusNotification | 상태 변경 알림 | ✅ |
| Authorize | RFID 인증 | ✅ |
| StartTransaction | 충전 시작 | ✅ |
| StopTransaction | 충전 종료 | ✅ |
| MeterValues | 계량 데이터 | ✅ |

### CS → CP (서버 → 충전기)

| Action | 설명 | 구현 |
|--------|------|:----:|
| RemoteStartTransaction | 원격 충전 시작 | ❌ |
| RemoteStopTransaction | 원격 충전 중지 | ❌ |
| Reset | 충전기 재시작 | ❌ |
| ChangeConfiguration | 설정 변경 | ❌ |

---

## OCPP 메시지 포맷

### Call (요청) - Type 2

```json
[2, "uniqueId", "Action", {payload}]
```

**예시: BootNotification**
```json
[2, "boot-001", "BootNotification", {
  "chargePointVendor": "TestVendor",
  "chargePointModel": "TestModel"
}]
```

### CallResult (성공 응답) - Type 3

```json
[3, "uniqueId", {payload}]
```

**예시: BootNotification 응답**
```json
[3, "boot-001", {
  "status": "Accepted",
  "currentTime": "2024-01-15T10:00:00Z",
  "interval": 300
}]
```

### CallError (에러 응답) - Type 4

```json
[4, "uniqueId", "errorCode", "description", {details}]
```

---

## 테스트 시나리오

전체 충전 흐름 테스트:

```
1. BootNotification  → 충전기 등록
2. Heartbeat         → 연결 확인
3. StatusNotification → Available 상태 알림
4. Authorize         → RFID 인증
5. StartTransaction  → 충전 시작
6. MeterValues       → 계량 데이터 전송
7. StopTransaction   → 충전 종료
8. StatusNotification → Available 상태 복귀
```

---

## 기술 스택

- **Java 17**
- **Gradle**
- **Java-WebSocket** (WebSocket 서버)
- **Gson** (JSON 파싱)
- **JUnit 5** (테스트)
- **SLF4J + Logback** (로깅)

---

## 문서

- [01-OCPP-OVERVIEW.md](docs/01-OCPP-OVERVIEW.md) - OCPP 개요
- [02-MESSAGE-FORMAT.md](docs/02-MESSAGE-FORMAT.md) - 메시지 포맷
- [03-PROJECT-STRUCTURE.md](docs/03-PROJECT-STRUCTURE.md) - 프로젝트 구조
- [04-IMPLEMENTATION-GUIDE.md](docs/04-IMPLEMENTATION-GUIDE.md) - 구현 가이드
- [05-STATE-MACHINE.md](docs/05-STATE-MACHINE.md) - 상태 머신
- [06-ROADMAP.md](docs/06-ROADMAP.md) - 로드맵 및 개선 계획

---

## 다음 단계

- [ ] 서버 → 충전기 요청 구현 (RemoteStartTransaction, RemoteStopTransaction)
- [ ] 상태 전이 검증 로직 추가
- [ ] 트랜잭션 관리 (메모리 저장)
- [ ] 로깅 개선

---

## 라이선스

MIT License