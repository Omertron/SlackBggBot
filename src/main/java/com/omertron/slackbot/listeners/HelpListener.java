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
import com.omertron.slackbot.model.HelpInfo;
import com.omertron.slackbot.utils.GitRepositoryState;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author stuar
 */
public class HelpListener implements SlackMessagePostedListener {

    private static final Logger LOG = LoggerFactory.getLogger(HelpListener.class);
    private static final List<HelpInfo> INFO = new ArrayList<>();
    private static final Pattern PAT_HELP;
    private static SlackAttachment helpMessage = null;
    private static SlackAttachment helpMessageAdmin = null;
    private static SlackAttachment aboutMessage = null;

    static {
        List<String> commands = new ArrayList<>();
        commands.add("help");
        commands.add("about");
        addHelpMessage("about", "", "Get information about the bot", false);
        commands.add("stats");
        addHelpMessage("stats", "", "Get some stats about the bot", false);

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
        SlackUser sender = event.getSender();

        // Filter out the bot's own messages
        if (session.sessionPersona().getId().equals(event.getSender().getId())) {
            return;
        }

        // Search for a user commnd pattern
        Matcher m = PAT_HELP.matcher(msgContent);
        if (m.matches()) {
            String command = m.group(1).toUpperCase();
            switch (command) {
                case "HELP":
                    session.sendMessage(msgChannel, "", getHelpMessage());

                    if (SlackBot.isBotAdmin(sender)) {
                        session.sendMessageToUser(sender, "", getHelpMessageAdmin());
                    }
                    break;
                case "ABOUT":
                    session.sendMessage(msgChannel, "", getAboutMessage());
                    break;
                case "STATS":
                    session.sendMessage(msgChannel, "Sorry, not implemented yet!");
                    break;
                default:
                    LOG.warn("Unknown command recieved: '{}'", command);
            }
        }
    }

    /**
     * Add a help message to the list
     *
     * @param command The command word itself
     * @param message The description of the command
     */
    public static void addHelpMessage(String command, String message) {
        addHelpMessage(command, "", message, false);
    }

    /**
     * Add a help message to the list with parameters
     *
     * @param command The command word itself
     * @param param Any parameters for the command
     * @param message The description of the command
     */
    public static void addHelpMessage(String command, String param, String message) {
        helpMessage = null;
        INFO.add(new HelpInfo(command, param, message, false));
    }

    /**
     * Add a help message to the list
     *
     * @param command The command word itself
     * @param param Any parameters for the command
     * @param message The description of the command
     * @param adminOnly Is this command for BOT admins only?
     */
    public static void addHelpMessage(String command, String param, String message, boolean adminOnly) {
        helpMessage = null;
        INFO.add(new HelpInfo(command, param, message, adminOnly));
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
            helpMessage.setColor("good");

            for (HelpInfo hi : INFO) {
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

            for (HelpInfo hi : INFO) {
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
            aboutMessage.addField("Author", "Stuart Boston (<" + Constants.BGG_USER_LINK + "Omertron|Omertron>)", true);

            aboutMessage.addField("Build Version", "<https://github.com/Omertron/SlackBggBot|" + grs.getBuildVersion() + ">", true);
            aboutMessage.addField("Commit ID", grs.getCommitIdAbbrev(), true);
            aboutMessage.addField("Commit Time", grs.getCommitTime(), false);
            aboutMessage.addField("Build Time", grs.getBuildTime(), false);
            aboutMessage.addField("Commit Message", grs.getCommitMessageFull(), false);
        }

        return aboutMessage;
    }
}
