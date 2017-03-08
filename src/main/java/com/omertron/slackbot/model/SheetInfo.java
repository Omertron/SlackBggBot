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
package com.omertron.slackbot.model;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.math.NumberUtils;

public class SheetInfo {

    private int lastRow;
    private int nextGameId;
    private String gameImageUrl;
    private String defaultImageUrl;
    private String pinHolder;
    private String gameChooser;
    private String gameName;
    private String gameDate;
    private String nextChooser;
    private List<PlayerInfo> players = new ArrayList<>();

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
            gameDate = value;
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

    public String getGameDate() {
        return gameDate;
    }

    public void setGameDate(String gameDate) {
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
    }

    public void addPlayer(PlayerInfo player) {
        this.players.add(player);
    }

    public String getInitialList() {
        List<String> list = new ArrayList<>();
        for (PlayerInfo player : players) {
            list.add(player.getInitial());
        }
        return StringUtils.join(list, ",");
    }

    public String getNameList() {
        List<String> list = new ArrayList<>();
        for (PlayerInfo player : players) {
            list.add(player.getName());
        }
        return StringUtils.join(list, ",");
    }

}
