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

import com.omertron.bgg.BggException;
import com.omertron.bgg.enums.HotItemType;
import com.omertron.bgg.enums.IncludeExclude;
import com.omertron.bgg.model.BoardGameExtended;
import com.omertron.bgg.model.CollectionItem;
import com.omertron.bgg.model.CollectionItemWrapper;
import com.omertron.bgg.model.HotListItem;
import com.omertron.bgg.model.IdValue;
import com.omertron.bgg.model.OwnerStatus;
import com.omertron.bgg.model.RankedList;
import com.omertron.bgg.model.SearchWrapper;
import com.omertron.bgg.model.Thing;
import com.omertron.bgg.model.UserInfo;
import com.omertron.slackbot.Constants;
import static com.omertron.slackbot.Constants.DELIM_LEFT;
import static com.omertron.slackbot.Constants.DELIM_RIGHT;
import com.omertron.slackbot.enumeration.ExitCode;
import com.omertron.slackbot.enumeration.StatCategory;
import com.omertron.slackbot.functions.BotStatistics;
import com.omertron.slackbot.functions.BotWelcome;
import com.omertron.slackbot.functions.Meetup;
import com.omertron.slackbot.utils.PropertiesUtil;
import com.ullink.slack.simpleslackapi.*;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.common.exception.ApiException;

/**
 *
 * @author Omertron
 */
public class BoardGameListener extends AbstractListener {

    private static final Logger LOG = LoggerFactory.getLogger(BoardGameListener.class);
    private static final Pattern PAT_COMMAND;
    private static final Pattern PAT_ADMIN;
    private static final Pattern PAT_COLL_PARAM = Pattern.compile("^(\\w*)(\\s(.+))?$");
    private static final String BGG_ID = "BGG ID";
    private static final String USERNAME = "username";
    private static final String INFORMATION_ON = "Information on ";
    private static final String UNKNOWN = " (Unknown)";

    static {
        List<String> commands = new ArrayList<>();
        commands.add("search");
        HelpListener.addHelpMessage(10, "search", "game name", "Search for games called *<game name>*.\nThis does not need to be the exact name of the game.", false);
        commands.add("game");
        HelpListener.addHelpMessage(11, "game", BGG_ID, "Get information about the game with this *<BGG ID>*.", false);
        HelpListener.addHelpMessage(12, "game", "game title", "Get information about *<game title>*.\nThis must be the exact name of the game as per BGG.", false);
        commands.add("coll");
        HelpListener.addHelpMessage(15, "coll", USERNAME, "Get a list of the owned games for a BGG user.", false);
        HelpListener.addHelpMessage(16, "coll", new String[]{USERNAME, "ID list"},
                "Get a list of the owned games for a BGG user that match the ID list.", false);
        commands.add("user");
        HelpListener.addHelpMessage(20, "user", USERNAME, "Get information on a BGG user.", false);
        if (PropertiesUtil.getBooleanProperty(Constants.MEETUP_ENABLE, true)) {
            commands.add("meetup");
            HelpListener.addHelpMessage(25, "meetup", new String[]{"Quantity", "DETAILED"}, "Get a list of the *<Quantity>* upcoming MeetUps.\nAdd the *<Detailed>* keyword to get more information.", false);
        }
        commands.add("hot");
        HelpListener.addHelpMessage(26, "hot", new String[]{"boardgame", "person", "company"}, "Get the top 10 items from the category passed.\nDefault, if empty, is boardgames.", false);

        String regex = new StringBuilder("(?i)")
                .append("\\").append(DELIM_LEFT).append("\\").append(DELIM_LEFT)
                .append("(").append(StringUtils.join(commands, "|")).append(")(?:\\W*)(.*)")
                .append("\\").append(DELIM_RIGHT).append("\\").append(DELIM_RIGHT)
                .toString();

        LOG.info("Command Pattern: {}", regex);
        PAT_COMMAND = Pattern.compile(regex);

        commands.clear();
        commands.add("welcome");
        HelpListener.addHelpMessage(97, "welcome", "user", "Send welcome message to *<user>*", true);
        commands.add("restart");
        HelpListener.addHelpMessage(98, "restart", "", "Shutdown and restart the bot.\nThis is used to upgrade the bot to the latest version", true);
        commands.add("quit");
        HelpListener.addHelpMessage(99, "quit", "", "Shutdown the bot.\nNote the bot will need to be manually restarted", true);

        // (?:<.+?>\W+|\[\[){1}?(quit|restart)(.*?)(?:\]\])?$
        regex = new StringBuilder("(?:<.+?>\\W+|")
                .append("\\").append(DELIM_LEFT).append("\\").append(DELIM_LEFT)
                .append("){1}?(")
                .append(StringUtils.join(commands, "|"))
                .append(")(.*?)(?:")
                .append("\\").append(DELIM_RIGHT).append("\\").append(DELIM_RIGHT)
                .append(")?$")
                .toString();

        LOG.info("Admin Pattern: {}", regex);
        PAT_ADMIN = Pattern.compile(regex);
    }

