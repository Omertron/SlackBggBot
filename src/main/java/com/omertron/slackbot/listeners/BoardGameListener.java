package com.omertron.slackbot.listeners;

import com.omertron.bgg.BggApi;
import com.omertron.bgg.BggException;
import com.omertron.bgg.model.SearchWrapper;
import com.omertron.bgg.model.Thing;
import com.omertron.bgg.model.BoardGameExtended;
import com.omertron.bgg.model.CollectionItem;
import com.omertron.bgg.model.CollectionItemWrapper;
import com.omertron.bgg.model.IdValue;
import com.omertron.bgg.model.RankedList;
import com.omertron.bgg.model.UserInfo;
import com.ullink.slack.simpleslackapi.*;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.omertron.bgg.enums.IncludeExclude;
import com.omertron.slackbot.Constants;
import static com.omertron.slackbot.Constants.DELIM_LEFT;
import static com.omertron.slackbot.Constants.DELIM_RIGHT;

/**
 *
 * @author stuar
 */
public class BoardGameListener implements SlackMessagePostedListener {

    private static final Logger LOG = LoggerFactory.getLogger(BoardGameListener.class);
    private static final BggApi BGG = new BggApi();
    private static final Pattern PAT_COMMAND;
    private static final Pattern PAT_ADMIN;

    static {
        List<String> commands = new ArrayList<>();
        commands.add("search");
        HelpListener.addHelpMessage("search", "game name", "Search for games called *{game name}*.\nThis does not need to be the exact name of the game.");
        commands.add("game");
        HelpListener.addHelpMessage("game", "game title", "Get information about *{game title}*.\nThis must be the exact name of the game as per BGG.");
        HelpListener.addHelpMessage("game", "BGG ID", "Get information about the game with this *{BGG ID}*.");
        commands.add("user");
        HelpListener.addHelpMessage("user", "username", "Get information on a BGG user.");
        commands.add("coll");
        HelpListener.addHelpMessage("coll", "username", "Get a list of the owned games for a BGG user.");

        String regex = new StringBuilder("(?i)")
                .append("\\").append(DELIM_LEFT).append("\\").append(DELIM_LEFT)
                .append("(").append(StringUtils.join(commands, "|")).append(")(?:\\W*)(.*)")
                .append("\\").append(DELIM_RIGHT).append("\\").append(DELIM_RIGHT)
                .toString();

        LOG.info("Command Pattern: {}", regex);
        PAT_COMMAND = Pattern.compile(regex);

        commands.clear();
        commands.add("quit");
        commands.add("restart");

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
            String command = mCmd.group(1).toUpperCase();
            String query = mCmd.group(2);

            LOG.info("Command '{}', query '{}'", command, query);

            switch (command) {
                case "SEARCH":
                    botUpdateChannel(session, msgChannel, event);
                    commandSearch(session, msgChannel, query);
                    break;
                case "GAME":
                    botUpdateChannel(session, msgChannel, event);
                    commandGame(session, msgChannel, query);
                    break;
                case "USER":
                    botUpdateChannel(session, msgChannel, event);
                    commandUser(session, msgChannel, query);
                    break;
                case "COLL":
                    botUpdateChannel(session, msgChannel, event);
                    commandCollection(session, msgChannel, query);
                    break;
                default:
                    LOG.info("Unknown command '" + command + "' found. Ignoring.");
            }
            return;
        }

