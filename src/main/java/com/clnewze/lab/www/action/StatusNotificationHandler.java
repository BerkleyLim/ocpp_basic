package com.clnewze.lab.www.action;

import com.clnewze.lab.www.protocol.CallResult;
import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.session.ChargePointSession;
import com.clnewze.lab.www.state.ChargePointState;
import com.google.gson.JsonObject;

/**
 * StatusNotification 핸들러
 * 충전기가 상태 변경을 서버에 알림
 */
public class StatusNotificationHandler implements ActionHandler {

    @Override
    public String getAction() {
        return "StatusNotification";
    }

    @Override
    public OcppMessage handle(ChargePointSession session, JsonObject payload) {
        int connectorId = payload.get("connectorId").getAsInt();
        String errorCode = payload.get("errorCode").getAsString();
        String status = payload.get("status").getAsString();

        System.out.println("[StatusNotification] " + session.getChargePointId() +
                " - Connector: " + connectorId +
                ", Status: " + status +
                ", ErrorCode: " + errorCode);

        // 상태 매핑 및 업데이트
        ChargePointState newState = mapStatus(status);
        if (newState != null) {
            session.setState(newState);
        }

        // 응답: 빈 객체
        return new CallResult(session.getCurrentUniqueId(), new JsonObject());
    }

    /**
     * OCPP 상태 문자열을 ChargePointState로 매핑
     */
    private ChargePointState mapStatus(String status) {
        return switch (status) {
            case "Available" -> ChargePointState.AVAILABLE;
            case "Preparing" -> ChargePointState.PREPARING;
            case "Charging" -> ChargePointState.CHARGING;
            case "SuspendedEV", "SuspendedEVSE" -> ChargePointState.CHARGING; // 일시 중지도 충전 중으로
            case "Finishing" -> ChargePointState.FINISHING;
            case "Reserved" -> ChargePointState.RESERVED;
            case "Unavailable" -> ChargePointState.UNAVAILABLE;
            case "Faulted" -> ChargePointState.FAULTED;
            default -> null;
        };
    }
}