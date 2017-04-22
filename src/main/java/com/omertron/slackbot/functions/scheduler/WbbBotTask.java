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
import com.omertron.slackbot.listeners.GoogleSheetsListener;
import com.omertron.slackbot.model.sheets.SheetInfo;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPreparedMessage;
import com.ullink.slack.simpleslackapi.SlackSession;
import java.time.LocalDate;
import java.time.Period;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.commons.lang3.StringUtils;
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

        SheetInfo sheetInfo = GoogleSheetsListener.getSheetInfo();
        LOG.info("Date of next game is {}", sheetInfo.getFormattedDate("EEEE, d MMMM YYYY"));

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
                getSession().sendMessage(getChannel(), createMessage(sheetInfo, diff));
                break;
        }
    }

    /**
     * Create a formatted message about the future game night
     *
     * @param sheetInfo SheetInfo
     * @param diff Days to next game night
     * @return Slack Prepared Message
     */
    private SlackPreparedMessage createMessage(SheetInfo sheetInfo, Period diff) {
        SlackPreparedMessage.Builder spm = new SlackPreparedMessage.Builder().withUnfurl(false);

        StringBuilder sb = new StringBuilder("Game night is ");
        sb.append(sheetInfo.getFormattedDate("EEEE, d MMMM"))
                .append(", still ").append(diff.getDays()).append(" days away :no_mouth:\n");

        if (StringUtils.isBlank(sheetInfo.getGameChooser())) {
            sb.append("There is no-one to chose the next game!!!");
        } else {
            if ("All".equalsIgnoreCase(sheetInfo.getGameChooser())) {
                sb.append("The group is choosing");
            } else if ("Other".equalsIgnoreCase(sheetInfo.getGameChooser())) {
                sb.append("It's someone else's turn to choose");
            } else {
                sb.append("It's *").append(sheetInfo.getGameChooser()).append("'s* turn to choose");
            }
            
            if (sheetInfo.getNextGameId() <= 0) {
                sb.append(", but no game has been selected yet\n");
            } else {
                sb.append(" and *")
                        .append(SlackBot.formatLink(Constants.BGG_LINK_GAME + sheetInfo.getNextGameId(), sheetInfo.getGameName()))
                        .append("* has been picked.\n");
            }

        }

        // Who's attending?
        if (sheetInfo.getPlayers().isEmpty()) {
            sb.append("No-one has said they are going!");
        } else {
            sb.append(sheetInfo.getNameList(", ")).append(" are attending");
        }

        spm.withMessage(sb.toString());

        return spm.build();
    }

}
