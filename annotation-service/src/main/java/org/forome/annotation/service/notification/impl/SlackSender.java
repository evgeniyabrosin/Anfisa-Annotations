/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

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

