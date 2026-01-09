package com.clnewze.lab.www.action;

import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.session.ChargePointSession;
import com.google.gson.JsonObject;

/**
 * OCPP Action 핸들러 인터페이스
 * 각 Action(BootNotification, Heartbeat 등)마다 구현체 생성
 */
public interface ActionHandler {

    /**
     * 처리할 Action 이름 반환
     * 예: "BootNotification", "Heartbeat"
     */
    String getAction();

    /**
     * Action 처리
     * @param session 충전기 세션
     * @param payload 요청 데이터
     * @return 응답 메시지 (CallResult 또는 CallError)
     */
    OcppMessage handle(ChargePointSession session, JsonObject payload);
}