        // Search for a direct command pattern
        Matcher mAdmin = PAT_ADMIN.matcher(msgContent);
        if (mAdmin.matches()) {
            String command = mAdmin.group(1).toUpperCase();
            String params = mAdmin.group(2);

            if (msgSender.isAdmin()) {
                LOG.info("Command '{}' recieved from '{}' ({}) with params '{}'", command, msgSender.getUserName(), msgSender.getId(), params);
                switch (command) {
                    case "QUIT":
                        session.sendMessage(msgChannel, "Bot will now quit.\nGoodbye!");
                        System.exit(0);
                        break;
                    case "RESTART":
                        session.sendMessage(msgChannel, "Bot will now attempt to restart.\nSee you soon!");
                        System.exit(1);
                        break;
                    default:
                        LOG.info("Unknown command '{}' received from {}", command, msgSender.getUserName());
                }
            } else {
                session.sendMessageToUser(msgSender, "You are not authorised to use this command", null);
            }
        }
    }

    /**
     * Add a reaction to the message that called us and send the "typing..." indicator
     *
     * @param session
     * @param msgChannel
     * @param event
     */
    private void botUpdateChannel(SlackSession session, SlackChannel msgChannel, SlackMessagePosted event) {
        session.addReactionToMessage(msgChannel, event.getTimeStamp(), "grey_exclamation");
        session.sendTyping(msgChannel);
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

        LOG.info("response:\n{}", response.toString());

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
        sa.setAuthorName(StringUtils.joinWith(" ", result.getFirstName(), result.getLastName()));
        sa.setAuthorLink("https://boardgamegeek.com/user/" + result.getName());
        sa.setAuthorIcon(formatHttpLink(result.getAvatarLink()));
        sa.setColor("good");
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
     * @param username
     */
    private void commandCollection(SlackSession session, SlackChannel msgChannel, String username) {
        CollectionItemWrapper result;
        try {
            List<IncludeExclude> includes = new ArrayList<>();
            includes.add(IncludeExclude.OWN);
            result = BGG.getCollectionInfo(username, includes, null);
        } catch (BggException ex) {
            LOG.warn("Failed to get collection for user '{}'", username, ex);
            session.sendMessage(msgChannel, "Failed to get collection for user " + username);
            return;
        }

        if (result.getTotalItems() == 0) {
            session.sendMessage(msgChannel, "No information found for username '" + username + "'");
            return;
        }

        int total = result.getTotalItems();
        int perPart = 75;
        int totalParts = (total + perPart - 1) / perPart;
        int count = 0;
        int partCount = 1;

        session.sendMessage(msgChannel, username + " has " + total + " items in their collection. There will be " + totalParts + " parts listed.");

        SlackAttachment sa = new SlackAttachment();
        sa.setFallback(username + "'s collection - part " + partCount + " of " + totalParts);
        sa.setAuthorName(username + "'s collection - part " + partCount + " of " + totalParts);
        sa.setAuthorLink("https://boardgamegeek.com/collection/user/" + username);
        sa.setColor("good");

        List<SlackAttachment> collList = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        for (CollectionItem item : result.getItems()) {
            count++;
            if (count >= perPart) {
                sa.setText(sb.toString());
                collList.add(sa);

                count = 0;
                sb = new StringBuilder();
                sa = new SlackAttachment();
                partCount++;
                sa.setFallback(username + "'s collection - part " + partCount + " of " + totalParts);
                sa.setAuthorName(username + "'s collection - part " + partCount + " of " + totalParts);
                sa.setAuthorLink("https://boardgamegeek.com/collection/user/" + username);
                sa.setColor("good");
            }

            sb.append(String.format("%1$s (%2$s) - <https://boardgamegeek.com/boardgame/%3$d|%3$d>\n",
                    item.getName(),
                    item.getYearPublished(),
                    item.getObjectId()));
        }
        sa.setText(sb.toString());
        collList.add(sa);

        SlackPreparedMessage spm = new SlackPreparedMessage.Builder()
                .withUnfurl(false)
                .addAttachments(collList)
                .build();
        session.sendMessage(msgChannel, spm);
    }

    /**
     * Make a simple attachment for listing multiple games
     *
     * @param game
     * @return
     */
    private SlackAttachment makeSimpleAttachment(Thing game) {
        String year = game.getYearPublished() == null ? " (Unknown)" : " (" + game.getYearPublished() + ")";

        SlackAttachment sa = new SlackAttachment();
        sa.setFallback("Information on " + game.getPrimaryName());
        sa.setAuthorName(game.getName() + year);
        sa.setAuthorLink(Constants.BGG_GAME_LINK + game.getId());
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
        String year = game.getYearPublished() == null ? " (Unknown)" : " (" + game.getYearPublished() + ")";

        sa.setFallback("Information on " + game.getName());
        sa.setAuthorName(game.getName() + year);
        sa.setAuthorLink(Constants.BGG_GAME_LINK + game.getId());
        sa.setAuthorIcon(game.getThumbnail());
        sa.setText(game.getDescription());
        sa.setColor("good");
        sa.setThumbUrl(formatHttpLink(game.getThumbnail()));
        sa.addField("BGG ID", String.valueOf(game.getId()), true);
        sa.addField("Player Count", game.getMinPlayers() + "-" + game.getMaxPlayers(), true);
        sa.addField("Playing Time", String.valueOf(game.getPlayingtime()), true);
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
     * Add the missing "https:" to the front of a URL string
     *
     * @param link
     * @return
     */
    private String formatHttpLink(String link) {
        if (link == null || link.isEmpty() || link.startsWith("http")) {
            return link;
        } else {
            return "https:" + link;
        }
    }

}
