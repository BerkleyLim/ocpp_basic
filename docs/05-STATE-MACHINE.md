# 상태 머신 (State Machine)

## 왜 상태 머신인가?

충전기는 **항상 특정 상태**에 있다:
- 충전 중인데 또 충전 시작? ❌
- 연결 안 됐는데 충전 시작? ❌
- 고장 상태에서 예약? ❌

**상태에 따라 허용되는 동작이 다르다.**

```java
// 나쁜 예: 상태 없이 처리
void startCharging() {
    // 그냥 시작... 지금 뭔 상태인지 모름
}

// 좋은 예: 상태 확인 후 처리
void startCharging() {
    if (state != AVAILABLE && state != PREPARING) {
        throw new IllegalStateException("Cannot start charging in " + state);
    }
    state = CHARGING;
}
```

---

## OCPP 1.6 상태 종류

### ChargePointStatus (커넥터 상태)

| 상태 | 설명 | 충전 가능? |
|------|------|-----------|
| **Available** | 사용 가능, 대기 중 | O |
| **Preparing** | 충전 준비 중 (케이블 연결됨) | △ |
| **Charging** | 충전 중 | X (이미 중) |
| **SuspendedEV** | EV가 충전 일시 중지 | X |
| **SuspendedEVSE** | 충전기가 충전 일시 중지 | X |
| **Finishing** | 충전 완료 처리 중 | X |
| **Reserved** | 예약됨 | X |
| **Unavailable** | 사용 불가 | X |
| **Faulted** | 오류 상태 | X |

### 우리가 사용할 상태 (단순화)

```java
public enum ChargePointState {
    DISCONNECTED,   // WebSocket 연결 끊김
    CONNECTED,      // WebSocket 연결됨 (Boot 전)
    AVAILABLE,      // 충전 가능
    PREPARING,      // 충전 준비 중
    CHARGING,       // 충전 중
    FINISHING,      // 충전 완료 처리 중
    RESERVED,       // 예약됨
    UNAVAILABLE,    // 사용 불가
    FAULTED         // 오류
}
```

---

## 상태 전이 다이어그램

### 전체 흐름

```
                    ┌────────────────────────────────────┐
                    │                                    │
                    ▼                                    │
    ┌──────────────────────────┐                        │
    │      DISCONNECTED        │                        │
    └────────────┬─────────────┘                        │
                 │ WebSocket 연결                        │
                 ▼                                       │
    ┌──────────────────────────┐                        │
    │       CONNECTED          │                        │
    └────────────┬─────────────┘                        │
                 │ BootNotification                     │
                 │ → Accepted                           │
                 ▼                                       │
    ┌──────────────────────────┐◄───────────────────────┤
    │       AVAILABLE          │         StopTransaction │
    └────────────┬─────────────┘         또는 Finishing  │
                 │                       완료            │
                 │ 케이블 연결 or                        │
                 │ RemoteStart                          │
                 ▼                                       │
    ┌──────────────────────────┐                        │
    │       PREPARING          │                        │
    └────────────┬─────────────┘                        │
                 │ StartTransaction                     │
                 ▼                                       │
    ┌──────────────────────────┐                        │
    │       CHARGING           │                        │
    └────────────┬─────────────┘                        │
                 │ 충전 완료                             │
                 ▼                                       │
    ┌──────────────────────────┐                        │
    │       FINISHING          ├────────────────────────┘
    └──────────────────────────┘
```

### 에러 상태

```
    어떤 상태에서든
           │
           │ 오류 발생
           ▼
    ┌──────────────────────────┐
    │        FAULTED           │
    └────────────┬─────────────┘
                 │ 오류 해결
                 ▼
    ┌──────────────────────────┐
    │       AVAILABLE          │
    └──────────────────────────┘
```

---

## 상태 전이 규칙

### 허용되는 전이

