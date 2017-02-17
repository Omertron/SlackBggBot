package com.omertron.slackbot;

public interface Constants {

    // Bot information
    String BOT_NAME = "BGG Slack Bot";
    String BOT_VERSION = "1.0";

    // Slack Auth Token property name for property file
    String BOT_TOKEN = "botToken";

    // Proxy property names for property file
    String PROXY_HOST = "proxyHost";
    String PROXY_PORT = "proxyPort";

    // BGG Stuff
    String BGG_GAME_LINK = "https://boardgamegeek.com/boardgame/";
    String DELIM_LEFT = "[";
    String DELIM_RIGHT = "]";

}
