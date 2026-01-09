package com.clnewze.lab.www.action;

import com.clnewze.lab.www.protocol.CallResult;
import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.session.ChargePointSession;
import com.clnewze.lab.www.state.ChargePointState;
import com.google.gson.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * StartTransaction 핸들러
 * 충전 시작 요청 처리
 */
public class StartTransactionHandler implements ActionHandler {

    // 트랜잭션 ID 생성기 (실제로는 DB에서 관리)
    private static final AtomicInteger transactionIdGenerator = new AtomicInteger(1);

    @Override
    public String getAction() {
        return "StartTransaction";
    }

    @Override
    public OcppMessage handle(ChargePointSession session, JsonObject payload) {
        int connectorId = payload.get("connectorId").getAsInt();
        String idTag = payload.get("idTag").getAsString();
        int meterStart = payload.get("meterStart").getAsInt();
        String timestamp = payload.has("timestamp") ? payload.get("timestamp").getAsString() : "";

        // 트랜잭션 ID 생성
        int transactionId = transactionIdGenerator.getAndIncrement();

        System.out.println("[StartTransaction] " + session.getChargePointId() +
                " - Connector: " + connectorId +
                ", IdTag: " + idTag +
                ", MeterStart: " + meterStart +
                ", TransactionId: " + transactionId);

        // 상태 변경: CHARGING
        session.setState(ChargePointState.CHARGING);

        // 응답 생성
        JsonObject idTagInfo = new JsonObject();
        idTagInfo.addProperty("status", "Accepted");

        JsonObject response = new JsonObject();
        response.addProperty("transactionId", transactionId);
        response.add("idTagInfo", idTagInfo);

        return new CallResult(session.getCurrentUniqueId(), response);
    }
}