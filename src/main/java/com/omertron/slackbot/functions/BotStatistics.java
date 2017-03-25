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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.omertron.slackbot.Constants;
import com.omertron.slackbot.enumeration.StatCategory;
import com.omertron.slackbot.model.StatHolder;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to store any statistics about the bot
 *
 * @author stuart
 */
public final class BotStatistics {

    private static final Logger LOG = LoggerFactory.getLogger(BotStatistics.class);
    private static final Map<StatCategory, StatHolder> STATISTICS = new EnumMap<>(StatCategory.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // Initialise the values
        for (StatCategory stat : StatCategory.values()) {
            STATISTICS.put(stat, new StatHolder(stat));
        }
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private BotStatistics() {
        throw new IllegalArgumentException("Class cannot be instantiated");
    }

    /**
     * Get the current total value of the required statistic
     *
     * @param stat
     * @return
     */
    public static int getStatisticTotal(StatCategory stat) {
        return STATISTICS.get(stat).getTotal();
    }

    /**
     * Set the statistic to a specific value
     *
     * @param stat
     * @param username
     * @param value
     */
    public static synchronized void setStatistic(StatCategory stat, String username, int value) {
        STATISTICS.get(stat).setValue(username, value);
    }

    /**
     * Increment the statistic by 1
     *
     * @param stat
     * @param username
     */
    public static synchronized void increment(StatCategory stat, String username) {
        increment(stat, username, 1);
    }

    /**
     * Increment the statistic by the value
     *
     * @param stat
     * @param username
     * @param amount
     */
    public static synchronized void increment(StatCategory stat, String username, int amount) {
        STATISTICS.get(stat).increment(username, amount);
    }

    /**
     * Decrement the statistic by 1
     *
     * @param stat
     * @param username
     */
    public static synchronized void decrement(StatCategory stat, String username) {
        decrement(stat, username, 1);
    }

    /**
     * Decrement the statistic by the value
     *
     * @param stat
     * @param username
     * @param amount
     */
    public static synchronized void decrement(StatCategory stat, String username, int amount) {
        STATISTICS.get(stat).decrement(username, amount);
    }

    /**
     * Output the jukebox statistics
     *
     * @param skipZero Skip zero values from the output
     * @param detailed Provide detailed username breakdown of usage
     * @return
     */
    public static String generateStatistics(boolean skipZero, boolean detailed) {
        StringBuilder statOutput = new StringBuilder("Statistics:\n");

        // Build the counts
        for (StatHolder stat : STATISTICS.values()) {
            int value = stat.getTotal();
            if (value > 0 || !skipZero) {
                statOutput.append(stat.formatOutput(detailed));
            }
        }

        return statOutput.toString();
    }

    public static void writeFile() {
        try {
            MAPPER.writeValue(new File(Constants.FILENAME_STAT), STATISTICS);
        } catch (IOException ex) {
            LOG.warn("Failed to write stats to {}", Constants.FILENAME_STAT, ex);
        }
    }

    public static void readFile() {
        File f = new File(Constants.FILENAME_STAT);
        if (!f.exists()) {
            LOG.info("File '{}' was not found.", Constants.FILENAME_STAT);
            return;
        }

        try {
            TypeReference<EnumMap<StatCategory, StatHolder>> typeRef = new TypeReference<EnumMap<StatCategory, StatHolder>>() {
            };
            Map<StatCategory, StatHolder> readObj = MAPPER.readValue(f, typeRef);

            STATISTICS.clear();
            STATISTICS.putAll(readObj);
            LOG.info("File '{}' was read successfully.", Constants.FILENAME_STAT);
        } catch (IOException ex) {
            LOG.warn("Failed to read stats from {}", Constants.FILENAME_STAT, ex);
        }
    }
}
