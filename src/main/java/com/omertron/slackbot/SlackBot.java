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

import com.omertron.slackbot.enumeration.ExitCode;
import com.omertron.slackbot.functions.BotStatistics;
import com.omertron.slackbot.functions.BotWelcome;
import com.omertron.slackbot.functions.scheduler.BotTaskExecutor;
import com.omertron.slackbot.listeners.BoardGameListener;
import com.omertron.slackbot.listeners.GoogleSheetsListener;
import com.omertron.slackbot.listeners.HelpListener;
import com.omertron.slackbot.utils.PropertiesUtil;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPersona;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlackBot {

    private static final Logger LOG = LoggerFactory.getLogger(SlackBot.class);
    private static final String DEFAULT_PROPERTIES_FILE = "application.properties";
    private static final List<SlackUser> BOT_ADMINS = new ArrayList<>();
    private static BotTaskExecutor executor;
    private static final List<SlackMessagePostedListener> LISTENER_MP = new ArrayList<>();

    private SlackBot() {
        // No need for a constructor in the main class
    }

    public static void main(String[] args) throws Exception {
        LOG.info("Starting {} v{} ...", Constants.BOT_NAME, Constants.BOT_VERSION);

        // Load the properties
        PropertiesUtil.setPropertiesStreamName(DEFAULT_PROPERTIES_FILE);

        LOG.info("Starting session...");
        SlackSession session;

        String proxyURL = PropertiesUtil.getProperty(Constants.PROXY_HOST);
        if (StringUtils.isNotBlank(proxyURL)) {
            int proxyPort = Integer.parseInt(PropertiesUtil.getProperty(Constants.PROXY_PORT, "80"));
            session = SlackSessionFactory.getSlackSessionBuilder(Constants.BOT_TOKEN).withProxy(Proxy.Type.HTTP, proxyURL, proxyPort).build();
        } else {
            session = SlackSessionFactory.createWebSocketSlackSession(PropertiesUtil.getProperty(Constants.BOT_TOKEN));
        }

        session.connect();

        // Populate the BOT admins
        populateBotAdmins(session);

        // Notify BOT admins
        notifyStartup(session);

        // Add the listeners to the session
        addListeners(session);

        LOG.info("Session connected: {}", session.isConnected());
        LOG.info("\tConnected to {} ({})", session.getTeam().getName(), session.getTeam().getId());
        LOG.info("\tFound {} channels and {} users", session.getChannels().size(), session.getUsers().size());

        outputBotAdminsMessage();

        LOG.info("Starting the Task Executor");
        executor = new BotTaskExecutor(session);

        LOG.info("Checking for users welcomed list");
        BotWelcome.readFile();

        LOG.info("Checking for stats file");
        BotStatistics.readFile();
        LOG.info("Stats read:\n{}", BotStatistics.generateStatistics(false, true));

        Thread.sleep(Long.MAX_VALUE);
    }

    /**
     * Add the listeners to the session.
     *
     * @param session SlackSession
     */
    private static synchronized void addListeners(SlackSession session) {
        // Only add the listeners once
        if (LISTENER_MP.isEmpty()) {
            // Add board game listner
            LISTENER_MP.add(new BoardGameListener());
            // Add Wirral Biscuits and Boardgames 
            LISTENER_MP.add(new GoogleSheetsListener());
            // Add Game Night listener
//            LISTENER_MP.add(new GameNightListener());
            // Add help listener
            LISTENER_MP.add(new HelpListener());

            for (SlackMessagePostedListener l : LISTENER_MP) {
                session.addMessagePostedListener(l);
            }
        }
    }

    /**
     * Output log messages about the bot admins
     */
    private static void outputBotAdminsMessage() {
        switch (BOT_ADMINS.size()) {
            case 0:
                LOG.warn("\tThere are no BOT Admins found! Please add at least 1 in the properties file!");
                LOG.warn("\tUser the property '{}' to add them", Constants.BOT_ADMINS);
                break;
            case 1:
                LOG.info("\tThere is 1 BOT admin: {}", StringUtils.join(BOT_ADMINS, ","));
                break;
            default:
                List<String> names = new ArrayList<>();
                BOT_ADMINS.forEach(su -> names.add(su.getUserName()));
                LOG.info("\tThere are {} BOT admins: {}", BOT_ADMINS.size(), StringUtils.join(names, ","));
        }
    }

    /**
     * Shut down the bot with the given exit code.
     *
     * @param exitCode
     */
    public static void shutdown(ExitCode exitCode) {
        if (executor != null) {
            executor.stopAll();
        }
        System.exit(exitCode.getValue());
    }

    /**
     * Send a start up message to all BOT admins to inform them of the bot's
     * restart
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
        String users = PropertiesUtil.getProperty(Constants.BOT_ADMINS, "");

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
     * Create a Slack formatted URL link
     *
     * @param url URL
     * @param text Text to display
     * @return
     */
    public static String formatLink(String url, String text) {
        return formatLink(null, url, text);
    }

    /**
     * Format a link
     *
     * @param preMarker
     * @param url
     * @param text
     * @return
     */
    private static String formatLink(String preMarker, String url, String text) {
        StringBuilder formatted = new StringBuilder("<");
        if (preMarker != null) {
            formatted.append(preMarker);
        }
        formatted.append(url)
                .append("|")
                .append(text)
                .append(">");
        return formatted.toString();
    }
}