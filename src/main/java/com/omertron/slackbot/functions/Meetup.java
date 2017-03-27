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
package com.omertron.slackbot.functions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omertron.slackbot.Constants;
import com.omertron.slackbot.SlackBot;
import com.omertron.slackbot.model.meetup.MeetupDetails;
import com.omertron.slackbot.utils.HttpTools;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.common.exception.ApiException;
import org.yamj.api.common.exception.ApiExceptionType;

public class Meetup {

    private static final Logger LOG = LoggerFactory.getLogger(Meetup.class);
    private static final ObjectMapper MAPPER;
    private static final List<MeetupDetails> MEETUPS = new ArrayList<>();
    private static final String BASE_URL;

    static {
        MAPPER = new ObjectMapper();
        BASE_URL = SlackBot.getProperty(Constants.MEETUP_URL, "");
    }

    private Meetup() {
        // Static class
    }

    public static void readMeetUp(int pageSize) throws ApiException {
        MEETUPS.clear();

        if (StringUtils.isBlank(BASE_URL)) {
            throw new ApiException(ApiExceptionType.INVALID_URL, "Meetup URL is not set in the properties file! Use the property " + Constants.MEETUP_URL);
        }

        try {
            URL u = HttpTools.createUrl(BASE_URL + pageSize);
            List<MeetupDetails> meetups = MAPPER.readValue(u, new TypeReference<List<MeetupDetails>>() {
            });
            MEETUPS.addAll(meetups);
        } catch (IOException ex) {
            LOG.warn("Failed to read MeetUp data: {}", ex.getMessage(), ex);
            return;
        } catch (ApiException ex) {
            LOG.warn("Failed to convert URL: {}", ex.getMessage(), ex);
            return;
        }

        LOG.info("Processed {} MeetUp events", MEETUPS.size());
    }

    public static List<SlackAttachment> getMeetupAttachment(int quantity, boolean detailed) {
        List<SlackAttachment> attachments = new ArrayList<>();
        LOG.info("Processing {} of the {} meetups read.", Math.max(1, quantity), MEETUPS.size());
        for (int loop = 0; loop < Math.max(1, quantity); loop++) {
            MeetupDetails m = MEETUPS.get(loop);
            LOG.info("\t{}: {}", loop + 1, m.getName());
            SlackAttachment sa = new SlackAttachment();
            sa.addMarkdownIn("text");
            sa.setColor("good");
            sa.setTitle(m.getName());
            sa.setTitleLink(m.getLink());

            if (detailed) {
                sa.setText(reformatDescription(m.getDescription()));
                sa.addField("Duration", String.format("%1$d hour(s)", m.getDuration()), true);
                sa.addField("Status", m.getStatus(), true);
            }

            if (m.getVenue() != null) {
                sa.addField("Venue", m.getVenue().getName(), false);
            }

            sa.addField("Date", DateFormatUtils.format(m.getTime(), "EEEE d MMMM h:mma"), true);

            if (m.getHowToFindUs() != null && detailed) {
                sa.addField("How to find us", m.getHowToFindUs(), false);
            }

            attachments.add(sa);
        }

        LOG.info("Finished processing {} meetups", Math.max(1, quantity));
        return attachments;
    }

    /**
     * Remove MeetUp formatting from the description
     *
     * @param desc
     * @return
     */
    private static String reformatDescription(final String desc) {
        // Replace <p>xxx</p> with \n
        // Replace <b> with *
        // Replace <a href

        return desc.replaceAll("<p>(.*?)</p>", "$1\n")
                .replaceAll("</?b>", "*")
                .replaceAll("<a.*?>(.*?)</a>", "$1");
    }
}
