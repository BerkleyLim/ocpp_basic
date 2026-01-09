# OCPP Practice Server - Roadmap

프로젝트 개선 및 확장 계획.

---

## Phase 1 - 기본 구현 (완료)

OCPP 엔진/통신 도메인 핵심 구현.

- [x] OCPP 1.6J 메시지 구조 (Call/CallResult/CallError)
- [x] WebSocket 서버 (Java-WebSocket)
- [x] 세션 관리 (ConcurrentHashMap)
- [x] 상태 머신 (ChargePointState enum)
- [x] Action Handler 7개 구현
  - BootNotification, Heartbeat, StatusNotification
  - Authorize, StartTransaction, StopTransaction, MeterValues
- [x] 메시지 라우팅 (MessageRouter)
- [x] 테스트 클라이언트 3종 (Python, Node.js, Java)
- [x] JUnit 테스트 28개

---

## Phase 2 - 실무급 보완 (예정)

안정성 및 운영 품질 향상.

### 2.1 uniqueId 상관관계 관리

```
현재: Call → 즉시 응답
개선: Call → pending map 저장 → timeout 관리 → 응답 매칭
```

- [ ] PendingRequestManager 구현
- [ ] 타임아웃 처리 (기본 30초)
- [ ] 메모리 누수 방지 (만료된 요청 정리)

### 2.2 상태 전이 규칙 검증

```java
// 현재
session.setState(ChargePointState.CHARGING);

// 개선
session.transitionTo(ChargePointState.CHARGING); // 허용된 전이만 가능
```

- [ ] 상태 전이 규칙 정의
- [ ] 잘못된 전이 시 예외/로그 처리
- [ ] 상태별 허용 Action 검증

### 2.3 비정상 시나리오 처리

- [ ] StartTransaction 중복 요청
- [ ] StopTransaction - 존재하지 않는 transactionId
- [ ] MeterValues - 값 누락/역전/지연
- [ ] Authorize - 블랙리스트 idTag

### 2.4 로깅 개선

```
현재: System.out.println("[Received] CP001: ...")
개선: 2024-01-15 10:00:00.123 INFO [CP001] [boot-001] BootNotification - 15ms
```

- [ ] SLF4J + Logback 설정 (logback.xml)
- [ ] 필수 필드: chargePointId, uniqueId, action, 처리시간(ms)
- [ ] 로그 레벨 분리 (DEBUG/INFO/WARN/ERROR)

---

## Phase 3 - 운영/확장 (미래)

대규모 운영 및 실서비스 대응.

### 3.1 CS → CP 요청 구현

서버에서 충전기로 명령 전송.

- [ ] RemoteStartTransaction
- [ ] RemoteStopTransaction
- [ ] Reset
- [ ] ChangeConfiguration

### 3.2 벤더 호환성 레이어

```
문제: 벤더마다 OCPP 스펙 해석이 다름
- 필드 누락
- 형식 불일치
- 순서 이상
- 자체 확장 필드
```

- [ ] VendorProfile 인터페이스
- [ ] Feature flag 기반 분기
- [ ] 유효성 검증 완화 옵션

### 3.3 정산/요금 비즈니스

- [ ] 트랜잭션 저장 (메모리 → DB)
- [ ] 요금 계산 로직
- [ ] 미터값 기반 정산

### 3.4 수평 확장 아키텍처

```
현재: 단일 서버, 메모리 세션

확장:
┌─────────┐     ┌─────────┐
│ Server1 │────▶│  Redis  │◀────│ Server2 │
└─────────┘     └─────────┘     └─────────┘
```

- [ ] Redis 기반 세션 공유
- [ ] 이벤트/메시지 브로커 연동
- [ ] 재연결 시 상태 복구

---

## 코드 리뷰 피드백 요약

### 잘된 부분

| 파일 | 평가 | 이유 |
|------|:----:|------|
| Call/CallResult/CallError | ★★★★★ | JSON 배열 구조를 객체로 정확히 모델링 |
| MessageRouter | ★★★★☆ | RPC 프로토콜 감각이 코드에서 드러남 |
| SessionManager | ★★★★☆ | ConcurrentHashMap 적용, 생명주기 명확 |
| OcppWebSocketServer | ★★★★☆ | Spring 없이 WS lifecycle 직접 경험 |
| ChargePointSession | ★★★★☆ | chargePointId 중심 세션 모델 |

### 보완 포인트

1. **ActionPolicy 분리**: unknown action 처리를 전략 패턴으로
2. **상태 전이 메서드화**: `state.transitionTo()` 방식
3. **타임아웃 관리**: pending request 만료 처리
4. **구조화된 로깅**: 처리시간, 필수 필드 포함

---

## 학습 성과

> "이것만(직접 구현해서 메시지 파싱/라우팅/세션/핸들러까지) 하면 OCPP '프로토콜 감각'은 거의 다 잡힌다"

**터득한 것:**
- OCPP가 RPC 프로토콜이라는 감각
- Action 라우팅 구조
- 세션/상태 중요성
- steve 라이브러리의 구조적 문제점 (직접 구현으로 증명)

**남은 것:**
- 충전 비즈니스/운영 도메인
- 벤더 호환성 대응
- 대규모 운영 아키텍처