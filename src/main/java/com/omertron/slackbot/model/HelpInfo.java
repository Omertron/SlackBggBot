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

import org.apache.commons.lang3.StringUtils;

/**
 * Holds the help information about a bot command
 *
 */
public class HelpInfo {

    private String command;
    private String param;
    private String message;
    private boolean admin;
    private static final String FORMAT_PARAM = "%1$s <%2$s>";
    private static final String FORMAT_SINGLE = "%1$s";

    public HelpInfo() {
    }

    public HelpInfo(String command, String param, String message, boolean admin) {
        this.command = command;
        this.param = param;
        this.message = message;
        this.admin = admin;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    /**
     * Get a formatted string for the command and optionally parameter
     *
     * @return
     */
    public String getFormattedCommand() {
        if (StringUtils.isBlank(param)) {
            return String.format(FORMAT_SINGLE, command);
        } else {
            return String.format(FORMAT_PARAM, command, param);
        }
    }
}
