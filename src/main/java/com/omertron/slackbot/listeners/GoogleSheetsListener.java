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

import com.google.api.services.sheets.v4.model.ValueRange;
import com.omertron.bgg.BggException;
import com.omertron.bgg.model.BoardGameExtended;
import com.omertron.bgg.model.CollectionItem;
import com.omertron.bgg.model.CollectionItemWrapper;
import com.omertron.slackbot.Constants;
import com.omertron.slackbot.SlackBot;
import com.omertron.slackbot.functions.GoogleSheets;
import static com.omertron.slackbot.listeners.AbstractListener.BGG;
import com.omertron.slackbot.model.HelpInfo;
import com.omertron.slackbot.model.sheets.GameLogRow;
import com.omertron.slackbot.model.sheets.PlayerInfo;
import com.omertron.slackbot.model.sheets.SheetInfo;
import com.omertron.slackbot.utils.PropertiesUtil;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPreparedMessage;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.text.similarity.FuzzyScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleSheetsListener extends AbstractListener {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleSheetsListener.class);
    private static final Pattern PAT_SHEETS = Pattern.compile("^\\Qwbb\\E(\\s\\w*)?(\\s.*)?", Pattern.CASE_INSENSITIVE);
    private static final String SS_ID = "1Tbnvj3Colt5CnxlDUNk1L10iANm4jVUvJpD53mjKOYY";
    private static final List<String> CHANNELS = new ArrayList<>();
    private static final Map<String, PlayerInfo> PLAYERS = new HashMap<>();
    private static final FuzzyScore SCORE = new FuzzyScore(Locale.ENGLISH);
    // Cached Sheet Information
    private static SheetInfo sheetInfo = null;
    private static final String SHORT_DATE_FORMAT = "EEE dd MMM";
    // Help data
    private static final Map<Integer, HelpInfo> HELP = new TreeMap<>();
    private static SlackAttachment helpMessage;
    // Sheet ranges
    private static final String RANGE_PLAYER_NAMES = "Stats!B4:D18";
    private static final String RANGE_NEXT_GAME_DATA = "Stats!R20:S31";
    private static final String RANGE_GAME_NAME = "Game Log!B";
    private static final String RANGE_GAME_CHOOSER = "Game Log!E";
    private static final String RANGE_GAME_ATTENDEES = "Game Log!F";
    private static final String RANGE_GAME_WINNERS = "Game Log!G";
    private static final String RANGE_GAME_OWNER = "Game Log!I";

    /**
     * Listens for commands to do with the Wirral Biscuits & Boardgame's Google spreadsheet
     *
     */
    public GoogleSheetsListener() {
        // Add the allowed channels
        CHANNELS.add("G3RU2Q5MG"); // bot test
        CHANNELS.add(PropertiesUtil.getProperty(Constants.WBB_CHANNEL_ID)); // WBB channel

        // Attempt to initialise the sheet reader
        if (!GoogleSheets.isAuthorised()) {
            GoogleSheets.initialise();
        }

        generateHelpMessage();

        // Get the static data
        getStaticData();
    }

    /**
     * Create the help message for the bot
     */
    private static void generateHelpMessage() {
        HELP.clear();
        // Add the help commands
        HELP.put(10, new HelpInfo("NEXT", "", "Get information on the next scheduled game for the group", false));
        HELP.put(21, new HelpInfo("CHOOSER", "Name", "Set *<name>* to be the chooser for the next game.\nIf blank, will clear the chooser.", false));
        HELP.put(22, new HelpInfo("ADD", "Name", "Add *<name>* to the play list for this game.\nIf blank, will add *YOU*", false));
        HELP.put(23, new HelpInfo("REMOVE", "Name", "Remove *<name>* to the play list for this game.\nIf blank, will remove *YOU*", false));
        HELP.put(24, new HelpInfo("OWNER", "Name", "Set *<name>* to be the owner of the game.\nIf blank, will clear the current name", false));
        HELP.put(31, new HelpInfo("GAME", "Game Name", "Sets the next game to be played to *<Game Name>*\nIf blank, will clear the current game name", false));
        HELP.put(32, new HelpInfo("WINNER", "Player", "Sets the winner of the game.\nIf blank, will clear the current winners.\nCan be multiple names comma separated.", false));
        HELP.put(39, new HelpInfo("NIGHT", "", "Displays the game night information.", false));

        helpMessage = new SlackAttachment();

        helpMessage.setFallback("Help commads for the bot");

        StringBuilder text = new StringBuilder("The following commands are available from the game bot for this channel.\n\n");
        text.append("The spreadsheet for the group can be found ").append("<https://docs.google.com/spreadsheets/d/").append(SS_ID).append("|*HERE*>\n\n");
        text.append("The following commands can be used to edit the sheet for the current game.\n");
        text.append("They should be typed on a line on thier own after the base command `WBB`.\n")
                .append("E.G. `WBB NEXT`\n");

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
        if (!authenticate(session, msgChannel, event.getSender())) {
            return;
        }

        Matcher m = PAT_SHEETS.matcher(event.getMessageContent());
        if (m.matches()) {
            String command = StringUtils.trimToNull(m.group(1)) == null ? "HELP" : m.group(1).toUpperCase().trim();
            String params = StringUtils.trimToNull(m.group(2));
            LOG.info("Command '{}' & params '{}'", command, params);

            // Do an initial read of the sheet information
            if (sheetInfo == null) {
                readSheetInfo();
            }

            switch (command) {
                case "HELP":
                    session.sendMessage(msgChannel, "", helpMessage);
                    break;
                case "NEXT":
                    botUpdateChannel(session, event, E_GAME_DIE);
                    readSheetInfo();
                    session.sendMessage(msgChannel, "", createGameInfo(sheetInfo));
                    break;
                case "ADD":
                    addNameToNextGame(session, msgChannel, params, event.getSender());
                    break;
                case "REMOVE":
                    removeNameFromNextGame(session, msgChannel, params, event.getSender());
                    break;
                case "GAME":
                    updateGameName(session, msgChannel, params);
                    break;
                case "WINNER":
                    updateGenericPlayer(session, msgChannel, RANGE_GAME_WINNERS, params, "winner", true);
                    break;
                case "OWNER":
                    updateGenericPlayer(session, msgChannel, RANGE_GAME_OWNER, params, "owner", false);
                    break;
                case "CHOOSER":
                    updateGenericPlayer(session, msgChannel, RANGE_GAME_CHOOSER, params, "chooser", false);
                    break;
                case "NIGHT":
                    createGameNightMessage(session, msgChannel);
                    break;
                default:
                    session.sendMessage(msgChannel, "Sorry, '" + command + "' is not implemented yet.");
            }
        }
    }

    /**
     * Check the channel and user to see if the bot has been called from the correct place(s)
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
     * Get the sheet information
     *
     * @return
     */
    public static SheetInfo getSheetInfo() {
        return getSheetInfo(false);
    }

    /**
     * Get the sheet information
     *
     * @param forceUpdate Force an update of the sheet information, default false
     * @return
     */
    public static SheetInfo getSheetInfo(boolean forceUpdate) {
        if (sheetInfo == null || forceUpdate) {
            readSheetInfo();
        }

        return sheetInfo;
    }

    /**
     * Retrieve and display the next game information from the sheet
     *
     * @param session
     * @param msgChannel
     */
    private synchronized static void readSheetInfo() {
        sheetInfo = new SheetInfo();
        ValueRange response = GoogleSheets.getSheetData(SS_ID, RANGE_NEXT_GAME_DATA);

        List<List<Object>> values = response.getValues();
        if (values != null && !values.isEmpty()) {
            for (List row : values) {
                if (!row.isEmpty()) {
                    decodeRowFromSheet(row);
                }
            }
        }

        GameLogRow row = readGameLogRow(sheetInfo.getLastRow());
        if (row.getAttendees() != null) {
            String players = row.getAttendees();
            for (String p : StringUtils.split(players, ",")) {
                sheetInfo.addPlayer(findPlayer(p));
            }
        }

        // Deal with the issue that the game ID may have been read as "#NAME?" due to the sheet recalculating
        if (sheetInfo.getNextGameId() <= 0) {
            LOG.info("Updated game ID from {} to {}", sheetInfo.getNextGameId(), row.getGameId());
            sheetInfo.setNextGameId(row.getGameId());
        }

        LOG.info("SheetInfo READ:\n{}", ToStringBuilder.reflectionToString(sheetInfo, ToStringStyle.MULTI_LINE_STYLE));
    }

    /**
     * Extract the information from the sheet row.
     *
     * @param row
     */
    private static void decodeRowFromSheet(List row) {
        String key, value;
        key = row.get(0).toString().toUpperCase();
        value = row.size() > 1 ? row.get(1).toString() : null;
        if (sheetInfo.addItem(key, value)) {
            LOG.info("Added: '{}'='{}'", key, value);
        } else {
            LOG.info("Unmatched row: '{}'='{}'", key, value);
        }
    }

    /**
     * Generate the next game attachment
     *
     * @return SlackAttachment
     */
    public static SlackAttachment createGameInfo() {
        if (sheetInfo == null) {
            readSheetInfo();
        }
        return createGameInfo(sheetInfo);
    }

    /**
     * Generate the next game attachment
     *
     * @param sheetInfo Google SheetInfo
     * @return SlackAttachment
     */
    public static SlackAttachment createGameInfo(SheetInfo sheetInfo) {
        SlackAttachment sa = new SlackAttachment();

        if (sheetInfo.getNextGameId() > 0) {
            BoardGameExtended game = getGameInfo(sheetInfo.getNextGameId());

            if (game != null) {
                sa.setTitle(game.getName());
                sa.setTitleLink(Constants.BGG_LINK_GAME + game.getId());
                sa.setFallback(game.getName() + " for " + sheetInfo.getFormattedDate(SHORT_DATE_FORMAT));
                sa.setThumbUrl(BoardGameListener.formatHttpLink(game.getThumbnail()));
            } else {
                sa.setTitle(sheetInfo.getGameName());
                sa.setTitleLink(Constants.BGG_LINK_GAME + sheetInfo.getNextGameId());
                sa.setFallback(sheetInfo.getGameName() + " for " + sheetInfo.getFormattedDate(SHORT_DATE_FORMAT));
                sa.setThumbUrl(sheetInfo.getGameImageUrl());
            }
            sa.setAuthorName("Chosen by " + sheetInfo.getGameChooser());
        } else {
            sa.setAuthorName(sheetInfo.getGameChooser() + " choose a game already!");
            sa.setFallback("No game chosen for " + sheetInfo.getFormattedDate(SHORT_DATE_FORMAT));
            sa.setTitle(sheetInfo.getGameChooser() + " has not chosen a game yet");
            sa.setThumbUrl(sheetInfo.getGameImageUrl());
        }

        sa.setText("Next game night is " + sheetInfo.getFormattedDate("EEEE, d MMMM"));

        if (!sheetInfo.getPlayers().isEmpty()) {
            sa.addField("Attendees", sheetInfo.getNameList(", "), true);
        }

        sa.addField("Pin Holder", sheetInfo.getPinHolder(), true);
        sa.addField("Next Chooser", sheetInfo.getNextChooser(), true);

        return sa;
    }

    private static BoardGameExtended getGameInfo(int bggId) {
        if (bggId > 0) {
            try {
                List<BoardGameExtended> results = BGG.getBoardGameInfo(bggId);
                if (results == null || results.isEmpty()) {
                    return null;
                }

                return results.get(0);
            } catch (BggException ex) {
                LOG.warn("Failed to get information from BGG on game ID {}", bggId, ex);
                return null;
            }
        }

        LOG.warn("No BGG Id given to find");
        return null;
    }

    /**
     * Read information from the sheet, such as user names and store in objects
     */
    private void getStaticData() {
        // get the player data
        ValueRange response = GoogleSheets.getSheetData(SS_ID, RANGE_PLAYER_NAMES);

        LOG.info("Getting players from sheet:");
        PLAYERS.clear();
        List<List<Object>> values = response.getValues();
        if (values != null && !values.isEmpty()) {
            PlayerInfo pi;
            for (List row : values) {
                if (!row.isEmpty()) {
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

    /**
     * Attempt to find the user from the parameters passed.<p>
     * If the name is blank or "me", use the first name of the user from their user profile.
     *
     * @param name Name to add
     * @param user Slack user details to use instead
     * @return The closest match to the user searched for.
     */
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

    /**
     * Attempt to find the player in the list of names from the sheet.
     *
     * @param search Name to search for
     * @return The closest match for the search name
     */
    private static PlayerInfo findPlayer(final String player) {
        String search = StringUtils.trimToEmpty(player).toUpperCase();
        LOG.info("Searching for player '{}'", search);

        if (PLAYERS.containsKey(search)) {
            LOG.info("Found direct inital match: {}", PLAYERS.get(search).toString());
            return PLAYERS.get(search);
        }

        PlayerInfo matchedPlayer = null;
        int bestMatch = 0;
        LOG.info("Searching player list for match to '{}'", search);
        for (PlayerInfo pi : PLAYERS.values()) {
            if (search.equalsIgnoreCase(pi.getInitial())) {
                LOG.info("Found direct inital match: {}", pi.toString());
                return pi;
            }

            int newScore = SCORE.fuzzyScore(search, pi.getName());
            if (newScore > bestMatch) {
                LOG.info("\tBetter match found for '{}' with '{}', new score={}", search, pi.getName(), newScore);
                bestMatch = newScore;
                matchedPlayer = pi;
            }
        }

        if (bestMatch < 5) {
            LOG.info("No definitive match found for '{}' (best score was {}), using 'Other'", search, bestMatch);
            return PLAYERS.get("O");
        } else if (matchedPlayer == null) {
            LOG.info("No match found for '{}', using 'Other'", search);
            return PLAYERS.get("O");
        } else {
            LOG.info("Matched '{}' to '{}' with score of {}",
                    matchedPlayer.getName(),
                    search,
                    bestMatch);
            return matchedPlayer;
        }
    }

    /**
     * Add a player to the next game's player list
     *
     * @param session
     * @param msgChannel
     * @param nameToAdd
     * @param requestor
     */
    private void addNameToNextGame(SlackSession session, SlackChannel msgChannel, final String nameToAdd, final SlackUser requestor) {
        // If we have no sheet informatiom, then populate it
        if (sheetInfo == null) {
            readSheetInfo();
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

        // Send the data to the sheet and output a message
        if (GoogleSheets.writeValueToCell(SS_ID, RANGE_GAME_ATTENDEES + sheetInfo.getLastRow(), sheetInfo.getInitialList())) {
            String message = String.format("Successfully added '%1$s' (%2$s) to the next game.", pi.getName(), pi.getInitial());
            session.sendMessage(msgChannel, message);
        } else {
            String message = String.format("Failed to add '%1$s' (%2$s) to the next game.", pi.getName(), pi.getInitial());
            session.sendMessage(msgChannel, message);
        }
    }

    /**
     * Remove a player from the next game's player list
     *
     * @param session
     * @param msgChannel
     * @param nameToAdd
     * @param requestor
     */
    private void removeNameFromNextGame(SlackSession session, SlackChannel msgChannel, final String nameToAdd, final SlackUser requestor) {
        // If we have no sheet informatiom, then populate it
        if (sheetInfo == null) {
            readSheetInfo();
        }

        LOG.info("Current player list: {}", sheetInfo.getInitialList());

        PlayerInfo pi = decodeName(nameToAdd, requestor);

        if (!sheetInfo.getInitialList().contains(pi.getInitial())) {
            // Player isn't there anyway!
            String message = String.format("Player '%1$s' (%2$s) is currently not scheduled to play.", pi.getName(), pi.getInitial());
            LOG.info(message);
            session.sendMessage(msgChannel, message);
            return;
        }

        // Add the player to the sheet info (so we can use the inital list).
        sheetInfo.removePlayer(pi);
        LOG.info("New player list: {}", sheetInfo.getInitialList());

        // Send the data to the sheet and output a message
        if (GoogleSheets.writeValueToCell(SS_ID, RANGE_GAME_ATTENDEES + sheetInfo.getLastRow(), sheetInfo.getInitialList())) {
            String message = String.format("Successfully removed '%1$s' (%2$s) from the next game.", pi.getName(), pi.getInitial());
            session.sendMessage(msgChannel, message);
        } else {
            String message = String.format("Failed to remove '%1$s' (%2$s) from the next game.", pi.getName(), pi.getInitial());
            session.sendMessage(msgChannel, message);
        }
    }

    /**
     * Read a row of the game log
     *
     * @param row Row to read
     * @return Values in an object
     */
    private static GameLogRow readGameLogRow(int row) {
        String sheetRow = String.format("Game Log!A%1$d:I%1$d", row);
        LOG.info("Getting data from '{}'", sheetRow);
        ValueRange vr = GoogleSheets.getSheetData(SS_ID, sheetRow);
        return new GameLogRow(vr);
    }

    /**
     * Update the game name<p>
     * Blank or null will clear the game name
     *
     * @param session
     * @param msgChannel
     * @param gameName
     */
    private void updateGameName(SlackSession session, SlackChannel msgChannel, final String gameName) {
        LOG.info("Updating game name from '{}' to '{}'", sheetInfo.getGameName(), gameName);

        String message;
        // Send the data to the sheet and output a message
        if (GoogleSheets.writeValueToCell(SS_ID, RANGE_GAME_NAME + sheetInfo.getLastRow(), gameName)) {
            if (StringUtils.isBlank(gameName)) {
                message = "Successfully cleared the game name";
            } else {
                message = String.format("Successfully updated the game name to '%1$s'.", gameName);
            }
        } else {
            message = String.format("Failed to update the game name to '%1$s'.", gameName);
        }
        session.sendMessage(msgChannel, message);
    }

    /**
     * Method to update the sheet with a player or players for a given cell
     *
     * @param session
     * @param msgChannel
     * @param value
     * @param updateType
     * @param useInitials
     */
    private void updateGenericPlayer(SlackSession session, SlackChannel msgChannel,
            final String cellRef,
            final String value,
            final String updateType,
            boolean useInitials) {
        String message;
        boolean success;

        if (StringUtils.isBlank(value)) {
            // Blank will clear the current cell
            success = GoogleSheets.writeValueToCell(SS_ID, cellRef + sheetInfo.getLastRow(), "");
            message = String.format("Cleared the %1$s from the game", updateType);
        } else if (value.contains(",")) {
            // Value contains multiple people, so process accordingly
            Set<String> nameList = new TreeSet<>();
            // Split the given list
            for (String name : StringUtils.split(value, ",")) {
                PlayerInfo pi = findPlayer(name);
                if (useInitials) {
                    nameList.add(pi.getInitial());
                } else {
                    nameList.add(pi.getName());
                }
            }

            String concatNames = StringUtils.join(nameList, ",");
            success = GoogleSheets.writeValueToCell(SS_ID, cellRef + sheetInfo.getLastRow(), concatNames);
            message = String.format("Successfully updated the %1$s to '%2$s'.", updateType, concatNames);
        } else {
            // Assume a single person 
            PlayerInfo player = findPlayer(value);
            LOG.info("Setting {} to '{}' ({})", updateType, player.getName(), player.getInitial());
            String playerValue;
            if (useInitials) {
                playerValue = player.getInitial();
            } else {
                playerValue = player.getName();
            }
            success = GoogleSheets.writeValueToCell(SS_ID, cellRef + sheetInfo.getLastRow(), playerValue);
            message = String.format("Successfully updated the %1$s to '%2$s' (%3$s).", updateType, player.getName(), player.getInitial());
        }

        // Send the data to the sheet and output a message
        if (!success) {
            message = String.format("Failed to update the %1$s to '%2$s'.", updateType, value);
        }
        session.sendMessage(msgChannel, message);
    }

    public static void createGameNightMessage(SlackSession session, SlackChannel msgChannel) {
        LocalDate now = LocalDate.now();
        Period diff = Period.between(now, sheetInfo.getGameDate());

        switch (diff.getDays()) {
            case 0:
                session.sendMessage(msgChannel, "Game night is tonight!! :grin:", GoogleSheetsListener.createGameInfo());
                break;
            case 1:
                session.sendMessage(msgChannel, "Game night is tomorrow! :smile:", GoogleSheetsListener.createGameInfo());
                break;
            default:
                session.sendMessage(msgChannel, GoogleSheetsListener.createSimpleNightMessage(sheetInfo, diff));
                break;
        }
    }

    /**
     * Create a simple formatted message about the future game night
     *
     * @param sheetInfo SheetInfo
     * @param diff Days to next game night
     * @return Slack Prepared Message
     */
    public static SlackPreparedMessage createSimpleNightMessage(SheetInfo sheetInfo, Period diff) {
        SlackPreparedMessage.Builder spm = new SlackPreparedMessage.Builder().withUnfurl(false);

        StringBuilder sb = new StringBuilder("Game night is ");
        sb.append(sheetInfo.getFormattedDate("EEEE, d MMMM"))
                .append(", still ").append(diff.getDays()).append(" days away\n");

        if (StringUtils.isBlank(sheetInfo.getGameChooser())) {
            sb.append("There is no-one to chose the next game!!! :astonished:");
        } else {
            if ("All".equalsIgnoreCase(sheetInfo.getGameChooser())) {
                sb.append("The group is choosing :open_mouth:");
            } else if ("Other".equalsIgnoreCase(sheetInfo.getGameChooser())) {
                sb.append("It's someone else's turn to choose :open_mouth:");
            } else {
                sb.append("It's *").append(sheetInfo.getGameChooser()).append("'s* turn to choose");
            }

            if (sheetInfo.getNextGameId() <= 0) {
                // There's no game ID, this could be because there's no game selected or an error reading
                if (StringUtils.isBlank(sheetInfo.getGameName())) {
                    // No game chosen
                    LOG.error("SheetInfo READ:\n{}", ToStringBuilder.reflectionToString(sheetInfo, ToStringStyle.MULTI_LINE_STYLE));
                    sb.append(", but no game has been selected yet :angry:\n");
                } else {
                    // Error with reading the sheet or with google's API
                    sb.append(" and *").append(sheetInfo.getGameName()).append("* has been chosen.\n");
                }
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

    /**
     * UNFINISHED
     *
     * @param user
     * @param gameId
     * @return
     */
    private SlackAttachment getGameStats(String user, int gameId) {
        SlackAttachment sa = new SlackAttachment();
        sa.setTitle(user);

        CollectionItemWrapper collectionList;
        try {
            collectionList = BGG.getCollectionInfo(user, Integer.toString(gameId), null, null, false);
        } catch (BggException ex) {
            LOG.warn("Failed to get collection details for {}, game ID {}", user, gameId, ex);
            return null;
        }

        if (collectionList.getItems().isEmpty()) {
            LOG.info("User '{} has no plays for this game");

            sa.addField("Plays", "0", true);
            sa.addField("Last Play", "Never", true);

            return sa;
        }

        for (CollectionItem item : collectionList.getItems()) {
            LOG.info("\t{}", item.toString());
        }

        CollectionItem item = collectionList.getItems().get(0);
        sa.addField("Plays", Integer.toString(item.getNumPlays()), true);

        return sa;
    }
}
