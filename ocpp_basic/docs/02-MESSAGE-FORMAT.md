# OCPP 메시지 포맷

OCPP 1.6J는 **JSON 배열** 형태로 메시지를 주고받는다.

## 메시지 타입 개요

| Type ID | 이름 | 방향 | 설명 |
|---------|------|------|------|
| **2** | Call | 요청자 → 수신자 | 함수 호출 (요청) |
| **3** | CallResult | 수신자 → 요청자 | 성공 응답 |
| **4** | CallError | 수신자 → 요청자 | 에러 응답 |

---

## 1. Call (요청 메시지)

### 형식

```json
[2, "uniqueId", "action", {payload}]
```

| 인덱스 | 이름 | 타입 | 설명 |
|--------|------|------|------|
| 0 | MessageTypeId | Integer | 항상 `2` |
| 1 | UniqueId | String | 요청 식별자 (UUID 권장) |
| 2 | Action | String | 호출할 기능 이름 |
| 3 | Payload | Object | 요청 데이터 |

### 예시: BootNotification

```json
[
  2,
  "19223201",
  "BootNotification",
  {
    "chargePointVendor": "VendorX",
    "chargePointModel": "Model-1",
    "chargePointSerialNumber": "CP001",
    "firmwareVersion": "1.0.0"
  }
]
```

### 예시: Heartbeat

```json
[
  2,
  "19223202",
  "Heartbeat",
  {}
]
```

### 예시: Authorize

```json
[
  2,
  "19223203",
  "Authorize",
  {
    "idTag": "RFID12345678"
  }
]
```

### 예시: StartTransaction

```json
[
  2,
  "19223204",
  "StartTransaction",
  {
    "connectorId": 1,
    "idTag": "RFID12345678",
    "meterStart": 0,
    "timestamp": "2024-01-15T10:30:00Z"
  }
]
```

### 예시: StopTransaction

```json
[
  2,
  "19223205",
  "StopTransaction",
  {
    "transactionId": 12345,
    "meterStop": 15000,
    "timestamp": "2024-01-15T11:30:00Z"
  }
]
```

### 예시: StatusNotification

```json
[
  2,
  "19223206",
  "StatusNotification",
  {
    "connectorId": 1,
    "errorCode": "NoError",
    "status": "Available",
    "timestamp": "2024-01-15T10:00:00Z"
  }
]
```

---

## 2. CallResult (성공 응답)

### 형식

```json
[3, "uniqueId", {payload}]
```

| 인덱스 | 이름 | 타입 | 설명 |
|--------|------|------|------|
| 0 | MessageTypeId | Integer | 항상 `3` |
| 1 | UniqueId | String | 원본 Call의 UniqueId와 동일 |
| 2 | Payload | Object | 응답 데이터 |

### 예시: BootNotification 응답

```json
[
  3,
  "19223201",
  {
    "status": "Accepted",
    "currentTime": "2024-01-15T10:00:00Z",
    "interval": 300
  }
]
```

**status 값:**
- `Accepted`: 등록 성공, 정상 운영 가능
- `Pending`: 등록 대기 중, interval 후 재시도
- `Rejected`: 등록 거부

### 예시: Heartbeat 응답

```json
[
  3,
  "19223202",
  {
    "currentTime": "2024-01-15T10:05:00Z"
  }
]
```

### 예시: Authorize 응답

```json
[
  3,
  "19223203",
  {
    "idTagInfo": {
      "status": "Accepted",
      "expiryDate": "2024-12-31T23:59:59Z"
    }
  }
]
```

**idTagInfo.status 값:**
- `Accepted`: 인증 성공
- `Blocked`: 차단된 태그
- `Expired`: 만료된 태그
- `Invalid`: 유효하지 않음
- `ConcurrentTx`: 다른 거래 진행 중

### 예시: StartTransaction 응답

```json
[
  3,
  "19223204",
  {
    "transactionId": 12345,
    "idTagInfo": {
      "status": "Accepted"
    }
  }
]
```

### 예시: StopTransaction 응답

```json
[
  3,
  "19223205",
  {
    "idTagInfo": {
      "status": "Accepted"
    }
  }
]
```

### 예시: StatusNotification 응답

```json
[
  3,
  "19223206",
  {}
]
```

---

## 3. CallError (에러 응답)

### 형식

```json
[4, "uniqueId", "errorCode", "errorDescription", {errorDetails}]
```

| 인덱스 | 이름 | 타입 | 설명 |
|--------|------|------|------|
| 0 | MessageTypeId | Integer | 항상 `4` |
| 1 | UniqueId | String | 원본 Call의 UniqueId와 동일 |
| 2 | ErrorCode | String | 에러 코드 |
| 3 | ErrorDescription | String | 에러 설명 |
| 4 | ErrorDetails | Object | 추가 에러 정보 (선택) |

### 에러 코드 목록

