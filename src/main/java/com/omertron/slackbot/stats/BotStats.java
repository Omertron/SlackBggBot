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

import com.omertron.slackbot.Constants;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the bot statistics
 *
 * @author stuart
 */
public final class BotStats implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(BotStats.class);
    private static final Map<StatCategory, Map<String, Integer>> STATS;

    static {
        BotStats readInstance = readStatsFromFile();
        if (readInstance != null) {
            LOG.debug("Sucessfully read stats from file");
            STATS = readInstance.getSTATS();
        } else {
            STATS = new EnumMap<>(StatCategory.class);

            LOG.debug("Creating entries for the {} stats categories", StatCategory.values().length);
            for (StatCategory sc : StatCategory.values()) {
                LOG.debug("\tCreating new entry for {}", sc.toString());
                STATS.put(sc, new HashMap<String, Integer>());
            }
        }
    }

    protected BotStats() {
        // Exists only to thwart instantiation.
    }

    protected Map<StatCategory, Map<String, Integer>> getSTATS() {
        return STATS;
    }

    /**
     * Write out the object to a file
     */
    public void saveStatsToFile() {
        File serFile = new File(Constants.STAT_FILENAME);

        if (serFile.exists()) {
            serFile.delete();
        }

        try {
            byte[] serObject = SerializationUtils.serialize(this);
            FileUtils.writeByteArrayToFile(serFile, serObject);
        } catch (IOException ex) {
            LOG.info("Failed to write object to '{}': {}", Constants.STAT_FILENAME, ex.getMessage(), ex);
        }
    }

    /**
     * Read the object back from a file
     *
     * @param <T>
     * @param filename
     * @return
     */
    private static BotStats readStatsFromFile() {
        File serFile = new File(Constants.STAT_FILENAME);

        if (!serFile.exists()) {
            LOG.info("'{}' file doesn't exist", Constants.STAT_FILENAME);
            return null;
        }

        LOG.info("Reading object from '{}'", Constants.STAT_FILENAME);
        try {
            byte[] serObject = FileUtils.readFileToByteArray(serFile);
            return (BotStats) SerializationUtils.deserialize(serObject);
        } catch (IOException ex) {
            LOG.info("Failed to read {}: {}", Constants.STAT_FILENAME, ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Add the value to a statistic for a category
     *
     * @param category
     * @param key
     * @param value
     */
    public void addStat(StatCategory category, String key, Integer value) {
        LOG.info("Updating {} category with key '{}' and value '{}'", category, key, value);
        Map<String, Integer> cMap = STATS.get(category);

        if (cMap == null) {
            LOG.info("\tCategeory {} was empty, initialising", category);
            cMap = new HashMap<>();
            STATS.put(category, cMap);
        }

        Integer cValue = cMap.get(key);
        if (cValue == null) {
            LOG.info("\tKey '{}' was empty, setting to value '{}'", key, value);
            cMap.put(key, value);
        } else {
            LOG.info("\tKey '{}' - Previous value '{}', new value '{}'", key, cValue, cValue + value);
            cMap.put(key, cValue + value);
        }

        saveStatsToFile();
    }

    public String getFormattedStats() {
        StringBuilder stats = new StringBuilder();

        LOG.debug("There are {} categories to get stats on", STATS.size());
        for (Map.Entry<StatCategory, Map<String, Integer>> catEntry : STATS.entrySet()) {
            String category = StringUtils.capitalize(catEntry.getKey().toString().toLowerCase());
            int total = 0;
            for (Map.Entry<String, Integer> statEntry : catEntry.getValue().entrySet()) {
                total += statEntry.getValue();
            }
            LOG.info("\t{} = {}", category, total);
            stats.append(category).append(" used ").append(total).append(" times.\n");
        }
        return stats.toString();
    }
}
