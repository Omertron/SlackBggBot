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

import com.omertron.slackbot.enumeration.StatCategory;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.text.WordUtils;

/**
 * Holds statistics information generated by the bot
 *
 * @author Omertron
 */
public class StatHolder {

    private StatCategory category;
    private int total;
    private final Map<String, Integer> usage;

    protected StatHolder() {
        this.category = null;
        this.total = 0;
        this.usage = new HashMap<>();
    }

    public StatHolder(StatCategory category) {
        this.category = category;
        this.total = 0;
        this.usage = new HashMap<>();
    }

    /**
     * The category that the statistics represent
     *
     * @return category
     */
    public StatCategory getCategory() {
        return category;
    }

    /**
     * Set or change the category that the statistics represent<br>
     * Use with caution as this will divorce the usage statistics from the
     * category.
     *
     * @param category
     */
    public void setCategory(StatCategory category) {
        this.category = category;
    }

    /**
     * Grand total of all the usage for this category
     *
     * @return integer total
     */
    public int getTotal() {
        return total;
    }

    /**
     * The full break down of the usage for the category
     *
     * @return map of usernames & values
     */
    public Map<String, Integer> getUsage() {
        Map<String, Integer> copy = new HashMap<>();
        copy.putAll(usage);
        return copy;
    }

    /**
     * Set the usage value for a specific user
     *
     * @param username Username to update
     * @param value Value to update to.
     */
    public void setValue(final String username, int value) {
        int current = usage.get(username);
        usage.put(username, value);
        this.total = total + value - current;
    }

    /**
     * Increment the statistic for the given username by the value
     *
     * @param username the user to update
     * @param value the value to increment by
     */
    public void increment(final String username, int value) {
        if (usage.containsKey(username)) {
            usage.put(username, usage.get(username) + value);
        } else {
            usage.put(username, value);
        }
        total += value;
    }

    /**
     * Increment the statistic for the given username
     *
     * @param username the user to update
     */
    public void increment(String username) {
        increment(username, 1);
    }

    /**
     * Decrement the statistic for the given username by the value
     *
     * @param username the user to update
     * @param value the value to increment by
     */
    public void decrement(final String username, int value) {
        if (usage.containsKey(username)) {
            usage.put(username, usage.get(username) - value);
        } else {
            usage.put(username, value);
        }
        total -= value;
    }

    /**
     * Decrement the statistic for the given username
     *
     * @param username the user to update
     */
    public void decrement(String username) {
        decrement(username, 1);
    }

    /**
     * Format the statistics in to a string
     *
     * @param detailed
     * @return A formatted, multi-line string output
     */
    public String formatOutput(boolean detailed) {
        StringBuilder output = new StringBuilder();

        output.append(WordUtils.capitalizeFully(category.toString().toLowerCase()));
        if (detailed) {
            output.append(" - total: ").append(total).append("\n");
            for (Map.Entry<String, Integer> stat : usage.entrySet()) {
                output.append("\t").append(stat.getKey()).append(" - ").append(stat.getValue()).append("\n");
            }
        } else {
            output.append(": ").append(total).append("\n");
        }

        return output.toString();
    }
}