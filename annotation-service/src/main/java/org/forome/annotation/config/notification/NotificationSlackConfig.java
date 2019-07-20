package org.forome.annotation.config.notification;

import net.minidev.json.JSONObject;

public class NotificationSlackConfig {

    public final String webhookUrl;
    public final String channel;

    public NotificationSlackConfig(JSONObject parse) {
        this.webhookUrl = parse.getAsString("webhook_url");
        this.channel = parse.getAsString("channel");
    }
}