| ErrorCode | 설명 |
|-----------|------|
| `NotImplemented` | 해당 Action을 지원하지 않음 |
| `NotSupported` | 해당 기능을 지원하지 않음 |
| `InternalError` | 내부 서버 오류 |
| `ProtocolError` | 프로토콜 위반 |
| `SecurityError` | 보안 관련 오류 |
| `FormationViolation` | 메시지 형식 오류 (JSON 파싱 실패) |
| `PropertyConstraintViolation` | 필드 제약 조건 위반 |
| `OccurrenceConstraintViolation` | 필수 필드 누락 |
| `TypeConstraintViolation` | 타입 불일치 |
| `GenericError` | 기타 오류 |

### 예시: 지원하지 않는 Action

```json
[
  4,
  "19223207",
  "NotImplemented",
  "The action DataTransfer is not implemented",
  {}
]
```

### 예시: 형식 오류

```json
[
  4,
  "19223208",
  "FormationViolation",
  "Failed to parse JSON message",
  {
    "detail": "Unexpected token at position 45"
  }
]
```

---

## UniqueId의 역할

### 왜 필요한가?

OCPP는 **비동기** 통신이다:
- 요청을 보내고 응답을 기다리는 동안 다른 요청을 보낼 수 있음
- 응답이 순서대로 오지 않을 수 있음

```
충전기                              서버
  │──[2, "aaa", "Heartbeat", {}]──────►│
  │──[2, "bbb", "StatusNotification"]──►│
  │                                    │
  │◄──[3, "bbb", {...}]────────────────│  bbb 응답이 먼저 올 수 있음
  │◄──[3, "aaa", {...}]────────────────│
```

### 구현 시 고려사항

```java
// 요청 보낼 때
String uniqueId = UUID.randomUUID().toString();
pendingCalls.put(uniqueId, new PendingCall(action, callback));
send([2, uniqueId, action, payload]);

// 응답 받을 때
String uniqueId = message[1];
PendingCall pending = pendingCalls.remove(uniqueId);
pending.callback.onResult(message[2]);
```

---

## 메시지 흐름 다이어그램

### BootNotification 흐름

```
충전기(CP)                                      서버(CS)
    │                                              │
    │  TCP/WebSocket 연결                           │
    ├──────────────────────────────────────────────►│
    │                                              │
    │  [2, "1", "BootNotification", {...}]         │
    ├──────────────────────────────────────────────►│
    │                                              │
    │              메시지 파싱                       │
    │              Action 라우팅                    │
    │              핸들러 실행                       │
    │                                              │
    │  [3, "1", {status:"Accepted", interval:300}] │
    │◄──────────────────────────────────────────────┤
    │                                              │
    │  interval(300초) 마다 Heartbeat               │
    │                                              │
```

### 충전 시작 흐름

```
충전기(CP)                                      서버(CS)
    │                                              │
    │  [2, "10", "Authorize", {idTag:"..."}]       │
    ├──────────────────────────────────────────────►│
    │                                              │
    │  [3, "10", {idTagInfo:{status:"Accepted"}}]  │
    │◄──────────────────────────────────────────────┤
    │                                              │
    │  [2, "11", "StartTransaction", {...}]        │
    ├──────────────────────────────────────────────►│
    │                                              │
    │  [3, "11", {transactionId:123, ...}]         │
    │◄──────────────────────────────────────────────┤
    │                                              │
    │  [2, "12", "StatusNotification",             │
    │           {status:"Charging"}]               │
    ├──────────────────────────────────────────────►│
    │                                              │
    │  [3, "12", {}]                               │
    │◄──────────────────────────────────────────────┤
    │                                              │
```

---

## Java에서 메시지 파싱

### JSON 배열 파싱 예시

```java
// Gson 사용
JsonArray array = JsonParser.parseString(rawMessage).getAsJsonArray();
int messageType = array.get(0).getAsInt();

switch (messageType) {
    case 2: // Call
        String uniqueId = array.get(1).getAsString();
        String action = array.get(2).getAsString();
        JsonObject payload = array.get(3).getAsJsonObject();
        handleCall(uniqueId, action, payload);
        break;

    case 3: // CallResult
        String uniqueId = array.get(1).getAsString();
        JsonObject payload = array.get(2).getAsJsonObject();
        handleCallResult(uniqueId, payload);
        break;

    case 4: // CallError
        String uniqueId = array.get(1).getAsString();
        String errorCode = array.get(2).getAsString();
        String errorDesc = array.get(3).getAsString();
        JsonObject details = array.get(4).getAsJsonObject();
        handleCallError(uniqueId, errorCode, errorDesc, details);
        break;
}
```

---

## 주의사항

### 1. 배열 형식

메시지는 **Object가 아닌 Array**:
```json
// ❌ 틀림
{"type": 2, "id": "abc", ...}

// ✅ 맞음
[2, "abc", "Action", {...}]
```

### 2. UniqueId는 String

숫자처럼 보여도 항상 **문자열**:
```json
// ❌ 틀림
[2, 123, "Action", {}]

// ✅ 맞음
[2, "123", "Action", {}]
```

### 3. 타임스탬프 형식

ISO 8601 형식 사용:
```
"2024-01-15T10:30:00Z"      // UTC
"2024-01-15T19:30:00+09:00" // KST
```

---

## 다음 단계

→ [03-PROJECT-STRUCTURE.md](./03-PROJECT-STRUCTURE.md): 프로젝트 구조