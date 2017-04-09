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
package com.omertron.slackbot.model.sheets;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SheetInfo {

    private static final Logger LOG = LoggerFactory.getLogger(SheetInfo.class);
    private static final String SHEET_FORMAT_STRING = "EEE, d MMM yy";
    private static final String DEFAULT_DATE_FORMAT = "d MMM yy";
    private static final DateTimeFormatter SHEET_DATE_FORMAT = DateTimeFormatter.ofPattern(SHEET_FORMAT_STRING);

    private int lastRow;
    private int nextGameId;
    private String gameImageUrl;
    private String defaultImageUrl;
    private String pinHolder;
    private String gameChooser;
    private String gameName;
    private LocalDate gameDate;
    private String nextChooser;
    private List<PlayerInfo> players = new ArrayList<>();
    // Cached lists
    private String cachePlayerInitList = null;
    private String cachePlayerNameList = null;

    public boolean addItem(final String key, final String value) {
        if (key.startsWith("LAST")) {
            lastRow = NumberUtils.toInt(value, 0);
            return true;
        }

        if (key.startsWith("NEXT GAME ID")) {
            nextGameId = NumberUtils.toInt(value, 0);
            return true;
        }

        if (key.startsWith("IMAGE")) {
            gameImageUrl = value;
            return true;
        }

        if (key.startsWith("DEFAULT IMAGE")) {
            defaultImageUrl = value;
            return true;
        }

        if (key.startsWith("CURRENT PIN")) {
            pinHolder = value;
            return true;
        }
        if (key.startsWith("CHOSEN BY")) {
            gameChooser = value;
            return true;
        }
        if (key.startsWith("NEXT GAME NAME")) {
            gameName = value;
            return true;
        }
        if (key.startsWith("NEXT DATE")) {
            try {
                gameDate = LocalDate.parse(value, SHEET_DATE_FORMAT);
            } catch (DateTimeParseException ex) {
                LOG.info("Failed to parse date: '{}'", ex.getMessage(), ex);
            }
            return true;
        }
        if (key.startsWith("NEXT CHOOSER")) {
            nextChooser = value;
            return true;
        }
        return false;
    }

    public int getLastRow() {
        return lastRow;
    }

    public void setLastRow(int lastRow) {
        this.lastRow = lastRow;
    }

    public int getNextGameId() {
        return nextGameId;
    }

    public void setNextGameId(int nextGameId) {
        this.nextGameId = nextGameId;
    }

    public String getGameImageUrl() {
        return gameImageUrl;
    }

    public void setGameImageUrl(String gameImageUrl) {
        this.gameImageUrl = gameImageUrl;
    }

    public String getDefaultImageUrl() {
        return defaultImageUrl;
    }

    public void setDefaultImageUrl(String defaultImageUrl) {
        this.defaultImageUrl = defaultImageUrl;
    }

    public String getPinHolder() {
        return pinHolder;
    }

    public void setPinHolder(String pinHolder) {
        this.pinHolder = pinHolder;
    }

    public String getGameChooser() {
        return gameChooser;
    }

    public void setGameChooser(String gameChooser) {
        this.gameChooser = gameChooser;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public LocalDate getGameDate() {
        return gameDate;
    }

    public String getFormattedDate() {
        return getFormattedDate(DEFAULT_DATE_FORMAT);
    }

    public String getFormattedDate(String format) {
        return gameDate.format(DateTimeFormatter.ofPattern(format));
    }

    public void setGameDate(LocalDate gameDate) {
        this.gameDate = gameDate;
    }

    public String getNextChooser() {
        return nextChooser;
    }

    public void setNextChooser(String nextChooser) {
        this.nextChooser = nextChooser;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerInfo> players) {
        this.players = players;
        this.cachePlayerInitList = null;
        this.cachePlayerNameList = null;
    }

    public void addPlayer(PlayerInfo player) {
        this.players.add(player);
        this.cachePlayerInitList = null;
        this.cachePlayerNameList = null;
    }

    public void removePlayer(PlayerInfo player) {
        this.players.remove(player);
        this.cachePlayerInitList = null;
        this.cachePlayerNameList = null;
    }

    /**
     * Get a delimited list of the player's initials
     *
     * @param delim Delimiter to use (default to "," if null
     * @return player initials list
     */
    public String getInitialList(String delim) {
        if (cachePlayerInitList == null) {
            List<String> list = new ArrayList<>();
            for (PlayerInfo player : players) {
                list.add(player.getInitial());
            }
            Collections.sort(list);
            cachePlayerInitList = StringUtils.join(list, StringUtils.isBlank(delim) ? "," : delim);
        }

        return cachePlayerInitList;
    }

    public String getInitialList() {
        return getInitialList(",");
    }

    /**
     * Get a delimited list of the players names
     *
     * @param delim Delimiter to use (default to "," if null
     * @return Player name list
     */
    public String getNameList(String delim) {
        if (cachePlayerNameList == null) {
            List<String> list = new ArrayList<>();
            for (PlayerInfo player : players) {
                list.add(player.getName());
            }
            Collections.sort(list);
            cachePlayerNameList = StringUtils.join(list, StringUtils.isBlank(delim) ? "," : delim);
        }

        return cachePlayerNameList;
    }

    public String getNameList() {
        return getNameList(",");
    }

}
