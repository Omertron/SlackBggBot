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

import com.omertron.slackbot.listeners.GoogleSheetsListener;
import com.omertron.slackbot.model.sheets.SheetInfo;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import java.time.LocalDate;
import java.time.Period;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WbbBotTask extends AbstractBotTask {

    private static final Logger LOG = LoggerFactory.getLogger(WbbBotTask.class);

    public WbbBotTask(ScheduledExecutorService executorService, String name, int targetHour, int targetMin, SlackSession session, SlackChannel channel) {
        super(executorService, name, targetHour, targetMin, session, channel);
    }

    @Override
    public void doWork() {
        LOG.info("{} is running", getName());
        /*
        Get the sheet info with date of next meeting
        Send message if today or tomorrow
         */

        SheetInfo sheetInfo = GoogleSheetsListener.getSheetInfo();
        LOG.info("Date of next game is {}", sheetInfo.getFormattedDate("d LLLL"));
        getSession().sendMessage(getChannel(), "Date of next game is " + sheetInfo.getFormattedDate());

        LocalDate now = LocalDate.now();
        Period diff = Period.between(now, sheetInfo.getGameDate());

        switch (diff.getDays()) {
            case 0:
                getSession().sendMessage(getChannel(), "Game night is tonight!!", GoogleSheetsListener.createGameInfo());
                break;
            case 1:
                getSession().sendMessage(getChannel(), "Game night is tomorrow!", GoogleSheetsListener.createGameInfo());
                break;
            default:
                getSession().sendMessage(getChannel(), "Game night is still " + diff.getDays() + " days away :no_mouth:");
                break;
        }

    }

}