```java
public class StateTransition {

    private static final Map<ChargePointState, Set<ChargePointState>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(ChargePointState.class);

        // DISCONNECTED → CONNECTED (WebSocket 연결)
        ALLOWED_TRANSITIONS.put(DISCONNECTED, Set.of(CONNECTED));

        // CONNECTED → AVAILABLE (Boot 성공) or 다시 DISCONNECTED
        ALLOWED_TRANSITIONS.put(CONNECTED, Set.of(AVAILABLE, DISCONNECTED));

        // AVAILABLE → PREPARING, RESERVED, UNAVAILABLE, FAULTED
        ALLOWED_TRANSITIONS.put(AVAILABLE, Set.of(PREPARING, RESERVED, UNAVAILABLE, FAULTED, DISCONNECTED));

        // PREPARING → CHARGING, AVAILABLE (취소), FAULTED
        ALLOWED_TRANSITIONS.put(PREPARING, Set.of(CHARGING, AVAILABLE, FAULTED, DISCONNECTED));

        // CHARGING → FINISHING, FAULTED
        ALLOWED_TRANSITIONS.put(CHARGING, Set.of(FINISHING, FAULTED, DISCONNECTED));

        // FINISHING → AVAILABLE, FAULTED
        ALLOWED_TRANSITIONS.put(FINISHING, Set.of(AVAILABLE, FAULTED, DISCONNECTED));

        // RESERVED → AVAILABLE (예약 취소), PREPARING (예약자 도착), FAULTED
        ALLOWED_TRANSITIONS.put(RESERVED, Set.of(AVAILABLE, PREPARING, FAULTED, DISCONNECTED));

        // UNAVAILABLE → AVAILABLE, FAULTED
        ALLOWED_TRANSITIONS.put(UNAVAILABLE, Set.of(AVAILABLE, FAULTED, DISCONNECTED));

        // FAULTED → AVAILABLE (오류 해결)
        ALLOWED_TRANSITIONS.put(FAULTED, Set.of(AVAILABLE, DISCONNECTED));
    }

    public static boolean canTransition(ChargePointState from, ChargePointState to) {
        Set<ChargePointState> allowed = ALLOWED_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public static void validateTransition(ChargePointState from, ChargePointState to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException(
                "Invalid state transition: " + from + " → " + to
            );
        }
    }
}
```

---

## 상태별 허용 Action

### DISCONNECTED

| Action | 허용? | 설명 |
|--------|-------|------|
| 모든 Action | ❌ | 연결 안 됨 |

### CONNECTED

| Action | 허용? | 설명 |
|--------|-------|------|
| BootNotification | ✅ | 부팅 알림 (필수) |
| Heartbeat | ❌ | Boot 후에만 |
| 기타 | ❌ | Boot 후에만 |

### AVAILABLE

| Action | 허용? | 설명 |
|--------|-------|------|
| Heartbeat | ✅ | 연결 유지 |
| StatusNotification | ✅ | 상태 알림 |
| Authorize | ✅ | 인증 요청 |
| StartTransaction | ❌ | PREPARING 거쳐야 함 |
| StopTransaction | ❌ | 충전 중 아님 |

### PREPARING

| Action | 허용? | 설명 |
|--------|-------|------|
| StartTransaction | ✅ | 충전 시작 |
| StatusNotification | ✅ | 상태 알림 |
| Heartbeat | ✅ | 연결 유지 |

### CHARGING

| Action | 허용? | 설명 |
|--------|-------|------|
| MeterValues | ✅ | 계량 데이터 전송 |
| StopTransaction | ✅ | 충전 종료 |
| StatusNotification | ✅ | 상태 알림 |
| Heartbeat | ✅ | 연결 유지 |
| StartTransaction | ❌ | 이미 충전 중 |

### FAULTED

| Action | 허용? | 설명 |
|--------|-------|------|
| StatusNotification | ✅ | 오류 상태 알림 |
| Heartbeat | ✅ | 연결 유지 |
| StartTransaction | ❌ | 오류 상태 |
| StopTransaction | ❌ | 충전 중 아님 |

---

## 구현 예제

### ChargePointSession에 상태 전이 적용

```java
public class ChargePointSession {
    private ChargePointState state = ChargePointState.CONNECTED;

    public void setState(ChargePointState newState) {
        StateTransition.validateTransition(this.state, newState);
        System.out.println("[State] " + chargePointId + ": " + state + " → " + newState);
        this.state = newState;
    }

    public boolean isInState(ChargePointState... allowedStates) {
        for (ChargePointState s : allowedStates) {
            if (this.state == s) return true;
        }
        return false;
    }
}
```

### Handler에서 상태 체크

```java
public class StartTransactionHandler implements ActionHandler {

    @Override
    public OcppMessage handle(ChargePointSession session, JsonObject payload) {
        // 상태 체크
        if (!session.isInState(ChargePointState.PREPARING, ChargePointState.AVAILABLE)) {
            return new CallError(
                session.getCurrentUniqueId(),
                ErrorCode.GENERIC_ERROR,
                "Cannot start transaction in state: " + session.getState()
            );
        }

        // 상태 전이
        session.setState(ChargePointState.CHARGING);

        // 응답 생성
        JsonObject response = new JsonObject();
        response.addProperty("transactionId", generateTransactionId());
        response.add("idTagInfo", createIdTagInfo("Accepted"));

        return new CallResult(session.getCurrentUniqueId(), response);
    }
}
```

### StatusNotification에서 상태 업데이트

