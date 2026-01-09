package com.clnewze.lab.www.state;

/**
 * 충전기 상태
 */
public enum ChargePointState {
    DISCONNECTED,   // WebSocket 연결 끊김
    CONNECTED,      // WebSocket 연결됨 (Boot 전)
    AVAILABLE,      // 충전 가능
    PREPARING,      // 충전 준비 중
    CHARGING,       // 충전 중
    FINISHING,      // 충전 완료 처리 중
    RESERVED,       // 예약됨
    UNAVAILABLE,    // 사용 불가
    FAULTED         // 오류 상태
}