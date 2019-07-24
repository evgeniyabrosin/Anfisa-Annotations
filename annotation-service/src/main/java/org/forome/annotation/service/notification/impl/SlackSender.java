package org.forome.annotation.service.notification.impl;

import in.ashwanthkumar.slack.webhook.Slack;
import in.ashwanthkumar.slack.webhook.SlackMessage;
import org.forome.annotation.config.notification.NotificationSlackConfig;
import org.forome.annotation.service.notification.Sender;

import java.io.IOException;

public class SlackSender implements Sender {

    public final Slack slack;

    public SlackSender(NotificationSlackConfig config) {
        slack = new Slack(config.webhookUrl)
                .sendToChannel(config.channel);
    }

    public void send(String message) throws IOException {
        slack.push(new SlackMessage(message));
    }
}

