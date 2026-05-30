package com.dway.dwaybackend.infrastructure.firebase;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FirebaseMessaging firebaseMessaging;

    @Async("fcmExecutor")
    public void sendNotification(String token, String title, String body) {
        if (token == null || token.isBlank()) {
            log.debug("FCM skipped: user has no push token");
            return;
        }
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();
            String messageId = firebaseMessaging.send(message);
            log.info("FCM sent: {}", messageId);
        } catch (FirebaseMessagingException e) {
            log.error("FCM failed for token [{}]: {} ({})", token, e.getMessage(), e.getMessagingErrorCode());
        }
    }
}