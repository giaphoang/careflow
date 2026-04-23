package com.example.hms.notification.service;

import com.example.hms.notification.model.NotificationMessage;

public interface NotificationDispatcher {
    void dispatch(NotificationMessage message);
}
