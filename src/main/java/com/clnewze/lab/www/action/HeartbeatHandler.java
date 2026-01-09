package com.clnewze.lab.www.action;

import com.clnewze.lab.www.protocol.CallResult;
import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.session.ChargePointSession;
import com.google.gson.JsonObject;

import java.time.Instant;

/**
 * Heartbeat 핸들러
 * 충전기가 주기적으로 연결 유지 확인
 */
public class HeartbeatHandler implements ActionHandler {

    @Override
    public String getAction() {
        return "Heartbeat";
    }

    @Override
    public OcppMessage handle(ChargePointSession session, JsonObject payload) {
        System.out.println("[Heartbeat] " + session.getChargePointId());

        // 응답: 현재 시간만 반환
        JsonObject response = new JsonObject();
        response.addProperty("currentTime", Instant.now().toString());

        return new CallResult(session.getCurrentUniqueId(), response);
    }
}