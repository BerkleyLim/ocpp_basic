package com.clnewze.lab.www.router;

import com.clnewze.lab.www.action.ActionHandler;
import com.clnewze.lab.www.action.BootNotificationHandler;
import com.clnewze.lab.www.action.HeartbeatHandler;
import com.clnewze.lab.www.action.StatusNotificationHandler;
import com.clnewze.lab.www.action.AuthorizeHandler;
import com.clnewze.lab.www.action.StartTransactionHandler;
import com.clnewze.lab.www.action.StopTransactionHandler;
import com.clnewze.lab.www.action.MeterValuesHandler;
import com.clnewze.lab.www.protocol.*;
import com.clnewze.lab.www.session.ChargePointSession;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

/**
 * 메시지 라우터
 * Action 이름으로 적절한 Handler를 찾아서 실행
 */
public class MessageRouter {

    private final Map<String, ActionHandler> handlers = new HashMap<>();

    public MessageRouter() {
        // 핸들러 등록
        register(new BootNotificationHandler());
        register(new HeartbeatHandler());
        // 응용단계 핸들러 등록
        register(new StatusNotificationHandler());
        register(new AuthorizeHandler());
        register(new StartTransactionHandler());
        register(new StopTransactionHandler());
        register(new MeterValuesHandler());
    }

    private void register(ActionHandler handler) {
        handlers.put(handler.getAction(), handler);
    }

    /**
     * 메시지 라우팅 및 처리
     * @param session 충전기 세션
     * @param rawMessage 원본 JSON 메시지
     * @return 응답 메시지 (CallResult 또는 CallError)
     */
    public OcppMessage route(ChargePointSession session, String rawMessage) {
        try {
            // 1. JSON 파싱
            JsonArray array = JsonParser.parseString(rawMessage).getAsJsonArray();
            int messageTypeId = array.get(0).getAsInt();

            // 2. Call 메시지만 처리 (서버가 받는 요청)
            if (messageTypeId != MessageType.CALL.getId()) {
                System.out.println("[Router] Ignoring non-Call message: " + messageTypeId);
                return null;
            }

            // 3. Call 파싱
            Call call = Call.fromJson(rawMessage);
            session.setCurrentUniqueId(call.getUniqueId());

            // 4. 핸들러 찾기
            ActionHandler handler = handlers.get(call.getAction());
            if (handler == null) {
                System.out.println("[Router] Unknown action: " + call.getAction());
                return new CallError(
                        call.getUniqueId(),
                        ErrorCode.NOT_IMPLEMENTED,
                        "Unknown action: " + call.getAction()
                );
            }

            // 5. 핸들러 실행
            return handler.handle(session, call.getPayload());

        } catch (Exception e) {
            System.err.println("[Router] Error processing message: " + e.getMessage());
            e.printStackTrace();
            return new CallError(
                    "",
                    ErrorCode.INTERNAL_ERROR,
                    e.getMessage()
            );
        }
    }
}