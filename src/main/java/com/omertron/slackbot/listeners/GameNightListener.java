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

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import com.omertron.slackbot.Constants;
import com.omertron.slackbot.model.HelpInfo;
import com.omertron.slackbot.model.gamenight.GameNight;
import com.omertron.slackbot.utils.PropertiesUtil;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPreparedMessage;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameNightListener extends AbstractListener {

    private static final Logger LOG = LoggerFactory.getLogger(GameNightListener.class);
    private static final Pattern PAT_NIGHT = Pattern.compile("^\\Qgn\\E(\\s\\w*)?(\\s.*)?", Pattern.CASE_INSENSITIVE);
    private static final List<String> CHANNELS = new ArrayList<>();
    private static final List<GameNight> GAME_NIGHTS = new ArrayList<>();
    // Help data
    private static final Map<Integer, HelpInfo> HELP = new TreeMap<>();
    private static SlackAttachment helpMessage;

    /**
     * Listens for commands to do with the Wirral Biscuits & Boardgame's Google spreadsheet
     *
     */
    public GameNightListener() {
        // Add the allowed channels
        CHANNELS.add("G3RU2Q5MG"); // bot test
        CHANNELS.add(PropertiesUtil.getProperty(Constants.WBB_CHANNEL_ID)); // WBB channel

        generateHelpMessage();
    }

    /**
     * Create the help message for the bot
     */
    private static void generateHelpMessage() {
        HELP.clear();
        // Add the help commands
        HELP.put(10, new HelpInfo("LIST", "Days Ahead", "Display the upcomming games", false));
        HELP.put(20, new HelpInfo("CREATE", "", "Create a new game night", false));
        HELP.put(21, new HelpInfo("CANCEL", "ID", "Cancel a game night", false));
        HELP.put(30, new HelpInfo("ATTEND", "ID", "Mark your attendance to a game night", false));
        HELP.put(31, new HelpInfo("LEAVE", "ID", "Cancel your attendance of a game night", false));

        helpMessage = new SlackAttachment();

        helpMessage.setFallback("Help commads for the game night bot");

        StringBuilder text = new StringBuilder("The following commands are available from the game night bot for this channel.\n\n");
        text.append("They should be typed on a line on thier own after the base command `GN`.\n")
                .append("E.G. `GN LIST`\n");

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
        
        // check to see if we are in a conversation with the user already, if so, skip to the 

        Matcher m = PAT_NIGHT.matcher(event.getMessageContent());
        if (m.matches()) {
            String command = StringUtils.trimToNull(m.group(1)) == null ? "HELP" : m.group(1).toUpperCase().trim();
            String params = StringUtils.trimToNull(m.group(2));
            LOG.info("Command '{}' & params '{}'", command, params);

            switch (command) {
                case "HELP":
                    session.sendMessage(msgChannel, "", helpMessage);
                    break;
                case "LIST":
                    formatGameNightDate(session, msgChannel, null);
                    break;
                case "CREATE":
                    createGameNight(session, msgChannel, event.getSender(), params);
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
            LOG.debug("GameNight bot called from invalid channel: {}", msgChannel.getId());
            return false;
        }

        // Filter out the bot's own messages
        return !session.sessionPersona().getId().equals(msgSender.getId());
    }
    
    

    private void createGameNight(SlackSession session, SlackChannel channel, SlackUser host, String params) {
        session.sendMessage(channel, "Creating a game night with the following parameters: " + params);
        LocalDate date = getDateFromText(params);

        GameNight gn = new GameNight(host, date);
        session.sendMessage(channel, "Game Night", formatGameNightSingle(gn));
        GAME_NIGHTS.add(gn);
    }

    /**
     * Extract the date from the provided text
     *
     * @param text the string date to convert
     * @return a LocalDate object of the
     */
    private LocalDate getDateFromText(final String text) {
        LocalDate date = null;

        if (StringUtils.isBlank(text)) {
            // No date passed, assume it's now
            date = LocalDate.now();
        } else {
            Parser parser = new Parser();
            List<DateGroup> groups = parser.parse(text);

            for (DateGroup dg : groups) {
                LOG.info("There are {} dates", dg.getDates().size());
                for (Date d : dg.getDates()) {
                    LOG.info("Date: {}", d.toString());
                    date = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    break;
                }
            }
        }

        LOG.info("Got date '{}' from text '{}'", date == null ? "no date" : date.toString(), text);
        return date;
    }

    private void formatGameNightDate(SlackSession session, SlackChannel channel, LocalDate gameDate) {
        List<SlackAttachment> gameNights = new ArrayList<>();

        if (gameDate == null) {
            session.sendMessage(channel, "Displaying all game nights");
        } else {
            session.sendMessage(channel, "Displaying games for " + gameDate.toString());
        }

        for (GameNight night : GAME_NIGHTS) {
            gameNights.add(formatGameNightSingle(night));
        }

        if (gameNights.isEmpty()) {
            session.sendMessage(channel, "    There are currently no game nights scheduled.");
        } else {
            SlackPreparedMessage spm = new SlackPreparedMessage.Builder()
                    .addAttachments(gameNights)
                    .withUnfurl(false)
                    .build();
            session.sendMessage(channel, spm);
        }
    }

    /**
     * Take a single game night object and format a SlackAttachment output
     *
     * @param gameNight
     * @return formatted SlackAttachment
     */
    private SlackAttachment formatGameNightSingle(GameNight gameNight) {
        SlackAttachment sa = new SlackAttachment();

        sa.setTitle(String.format("%s's game night on %s", gameNight.getHost().getRealName(), gameNight.getGameDate().toString()));
        sa.setAuthorName(gameNight.getHost().getRealName());
        sa.addField("Game(s)", gameNight.getFormattedGameNameList(), true);
        return sa;
    }
}
