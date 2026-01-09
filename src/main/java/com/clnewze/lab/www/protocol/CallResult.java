package com.clnewze.lab.www.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * OCPP CallResult 메시지 (성공 응답)
 * 형식: [3, "uniqueId", {payload}]
 */
public class CallResult implements OcppMessage {

    private final String uniqueId;
    private final JsonObject payload;

    public CallResult(String uniqueId, JsonObject payload) {
        this.uniqueId = uniqueId;
        this.payload = payload;
    }

    @Override
    public MessageType getType() {
        return MessageType.CALL_RESULT;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    public JsonObject getPayload() {
        return payload;
    }

    @Override
    public String toJson() {
        JsonArray array = new JsonArray();
        array.add(MessageType.CALL_RESULT.getId());
        array.add(uniqueId);
        array.add(payload);
        return array.toString();
    }
}