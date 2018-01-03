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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MeetupDetails extends AbstractJsonMapping {

    @JsonProperty("created")
    private Date created;
    @JsonProperty("duration")
    private long duration;
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("rsvp_limit")
    private int rsvpLimit;
    @JsonProperty("status")
    private String status;
    @JsonProperty("time")
    private LocalDateTime meetupTime;
    @JsonProperty("updated")
    private Date updated;
    @JsonProperty("utc_offset")
    private long utcOffset;
    @JsonProperty("waitlist_count")
    private int waitlistCount;
    @JsonProperty("yes_rsvp_count")
    private int yesRsvpCount;
    @JsonProperty("group")
    private MeetupGroup group;
    @JsonProperty("link")
    private String link;
    @JsonProperty("description")
    private String description;
    @JsonProperty("visibility")
    private String visibility;
    @JsonProperty("how_to_find_us")
    private String howToFindUs;
    @JsonProperty("venue")
    private MeetupVenue venue;
    @JsonProperty("fee")
    private MeetupFee fee;

    public Date getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = new Date(created);
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = TimeUnit.MILLISECONDS.toHours(duration);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRsvpLimit() {
        return rsvpLimit;
    }

    public void setRsvpLimit(int rsvpLimit) {
        this.rsvpLimit = rsvpLimit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getMeetupTime() {
        return meetupTime;
    }

    public void setTime(long time) {
        this.meetupTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = new Date(updated);
    }

    public long getUtcOffset() {
        return utcOffset;
    }

    public void setUtcOffset(long utcOffset) {
        this.utcOffset = TimeUnit.MILLISECONDS.toHours(utcOffset);
    }

    public int getWaitlistCount() {
        return waitlistCount;
    }

    public void setWaitlistCount(int waitlistCount) {
        this.waitlistCount = waitlistCount;
    }

    public int getYesRsvpCount() {
        return yesRsvpCount;
    }

    public void setYesRsvpCount(int yesRsvpCount) {
        this.yesRsvpCount = yesRsvpCount;
    }

    public MeetupGroup getGroup() {
        return group;
    }

    public void setGroup(MeetupGroup group) {
        this.group = group;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getHowToFindUs() {
        return howToFindUs;
    }

    public void setHowToFindUs(String howToFindUs) {
        this.howToFindUs = howToFindUs;
    }

    public MeetupVenue getVenue() {
        return venue;
    }

    public void setVenue(MeetupVenue venue) {
        this.venue = venue;
    }

    public MeetupFee getFee() {
        return fee;
    }

    public void setFee(MeetupFee fee) {
        this.fee = fee;
    }

}
