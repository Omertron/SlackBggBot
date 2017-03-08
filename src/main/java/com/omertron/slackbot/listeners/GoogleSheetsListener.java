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
import com.omertron.slackbot.model.HelpInfo;
import com.omertron.slackbot.model.PlayerInfo;
import com.omertron.slackbot.model.SheetInfo;
import com.omertron.slackbot.sheets.GoogleSheets;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPreparedMessage;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleSheetsListener implements SlackMessagePostedListener {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleSheetsListener.class);
    private static final Pattern PAT_SHEETS = Pattern.compile("^\\Qwbb\\E(\\s\\w*)?(\\s.*)?", Pattern.CASE_INSENSITIVE);
    private static final String SS_ID = "1Tbnvj3Colt5CnxlDUNk1L10iANm4jVUvJpD53mjKOYY";
    private static final List<String> CHANNELS = new ArrayList<>();
    private final Sheets service;
    private static final Map<String, PlayerInfo> PLAYERS = new HashMap<>();
    // Help data
    private static final Map<Integer, HelpInfo> HELP = new TreeMap<>();
    private static SlackAttachment helpMessage;
    // Sheet ranges
    private static final String PLAYER_NAMES = "Stats!B4:D14";
    private static final String NEXT_GAME_DATA = "Stats!R16:S27";

    public GoogleSheetsListener() {
        // Add the allowed channels
        CHANNELS.add("G3RU2Q5MG"); // bot test
        CHANNELS.add("G3QQES762"); // WBB channel

        // Attempt to authorise the sheet reader
        if (!GoogleSheets.isAuthorised()) {
            GoogleSheets.authorise();
        }
        service = GoogleSheets.getSheetsService();

        generateHelpMessage();

        // Get the static data
        getStaticData();
    }

    private void generateHelpMessage() {
        HELP.clear();
        // Add the help commands
        HELP.put(10, new HelpInfo("NEXT", "", "Get information on the next scheduled game for the group", false));
        HELP.put(20, new HelpInfo("ADD", "Name", "Add *<name>* to the play list for this game.\nIf blank, will add *YOU*", false));
        HELP.put(30, new HelpInfo("REMOVE", "Name", "Remove *<name>* to the play list for this game.\nIf blank, will remove *YOU*", false));
        HELP.put(40, new HelpInfo("SET", "Game Name", "Sets the next game to be played to *<Game Name>*", false));

        helpMessage = new SlackAttachment();

        helpMessage.setFallback("Help commads for the bot");
        
        StringBuilder text = new StringBuilder("The following commands are available from the game bot for this channel.\n");
        text.append("These commands should be typed on a line on thier own after 'WBB'.\n")
                .append("E.G. `WBB NEXT`");
        
        helpMessage.setPretext(text.toString());
        helpMessage.addMarkdownIn("fields");
        helpMessage.setColor("good");

        for (Map.Entry<Integer, HelpInfo> entry : HELP.entrySet()) {
            HelpInfo hi = entry.getValue();
            if (!hi.isAdmin()) {
                helpMessage.addField(hi.getFormattedCommand(), hi.getMessage(), false);
            }
        }

    }

    @Override
    public void onEvent(SlackMessagePosted event, SlackSession session) {
        // Channel On Which Message Was Posted
        SlackChannel msgChannel = event.getChannel();
        SlackUser msgSender = event.getSender();
        if (!authenticate(session, msgChannel, msgSender)) {
            return;
        }

        String msgContent = event.getMessageContent();
        Matcher m = PAT_SHEETS.matcher(msgContent);
        if (m.matches()) {
            String command = m.group(1) == null ? "HELP" : m.group(1).toUpperCase().trim();
            String params = m.groupCount() > 2 ? m.group(2).trim() : null;
            LOG.info("Command '{}' & params '{}'", command, params);

            switch (command) {
                case "HELP":
                    session.sendMessage(msgChannel, "", helpMessage);
                    break;
                case "NEXT":
                    showNextGame(session, msgChannel);
                    break;
                default:
                    session.sendMessage(msgChannel, "Sorry, '" + command + "' is not implemented yet");
            }
        }
    }

    /**
     * Check the channel and user to see if the bot has been called from the
     * correct place(s)
     *
     * @param session
     * @param msgChannel
     * @param msgSender
     * @return
     */
    private boolean authenticate(SlackSession session, SlackChannel msgChannel, SlackUser msgSender) {
        // Check the channel is WBB channel (or test D40EZ44QZ)
        if (!CHANNELS.contains(msgChannel.getId()) && !msgChannel.getId().startsWith("D")) {
            // Not the right channel
            LOG.debug("Sheets bot called from invalid channel: {}", msgChannel.getId());
            return false;
        }

        // Filter out the bot's own messages
        return !session.sessionPersona().getId().equals(msgSender.getId());
    }

    private void showHelp(SlackSession session, SlackChannel msgChannel) {
        List<SlackAttachment> commands = new ArrayList<>();

        SlackPreparedMessage spm = new SlackPreparedMessage.Builder()
                .withMessage("The following commands are available from this bot")
                .withAttachments(commands)
                .build();

        session.sendMessage(msgChannel, spm);
    }

    private void showNextGame(SlackSession session, SlackChannel msgChannel) {
        SheetInfo si = new SheetInfo();
        ValueRange response = getResponse(NEXT_GAME_DATA);

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
        }

        response = getResponse("Game Log!F" + si.getLastRow());
        values = response.getValues();
        if (values != null && !values.isEmpty()) {
            List<Object> row = values.get(0);
            if (row != null && !row.isEmpty()) {
                String players = row.get(0).toString();
                for (String p : StringUtils.split(players, ",")) {
                    if (PLAYERS.containsKey(p)) {
                        si.addPlayer(PLAYERS.get(p).getName());
                    } else {
                        si.addPlayer(p);
                    }
                }
            }
        }

        LOG.info("{}", ToStringBuilder.reflectionToString(si, ToStringStyle.MULTI_LINE_STYLE));
        SlackAttachment sa = prepSheetInfo(si);
        session.sendMessage(msgChannel, "", sa);

    }

    /**
     * Get a range from the spreadsheet
     *
     * @param range
     * @return
     */
    private ValueRange getResponse(final String range) {
        try {
            return service.spreadsheets().values()
                    .get(SS_ID, range)
                    .execute();
        } catch (IOException ex) {
            LOG.info("IO Exception: {}", ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Generate the next game attachment
     *
     * @param sheetInfo
     * @return
     */
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

        if (!sheetInfo.getPlayers().isEmpty()) {
            sa.addField("Attendees", StringUtils.join(sheetInfo.getPlayers(), ","), true);
        }

        sa.addField("Pin Holder", sheetInfo.getPinHolder(), true);
        sa.addField("Next Chooser", sheetInfo.getNextChooser(), true);

        return sa;
    }

    private void getStaticData() {
        // get the player data
        ValueRange response = getResponse(PLAYER_NAMES);

        LOG.info("Adding players:");
        PLAYERS.clear();
        List<List<Object>> values = response.getValues();
        if (values != null && !values.isEmpty()) {
            PlayerInfo pi;
            for (List row : values) {
                if (row.size() > 0) {
                    pi = new PlayerInfo(row.get(0).toString(), row.get(1).toString());
                    if (row.size() > 2) {
                        pi.setBggUsername(row.get(2).toString());
                    }
                    LOG.info("\t{}", pi.toString());
                    PLAYERS.put(pi.getInitial(), pi);
                }
            }
        }
    }
}
