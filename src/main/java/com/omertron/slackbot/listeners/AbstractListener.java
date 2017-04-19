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
package com.omertron.slackbot.listeners;

import com.omertron.bgg.BggApi;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import org.apache.commons.lang3.StringUtils;

/**
 * Abstract class to hold some useful functions for all listeners
 *
 * @author Omertron
 */
public abstract class AbstractListener implements SlackMessagePostedListener {

    protected static final BggApi BGG = new BggApi();
    protected static final String E_GREY_EXCLAMATION = "grey_exclamation";
    protected static final String E_GAME_DIE = "game_die";

    /**
     * Add a reaction to the message that called us and send the "typing..."
     * indicator
     *
     * @param session
     * @param event
     * @param reactEmoji
     */
    protected void botUpdateChannel(SlackSession session, SlackMessagePosted event, String reactEmoji) {
        if (StringUtils.isNotBlank(reactEmoji)) {
            session.addReactionToMessage(event.getChannel(), event.getTimeStamp(), reactEmoji);
        }
        session.sendTyping(event.getChannel());
    }

    /**
     * Add the missing "https:" to the front of a URL string
     *
     * @param link
     * @return
     */
    protected static String formatHttpLink(final String link) {
        if (link == null || link.isEmpty() || link.startsWith("http")) {
            return link;
        } else {
            return "https:" + link;
        }
    }

}
