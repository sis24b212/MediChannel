package lk.lankacare.medichannel.service;

import lk.lankacare.medichannel.entity.Notification;
import lk.lankacare.medichannel.entity.User;
import lk.lankacare.medichannel.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Publishes real-time STOMP WebSocket updates (RT-01, RT-02, RT-03, RT-04).
 */
@Service
public class RealtimeEventService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    public RealtimeEventService(
            SimpMessagingTemplate messagingTemplate,
            NotificationRepository notificationRepository
    ) {
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
    }

    // RT-01: Broadcast live queue ("Now Serving") for a session
    public void broadcastQueueUpdate(Long sessionId, int currentServingNumber, String sessionStatus, int delayMinutes) {
        messagingTemplate.convertAndSend(
                "/topic/sessions/" + sessionId + "/queue",
                Map.of(
                        "sessionId", sessionId,
                        "currentServingNumber", currentServingNumber,
                        "status", sessionStatus,
                        "delayMinutes", delayMinutes,
                        "updatedAt", Instant.now().toString()
                )
        );
    }

    // RT-02: Broadcast live numbers left for anyone viewing a session
    public void broadcastAvailabilityUpdate(Long sessionId, int remainingNumbers, int lastIssuedNumber, int maxPatients) {
        messagingTemplate.convertAndSend(
                "/topic/sessions/" + sessionId + "/availability",
                Map.of(
                        "sessionId", sessionId,
                        "remainingNumbers", remainingNumbers,
                        "lastIssuedNumber", lastIssuedNumber,
                        "maxPatients", maxPatients,
                        "updatedAt", Instant.now().toString()
                )
        );
    }

    // RT-04: Persist and send targeted notification ONLY to the affected user
    public Notification sendUserNotification(User targetUser, String type, String title, String message) {
        Notification notif = new Notification();
        notif.setUser(targetUser);
        notif.setNotificationType(type);
        notif.setTitle(title);
        notif.setMessage(message);
        Notification saved = notificationRepository.save(notif);

        messagingTemplate.convertAndSendToUser(
                String.valueOf(targetUser.getId()),
                "/queue/notifications",
                Map.of(
                        "id", saved.getId(),
                        "type", saved.getNotificationType(),
                        "title", saved.getTitle(),
                        "message", saved.getMessage(),
                        "createdAt", saved.getCreatedAt().toString()
                )
        );
        return saved;
    }
}
