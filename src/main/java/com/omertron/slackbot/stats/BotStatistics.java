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
package com.omertron.slackbot.stats;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omertron.slackbot.Constants;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to store any statistics about the bot
 *
 * @author stuart
 */
public final class BotStatistics {

    private static final Logger LOG = LoggerFactory.getLogger(BotStatistics.class);
    private static final Map<StatCategory, Integer> STATISTICS = new EnumMap<>(StatCategory.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // Initialise the values
        for (StatCategory stat : StatCategory.values()) {
            STATISTICS.put(stat, 0);
        }
    }

    private BotStatistics() {
        throw new IllegalArgumentException("Class cannot be instantiated");
    }

    /**
     * Get the current value of the required statistic
     *
     * @param stat
     * @return
     */
    public static int getStatistic(StatCategory stat) {
        return STATISTICS.get(stat);
    }

    /**
     * Set the statistic to a specific value
     *
     * @param stat
     * @param value
     */
    public static synchronized void setStatistic(StatCategory stat, Integer value) {
        STATISTICS.put(stat, value);
    }

    /**
     * Increment the statistic by 1
     *
     * @param stat
     */
    public static synchronized void increment(StatCategory stat) {
        increment(stat, 1);
    }

    /**
     * Increment the statistic by the value
     *
     * @param stat
     * @param amount
     */
    public static synchronized void increment(StatCategory stat, Integer amount) {
        Integer current = STATISTICS.get(stat);
        STATISTICS.put(stat, current + amount);
    }

    /**
     * Decrement the statistic by 1
     *
     * @param stat
     */
    public static synchronized void decrement(StatCategory stat) {
        decrement(stat, 1);
    }

    /**
     * Decrement the statistic by the value
     *
     * @param stat
     * @param amount
     */
    public static synchronized void decrement(StatCategory stat, Integer amount) {
        Integer current = STATISTICS.get(stat);
        STATISTICS.put(stat, current - amount);
    }

    /**
     * Output the jukebox statistics
     *
     * @param skipZero Skip zero values from the output
     * @return
     */
    public static String generateStatistics(Boolean skipZero) {
        StringBuilder statOutput = new StringBuilder("Statistics:\n");

        // Build the counts
        int value;
        for (StatCategory stat : StatCategory.values()) {
            value = STATISTICS.get(stat);
            if (value > 0 || !skipZero) {
                statOutput.append(WordUtils.capitalizeFully(stat.toString().replace("_", " ").toLowerCase()));
                statOutput.append(" = ").append(value).append("\n");
            }
        }

        return statOutput.toString();
    }

    public static void writeFile() {
        try {
            MAPPER.writeValue(new File(Constants.STAT_FILENAME), STATISTICS);
        } catch (IOException ex) {
            LOG.warn("Failed to write stats to {}", Constants.STAT_FILENAME,ex);
        }
    }

    public static void readFile() {
        try {
            File f = new File(Constants.STAT_FILENAME);
            TypeReference<EnumMap<StatCategory, Integer>> typeRef = new TypeReference<EnumMap<StatCategory, Integer>>(){};
            Map<StatCategory, Integer> readObj = MAPPER.readValue(f, typeRef);
            
            STATISTICS.clear();
            STATISTICS.putAll(readObj);
        } catch (IOException ex) {
            LOG.warn("Failed to read stats from {}", Constants.STAT_FILENAME,ex);
        }
    }
}
