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