```java
public class StatusNotificationHandler implements ActionHandler {

    @Override
    public OcppMessage handle(ChargePointSession session, JsonObject payload) {
        String statusStr = payload.get("status").getAsString();
        ChargePointState newState = mapStatus(statusStr);

        // 상태 전이 (유효하면)
        if (StateTransition.canTransition(session.getState(), newState)) {
            session.setState(newState);
        } else {
            System.out.println("[Warning] Invalid transition ignored: " +
                session.getState() + " → " + newState);
        }

        // 응답 (항상 빈 객체)
        return new CallResult(session.getCurrentUniqueId(), new JsonObject());
    }

    private ChargePointState mapStatus(String status) {
        return switch (status) {
            case "Available" -> ChargePointState.AVAILABLE;
            case "Preparing" -> ChargePointState.PREPARING;
            case "Charging" -> ChargePointState.CHARGING;
            case "Finishing" -> ChargePointState.FINISHING;
            case "Reserved" -> ChargePointState.RESERVED;
            case "Unavailable" -> ChargePointState.UNAVAILABLE;
            case "Faulted" -> ChargePointState.FAULTED;
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
    }
}
```

---

## 충전 시나리오 예시

### 정상 충전 흐름

```
시간  상태          이벤트
─────────────────────────────────────────
T0    DISCONNECTED  (서버 대기 중)
T1    CONNECTED     WebSocket 연결
T2    CONNECTED     → BootNotification
T3    AVAILABLE     ← Accepted
T4    AVAILABLE     사용자 RFID 태그
T5    AVAILABLE     → Authorize
T6    AVAILABLE     ← Accepted
T7    PREPARING     케이블 연결
T8    PREPARING     → StatusNotification(Preparing)
T9    PREPARING     → StartTransaction
T10   CHARGING      ← transactionId: 123
T11   CHARGING      → StatusNotification(Charging)
T12   CHARGING      → MeterValues (주기적)
T13   CHARGING      충전 완료 버튼
T14   FINISHING     → StopTransaction
T15   FINISHING     → StatusNotification(Finishing)
T16   AVAILABLE     케이블 분리
T17   AVAILABLE     → StatusNotification(Available)
```

### 오류 발생 시

```
시간  상태          이벤트
─────────────────────────────────────────
T10   CHARGING      충전 중...
T11   CHARGING      과전류 감지!
T12   FAULTED       → StatusNotification(Faulted)
T13   FAULTED       기사 점검
T14   FAULTED       오류 해결
T15   AVAILABLE     → StatusNotification(Available)
```

---

## 핵심 포인트

### 1. 상태는 메모리에 있다

```java
// ❌ 틀림: DB에서 매번 조회
ChargePointState state = db.getState(chargePointId);

// ✅ 맞음: 세션에서 바로 접근
ChargePointState state = session.getState();
```

**이유:**
- 실시간성 (DB 조회 지연 없음)
- 동시성 제어 용이
- 연결 끊기면 상태도 사라짐 (자연스러움)

### 2. 상태 전이는 검증한다

```java
// ❌ 틀림: 무조건 변경
session.state = newState;

// ✅ 맞음: 전이 가능 여부 확인
StateTransition.validateTransition(session.getState(), newState);
session.state = newState;
```

### 3. 상태와 DB는 분리한다

```
┌─────────────────┐     ┌─────────────────┐
│  메모리 상태     │     │    DB 기록      │
│ (실시간 제어용)  │     │  (이력 저장용)   │
│                 │     │                 │
│ - CHARGING      │ --> │ - 충전 시작 시각 │
│ - connectorId   │     │ - 충전량        │
│ - transactionId │     │ - 사용자 ID     │
└─────────────────┘     └─────────────────┘
```

- **메모리 상태:** 현재 뭘 할 수 있는지 결정
- **DB 기록:** 나중에 조회/정산용

---

## 요약

| 개념 | 설명 |
|------|------|
| **상태** | 충전기가 지금 뭘 하고 있는지 |
| **전이** | 상태 A에서 B로 바뀌는 것 |
| **전이 규칙** | A에서 B로 갈 수 있는지 정의 |
| **저장 위치** | 메모리 (세션 객체) |

**상태 머신을 직접 구현해봐야 "아 DB가 아니라 메모리 상태가 핵심이구나"가 체감된다.**

---

## 문서 끝

모든 문서 작성 완료:
1. [01-OCPP-OVERVIEW.md](./01-OCPP-OVERVIEW.md) - OCPP 개요
2. [02-MESSAGE-FORMAT.md](./02-MESSAGE-FORMAT.md) - 메시지 포맷
3. [03-PROJECT-STRUCTURE.md](./03-PROJECT-STRUCTURE.md) - 프로젝트 구조
4. [04-IMPLEMENTATION-GUIDE.md](./04-IMPLEMENTATION-GUIDE.md) - 구현 가이드
5. [05-STATE-MACHINE.md](./05-STATE-MACHINE.md) - 상태 머신 (현재 문서)