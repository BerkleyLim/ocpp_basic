package com.clnewze.lab.www.action;

import com.clnewze.lab.www.protocol.CallResult;
import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.session.ChargePointSession;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * MeterValues 핸들러
 * 충전 중 계량 데이터 수신
 */
public class MeterValuesHandler implements ActionHandler {

    @Override
    public String getAction() {
        return "MeterValues";
    }

    @Override
    public OcppMessage handle(ChargePointSession session, JsonObject payload) {
        int connectorId = payload.get("connectorId").getAsInt();

        // transactionId는 선택
        Integer transactionId = payload.has("transactionId")
                ? payload.get("transactionId").getAsInt()
                : null;

        // meterValue 배열 처리
        JsonArray meterValues = payload.getAsJsonArray("meterValue");

        System.out.println("[MeterValues] " + session.getChargePointId() +
                " - Connector: " + connectorId +
                ", TransactionId: " + transactionId +
                ", Values: " + meterValues.size() + "개");

        // 상세 로깅 (연습용)
        for (int i = 0; i < meterValues.size(); i++) {
            JsonObject meterValue = meterValues.get(i).getAsJsonObject();
            String timestamp = meterValue.has("timestamp")
                    ? meterValue.get("timestamp").getAsString()
                    : "";

            JsonArray sampledValues = meterValue.getAsJsonArray("sampledValue");
            for (int j = 0; j < sampledValues.size(); j++) {
                JsonObject sample = sampledValues.get(j).getAsJsonObject();
                String value = sample.get("value").getAsString();
                String measurand = sample.has("measurand")
                        ? sample.get("measurand").getAsString()
                        : "Energy.Active.Import.Register";
                String unit = sample.has("unit")
                        ? sample.get("unit").getAsString()
                        : "Wh";

                System.out.println("  - " + measurand + ": " + value + " " + unit +
                        " (" + timestamp + ")");
            }
        }

        // 응답: 빈 객체
        return new CallResult(session.getCurrentUniqueId(), new JsonObject());
    }
}