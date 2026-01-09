package com.clnewze.lab.www.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * OCPP CallError 메시지 (에러 응답)
 * 형식: [4, "uniqueId", "errorCode", "errorDescription", {errorDetails}]
 */
public class CallError implements OcppMessage {

    private final String uniqueId;
    private final ErrorCode errorCode;
    private final String errorDescription;
    private final JsonObject errorDetails;

    public CallError(String uniqueId, ErrorCode errorCode, String errorDescription) {
        this(uniqueId, errorCode, errorDescription, new JsonObject());
    }

    public CallError(String uniqueId, ErrorCode errorCode, String errorDescription, JsonObject errorDetails) {
        this.uniqueId = uniqueId;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.errorDetails = errorDetails;
    }

    @Override
    public MessageType getType() {
        return MessageType.CALL_ERROR;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public JsonObject getErrorDetails() {
        return errorDetails;
    }

    @Override
    public String toJson() {
        JsonArray array = new JsonArray();
        array.add(MessageType.CALL_ERROR.getId());
        array.add(uniqueId);
        array.add(errorCode.getValue());
        array.add(errorDescription);
        array.add(errorDetails);
        return array.toString();
    }
}