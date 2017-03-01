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

    public StatCategory getCategory() {
        return category;
    }

    public void setCategory(StatCategory category) {
        this.category = category;
    }

    public int getTotal() {
        return total;
    }

    public Map<String, Integer> getUsage() {
        Map<String, Integer> copy = new HashMap<>();
        copy.putAll(usage);
        return copy;
    }

    public void setValue(final String user, int value) {
        usage.put(user, value);
        this.total = value;
    }

    public void increment(final String user, int value) {
        if (usage.containsKey(user)) {
            usage.put(user, usage.get(user) + value);
        } else {
            usage.put(user, value);
        }
        total += value;
    }

    public void increment(String user) {
        increment(user, 1);
    }

    public void decrement(final String user, int value) {
        if (usage.containsKey(user)) {
            usage.put(user, usage.get(user) - value);
        } else {
            usage.put(user, value);
        }
        total -= value;
    }

    public void decrement(String user) {
        decrement(user, 1);
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
