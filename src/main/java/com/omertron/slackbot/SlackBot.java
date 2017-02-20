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
import com.omertron.slackbot.utils.PropertyUtils;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import java.net.Proxy;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlackBot {

    private static final Logger LOG = LoggerFactory.getLogger(SlackBot.class);
    private static final Properties PROPS = new Properties();
    private static final String DEFAULT_PROPERTIES_FILE = "application.properties";

    public static void main(String[] args) throws Exception {
        LOG.info("Starting {} v{} ...", Constants.BOT_NAME, Constants.BOT_VERSION);
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
        // Add board game listner
        session.addMessagePostedListener(new BoardGameListener());
        // Add help listener
        session.addMessagePostedListener(new HelpListener());

        LOG.info("Session connected: {}", session.isConnected());
        LOG.info("  Connected to {} ({})", session.getTeam().getName(), session.getTeam().getId());
        LOG.info("  Found {} channels and {} users", session.getChannels().size(), session.getUsers().size());

        Thread.sleep(Long.MAX_VALUE);
    }

}
