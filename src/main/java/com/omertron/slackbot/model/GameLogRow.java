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

import com.google.api.services.sheets.v4.model.ValueRange;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameLogRow {

    private static final Logger LOG = LoggerFactory.getLogger(GameLogRow.class);
    private Date date;
    private String gameName;
    private int gameId;
    private String chooser;
    private String attendees;
    private String winners;
    private String owner;

    public GameLogRow() {
    }

    public GameLogRow(ValueRange valueRange) {
        processValueRange(valueRange);
    }

    /**
     * Take the row from the sheet API and convert it to this object
     *
     * @param valueRange
     */
    public final void processValueRange(ValueRange valueRange) {
        if (valueRange.getValues() != null && !valueRange.getValues().isEmpty()) {
            List<Object> row = valueRange.getValues().get(0);

            if (row.size() >= 1) {
                try {
                    this.date = DateUtils.parseDate(row.get(0).toString().substring(5), "dd MMM yy");
                } catch (ParseException ex) {
                    LOG.info("Failed to parse date: '{}'", ex.getMessage());
                }
            }
            if (row.size() >= 2) {
                this.gameName = row.get(1).toString();
            }
            if (row.size() >= 3) {
                this.gameId = NumberUtils.toInt(row.get(2).toString(), 0);
            }
            if (row.size() >= 5) {
                this.chooser = row.get(4).toString();
            }
            if (row.size() >= 6) {
                this.attendees = row.get(5).toString();
            }
            if (row.size() >= 7) {
                this.winners = row.get(6).toString();
            }
            if (row.size() >= 9) {
                this.owner = row.get(8).toString();
            }

            LOG.info("{}", ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE));
        }
    }

    public Date getDate() {
        return date;
    }
    
    public String getFormattedDate() {
        return getFormattedDate("dd MMM yy");
    }
    
    public String getFormattedDate(String format) {
        return DateFormatUtils.format(date, format);
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public String getChooser() {
        return chooser;
    }

    public void setChooser(String chooser) {
        this.chooser = chooser;
    }

    public String getAttendees() {
        return attendees;
    }

    public void setAttendees(String attendees) {
        this.attendees = attendees;
    }

    public String getWinners() {
        return winners;
    }

    public void setWinners(String winners) {
        this.winners = winners;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

}
