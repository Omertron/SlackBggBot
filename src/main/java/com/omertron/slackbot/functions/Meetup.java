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
import com.omertron.slackbot.model.meetup.MeetupDetails;
import com.omertron.slackbot.utils.HttpTools;
import com.omertron.slackbot.utils.PropertiesUtil;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.common.exception.ApiException;
import org.yamj.api.common.exception.ApiExceptionType;

public class Meetup {

    private static final Logger LOG = LoggerFactory.getLogger(Meetup.class);
    private static final ObjectMapper MAPPER;
    private static final List<MeetupDetails> MEETUPS = new ArrayList<>();
    private static final String BASE_URL;
    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMMM h:mma");

    static {
        MAPPER = new ObjectMapper();
        BASE_URL = PropertiesUtil.getProperty(Constants.MEETUP_URL, "");
    }

    private Meetup() {
        // Static class
    }

    /**
     * Retrieve and process the MeetUps from the site.
     *
     * @param pageSize
     * @throws ApiException
     */
    public static void readMeetUp(int pageSize) throws ApiException {
        MEETUPS.clear();

        if (StringUtils.isBlank(BASE_URL)) {
            throw new ApiException(ApiExceptionType.INVALID_URL, "Meetup URL is not set in the properties file! Use the property " + Constants.MEETUP_URL);
        }

        try {
            URL url = HttpTools.createUrl(BASE_URL + pageSize);
            List<MeetupDetails> meetups = MAPPER.readValue(url, new TypeReference<List<MeetupDetails>>() {
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

    /**
     * Format the MeetUp list into a list of Slack Attachments
     *
     * @param quantity
     * @param detailed
     * @return
     */
    public static List<SlackAttachment> getMeetupsQty(int quantity, boolean detailed) {
        List<SlackAttachment> attachments = new ArrayList<>();
        LOG.info("Processing {} of the {} meetups read.", Math.max(1, quantity), MEETUPS.size());
        for (int loop = 0; loop < Math.max(1, quantity); loop++) {
            MeetupDetails m = MEETUPS.get(loop);
            LOG.info("\t{}: {}", loop + 1, m.getName());
            attachments.add(makeSlackAttachment(m, detailed));
        }

        LOG.info("Finished processing {} meetups", Math.max(1, quantity));
        return attachments;
    }

    /**
     * Format the MeetUp list into a list of Slack Attachments, up to a certain number of days ahead
     *
     * @param daysAhead
     * @param detailed
     * @return
     */
    public static Map<LocalDateTime, SlackAttachment> getMeetupsDays(int daysAhead, boolean detailed) {
        LocalDate now = LocalDate.now();
        Map<LocalDateTime, SlackAttachment> results = new HashMap<>();

        Period diff;
        for (MeetupDetails md : MEETUPS) {
            // Correct for BST
            LocalDateTime meetTime = md.getMeetupTime().plusHours(1);

            diff = Period.between(now, meetTime.toLocalDate());
            if (diff.getDays() <= daysAhead) {
                LOG.info("Add: Days: {} - {} - {}", diff.getDays(), meetTime.format(DT_FORMAT), md.getName());
                results.put(meetTime, makeSlackAttachment(md, detailed));
            } else {
                LOG.info("Skip: Days: {} - {} - {}", diff.getDays(), meetTime.format(DT_FORMAT), md.getName());
            }
        }

        return results;
    }

    /**
     * Convert MeetupDetails into a SlackAttachment
     *
     * @param meetupDetails
     * @param detailed
     * @return
     */
    private static SlackAttachment makeSlackAttachment(MeetupDetails meetupDetails, boolean detailed) {
        SlackAttachment sa = new SlackAttachment();
        sa.addMarkdownIn("text");
        sa.setColor("good");
        sa.setTitle(meetupDetails.getName());
        sa.setTitleLink(meetupDetails.getLink());
        sa.setThumbUrl("https://secure.meetupstatic.com/photos/event/4/9/1/c/global_453258716.jpeg");

        if (detailed) {
            sa.setText(reformatDescription(meetupDetails.getDescription()));
            sa.addField("Duration", String.format("%1$d hour(s)", meetupDetails.getDuration()), true);
            sa.addField("Status", meetupDetails.getStatus(), true);
        }

        if (meetupDetails.getVenue() != null) {
            sa.addField("Venue", meetupDetails.getVenue().getName(), true);
        }

        // Correct for BST
        LocalDateTime meetTime = meetupDetails.getMeetupTime().plusHours(1);
        sa.addField("Date", meetTime.format(DT_FORMAT), true);

        if (meetupDetails.getHowToFindUs() != null && detailed) {
            sa.addField("How to find us", meetupDetails.getHowToFindUs(), false);
        }
        return sa;
    }

    /**
     * Remove MeetUp formatting from the description
     *
     * @param desc
     * @return
     */
    private static String reformatDescription(final String desc) {
        return desc.replaceAll("<p>(.*?)</p>", "$1\n")
                .replaceAll("</?b>", "*")
                .replaceAll("<a.*?>(.*?)</a>", "$1");
    }
}
