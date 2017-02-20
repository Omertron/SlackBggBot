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
import com.omertron.slackbot.utils.GitRepositoryState;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static SlackAttachment aboutMessage = null;

    static {
        String regex = new StringBuilder("(?i)")
                .append("\\").append(DELIM_LEFT).append("\\").append(DELIM_LEFT)
                .append("(help|about)(?:\\W*)(.*)")
                .append("\\").append(DELIM_RIGHT).append("\\").append(DELIM_RIGHT)
                .toString();

        LOG.info("Help Pattern: {}", regex);
        PAT_HELP = Pattern.compile(regex);
    }

    static class HelpInfo {

        String command;
        String param;
        String message;

        public HelpInfo(String command, String param, String message) {
            this.command = command;
            this.param = param;
            this.message = message;
        }
    }

    @Override
    public void onEvent(SlackMessagePosted event, SlackSession session) {
        // Channel On Which Message Was Posted
        SlackChannel msgChannel = event.getChannel();
        String msgContent = event.getMessageContent();

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
                    break;
                case "ABOUT":
                    session.sendMessage(msgChannel, "", getAboutMessage());
                    break;
                default:
                    LOG.warn("Unknown command recieved: '{}'", command);
            }
        }
    }

    /**
     * Add a help message to the list
     *
     * @param command
     * @param message
     */
    public static void addHelpMessage(String command, String message) {
        addHelpMessage(command, "", message);
    }

    /**
     * Add a help message to the list with parameters
     *
     * @param command
     * @param param
     * @param message
     */
    public static void addHelpMessage(String command, String param, String message) {
        helpMessage = null;
        INFO.add(new HelpInfo(command, param, message));
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
            helpMessage.setPretext("The following commands are available from the game bot");
            helpMessage.addMarkdownIn("fields");
            helpMessage.setColor("good");

            for (HelpInfo hi : INFO) {
                helpMessage.addField(String.format("%1$s <%2$s>", hi.command, hi.param), hi.message, false);
            }
        }

        return helpMessage;
    }

    private SlackAttachment getAboutMessage() {
        if (aboutMessage == null) {
            GitRepositoryState grs = new GitRepositoryState();

            aboutMessage = new SlackAttachment();

            aboutMessage.setFallback("Information about the bot");
            aboutMessage.setPretext("Information about this bot:");
//            aboutMessage.addMarkdownIn("fields");
            aboutMessage.setColor("good");

            aboutMessage.addField("Name", Constants.BOT_NAME, true);
            aboutMessage.addField("Version", Constants.BOT_VERSION, true);
            aboutMessage.addField("Author", "Stuart Boston (Omertron)", true);

            aboutMessage.addField("Build Version", grs.getBuildVersion(), true);
            aboutMessage.addField("Commit ID", grs.getCommitIdAbbrev(), true);
            aboutMessage.addField("Commit Time", grs.getCommitTime(), false);
            aboutMessage.addField("Build Time", grs.getBuildTime(), false);
            aboutMessage.addField("Commit Message", grs.getCommitMessageFull(), false);
        }

        return aboutMessage;
    }
}
