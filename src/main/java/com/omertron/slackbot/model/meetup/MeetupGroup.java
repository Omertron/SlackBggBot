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
package com.omertron.slackbot.model.meetup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.omertron.slackbot.model.AbstractJsonMapping;
import java.util.Date;

public class MeetupGroup extends AbstractJsonMapping {

    @JsonProperty("created")
    private Date created;
    @JsonProperty("name")
    private String name;
    @JsonProperty("id")
    private long id;
    @JsonProperty("join_mode")
    private String joinMode;
    @JsonProperty("lat")
    private float latitude;
    @JsonProperty("lon")
    private float longitude;
    @JsonProperty("urlname")
    private String urlname;
    @JsonProperty("who")
    private String who;

    public Date getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = new Date(created);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getJoinMode() {
        return joinMode;
    }

    public void setJoinMode(String joinMode) {
        this.joinMode = joinMode;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public String getUrlname() {
        return urlname;
    }

    public void setUrlname(String urlname) {
        this.urlname = urlname;
    }

    public String getWho() {
        return who;
    }

    public void setWho(String who) {
        this.who = who;
    }

}
