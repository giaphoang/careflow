package com.example.hms.notification.service.impl;

import com.example.hms.notification.channel.NotificationChannel;
import com.example.hms.notification.model.NotificationMessage;
import com.example.hms.notification.service.NotificationDispatcher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationDispatcherImpl implements NotificationDispatcher {

    private final List<NotificationChannel> channels;

    public NotificationDispatcherImpl(List<NotificationChannel> channels) {
        this.channels = channels;
    }

    @Override
    public void dispatch(NotificationMessage message) {
        List<NotificationChannel> matching = new ArrayList<>();
        for (NotificationChannel channel : channels) {
            if (channel.medium() == message.medium()) {
                matching.add(channel);
            }
        }
        if (matching.isEmpty()) {
            throw new IllegalStateException("No channel configured for medium: " + message.medium());
        }

        // Prefer real providers first.
        matching.sort((a, b) -> Boolean.compare(isSimulation(a), isSimulation(b)));

        // If we have any real provider mapped to this medium, we should NOT fallback to simulation 
        // if the real provider throws an exception, otherwise retries are bypassed entirely.
        NotificationChannel targetChannel = matching.stream()
                .filter(ch -> !isSimulation(ch))
                .findFirst()
                .orElse(matching.get(0));

        try {
            targetChannel.send(message);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Notification dispatch failed for medium: " + message.medium(), ex);
        }
    }

    private boolean isSimulation(NotificationChannel channel) {
        return channel.getClass().getSimpleName().startsWith("Simulation");
    }
}
