package com.omertron.slackbot;

import com.omertron.slackbot.listeners.BoardGameListener;
import com.omertron.slackbot.listeners.HelpListener;
import com.omertron.slackbot.utils.PropertyUtils;
import java.net.Proxy;
import java.util.Properties;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
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
