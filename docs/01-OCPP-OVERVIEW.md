# OCPP 개요

## OCPP란?

**OCPP (Open Charge Point Protocol)**는 충전기(Charge Point)와 중앙 관리 시스템(Central System) 간의 통신을 위한 개방형 프로토콜이다.

```
┌─────────────────┐         WebSocket          ┌─────────────────┐
│   Charge Point  │ ◄─────────────────────────►│  Central System │
│    (충전기)      │         OCPP 1.6J          │    (서버)        │
└─────────────────┘                            └─────────────────┘
```

### 핵심 구성 요소

| 구성 요소 | 역할 | 예시 |
|----------|------|------|
| **Charge Point (CP)** | 충전기 | 실제 EV 충전기 |
| **Central System (CS)** | 관리 서버 | 우리가 만들 서버 |
| **Connector** | 충전 포트 | 충전기 1대에 여러 개 있을 수 있음 |

---

## 왜 REST가 아닌가?

### REST의 한계

REST는 **요청-응답** 모델이다:
- 클라이언트가 요청 → 서버가 응답
- 서버가 먼저 클라이언트에게 메시지를 보낼 수 없음

```
❌ REST (단방향)
클라이언트 ──요청──► 서버
클라이언트 ◄──응답── 서버

서버가 먼저 메시지를 보내야 할 때?
→ 불가능. Polling 해야 함.
```

### OCPP의 요구사항

충전 시스템에서는 **양방향 통신**이 필수:

1. **충전기 → 서버**: "충전 시작됐어요" (StatusNotification)
2. **서버 → 충전기**: "이 충전 멈춰" (RemoteStopTransaction)

```
✅ OCPP/WebSocket (양방향)
충전기 ◄─────────────────► 서버
       양방향 메시지 가능
```

### 비교 요약

| 항목 | REST | OCPP (WebSocket) |
|------|------|------------------|
| 통신 방향 | 단방향 (요청-응답) | 양방향 |
| 서버 → 클라이언트 | Polling 필요 | 즉시 가능 |
| 연결 방식 | 요청마다 새 연결 | 지속 연결 |
| 실시간성 | 낮음 | 높음 |
| 충전 제어 | 어려움 | 쉬움 |

---

## OCPP는 RPC 프로토콜이다

OCPP는 **JSON-RPC 스타일**의 프로토콜이다.

### RPC (Remote Procedure Call)란?

원격에 있는 함수를 호출하는 것처럼 통신하는 방식.

```
로컬 함수 호출:
result = bootNotification(vendor, model)

RPC 호출 (OCPP):
→ [2, "123", "BootNotification", {vendor: "...", model: "..."}]
← [3, "123", {status: "Accepted", interval: 300}]
```

### OCPP 메시지 타입

| Type ID | 이름 | 방향 | 용도 |
|---------|------|------|------|
| **2** | Call | 요청 | 함수 호출 |
| **3** | CallResult | 응답 | 성공 응답 |
| **4** | CallError | 응답 | 에러 응답 |

### 메시지 흐름 예시

```
충전기                                서버
  │                                    │
  │──[2, "abc", "BootNotification", {...}]──►│  Call (요청)
  │                                    │
  │◄──[3, "abc", {status:"Accepted"}]──│  CallResult (응답)
  │                                    │
```

- `uniqueId`("abc")로 요청-응답 매칭
- 비동기 처리 가능 (여러 요청 동시에)

---

## OCPP 버전

### OCPP 1.6 (이 프로젝트에서 사용)

- **OCPP 1.6J**: JSON over WebSocket (우리가 쓸 것)
- OCPP 1.6S: SOAP over HTTP (레거시)

**선택 이유:**
- 실무에서 가장 많이 사용
- 구조가 단순해서 이해하기 쉬움
- 자료가 많음

### OCPP 2.0.1

- 보안 강화 (Device Management, Security)
- 기능 확장 (ISO 15118, Smart Charging 고도화)
- 1.6 개념 잡은 뒤에 확장하면 됨

---

## 핵심 개념

### 1. Action

OCPP에서 정의된 **명령(함수) 이름**.

**CP → CS (충전기가 서버에 요청)**
| Action | 설명 |
|--------|------|
| BootNotification | 충전기 부팅 알림 |
| Heartbeat | 연결 유지 확인 |
| Authorize | 사용자 인증 요청 |
| StartTransaction | 충전 시작 보고 |
| StopTransaction | 충전 종료 보고 |
| StatusNotification | 상태 변경 알림 |
| MeterValues | 계량 데이터 전송 |

**CS → CP (서버가 충전기에 요청)**
| Action | 설명 |
|--------|------|
| RemoteStartTransaction | 원격 충전 시작 |
| RemoteStopTransaction | 원격 충전 중지 |
| Reset | 충전기 재시작 |
| ChangeConfiguration | 설정 변경 |
| GetConfiguration | 설정 조회 |

### 2. Session

- 충전기 1대 = 1개의 논리적 세션
- WebSocket 연결이 세션
- **메모리에서 관리** (DB 아님)

```java
// 세션 = 충전기의 현재 상태를 담는 객체
class ChargePointSession {
    String chargePointId;     // 충전기 ID
    WebSocket connection;     // WebSocket 연결
    ChargePointState state;   // 현재 상태
    Map<String, Pending> pendingCalls;  // 응답 대기 중인 요청
}
```

### 3. State (상태)

충전기는 항상 특정 **상태**에 있다:

```
DISCONNECTED → CONNECTED → BOOT_ACCEPTED → AVAILABLE → CHARGING → ...
```

상태에 따라 허용되는 Action이 다름.
(상세 내용은 05-STATE-MACHINE.md 참고)

---

## 이 프로젝트의 목표

### 최소 목표 (이것만 되면 성공)

1. WebSocket 서버 띄운다
2. 충전기 1대 접속한다
3. `BootNotification` → `Accepted` 반환
4. `Heartbeat` → `currentTime` 반환
5. 세션이 유지된다

### 하지 않을 것

- Spring Boot 사용 ❌
- DB 연동 ❌
- 인증/보안 ❌
- 모든 Action 구현 ❌

### 왜?

> **OCPP 연습은 "프레임워크 제거"가 아니라 "프로토콜 노출"이 목적이다.**

Spring 쓰면:
- OCPP 개념보다 Spring 설정이 먼저 보임
- WebSocket이 OCPP인지 Spring WS인지 헷갈림
- "이게 프로토콜 구조인지 프레임워크 구조인지" 감이 흐려짐

연습 목적:
> OCPP가 어떤 메시지가 오고 → 어떻게 라우팅되고 → 상태가 어떻게 변하는지

---

## 다음 단계

→ [02-MESSAGE-FORMAT.md](./02-MESSAGE-FORMAT.md): 메시지 포맷 상세