    @Override
    public void onEvent(SlackMessagePosted event, SlackSession session) {
        // Channel On Which Message Was Posted
        SlackChannel msgChannel = event.getChannel();
        String msgContent = event.getMessageContent();
        SlackUser msgSender = event.getSender();

        // Filter out the bot's own messages
        if (session.sessionPersona().getId().equals(event.getSender().getId())) {
            return;
        }

        // Search for a user commnd pattern
        Matcher mCmd = PAT_COMMAND.matcher(msgContent);
        if (mCmd.matches()) {
            userCommand(session, msgChannel, event, msgSender, mCmd);
            return;
        }

        // Search for a direct command pattern
        Matcher mAdmin = PAT_ADMIN.matcher(msgContent);
        if (mAdmin.matches()) {
            adminCommand(session, msgChannel, msgSender, mAdmin);
        }
    }

    /**
     * Manage the user commands
     *
     * @param session
     * @param msgChannel
     * @param msgSender
     * @param mCmd
     */
    private void userCommand(SlackSession session, SlackChannel msgChannel, SlackMessagePosted event, SlackUser msgSender, Matcher mCmd) {
        String command = mCmd.group(1).toUpperCase();
        String query = mCmd.group(2);

        LOG.info("Command '{}', query '{}'", command, query);

        switch (command) {
            case "SEARCH":
                botUpdateChannel(session, event, E_GREY_EXCLAMATION);
                BotStatistics.increment(StatCategory.SEARCH, msgSender.getUserName());
                commandSearch(session, msgChannel, query);
                break;
            case "GAME":
                botUpdateChannel(session, event, E_GREY_EXCLAMATION);
                BotStatistics.increment(StatCategory.GAME, msgSender.getUserName());
                commandGame(session, msgChannel, query);
                break;
            case "USER":
                botUpdateChannel(session, event, E_GREY_EXCLAMATION);
                BotStatistics.increment(StatCategory.USER, msgSender.getUserName());
                commandUser(session, msgChannel, query);
                break;
            case "COLL":
                botUpdateChannel(session, event, E_GREY_EXCLAMATION);
                BotStatistics.increment(StatCategory.COLLECTION, msgSender.getUserName());
                commandCollection(session, msgChannel, query);
                break;
            case "MEETUP":
                botUpdateChannel(session, event, E_GREY_EXCLAMATION);
                BotStatistics.increment(StatCategory.MEETUP, msgSender.getUserName());
                commandMeetup(session, msgChannel, query);
                break;
            case "HOT":
                botUpdateChannel(session, event, E_GREY_EXCLAMATION);
                BotStatistics.increment(StatCategory.HOT, msgSender.getUserName());
                commandHotList(session, msgChannel, query);
                break;
            default:
                LOG.info("Unknown command '" + command + "' found. Ignoring.");
        }
    }

