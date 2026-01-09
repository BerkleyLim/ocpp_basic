package com.clnewze.lab.www.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * OCPP Call 메시지 (요청)
 * 형식: [2, "uniqueId", "action", {payload}]
 */
public class Call implements OcppMessage {

    private final String uniqueId;
    private final String action;
    private final JsonObject payload;

    public Call(String uniqueId, String action, JsonObject payload) {
        this.uniqueId = uniqueId;
        this.action = action;
        this.payload = payload;
    }

    /**
     * JSON 문자열에서 Call 객체 생성
     */
    public static Call fromJson(String json) {
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        // array[0] = 2 (MessageType)
        String uniqueId = array.get(1).getAsString();
        String action = array.get(2).getAsString();
        JsonObject payload = array.get(3).getAsJsonObject();
        return new Call(uniqueId, action, payload);
    }

    @Override
    public MessageType getType() {
        return MessageType.CALL;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    public String getAction() {
        return action;
    }

    public JsonObject getPayload() {
        return payload;
    }

    @Override
    public String toJson() {
        JsonArray array = new JsonArray();
        array.add(MessageType.CALL.getId());
        array.add(uniqueId);
        array.add(action);
        array.add(payload);
        return array.toString();
    }
}