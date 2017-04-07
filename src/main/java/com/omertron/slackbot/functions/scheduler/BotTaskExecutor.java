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

import com.omertron.slackbot.Constants;
import com.omertron.slackbot.SlackBot;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPreparedMessage;
import com.ullink.slack.simpleslackapi.SlackSession;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute {@link AbstractBotTask} once per day.
 */
public class BotTaskExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(BotTaskExecutor.class);
    private static final int START_HOUR;
    private static final int START_MIN;

    // Get a single thread to execute the messages
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);
    private static final List<BotTaskInterface> TASKS = new ArrayList<>();

    static {
        START_HOUR = NumberUtils.toInt(SlackBot.getProperty(Constants.BOT_START_HOUR, "8"), 8);
        START_MIN = NumberUtils.toInt(SlackBot.getProperty(Constants.BOT_START_MIN, "30"), 30);
    }

    public BotTaskExecutor(SlackSession session) {
        LOG.info("Start time: {}", String.format("%1$-2d:%2$-2d", START_HOUR, START_MIN));

        initTask(session, session.findChannelByName("general"), "MEETUP");
        initTask(session, session.findChannelById("G3QQES762"), "WBB");

        startAll();
    }

    /**
     * Generic method to create the task
     *
     * @param session
     * @param channel
     * @param name
     */
    private void initTask(SlackSession session, SlackChannel channel, String name) {
        if (channel == null) {
            LOG.warn("Failed to start task '{}'", name);
        } else {
            TASKS.add(new MeetupBotTask(EXECUTOR_SERVICE, name, START_HOUR, START_MIN, session, channel));
        }
    }

    /**
     * Get the status of all the tasks
     *
     * @return
     */
    public static SlackPreparedMessage status() {
        SlackPreparedMessage.Builder message = new SlackPreparedMessage.Builder();
        message.withMessage("Status of the " + TASKS.size() + " tasks running");

        for (BotTaskInterface bti : TASKS) {
            message.addAttachment(bti.getStatus());
        }

        return message.build();
    }

    /**
     * Stop all the tasks
     */
    public final void startAll() {
        for (BotTaskInterface bt : TASKS) {
            LOG.info("Starting BotTask {}", bt.getName());
            bt.start();
        }
    }

    /**
     * Stop all the tasks
     */
    public final void stopAll() {
        for (BotTaskInterface bt : TASKS) {
            LOG.info("Stoping BotTask {}", bt.getName());
            bt.stop();
        }
        EXECUTOR_SERVICE.shutdown();
    }

}