    /**
     * Manage the admin commands
     *
     * @param session
     * @param msgChannel
     * @param msgSender
     * @param mAdmin
     */
    private void adminCommand(SlackSession session, SlackChannel msgChannel, SlackUser msgSender, Matcher mAdmin) {
        String command = mAdmin.group(1).toUpperCase();
        String params = mAdmin.group(2);

        if (msgSender.isAdmin()) {
            LOG.info("Command '{}' recieved from '{}' ({}) with params '{}'", command, msgSender.getUserName(), msgSender.getId(), params);
            BotStatistics.writeFile();
            switch (command) {
                case "QUIT":
                    adminQuitRestart(session, msgChannel, msgSender, true);
                    break;
                case "RESTART":
                    adminQuitRestart(session, msgChannel, msgSender, false);
                    break;
                case "WELCOME":
                    adminWelcome(params, session, msgChannel);
                    break;
                default:
                    LOG.info("Unknown command '{}' received from {}", command, msgSender.getUserName());
            }
        } else {
            session.sendMessageToUser(msgSender, "You are not authorised to use this command", null);
        }
    }

    /**
     * Quit or restart the bot
     *
     * @param session
     * @param channel
     * @param sender
     * @param quit
     */
    private void adminQuitRestart(SlackSession session, SlackChannel channel, SlackUser sender, boolean quit) {
        if (quit) {
            session.sendMessage(channel, "Bot will now quit :disappointed:");
        } else {
            session.sendMessage(channel, "Bot will now attempt to restart :relieved:");
        }

        // Update the stats
        BotStatistics.increment(StatCategory.ADMIN, sender.getUserName());

        // Quit or restart the bot
        com.omertron.slackbot.SlackBot.shutdown(quit ? ExitCode.QUIT : ExitCode.RESTART);
    }

    /**
     * Send out a welcome message.
     *
     * @param params
     * @param session
     * @param channel
     */
    private void adminWelcome(String params, SlackSession session, SlackChannel channel) {
        String user = StringUtils.trimToEmpty(params);
        if ("WHO".equalsIgnoreCase(user)) {
            BotWelcome.listUsers(session, channel);
            return;
        }

        SlackUser slackUser = session.findUserByUserName(user);
        if (slackUser == null) {
            session.sendMessage(channel, String.format("No user with username '%1$s' found", user));
        } else {
            session.sendMessage(channel, "Sending welcome message to " + slackUser.getUserName());
            BotWelcome.sendWelcomeMessage(session, channel, slackUser);
        }
    }

    /**
     * Search for a game
     *
     * @param session
     * @param msgChannel
     * @param msgSender
     * @param query
     */
    private void commandSearch(SlackSession session, SlackChannel msgChannel, String query) {
        StringBuilder response;
        SearchWrapper results;

        LOG.info("Search request for '{}'", query);
        session.sendTyping(msgChannel);

        try {
            results = BGG.searchBoardGame(query, false, false);
            LOG.info("Got {} results for '{}'", results.getTotal(), query);
        } catch (NullPointerException ex) {
            LOG.warn("Error getting BGG information: {}", ex.getMessage(), ex);
            session.sendMessage(msgChannel, "Could not find information from BGG for " + query + ".\n Error: " + ex.getMessage());
            return;
        } catch (BggException ex) {
            LOG.warn("Error getting BGG information: {}", ex.getMessage(), ex);
            session.sendMessage(msgChannel, "Could not get information from BGG for " + query + ".\n Error: " + ex.getMessage());
            return;
        }

        if (results.getItems() == null || results.getItems().isEmpty()) {
            session.sendMessage(msgChannel, "Could not find information from BGG for " + query);
            return;
        }

        response = new StringBuilder("Found ");
        response.append(results.getTotal())
                .append(" matches for *")
                .append(query)
                .append("*");

        List<SlackAttachment> attachments = new ArrayList<>();
        for (Thing item : results.getItems()) {
            attachments.add(makeSimpleAttachment(item));
        }

        SlackPreparedMessage spm = new SlackPreparedMessage.Builder()
                .withMessage(response.toString())
                .withAttachments(attachments)
                .withUnfurl(false)
                .build();

        session.sendMessage(msgChannel, spm);

    }

