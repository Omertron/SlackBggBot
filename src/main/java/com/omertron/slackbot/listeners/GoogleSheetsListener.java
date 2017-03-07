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
package com.omertron.slackbot.listeners;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.omertron.slackbot.Constants;
import com.omertron.slackbot.model.SheetInfo;
import com.omertron.slackbot.sheets.GoogleSheets;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleSheetsListener implements SlackMessagePostedListener {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleSheetsListener.class);
    private static final Pattern PAT_SHEETS = Pattern.compile("^\\Qwbb\\E(\\s\\w*)?(\\s.*)?");
    private static final String SS_ID = "1Tbnvj3Colt5CnxlDUNk1L10iANm4jVUvJpD53mjKOYY";
    private static final List<String> CHANNELS = new ArrayList<>();

    public GoogleSheetsListener() {
        CHANNELS.add("D40EZ44QZ");
    }

    @Override
    public void onEvent(SlackMessagePosted event, SlackSession session) {
        // Channel On Which Message Was Posted
        SlackChannel msgChannel = event.getChannel();

        // Check the channel is WBB channel (or test D40EZ44QZ)
        LOG.info("Chennel ID: {} - {}", msgChannel.getId(), CHANNELS.contains(msgChannel.getId()));

        // Filter out the bot's own messages
        if (session.sessionPersona().getId().equals(event.getSender().getId())) {
            return;
        }

        String msgContent = event.getMessageContent();
        SlackUser msgSender = event.getSender();

        Matcher m = PAT_SHEETS.matcher(msgContent);
        if (m.matches()) {
            if (!GoogleSheets.isAuthorised()) {
                GoogleSheets.authorize();
            }

            LOG.info("Matched with {} groups", m.groupCount());
            String command = m.group(1) == null ? "HELP" : m.group(1).toUpperCase().trim();
            String params = m.groupCount() > 2 ? m.group(2).trim() : null;
            LOG.info("Command '{}' & params '{}'", command, params);

            switch (command) {
                case "HELP":
                    session.sendMessage(msgChannel, "Will print help");
                    break;
                case "NEXT":
                    showNextGame(session, msgChannel);
                    break;
                case "ADD":
                    session.sendMessage(msgChannel, "Will attepmt to add '" + params + "' to the play list");
                    break;
                case "REMOVE":
                    session.sendMessage(msgChannel, "Will attepmt to remove '" + params + "' from the play list");
                    break;
                default:
                    session.sendMessage(msgChannel, "Sorry, I don't know that command");
            }
        } else {
            LOG.info("Not matched");
        }
    }

    private void showNextGame(SlackSession session, SlackChannel msgChannel) {
        String range = "Stats!R16:S27";
        SheetInfo si = new SheetInfo();
        ValueRange response;

        try {
            Sheets service = GoogleSheets.getSheetsService();
            response = service.spreadsheets().values()
                    .get(SS_ID, range)
                    .execute();
        } catch (IOException ex) {
            LOG.info("IO Exception: {}", ex.getMessage(), ex);
            return;
        }

        LOG.info("processing response");
        List<List<Object>> values = response.getValues();
        String key, value;
        if (values != null && !values.isEmpty()) {
            for (List row : values) {
                if (row.size() > 0) {
                    key = row.get(0).toString().toUpperCase();
                    value = row.size() > 1 ? row.get(1).toString() : null;
                    LOG.info("{}\t=\t\t'{}'", key, value);
                    if (!si.addItem(key, value)) {
                        LOG.info("Unmatched row: '{}'='{}'", key, value);
                    }
                }
            }

            LOG.info("{}", ToStringBuilder.reflectionToString(si, ToStringStyle.MULTI_LINE_STYLE));
            SlackAttachment sa = prepSheetInfo(si);
            session.sendMessage(msgChannel, "", sa);
        }

    }

    private SlackAttachment prepSheetInfo(SheetInfo sheetInfo) {
        SlackAttachment sa = new SlackAttachment();

        if (sheetInfo.getNextGameId() > 0) {
            sa.setTitle(sheetInfo.getGameName());
            sa.setTitleLink(Constants.BGG_GAME_LINK + sheetInfo.getNextGameId());
        } else {
            sa.setTitle(sheetInfo.getGameChooser() + " has not chosen a game yet");
        }
        sa.setAuthorName("Chosen by " + sheetInfo.getGameChooser());

        sa.setText("Next game night is " + sheetInfo.getGameDate());
        sa.setThumbUrl(sheetInfo.getGameImageUrl());
        sa.addField("Pin Holder", sheetInfo.getPinHolder(), true);
        sa.addField("Next Chooser", sheetInfo.getNextChooser(), true);

        return sa;
    }
}
