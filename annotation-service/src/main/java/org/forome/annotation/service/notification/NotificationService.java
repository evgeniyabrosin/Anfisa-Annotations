package org.forome.annotation.service.notification;

import org.forome.annotation.config.notification.NotificationSlackConfig;
import org.forome.annotation.service.notification.impl.SlackSender;

import java.io.IOException;

public class NotificationService {

    private final SlackSender slackSender;

    public NotificationService(NotificationSlackConfig config) {
        slackSender = new SlackSender(config);
    }

    public void send(String message) throws IOException {
        slackSender.send(message);
    }
}