    /**
     * Get information on a specific game
     *
     * @param session
     * @param msgChannel
     * @param msgSender
     * @param query
     */
    private void commandGame(SlackSession session, SlackChannel msgChannel, String query) {
        int bggId = 0;
        // If the query is a string, first search for the game
        if (!NumberUtils.isDigits(query)) {
            try {
                // Assume an exact search request
                SearchWrapper wrapper = BGG.searchBoardGame(query, true, false);
                if (wrapper.getTotal() > 0) {
                    bggId = wrapper.getItems().get(0).getId();
                }
            } catch (BggException ex) {
                LOG.warn("Failed to get exact search for {} from BGG", ex.getMessage(), ex);
                session.sendMessage(msgChannel, "Unable to find information on *'" + query + "'*");
                return;
            }
        } else {
            // Try converting the number
            bggId = NumberUtils.toInt(query, 0);
        }

        if (bggId > 0) {
            try {
                List<BoardGameExtended> results = BGG.getBoardGameInfo(bggId);
                if (results == null || results.isEmpty()) {
                    session.sendMessage(msgChannel, "No results found for BGG ID " + bggId);
                    return;
                }

                session.sendMessage(msgChannel, null, makeDetailedAttachment(results.get(0)));
            } catch (BggException ex) {
                LOG.warn("Failed to get information from BGG on game ID {} - Query '{}'", bggId, query, ex);
                session.sendMessage(msgChannel, "Failed to get information from BGG on game ID " + bggId + " - Query '" + query + "'");
            }
        } else {
            session.sendMessage(msgChannel, "Unable to find information for game with ID *" + query + "*");
        }

    }

    /**
     * Get BGG information on a user
     *
     * @param msgChannel
     * @param msgSender
     * @param username
     */
    private void commandUser(SlackSession session, SlackChannel msgChannel, String username) {
        LOG.info("COMMAND: User information for '{}'", username);

        UserInfo result;
        try {
            result = BGG.getUserInfo(username);
        } catch (BggException ex) {
            LOG.warn("Failed to get user information on '{}'", username, ex);
            session.sendMessage(msgChannel, "Failed to get user information on " + username);
            return;
        }

        if (result == null) {
            session.sendMessage(msgChannel, "No information found for username '" + username + "'");
            return;
        }

        SlackAttachment sa = new SlackAttachment();
        sa.setFallback("User information on " + username);
        sa.setTitle(StringUtils.joinWith(" ", result.getFirstName(), result.getLastName()));
        sa.setTitleLink(Constants.BGG_LINK_USER + result.getName());
        sa.setAuthorIcon(formatHttpLink(result.getAvatarLink()));
        sa.setColor(Constants.ATTACH_COLOUR_GOOD);
        sa.addField("Year Registered", "" + result.getYearRegistered(), true);
        sa.addField("Location", StringUtils.joinWith(", ", result.getStateOrProvince(), result.getCountry()), true);
        sa.addField("Trade Rating", "" + result.getTradeRating(), true);
        sa.addField("Last Login", result.getLastLogin(), true);

        boolean first = true;
        StringBuilder sb = new StringBuilder();
        if (!result.getHotList().isEmpty()) {
            for (RankedList line : result.getHotList()) {
                if (first) {
                    first = false;
                } else {
                    sb.append("\n");
                }
                sb.append(String.format("%1$2d-%2$s (<https://boardgamegeek.com/boardgame/%3$d|%3$d>)", line.getRank(), line.getName(), line.getId()));
            }
        }
        sa.addField("Hot List", sb.toString(), false);

        first = true;
        sb = new StringBuilder();
        if (!result.getTopList().isEmpty()) {
            for (RankedList line : result.getHotList()) {
                if (first) {
                    first = false;
                } else {
                    sb.append("\n");
                }
                sb.append(String.format("%1$2d-%2$s (<https://boardgamegeek.com/boardgame/%3$d|%3$d>)", line.getRank(), line.getName(), line.getId()));
            }
        }
        sa.addField("Top List", sb.toString(), false);

        session.sendMessage(msgChannel, result.getName(), sa);
    }

