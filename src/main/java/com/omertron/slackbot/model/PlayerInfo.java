package com.omertron.slackbot.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class PlayerInfo {

    private String initial;
    private String name;
    private String bggUsername;
    private String slackUsername;

    public PlayerInfo() {
        this("", "");
    }

    public PlayerInfo(String initial, String name) {
        this.initial = initial;
        this.name = name;
        this.bggUsername = "";
        this.slackUsername = "";
    }

    public String getInitial() {
        return initial;
    }

    public void setInitial(String initial) {
        this.initial = initial;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBggUsername() {
        return bggUsername;
    }

    public void setBggUsername(String bggUsername) {
        this.bggUsername = bggUsername;
    }

    public String getSlackUsername() {
        return slackUsername;
    }

    public void setSlackUsername(String slackUsername) {
        this.slackUsername = slackUsername;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }

}
