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

import com.omertron.slackbot.Constants;
import static com.omertron.slackbot.Constants.DELIM_LEFT;
import static com.omertron.slackbot.Constants.DELIM_RIGHT;
import com.omertron.slackbot.SlackBot;
import com.omertron.slackbot.enumeration.StatCategory;
import com.omertron.slackbot.model.HelpInfo;
import com.omertron.slackbot.functions.BotStatistics;
import com.omertron.slackbot.functions.BotWelcome;
import com.omertron.slackbot.functions.scheduler.BotTaskExecutor;
import com.omertron.slackbot.utils.GitRepositoryState;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPreparedMessage;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Omertron
 */
public class HelpListener implements SlackMessagePostedListener {

    private static final Logger LOG = LoggerFactory.getLogger(HelpListener.class);

    private static final Map<Integer, HelpInfo> INFO = new TreeMap<>();
    private static final Pattern PAT_HELP;
    private static SlackAttachment helpMessage = null;
    private static SlackAttachment helpMessageAdmin = null;
    private static SlackAttachment aboutMessage = null;

    static {
        List<String> commands = new ArrayList<>();
        commands.add("help");
        commands.add("about");
        addHelpMessage(90, "about", "Get information about the bot", false);
        commands.add("stats");
        addHelpMessage(91, "stats", "Get some stats about the bot", false);
        commands.add("tasks");
        addHelpMessage(92, "tasks", "Display stats about the tasks", true);

        String regex = new StringBuilder("(?i)")
                .append("\\").append(DELIM_LEFT).append("\\").append(DELIM_LEFT)
                .append("(").append(StringUtils.join(commands, "|")).append(")(?:\\W*)(.*)")
                .append("\\").append(DELIM_RIGHT).append("\\").append(DELIM_RIGHT)
                .toString();

        LOG.info("Help Pattern: {}", regex);
        PAT_HELP = Pattern.compile(regex);
    }

    @Override
    public void onEvent(SlackMessagePosted event, SlackSession session) {
        // Channel On Which Message Was Posted
        SlackChannel msgChannel = event.getChannel();
        String msgContent = event.getMessageContent();
        SlackUser msgSender = event.getSender();

        // Filter out the bot's own messages or messages from other bots
        if (session.sessionPersona().getId().equals(event.getSender().getId()) || event.getSender().isBot()) {
            return;
        }

        if (event.getMessageSubType() == SlackMessagePosted.MessageSubType.CHANNEL_JOIN
                || event.getMessageSubType() == SlackMessagePosted.MessageSubType.GROUP_JOIN) {

            // Send a welcome message (will not re-send if already sent)
            BotWelcome.sendWelcomeMessage(session, msgChannel, msgSender);

            // No need to continue, so return
            return;
        }

        // Search for a user commnd pattern
        Matcher m = PAT_HELP.matcher(msgContent);
        if (m.matches()) {
            BotStatistics.writeFile();
            String command = m.group(1).toUpperCase();
            switch (command) {
                case "HELP":
                    createHelpMessage(session, msgChannel, msgSender);
                    break;
                case "ABOUT":
                    BotStatistics.increment(StatCategory.ABOUT, msgSender.getUserName());
                    session.sendMessage(msgChannel, "", getAboutMessage());
                    break;
                case "STATS":
                    BotStatistics.increment(StatCategory.STATS, msgSender.getUserName());
                    String stats = BotStatistics.generateStatistics(true, SlackBot.isBotAdmin(msgSender));
                    session.sendMessage(msgChannel, stats);
                    break;
                case "TASKS":
                    SlackPreparedMessage message = BotTaskExecutor.status();
                    session.sendMessage(msgChannel, message);
                    break;
                default:
                    LOG.warn("Unknown command recieved: '{}'", command);
            }
        }
    }

    /**
     * Send the help message to the user
     *
     * @param session
     * @param msgChannel
     * @param msgSender
     */
    private void createHelpMessage(SlackSession session, SlackChannel msgChannel, SlackUser msgSender) {
        BotStatistics.increment(StatCategory.HELP, msgSender.getUserName());
        session.sendMessage(msgChannel, "", getHelpMessage());

        if (SlackBot.isBotAdmin(msgSender)) {
            session.sendMessageToUser(msgSender, "", getHelpMessageAdmin());
        }
    }

    /**
     * Add a help message to the list
     *
     * @param priority The order that a command should come in the help list
     * @param command The command word itself
     * @param message The description of the command
     * @param adminOnly
     */
    public static void addHelpMessage(Integer priority, String command, String message, boolean adminOnly) {
        addHelpMessage(priority, command, "", message, adminOnly);
    }