    /**
     * Get the collection for a user
     *
     * @param session
     * @param msgChannel
     * @param params
     */
    private void commandCollection(SlackSession session, SlackChannel msgChannel, String params) {
        LOG.info("Getting collection information, params: '{}'", params);
        // Split the parameters out (if multiple)
        String username;
        String ids;
        Matcher m = PAT_COLL_PARAM.matcher(params);
        if (m.matches()) {
            username = m.group(1);
            ids = StringUtils.trimToNull(m.group(2));
        } else {
            username = params;
            ids = null;
        }

        LOG.info("  Username: '{}'", username);
        if (ids != null) {
            LOG.info("  ID(s): '{}'", ids);
        }

        CollectionItemWrapper result;
        try {
            List<IncludeExclude> includes = new ArrayList<>();
            List<IncludeExclude> excludes = new ArrayList<>();

            if (ids == null) {
                // Just get the username's owned collection
                includes.add(IncludeExclude.OWN);
            } else {
                // Get the individual ID items
                includes.add(IncludeExclude.STATS);
            }

            LOG.info("Getting collection information for '{}' with IDs '{}' & includes '{}'", username, ids, includes);
            result = BGG.getCollectionInfo(username, ids, includes, excludes, false);
        } catch (BggException ex) {
            LOG.warn("Failed to get collection for user '{}'", username, ex);
            session.sendMessage(msgChannel, "Failed to get collection for user " + username);
            return;
        }

        if (result.getTotalItems() == 0) {
            String message = "No information found for username '" + username + "'";
            if (ids != null) {
                message += " with IDs '" + ids + "'";
            }
            session.sendMessage(msgChannel, message);
            return;
        } else {
            LOG.info("Found {} collection items for {}", result.getTotalItems(), username);
        }

        List<SlackAttachment> collList;
        if (ids == null) {
            LOG.info("Creating simple collection");
            collList = createSimpleCollection(session, msgChannel, result.getItems(), username);
        } else {
            LOG.info("Creating detailed collection");
            collList = createDetailedCollection(session, msgChannel, result.getItems(), username);
        }

        SlackPreparedMessage spm = new SlackPreparedMessage.Builder()
                .withUnfurl(false)
                .addAttachments(collList)
                .build();
        session.sendMessage(msgChannel, spm);
    }

    /**
     * Create a simple list of the collection items
     *
     * @param session
     * @param msgChannel
     * @param result
     * @param username
     * @return
     */
    private List<SlackAttachment> createSimpleCollection(SlackSession session, SlackChannel msgChannel, List<CollectionItem> result, String username) {
        int total = result.size();
        int perPart = 75;
        int totalParts = (total + perPart - 1) / perPart;
        int count = 0;
        int partCount = 1;

        String collectionFormat = "%1$s's collection - part %2$d of %3$d";

        LOG.info("\tMaking simple collection for {} with {} items and page size {}", username, total, perPart);
        session.sendMessage(msgChannel, username + " has " + total + " items in their collection. There will be " + totalParts + " parts listed.");

        SlackAttachment sa = new SlackAttachment();
        sa.setFallback(String.format(collectionFormat, username, partCount, totalParts));
        sa.setTitle(String.format(collectionFormat, username, partCount, totalParts));
        sa.setTitleLink(Constants.BGG_LINK_COLL + username);
        sa.setColor(Constants.ATTACH_COLOUR_GOOD);

        List<SlackAttachment> collList = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        for (CollectionItem item : result) {
            count++;
            if (count >= perPart) {
                sa.setText(sb.toString());
                collList.add(sa);

                count = 0;
                sb = new StringBuilder();
                sa = new SlackAttachment();
                partCount++;
                sa.setFallback(String.format(collectionFormat, username, partCount, totalParts));
                sa.setAuthorName(String.format(collectionFormat, username, partCount, totalParts));
                sa.setAuthorLink(Constants.BGG_LINK_COLL + username);
                sa.setColor(Constants.ATTACH_COLOUR_GOOD);
            }

            sb.append(String.format("%1$s (%2$s) - <%3$s%4$d|%4$d>\n",
                    item.getName(),
                    item.getYearPublished(),
                    Constants.BGG_LINK_GAME,
                    item.getObjectId()));
        }
        sa.setText(sb.toString());
        collList.add(sa);

        LOG.info("\tCollection size of {} items and {} parts", result.size(), collList.size());
        return collList;
    }

