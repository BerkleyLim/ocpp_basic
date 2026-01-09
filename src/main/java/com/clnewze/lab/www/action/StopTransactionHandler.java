package com.clnewze.lab.www.action;

import com.clnewze.lab.www.protocol.CallResult;
import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.session.ChargePointSession;
import com.clnewze.lab.www.state.ChargePointState;
import com.google.gson.JsonObject;

/**
 * StopTransaction 핸들러
 * 충전 종료 요청 처리
 */
public class StopTransactionHandler implements ActionHandler {

    @Override
    public String getAction() {
        return "StopTransaction";
    }

    @Override
    public OcppMessage handle(ChargePointSession session, JsonObject payload) {
        int transactionId = payload.get("transactionId").getAsInt();
        int meterStop = payload.get("meterStop").getAsInt();
        String timestamp = payload.has("timestamp") ? payload.get("timestamp").getAsString() : "";

        // 선택: idTag, reason
        String idTag = payload.has("idTag") ? payload.get("idTag").getAsString() : "";
        String reason = payload.has("reason") ? payload.get("reason").getAsString() : "Local";

        System.out.println("[StopTransaction] " + session.getChargePointId() +
                " - TransactionId: " + transactionId +
                ", MeterStop: " + meterStop +
                ", Reason: " + reason);

        // 상태 변경: FINISHING → AVAILABLE
        session.setState(ChargePointState.AVAILABLE);

        // 응답 생성
        JsonObject response = new JsonObject();

        // idTagInfo는 선택 (idTag가 있을 때만)
        if (!idTag.isEmpty()) {
            JsonObject idTagInfo = new JsonObject();
            idTagInfo.addProperty("status", "Accepted");
            response.add("idTagInfo", idTagInfo);
        }

        return new CallResult(session.getCurrentUniqueId(), response);
    }
}