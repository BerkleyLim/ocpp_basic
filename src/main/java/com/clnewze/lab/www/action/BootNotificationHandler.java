package com.clnewze.lab.www.action;

import com.clnewze.lab.www.protocol.CallResult;
import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.session.ChargePointSession;
import com.clnewze.lab.www.state.ChargePointState;
import com.google.gson.JsonObject;

import java.time.Instant;

/**
 * BootNotification 핸들러
 * 충전기가 부팅 후 서버에 등록 요청
 */
public class BootNotificationHandler implements ActionHandler {

    @Override
    public String getAction() {
        return "BootNotification";
    }

    @Override
    public OcppMessage handle(ChargePointSession session, JsonObject payload) {
        // 충전기 정보 추출
        String vendor = payload.get("chargePointVendor").getAsString();
        String model = payload.get("chargePointModel").getAsString();

        // 세션에 정보 저장
        session.setVendor(vendor);
        session.setModel(model);
        session.setState(ChargePointState.AVAILABLE);

        System.out.println("[BootNotification] " + session.getChargePointId() +
                " - Vendor: " + vendor + ", Model: " + model);

        // 응답 생성
        JsonObject response = new JsonObject();
        response.addProperty("status", "Accepted");
        response.addProperty("currentTime", Instant.now().toString());
        response.addProperty("interval", 300);  // 5분마다 Heartbeat

        return new CallResult(session.getCurrentUniqueId(), response);
    }
}