    /**
     * Create a detailed view of of the collection
     *
     * @param session
     * @param msgChannel
     * @param result
     * @param username
     * @return
     */
    private List<SlackAttachment> createDetailedCollection(SlackSession session, SlackChannel msgChannel, List<CollectionItem> result, String username) {
        List<SlackAttachment> collList = new ArrayList<>();

        String logMessage, attMessage;
        switch (result.size()) {
            case 0:
                session.sendMessage(msgChannel, "No items found in the collection of " + username);
                return collList;
            case 1:
                logMessage = "Creating detailed attatchment for {} for {} item";
                attMessage = "Getting informaton on the item from the collection of " + username;
                break;
            default:
                logMessage = "Creating detailed attachments for {} for {} items";
                attMessage = "Getting informaton on the " + result.size() + " items from the collection of " + username;
        }

        LOG.info(logMessage, username, result.size());
        session.sendMessage(msgChannel, attMessage);
        result.forEach(game -> collList.add(createGameAttachment(game)));
        return collList;
    }

    /**
     * Format the collection item (game) into an attachment
     *
     * @param game
     * @return
     */
    private SlackAttachment createGameAttachment(CollectionItem game) {
        SlackAttachment sa = new SlackAttachment();
        String year = game.getYearPublished() == null ? UNKNOWN : " (" + game.getYearPublished() + ")";

        sa.setFallback(INFORMATION_ON + game.getName());
        sa.setTitle(game.getName() + year);
        sa.setTitleLink(Constants.BGG_LINK_GAME + game.getObjectId());
        sa.setAuthorIcon(game.getThumbnail());
        sa.setText(StringEscapeUtils.unescapeHtml4(game.getComment()));
        sa.setColor(Constants.ATTACH_COLOUR_GOOD);
        sa.setThumbUrl(formatHttpLink(game.getThumbnail()));
        sa.addField(BGG_ID, String.valueOf(game.getObjectId()), true);
        if (game.getStats() != null && game.getStats().getRating() != null) {
            float value = game.getStats().getRating().getValue();
            sa.addField("Rating", "" + (value > 0 ? value : "Not Rated"), true);
        }

        if (game.getNumPlays() > 0) {
            sa.addField("Num Plays", "" + game.getNumPlays(), true);
        }

        LOG.info("Owner Status: {}", game.getOwnerStatus().toString());

        List<String> status = calculateStatus(game.getOwnerStatus());
        if (!status.isEmpty()) {
            sa.addField("Owner Status", StringUtils.join(status, ","), true);
        }
        LOG.info("Status: {}", status.toString());
        return sa;
    }

