package com.ticketwave.service;

import com.ticketwave.domain.AppUser;
import com.ticketwave.domain.Notification;
import com.ticketwave.domain.NotificationChannel;
import com.ticketwave.domain.NotificationType;
import com.ticketwave.dto.notification.NotificationResponse;
import com.ticketwave.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification send(AppUser user, NotificationType type, String subject, String body) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public NotificationResponse markRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new com.ticketwave.exception.ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(),
                notification.getChannel(), notification.getSubject(), notification.getBody(),
                notification.isRead(), notification.getCreatedAt());
    }
}