/*
 *      Copyright (c) 2017 Stuart Boston
 *
 *      This file is part of the BGG Slack Bot.
 *
 *      The BGG Slack Bot is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      The BGG Slack Bot is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the BGG Slack Bot.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.omertron.slackbot.functions.scheduler;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import java.util.concurrent.ScheduledExecutorService;

public class WbbBotTask extends AbstractBotTask {

    public WbbBotTask(ScheduledExecutorService executorService, String name, int targetHour, int targetMin, SlackSession session, SlackChannel channel) {
        super(executorService, name, targetHour, targetMin, session, channel);
    }

    @Override
    public void doWork() {
        getSession().sendMessage(getChannel(), getName() + ": RUNNING!!");
    }

}
