package com.clnewze.lab.www.protocol;

/**
 * OCPP 메시지 타입
 * - CALL (2): 요청 메시지
 * - CALL_RESULT (3): 성공 응답
 * - CALL_ERROR (4): 에러 응답
 */
public enum MessageType {
    CALL(2),
    CALL_RESULT(3),
    CALL_ERROR(4);

    private final int id;

    MessageType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static MessageType fromId(int id) {
        for (MessageType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type: " + id);
    }
}