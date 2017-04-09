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

import com.omertron.slackbot.SlackBot;
import com.omertron.slackbot.functions.Meetup;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPreparedMessage;
import com.ullink.slack.simpleslackapi.SlackSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.common.exception.ApiException;

public class MeetupBotTask extends AbstractBotTask {

    private static final Logger LOG = LoggerFactory.getLogger(MeetupBotTask.class);
    private static final int LOOK_AHEAD_DAYS = 2;

    public MeetupBotTask(ScheduledExecutorService executorService, String name, int targetHour, int targetMin, SlackSession session, SlackChannel channel) {
        super(executorService, name, targetHour, targetMin, session, channel);
    }

    @Override
    public void doWork() {
        LOG.info("{} is running at {}", getName(), formattedDateTime());

        try {
            // Read the next three meetups
            Meetup.readMeetUp(3);
        } catch (ApiException ex) {
            LOG.warn("Failed to read meetups in MeetupBotTask: {}", ex.getMessage(), ex);
            SlackBot.messageAdmins(getSession(), "Failed to read meetups in MeetupBotTask: " + ex.getMessage());
        }

        Map<LocalDateTime, SlackAttachment> meetupList = Meetup.getMeetupsDays(LOOK_AHEAD_DAYS, false);

        SlackPreparedMessage.Builder spmTodayBuilder = new SlackPreparedMessage.Builder();
        spmTodayBuilder.withMessage("Meetups happening today");

        SlackPreparedMessage.Builder spmTomorrowBuilder = new SlackPreparedMessage.Builder();
        spmTomorrowBuilder.withMessage("Meetups happening tomorrow");

        if (meetupList.isEmpty()) {
            LOG.info("No meetups scheduled for the next {} days", LOOK_AHEAD_DAYS);
        } else {
            LocalDate now = LocalDate.now();
            Period diff;
            boolean today = false;
            boolean tomorrow = false;

            for (Map.Entry<LocalDateTime, SlackAttachment> entry : meetupList.entrySet()) {
                diff = Period.between(now, entry.getKey().toLocalDate());
                LOG.info("Meetup date: {} - {} days away", entry.getKey().format(DateTimeFormatter.ISO_DATE), diff.getDays());

                switch (diff.getDays()) {
                    case 0:
                        LOG.info("\tAdded to today");
                        today = true;
                        spmTodayBuilder.addAttachment(entry.getValue());
                        break;
                    case 1:
                        LOG.info("\tAdded to tomorrow");
                        tomorrow = true;
                        spmTomorrowBuilder.addAttachment(entry.getValue());
                        break;
                    default:
                        LOG.info("\tSkipped.");
                        break;
                }
            }

            if (today) {
                getSession().sendMessage(getChannel(), spmTodayBuilder.build());
            }

            if (tomorrow) {
                getSession().sendMessage(getChannel(), spmTomorrowBuilder.build());
            }

        }
    }

}
