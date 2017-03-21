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
package com.omertron.slackbot.stats;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.omertron.slackbot.Constants;
import com.omertron.slackbot.SlackBot;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to store any statistics about the bot
 *
 * @author stuart
 */
public final class BotWelcome {

    private static final Logger LOG = LoggerFactory.getLogger(BotWelcome.class);
    private static final Map<String, String> USER_LIST = new HashMap<>();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private BotWelcome() {
        throw new IllegalArgumentException("Class cannot be instantiated");
    }

    /**
     * Check to see if a user has already been welcomed by the bot.
     *
     * @param username
     * @return
     */
    public static synchronized boolean isOnList(String username) {
        return USER_LIST.containsKey(username);
    }

    /**
     * Check to see if a user has already been welcomed by the bot.
     *
     * @param user
     * @return
     */
    public static synchronized boolean isOnList(SlackUser user) {
        return isOnList(user.getUserName());
    }

    /**
     * Add a user to the list<p>
     * Will also save the file
     *
     * @param username
     */
    public static synchronized void addUser(String username) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        df.setTimeZone(tz);

        String dateString = df.format(new Date());
        LOG.info("Adding '{}' to the welcomed list on {}", username, dateString);
        USER_LIST.put(username, dateString);
        // Save the file
        writeFile();
    }

    /**
     * Create/send a welcome message to the user
     *
     * @param session
     * @param msgChannel
     * @param msgSender
     */
    public static void sendWelcomeMessage(SlackSession session, SlackChannel msgChannel, SlackUser msgSender) {
        // First check to see if the user has already had their welcome message
        if (BotWelcome.isOnList(msgSender)) {
            LOG.info("User '{}' is already on the welcomed list.", msgSender.getUserName());
            return;
        }

        SlackAttachment msg = new SlackAttachment();
        msg.addMarkdownIn("fields");
        msg.addMarkdownIn("text");
        msg.setColor("good");
        msg.setFallback("Welcome message for new users");
        msg.setTitle("Welcome to " + session.getTeam().getName());

        String bot = SlackBot.formatUsernameLink(session.sessionPersona());
        StringBuilder text = new StringBuilder();
        text.append("Thank you for joining our slack group!\n")
                .append("The idea of this group is to facilitate informal chats and discussions about boardgaming, ")
                .append("although not limited to that at all!\n\n");
        text.append("You can join any of the public channels on the left by clicking on `CHANNELS`\n\n");

        text.append("We also have a BoardGameGeek Bot (")
                .append(bot)
                .append(") that can get useful information from _BGG_ for you.\n");

        text.append("Just send the command `[[help]]` in a direct message to the bot or in the ")
                .append(SlackBot.formatChannelLink(session.findChannelByName("general")))
                .append(" channel and the bot will tell you what it can do!\n\n");
        text.append("We hope you enjoy your chats!\n");

        msg.setText(text.toString());

        msg.addField("Group Admins", getBotAdmins(), false);

        String name = StringUtils.isBlank(msgSender.getRealName()) ? msgSender.getUserName() : msgSender.getRealName();
        session.sendMessage(msgChannel, "Hello " + name);
        session.sendMessageToUser(msgSender, "", msg);

        // Send admins a message
        SlackBot.messageAdmins(session, String.format("Sent welcome message to %1$s",
                SlackBot.formatUsernameLink(msgSender)));

        // Add user to welcomed list
        BotWelcome.addUser(msgSender.getUserName());
    }

    /**
     * Generate the list of bot admins
     *
     * @return
     */
    private static String getBotAdmins() {
        StringBuilder admins = new StringBuilder();

        boolean first = true;
        for (SlackUser name : SlackBot.getBotAdmins()) {
            if (first) {
                first = false;
            } else {
                admins.append("\n");
            }
            admins.append(SlackBot.formatUsernameLink(name))
                    .append(" - ").append(name.getRealName());
        }

        return admins.toString();
    }

    public static void writeFile() {
        try {
            MAPPER.writeValue(new File(Constants.FILENAME_USER_LIST), USER_LIST);
        } catch (IOException ex) {
            LOG.warn("Failed to write user list to {}", Constants.FILENAME_USER_LIST, ex);
        }
    }

    public static void readFile() {
        File f = new File(Constants.FILENAME_USER_LIST);
        if (!f.exists()) {
            LOG.info("File '{}' was not found", Constants.FILENAME_USER_LIST);
            return;
        }

        try {
            TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {
            };
            Map<String, String> readObj = MAPPER.readValue(f, typeRef);

            USER_LIST.clear();
            USER_LIST.putAll(readObj);
            LOG.info("File '{}' was read successfully, {} users added.", Constants.FILENAME_USER_LIST, USER_LIST.size());
        } catch (IOException ex) {
            LOG.warn("Failed to read user list from {}", Constants.FILENAME_USER_LIST, ex);
        }
    }

    /**
     * List all the users that have been welcomed by the bot
     *
     * @param session
     * @param msgChannel
     */
    public static void listUsers(SlackSession session, SlackChannel msgChannel) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : USER_LIST.entrySet()) {
            sb.append(entry.getKey()).append(" on ").append(entry.getValue()).append("\n");
        }

        SlackAttachment sa;

        if (sb.length() > 0) {
            sa = new SlackAttachment();
            sa.setFallback("Users welcomed by the bot");
            sa.setText(sb.toString());
            sa.setPretext("Users welcomed by the bot");
            sa.setColor("good");

            session.sendMessage(msgChannel, "", sa);
        } else {
            session.sendMessage(msgChannel, "No users have been welcomed by the bot!");
        }

        sb = new StringBuilder();
        for (SlackUser user : session.getUsers()) {
            if (isOnList(user) || user.isBot()) {
                continue;
            }
            sb.append(user.getUserName()).append("\n");
        }

        if (sb.length() > 0) {
            sa = new SlackAttachment();
            sa.setFallback("Users not welcomed by the bot");
            sa.setText(sb.toString());
            sa.setPretext("Users not welcomed by the bot");
            sa.setColor("bad");

            session.sendMessage(msgChannel, "", sa);
        } else {
            session.sendMessage(msgChannel, "All users have been welcomed :thumbsup:");
        }

    }

}
