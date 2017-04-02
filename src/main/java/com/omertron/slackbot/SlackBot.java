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
package com.omertron.slackbot;

import com.omertron.slackbot.listeners.BoardGameListener;
import com.omertron.slackbot.listeners.GoogleSheetsListener;
import com.omertron.slackbot.listeners.HelpListener;
import com.omertron.slackbot.functions.BotStatistics;
import com.omertron.slackbot.functions.BotWelcome;
import com.omertron.slackbot.utils.PropertyUtils;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPersona;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlackBot {

    private static final Logger LOG = LoggerFactory.getLogger(SlackBot.class);
    private static final Properties PROPS = new Properties();
    private static final String DEFAULT_PROPERTIES_FILE = "application.properties";
    private static final List<SlackUser> BOT_ADMINS = new ArrayList<>();

    private SlackBot() {
        // No need for a constructor in the main class
    }

    public static void main(String[] args) throws Exception {
        LOG.info("Starting {} v{} ...", Constants.BOT_NAME, Constants.BOT_VERSION);

        // Load the properties
        PropertyUtils.initProperties(PROPS, DEFAULT_PROPERTIES_FILE);

        LOG.info("Starting session...");
        SlackSession session;

        String proxyURL = PROPS.getProperty(Constants.PROXY_HOST);
        if (StringUtils.isNotBlank(proxyURL)) {
            int proxyPort = Integer.parseInt(PROPS.getProperty(Constants.PROXY_PORT, "80"));
            session = SlackSessionFactory.createWebSocketSlackSession(PROPS.getProperty(Constants.BOT_TOKEN), Proxy.Type.HTTP, proxyURL, proxyPort);
        } else {
            session = SlackSessionFactory.createWebSocketSlackSession(PROPS.getProperty(Constants.BOT_TOKEN));
        }

        session.connect();
        // Populate the BOT admins
        populateBotAdmins(session);
        // Notify BOT admins
        notifyStartup(session);
        // Add board game listner
        session.addMessagePostedListener(new BoardGameListener());
        // Add Wirral Biscuits and Boardgames 
        session.addMessagePostedListener(new GoogleSheetsListener());
        // Add help listener
        session.addMessagePostedListener(new HelpListener());

        LOG.info("Session connected: {}", session.isConnected());
        LOG.info("\tConnected to {} ({})", session.getTeam().getName(), session.getTeam().getId());
        LOG.info("\tFound {} channels and {} users", session.getChannels().size(), session.getUsers().size());
        switch (BOT_ADMINS.size()) {
            case 0:
                LOG.warn("\tThere are no BOT Admins found! Please add at least 1 in the properties file!");
                LOG.warn("\tUser the property '{}' to add them", Constants.BOT_ADMINS);
                break;
            case 1:
                LOG.info("\tThere is 1 BOT admin: {}", StringUtils.join(BOT_ADMINS, ","));
                break;
            default:
                LOG.info("\tThere are {} BOT admins: {}", BOT_ADMINS.size(), StringUtils.join(BOT_ADMINS, ","));
        }

        LOG.info("Checking for users welcomed list");
        BotWelcome.readFile();

        LOG.info("Checking for stats file");
        BotStatistics.readFile();
        LOG.info("Stats read:\n{}", BotStatistics.generateStatistics(false, true));

        Thread.sleep(Long.MAX_VALUE);
    }

    /**
     * Shut down the bot with the given exit code.
     *
     * @param exitCode
     */
    public static void shutdown(int exitCode) {
        System.exit(exitCode);
    }

    /**
     * Send a start up message to all BOT admins to inform them of the bot's restart
     *
     * @param session
     */
    private static void notifyStartup(SlackSession session) {
        String message = String.format("%1$s started at %2$s",
                Constants.BOT_NAME,
                DateFormatUtils.format(new Date(), "dd-MM-yyyy HH:mm:ss"));
        messageAdmins(session, message);
    }

    /**
     * Send a message to all admins
     *
     * @param session
     * @param message
     */
    public static void messageAdmins(SlackSession session, String message) {
        LOG.info("Sending message to admins: '{}'", message);
        for (SlackUser user : BOT_ADMINS) {
            LOG.info("\tSent to '{}'", user.getUserName());
            session.sendMessageToUser(user, message, null);
        }
    }

    /**
     * Populate the list of BOT Administrators from the property file.
     *
     * @param session
     */
    private static void populateBotAdmins(SlackSession session) {
        String users = PROPS.getProperty(Constants.BOT_ADMINS, "");

        if (StringUtils.isNotBlank(users)) {
            SlackUser sUser;
            for (String user : StringUtils.split(users, ",")) {
                sUser = session.findUserByUserName(StringUtils.trimToEmpty(user));
                if (sUser != null) {
                    LOG.info("Adding {} ({}) to {} admins", sUser.getUserName(), sUser.getRealName(), Constants.BOT_NAME);
                    // Add the uername to the list
                    BOT_ADMINS.add(sUser);
                } else {
                    LOG.warn("Username '{}' was not found in the list of slack users!", user);
                }
            }
        }
    }

    /**
     * List of the bot admins
     *
     * @return admin names
     */
    public static List<SlackUser> getBotAdmins() {
        return BOT_ADMINS;
    }

    /**
     * Determine if the user is one of the BOT administrators
     *
     * @param user
     * @return
     */
    public static boolean isBotAdmin(SlackUser user) {
        return BOT_ADMINS.contains(user);
    }

    /**
     * Create a formatted username string
     *
     * @param persona
     * @return
     */
    public static String formatUsernameLink(SlackPersona persona) {
        return formatLink("@", persona.getId(), persona.getUserName());
    }

    /**
     * Create a formatted channel string
     *
     * @param channel
     * @return
     */
    public static String formatChannelLink(SlackChannel channel) {
        return formatLink("#", channel.getId(), channel.getName());
    }

    /**
     * Format a link
     *
     * @param marker
     * @param id
     * @param text
     * @return
     */
    private static String formatLink(String marker, String id, String text) {
        StringBuilder formatted = new StringBuilder();

        formatted.append("<").append(marker)
                .append(id)
                .append("|")
                .append(text)
                .append(">");
        return formatted.toString();
    }

    /**
     * Get a property from the list
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static String getProperty(String key, String defaultValue) {
        return PROPS.getProperty(key, defaultValue);
    }

}
