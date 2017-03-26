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

public final class Constants {

    // Bot information
    public static final String BOT_NAME = "BGG Slack Bot";
    public static final String BOT_VERSION = "1.2";
    // Slack Auth Token property name for property file
    public static final String BOT_TOKEN = "botToken";

    // Filenames
    public static final String FILENAME_STAT = "bggbot_stats.json";
    public static final String FILENAME_USER_LIST = "bggbot_welcomed.json";

    // Bot config properties
    public static final String BOT_ADMINS = "botAdmins";
    public static final String DELIM_LEFT = "[";
    public static final String DELIM_RIGHT = "]";
    public static final String ATTACH_COLOUR = "good";

    // Proxy property names for property file
    public static final String PROXY_HOST = "proxyHost";
    public static final String PROXY_PORT = "proxyPort";

    // BGG Stuff
    public static final String BGG_GAME_LINK = "https://boardgamegeek.com/boardgame/";
    public static final String BGG_USER_LINK = "https://boardgamegeek.com/user/";
    public static final String BGG_COLL_LINK = "https://boardgamegeek.com/collection/user/";

    // Meetup stuff
    public static final String MEETUP_URL = "meetupUrl";

    private Constants() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }
}
