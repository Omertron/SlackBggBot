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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Holds the help information about a bot command
 *
 */
public class HelpInfo {

    private String command;
    private List<String> params = new LinkedList<>();
    private String message;
    private boolean admin;

    /**
     * Create a HelpInfo object with a single parameter
     *
     * @param command
     * @param param
     * @param message
     * @param admin
     */
    public HelpInfo(String command, String param, String message, boolean admin) {
        this.command = command;
        if (StringUtils.isNotEmpty(param)) {
            this.params.add(param);
        }
        this.message = message;
        this.admin = admin;
    }

    /**
     * Create a HelpInfo object with an array of parameters
     *
     * @param command
     * @param params
     * @param message
     * @param admin
     */
    public HelpInfo(String command, String[] params, String message, boolean admin) {
        this.command = command;
        this.params.addAll(Arrays.asList(params));
        this.message = message;
        this.admin = admin;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List< String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }

    public void addParam(String param) {
        this.params.add(param);
    }

    public void addParam(String[] params) {
        this.params.addAll(Arrays.asList(params));
    }

    public void addParam(List< String> params) {
        this.params.addAll(params);
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
        if (params.isEmpty()) {
            return command;
        } else {
            StringBuilder format = new StringBuilder(command);
            for (String p : params) {
                format.append(" <").append(p).append(">");
            }
            return format.toString();
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
