package com.clnewze.lab.www.protocol;

/**
 * OCPP 메시지 인터페이스
 * 모든 OCPP 메시지(Call, CallResult, CallError)가 구현
 */
public interface OcppMessage {

    /**
     * 메시지 타입 반환
     */
    MessageType getType();

    /**
     * 요청-응답 매칭용 고유 ID
     */
    String getUniqueId();

    /**
     * JSON 문자열로 변환
     */
    String toJson();
}