    /**
     * Create a string list with the list of owner status fields
     *
     * @param ownerStatus The object to process
     * @return A list of the string statuses
     */
    private List<String> calculateStatus(OwnerStatus ownerStatus) {
        List<String> status = new ArrayList<>();
        if (ownerStatus != null) {
            if (ownerStatus.isOwn()) {
                status.add("Own");
            }
            if (ownerStatus.isForTrade()) {
                status.add("For Trade");
            }
            if (ownerStatus.isPreordered()) {
                status.add("Pre-ordered");
            }
            if (ownerStatus.isPreviouslyOwned()) {
                status.add("Prev Owned");
            }
            if (ownerStatus.isWant()) {
                status.add("Wanted");
            }
            if (ownerStatus.isWantToBuy()) {
                status.add("Want To Buy");
            }
            if (ownerStatus.isWantToPlay()) {
                status.add("Want To Play");
            }
        }
        return status;
    }

    /**
     * Make a simple attachment for listing multiple games
     *
     * @param game
     * @return
     */
    private SlackAttachment makeSimpleAttachment(Thing game) {
        StringBuilder nameFormatted = new StringBuilder(game.getName());
        if (game.getYearPublished() == null) {
            nameFormatted.append(UNKNOWN);
        } else {
            nameFormatted.append(" (").append(game.getYearPublished()).append(")");
        }
        nameFormatted.append(" ID: ").append(game.getId());

        SlackAttachment sa = new SlackAttachment();
        sa.setFallback(INFORMATION_ON + game.getPrimaryName());
        sa.setTitle(nameFormatted.toString());
        sa.setTitleLink(Constants.BGG_LINK_GAME + game.getId());
        sa.setColor(Constants.ATTACH_COLOUR_GOOD);

        return sa;
    }

    /**
     * Make a detailed attachment for a game
     *
     * @param game
     * @return
     */
    private SlackAttachment makeDetailedAttachment(BoardGameExtended game) {
        SlackAttachment sa = new SlackAttachment();
        String year = game.getYearPublished() == null ? UNKNOWN : " (" + game.getYearPublished() + ")";

        sa.setFallback(INFORMATION_ON + game.getName());
        sa.setAuthorName(game.getName() + year);
        sa.setAuthorLink(Constants.BGG_LINK_GAME + game.getId());
        sa.setAuthorIcon(game.getThumbnail());
        sa.setText(StringEscapeUtils.unescapeHtml4(game.getDescription()));
        sa.setColor(Constants.ATTACH_COLOUR_GOOD);
        sa.setThumbUrl(formatHttpLink(game.getThumbnail()));
        sa.addField(BGG_ID, String.valueOf(game.getId()), true);
        sa.addField("Player Count", game.getMinPlayers() + "-" + game.getMaxPlayers(), true);
        sa.addField("Playing Time", String.valueOf(game.getPlayingTime()), true);
        sa.addField("Designer(s)", formatIdValue(game.getBoardGameDesigner()), true);
        sa.addField("Categories", formatIdValue(game.getBoardGameCategory()), true);
        sa.addField("Mechanics", formatIdValue(game.getBoardGameMechanic()), true);

        return sa;
    }

    /**
     * Format the values from an IdValue list into a comma separated string
     *
     * @param listToFormat
     * @return
     */
    private String formatIdValue(List<IdValue> listToFormat) {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (IdValue idv : listToFormat) {
            if (first) {
                first = false;
                result.append(idv.getValue());
            } else {
                result.append(", ").append(idv.getValue());
            }
        }
        return result.toString();
    }

