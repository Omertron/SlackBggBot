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

import com.ullink.slack.simpleslackapi.SlackSession;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute {@link AbstractBotTask} once per day.
 */
public class BotTaskExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(BotTaskExecutor.class);
    private static final ZoneId TIMEZONE = ZoneId.of("Europe/London");
    private static final int START_HOUR = 8;
    private static final int START_MIN = 30;

    // Get a single thread to execute the messages
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);
    private static final List<BotTaskInterface> TASKS = new ArrayList<>();

    public BotTaskExecutor(SlackSession session) {
//        ZonedDateTime zdt = ZonedDateTime.now(TIMEZONE).plusMinutes(2);
//
//        int hour = zdt.getHour();
//        int min = zdt.getMinute();

        TASKS.add(new MeetupBotTask(EXECUTOR_SERVICE, "MEETUP", START_HOUR, START_MIN, session, session.findChannelByName("general")));
        // G3QQES762
//        TASKS.add(new WbbBotTask(EXECUTOR_SERVICE, "WBB", hour, min, session, session.findChannelByName("random")));

        startAll();
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
