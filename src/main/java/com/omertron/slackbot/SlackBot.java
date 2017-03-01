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
import com.omertron.slackbot.listeners.HelpListener;
import com.omertron.slackbot.stats.BotStatistics;
import com.omertron.slackbot.utils.PropertyUtils;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import java.io.File;
import java.net.Proxy;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlackBot {

    private static final Logger LOG = LoggerFactory.getLogger(SlackBot.class);
    private static final Properties PROPS = new Properties();
    private static final String DEFAULT_PROPERTIES_FILE = "application.properties";
    private static final List<String> BOT_ADMINS = new ArrayList<>();

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

        LOG.info("Checking for stats file");
        File f = new File(Constants.STAT_FILENAME);
        if (f.exists()) {
            LOG.info("\tReading stats file");
            BotStatistics.readFile();
        }
        LOG.info("Stats read:\n{}", BotStatistics.generateStatistics(false, true));

        Thread.sleep(Long.MAX_VALUE);
    }

    /**
     * Send a start up message to all BOT admins to inform them of the bot's
     * restart
     *
     * @param session
     */
    private static void notifyStartup(SlackSession session) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        df.setTimeZone(tz);

        String message = Constants.BOT_NAME + " started at " + df.format(new Date());
        LOG.info("  {}", message);

        for (String username : BOT_ADMINS) {
            SlackUser user = session.findUserByUserName(username);
            if (user != null) {
                session.sendMessageToUser(user, message, null);
            } else {
                LOG.warn("  Admin user '{}' was not found.", username);
            }
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
                    BOT_ADMINS.add(sUser.getUserName());
                } else {
                    LOG.warn("Username '{}' was not found in the list of slack users!", user);
                }
            }
        }

    }

    /**
     * Determine if the username is one of the BOT administrators
     *
     * @param username
     * @return
     */
    public static boolean isBotAdmin(String username) {
        return BOT_ADMINS.contains(username);
    }

    /**
     * Determine if the user is one of the BOT administrators
     *
     * @param user
     * @return
     */
    public static boolean isBotAdmin(SlackUser user) {
        return BOT_ADMINS.contains(user.getUserName());
    }
}