    /**
     * Add a help message to the list
     *
     * @param priority The order that a command should come in the help list
     * @param command The command word itself
     * @param param Any parameters for the command
     * @param message The description of the command
     * @param adminOnly Is this command for BOT admins only?
     */
    public static void addHelpMessage(Integer priority, String command, String param, String message, boolean adminOnly) {
        helpMessage = null;

        if (INFO.containsKey(priority)) {
            LOG.warn("Help message with priority '{}' already exists: '{}'", priority, INFO.get(priority).getCommand());
        }

        INFO.put(priority, new HelpInfo(command, param, message, adminOnly));
        LOG.info("Added: {} - {}", priority, INFO.get(priority).toString());
    }

    /**
     * Add a help message to the list
     *
     * @param priority The order that a command should come in the help list
     * @param command The command word itself
     * @param params An ordered list of parameters for the command
     * @param message The description of the command
     * @param adminOnly Is this command for BOT admins only?
     */
    static void addHelpMessage(Integer priority, String command, String[] params, String message, boolean adminOnly) {
        helpMessage = null;

        if (INFO.containsKey(priority)) {
            LOG.warn("Help message with priority '{}' already exists: '{}'", priority, INFO.get(priority).getCommand());
        }

        INFO.put(priority, new HelpInfo(command, params, message, adminOnly));
        LOG.info("Added: {} - {}", priority, INFO.get(priority).toString());
    }

    /**
     * Format the help commands as an attachment
     *
     * @return
     */
    private SlackAttachment getHelpMessage() {
        if (helpMessage == null) {
            helpMessage = new SlackAttachment();

            helpMessage.setFallback("Help commads for the bot");

            StringBuilder text = new StringBuilder("The following commands are available from the game bot.\n");
            text.append("Surroung the entire command including any parameters with ")
                    .append(Constants.DELIM_LEFT).append(Constants.DELIM_LEFT)
                    .append(" and ")
                    .append(Constants.DELIM_RIGHT).append(Constants.DELIM_RIGHT);

            helpMessage.setPretext(text.toString());
            helpMessage.addMarkdownIn("fields");
            helpMessage.setColor(Constants.ATTACH_COLOUR_GOOD);

            for (Map.Entry<Integer, HelpInfo> entry : INFO.entrySet()) {
                HelpInfo hi = entry.getValue();
                if (!hi.isAdmin()) {
                    helpMessage.addField(hi.getFormattedCommand(), hi.getMessage(), false);
                }
            }
        }

        return helpMessage;
    }

    /**
     * Format the help commands as an attachment
     *
     * @return
     */
    private SlackAttachment getHelpMessageAdmin() {
        if (helpMessageAdmin == null) {
            helpMessageAdmin = new SlackAttachment();

            helpMessageAdmin.setFallback("Help commads for admins of the bot");
            helpMessageAdmin.setPretext("The following admin commands are available from the game bot");
            helpMessageAdmin.addMarkdownIn("fields");
            helpMessageAdmin.setColor("good");

            for (Map.Entry<Integer, HelpInfo> entry : INFO.entrySet()) {
                HelpInfo hi = entry.getValue();
                if (hi.isAdmin()) {
                    helpMessageAdmin.addField(hi.getFormattedCommand(), hi.getMessage(), false);
                }
            }
        }

        return helpMessageAdmin;
    }

    /**
     * Generate the about message
     *
     * @return
     */
    private SlackAttachment getAboutMessage() {
        if (aboutMessage == null) {
            GitRepositoryState grs = new GitRepositoryState();

            aboutMessage = new SlackAttachment();

            aboutMessage.setFallback("Information about the bot");
            aboutMessage.setPretext("Information about this bot:");
            aboutMessage.setColor("good");

            aboutMessage.setTitle(Constants.BOT_NAME);
            aboutMessage.setTitleLink("https://boardgamegeek.com/");
            aboutMessage.setThumbUrl("https://cf.geekdo-static.com/images/geekdo/bgg_cornerlogo.png");

            StringBuilder text = new StringBuilder("This is a bot to query Board Game Geek for information on games, users collections and other useful information.\n");
            text.append("You can access the detailed help information using the command ")
                    .append(Constants.DELIM_LEFT).append(Constants.DELIM_LEFT)
                    .append("help")
                    .append(Constants.DELIM_RIGHT).append(Constants.DELIM_RIGHT).append("\n");

            aboutMessage.setText(text.toString());

            aboutMessage.addField("Bot Version", Constants.BOT_VERSION, true);
            aboutMessage.addField("Author", "Stuart Boston (<" + Constants.BGG_LINK_USER + "Omertron|Omertron>)", true);

            aboutMessage.addField("Build Version", "<https://github.com/Omertron/SlackBggBot|" + grs.getBuildVersion() + ">", true);
            aboutMessage.addField("Commit ID", grs.getCommitIdAbbrev(), true);
            aboutMessage.addField("Commit Time", grs.getCommitTime(), false);
            aboutMessage.addField("Build Time", grs.getBuildTime(), false);
            aboutMessage.addField("Commit Message", grs.getCommitMessageFull(), false);
        }

        return aboutMessage;
    }

}
