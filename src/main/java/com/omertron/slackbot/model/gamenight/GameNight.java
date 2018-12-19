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
package com.omertron.slackbot.model.gamenight;

import com.omertron.slackbot.Constants;
import com.omertron.slackbot.utils.PropertiesUtil;
import com.ullink.slack.simpleslackapi.SlackUser;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to hold the game night information
 *
 * @author stuart
 */
public class GameNight {

    private static final Logger LOG = LoggerFactory.getLogger(GameNight.class);
    private static final int DEFAULT_TIME;

    private SlackUser host;
    private LocalDate gameDate;
    private int gameTime;
    private final Map<Integer, String> games = new HashMap<>();
    private final List<SlackUser> players = new ArrayList<>();
    private String comments = "";
    private String cachePlayerNameList = null;

    static {
        DEFAULT_TIME = Integer.parseInt(PropertiesUtil.getProperty(Constants.DEFAULT_GAME_TIME, "1900"));
    }

    public GameNight(SlackUser host, LocalDate gameDate) {
        this(host, gameDate, DEFAULT_TIME);
    }

    public GameNight(SlackUser host, LocalDate gameDate, int gameTime) {
        this.host = host;
        this.gameDate = gameDate;
        this.gameTime = gameTime;
    }

    public SlackUser getHost() {
        return host;
    }

    public void setHost(SlackUser host) {
        this.host = host;
    }

    public LocalDate getGameDate() {
        return gameDate;
    }

    public void setGameDate(LocalDate gameDate) {
        this.gameDate = gameDate;
    }

    public void setGameDate(String gameDate) {
        this.gameDate = LocalDate.parse(gameDate);
    }

    public int getGameTime() {
        return gameTime;
    }

    public void setGameTime(int gameTime) {
        // check for time with just hours and add "00" to the end
        if (gameTime >= 1 && gameTime <= 24) {
            this.gameTime = gameTime * 100;
            return;
        }

        this.gameTime = gameTime;
    }

    public void addGame(int id) {
        games.put(id, "");
    }

    public void removeGame(int id) {
        String removedGame = games.remove(id);
        LOG.info("Removed '{}' from the games list", removedGame);
    }

    public Map<Integer, String> getGames() {
        return games;
    }

    public String listGames() {
        if (games.isEmpty()) {
            return "No games listed";
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Integer, String> entry : games.entrySet()) {
                sb.append(entry.getValue()).append(" (").append(entry.getKey()).append(")\n");
            }
            return sb.toString();
        }
    }

    public void addPlayer(SlackUser player) {
        players.add(player);
    }

    public void removePlayer(SlackUser player) {
        players.remove(player);
    }

    public List<SlackUser> getPlayers() {
        return players;
    }

    public String listPlayers() {
        if (players.isEmpty()) {
            return "No players are atteding";
        } else {
            StringBuilder sb = new StringBuilder();
            for (SlackUser player : players) {
                sb.append(player.getRealName())
                        .append(" (").append(player.getUserName()).append(")\n");
            }
            return sb.toString();
        }
    }

    /**
     * Get a delimited list of the players names
     *
     * @param delim Delimiter to use (default to "," if null
     * @param realname include the users real name in the list
     * @param username include the users username in the list
     * @return Player name list
     */
    public String getNameList(String delim, boolean realname, boolean username) {
        if (cachePlayerNameList == null) {
            List<String> list = new ArrayList<>();
            for (SlackUser player : players) {
                list.add(getFormattedName(player, realname, username));
            }
            Collections.sort(list);
            cachePlayerNameList = StringUtils.join(list, StringUtils.isBlank(delim) ? "," : delim);
        }

        return cachePlayerNameList;
    }

    /**
     * Get a comma delimited list of the players names
     *
     * @param realname include the users real name in the list
     * @param username include the users username in the list
     * @return Player name list
     */
    public String getNameList(boolean realname, boolean username) {
        return getNameList(",", realname, username);
    }

    /**
     * Get a comma delimited list of the players names
     *
     * @return Player name list
     */
    public String getNameList() {
        return getNameList(",", true, true);
    }

    /**
     * Format the user's name
     *
     * @param user the user to format the name from
     * @param realname include the real name of the user
     * @param username include the username of the user
     * @return a formatted name
     */
    public String getFormattedName(SlackUser user, boolean realname, boolean username) {
        StringBuilder sb = new StringBuilder();

        if (realname) {
            sb.append(user.getRealName());
        }
        if (realname && username) {
            sb.append(" (");
        }
        if (username) {
            sb.append(user.getUserName());
        }
        if (realname && username) {
            sb.append(")");
        }

        return sb.toString();
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getFormattedGameNameList() {
        StringBuilder sb = new StringBuilder();
        if (games.isEmpty()) {
            sb.append("No games");
        } else {
            for (String name : games.values()) {
                sb.append(name).append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());
        }
        return sb.toString();
    }
}
