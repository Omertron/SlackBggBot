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
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    // Cached Sheet Information
    private static SheetInfo sheetInfo = null;
    // Help data
    private static final Map<Integer, HelpInfo> HELP = new TreeMap<>();
    private static SlackAttachment helpMessage;
    // Sheet ranges
    private static final String RANGE_PLAYER_NAMES = "Stats!B4:D14";
    private static final String RANGE_NEXT_GAME_DATA = "Stats!R16:S27";
    private static final String RANGE_GAME_ATTENDEES = "Game Log!F";

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
            String params = StringUtils.trimToNull(m.group(2));
            LOG.info("Command '{}' & params '{}'", command, params);

            switch (command) {
                case "HELP":
                    session.sendMessage(msgChannel, "", helpMessage);
                    break;
                case "NEXT":
                    processNextGame();
                    session.sendMessage(msgChannel, "", createNextGameInfo(sheetInfo));
                    break;
                case "ADD":
                    addNameToNextGame(session, msgChannel, params, msgSender);
                    session.sendMessage(msgChannel, "Added '" + params + "' to sheet");
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

    /**
     * Retrieve and display the next game information from the sheet
     *
     * @param session
     * @param msgChannel
     */
    private void processNextGame() {
        sheetInfo = new SheetInfo();
        ValueRange response = getSheetData(RANGE_NEXT_GAME_DATA);

        List<List<Object>> values = response.getValues();
        String key, value;
        if (values != null && !values.isEmpty()) {
            for (List row : values) {
                if (row.size() > 0) {
                    key = row.get(0).toString().toUpperCase();
                    value = row.size() > 1 ? row.get(1).toString() : null;
                    LOG.info("{}\t=\t\t'{}'", key, value);
                    if (!sheetInfo.addItem(key, value)) {
                        LOG.info("Unmatched row: '{}'='{}'", key, value);
                    }
                }
            }
        }

        response = getSheetData(RANGE_GAME_ATTENDEES + sheetInfo.getLastRow());
        values = response.getValues();
        if (values != null && !values.isEmpty()) {
            List<Object> row = values.get(0);
            if (row != null && !row.isEmpty()) {
                String players = row.get(0).toString();
                for (String p : StringUtils.split(players, ",")) {
                    if (PLAYERS.containsKey(p)) {
                        sheetInfo.addPlayer(PLAYERS.get(p));
                    } else {
                        sheetInfo.addPlayer(new PlayerInfo(p, "UNKNOWN"));
                    }
                }
            }
        }

        LOG.info("{}", ToStringBuilder.reflectionToString(sheetInfo, ToStringStyle.MULTI_LINE_STYLE));
    }

    /**
     * Get a range from the spreadsheet
     *
     * @param range
     * @return
     */
    private ValueRange getSheetData(final String range) {
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
    private SlackAttachment createNextGameInfo(SheetInfo sheetInfo) {
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
            sa.addField("Attendees", sheetInfo.getNameList(), true);
        }

        sa.addField("Pin Holder", sheetInfo.getPinHolder(), true);
        sa.addField("Next Chooser", sheetInfo.getNextChooser(), true);

        return sa;
    }

    /**
     * Read information from the sheet, such as user names and store in objects
     */
    private void getStaticData() {
        // get the player data
        ValueRange response = getSheetData(RANGE_PLAYER_NAMES);

        LOG.info("Getting players from sheet:");
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

    private PlayerInfo decodeName(final String name, final SlackUser user) {
        // If blank name, user user details
        if (StringUtils.isBlank(name) || "me".equalsIgnoreCase(name)) {
            String firstName = StringUtils.split(user.getRealName(), " ")[0];
            LOG.debug("No name passed (or 'me'), using '{}' for search", firstName);
            // Try the first name
            return findPlayer(firstName);
        }

        // Search for the name
        return findPlayer(name);
    }

    private PlayerInfo findPlayer(final String search) {
        LOG.info("Searching for player '{}'", search);

        if (PLAYERS.containsKey(search)) {
            LOG.info("Found direct inital match: {}", PLAYERS.get(search).toString());
            return PLAYERS.get(search);
        }

        PlayerInfo matchedPlayer = null;
        int bestMatch = 0;
        LOG.info("Searching player list for match to '{}'", search);
        for (PlayerInfo pi : PLAYERS.values()) {
            int newScore = StringUtils.getFuzzyDistance(search, pi.getName(), Locale.ENGLISH);
            if (newScore > bestMatch) {
                LOG.info("\tBetter match found for '{}' with '{}', new score={}", search, pi.getName(), newScore);
                bestMatch = newScore;
                matchedPlayer = pi;
            }
        }

        if (bestMatch < 5) {
            LOG.info("No definitive match found for '{}' (best score was {}), using 'Other'", search, bestMatch);
            return PLAYERS.get("O");
        } else {
            LOG.info("Matched '{}' to '{}' with score of {}", matchedPlayer.getName(), search, bestMatch);
            return matchedPlayer;
        }
    }

    private void addNameToNextGame(SlackSession session, SlackChannel msgChannel, final String nameToAdd, final SlackUser requestor) {

        // If we have no sheet informatiom, then populate it
        if (sheetInfo == null) {
            processNextGame();
        }

        LOG.info("Current player list: {}", sheetInfo.getInitialList());

        PlayerInfo pi = decodeName(nameToAdd, requestor);

        if (sheetInfo.getInitialList().contains(pi.getInitial())) {
            // Already there
            String message = String.format("Player '%1$s' (%2$s) is already playing.", pi.getName(), pi.getInitial());
            LOG.info(message);
            session.sendMessage(msgChannel, message);
            return;
        }

        // Add the player to the sheet info (so we can use the inital list).
        sheetInfo.addPlayer(pi);
        LOG.info("New player list: {}", sheetInfo.getInitialList());

        List<List<Object>> writeData = new ArrayList<>();
        List<Object> dataRow = new ArrayList<>();
        dataRow.add(sheetInfo.getInitialList());
        writeData.add(dataRow);

        ValueRange vr = new ValueRange().setValues(writeData).setMajorDimension("ROWS");
        try {
            String cellRef = RANGE_GAME_ATTENDEES + (sheetInfo.getLastRow() + 1);
            LOG.info("CellRef: {}", cellRef);
            service.spreadsheets().values()
                    .update(SS_ID, cellRef, vr)
                    .setValueInputOption("RAW")
                    .execute();
        } catch (IOException ex) {
            LOG.warn("IO Exception writing to sheet: {}", ex.getMessage(), ex);
        }
    }
}