    /**
     * Process the MeetUp details and present them
     *
     * @param session
     * @param msgChannel
     * @param query
     */
    private void commandMeetup(SlackSession session, SlackChannel msgChannel, String query) {
        int muQuantity = 1;
        boolean muDetailed = false;

        if (StringUtils.isNotBlank(query)) {
            LOG.info("Processing parameters");
            List<String> params = new ArrayList<>();
            if (query.contains(" ")) {
                params.addAll(Arrays.asList(query.split(" ")));
            } else {
                params.add(query);
            }

            LOG.info("Found {} parameters", params.size());

            for (String p : params) {
                if (StringUtils.isNumeric(p)) {
                    muQuantity = NumberUtils.toInt(p, 1);
                    continue;
                }

                if ("DETAILED".equalsIgnoreCase(p)) {
                    muDetailed = true;
                }
            }
        }

        LOG.info("Quantity: {}", muQuantity);
        LOG.info("Detailed: {}", muDetailed);

        try {
            Meetup.readMeetUp(muQuantity);
        } catch (ApiException ex) {
            LOG.warn("Failed to read data from meetup: {}", ex.getMessage(), ex);
            com.omertron.slackbot.SlackBot.messageAdmins(session, "Failed to read data from meetup: " + ex.getMessage());
            return;
        }

        Meetup.getMeetupsDays(7, false);

        List<SlackAttachment> attach = Meetup.getMeetupsQty(muQuantity, muDetailed);
        SlackPreparedMessage preparedMessage = new SlackPreparedMessage.Builder()
                .addAttachments(attach)
                .withMessage("These are the upcoming MeetUps:")
                .build();
        session.sendMessage(msgChannel, preparedMessage);
    }

    /**
     * Get the Hot List and format it
     *
     * @param session Session
     * @param channel Channel
     * @param param ItemType String
     */
    private void commandHotList(SlackSession session, SlackChannel channel, String param) {
        HotItemType itemType = validateHotParam(param);
        LOG.info("Getting hot list for '{}'", itemType.toString());

        try {
            List<HotListItem> results = BGG.getHotItems(itemType);

            List<SlackAttachment> listAttach = new ArrayList<>();
            for (HotListItem item : results) {
                if (item.getRank() <= 10) {
                    listAttach.add(convertHotListToAttach(itemType, item));
                }
            }

            SlackPreparedMessage spm = new SlackPreparedMessage.Builder()
                    .withMessage("Hot List for " + itemType.toString())
                    .addAttachments(listAttach)
                    .build();
            session.sendMessage(channel, spm);
        } catch (BggException ex) {
            LOG.info("Failed to get Hot List for {}", itemType.toString(), ex);
        }
    }

    /**
     * Format a HotListItem into a SlackAttachment
     *
     * @param listItem HotListItem to format
     * @return A SlackAttachment
     */
    private SlackAttachment convertHotListToAttach(HotItemType itemType, HotListItem listItem) {
        SlackAttachment sa = new SlackAttachment();

        sa.setColor(Constants.ATTACH_COLOUR_GOOD);
        sa.setThumbUrl(formatHttpLink(listItem.getThumbnail()));
        sa.setAuthorName("#" + listItem.getRank());

        if (listItem.getYearPublished() == null || listItem.getYearPublished() == 0) {
            sa.setTitle(listItem.getName());
        } else {
            sa.setTitle(String.format("%1$s (%2$d)", listItem.getName(), listItem.getYearPublished()));
        }

        switch (itemType) {
            case BOARDGAME:
                sa.setTitleLink(Constants.BGG_LINK_GAME + listItem.getId());
                break;
            case BOARDGAMECOMPANY:
                sa.setTitleLink(Constants.BGG_LINK_PUBLISHER + listItem.getId());
                break;
            case BOARDGAMEPERSON:
                sa.setTitleLink(Constants.BGG_LINK_DESIGNER + listItem.getId());
                break;
            default:
        }

        return sa;
    }

    /**
     * Validate the source string passed and convert to a HotItemType enum
     *
     * @param source Source string to convert
     * @return Matched HotItemType or default if unmatched
     */
    private HotItemType validateHotParam(String source) {
        LOG.info("Validating/Converting '{}' to a HotListItem");
        if (StringUtils.isBlank(source)) {
            return HotItemType.BOARDGAME;
        }

        if (StringUtils.equalsIgnoreCase("boardgame", source)) {
            return HotItemType.BOARDGAME;
        } else if (StringUtils.equalsIgnoreCase("person", source)) {
            return HotItemType.BOARDGAMEPERSON;
        } else if (StringUtils.equalsIgnoreCase("company", source)) {
            return HotItemType.BOARDGAMECOMPANY;
        } else {
            return HotItemType.BOARDGAME;
        }
    }

}
