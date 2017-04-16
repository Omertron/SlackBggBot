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
package com.omertron.slackbot.utils;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Properties processing class for YAMJ
 *
 * @author altman.matthew
 */
public final class PropertiesUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesUtil.class);
    private static final String PROPERTIES_CHARSET = "UTF-8";
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    private static final Properties PROPS = new Properties();

    private PropertiesUtil() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    /**
     * Set the properties filename
     *
     * @param streamName
     * @return
     */
    public static boolean setPropertiesStreamName(final String streamName) {
        LOG.info("Using properties file '{}'", FilenameUtils.normalize(streamName));
        InputStream propertiesStream = ClassLoader.getSystemResourceAsStream(streamName);

        try {
            if (propertiesStream == null) {
                propertiesStream = new FileInputStream(streamName);
            }

            try (Reader reader = new InputStreamReader(propertiesStream, PROPERTIES_CHARSET)) {
                PROPS.load(reader);
            }
        } catch (IOException error) {
            LOG.error("Failed loading file {}: Please check your configuration. The properties file should be in the classpath.", streamName, error);
            return Boolean.FALSE;
        } finally {
            try {
                if (propertiesStream != null) {
                    propertiesStream.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        return Boolean.TRUE;
    }

    /**
     * Get a property via a key.
     *
     * @param key
     * @return the value if found, otherwise null
     */
    public static String getProperty(String key) {
        return PROPS.getProperty(key);
    }

    /**
     * Get a property via a key
     *
     * @param key
     * @param defaultValue
     * @return the value if found, otherwise the default value
     */
    public static String getProperty(String key, String defaultValue) {
        return PROPS.getProperty(key, defaultValue);
    }

    /**
     * Return the key property as a boolean
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        return convertBooleanProperty(PROPS.getProperty(key), defaultValue);
    }

    /**
     * Return the key property as integer
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static int getIntProperty(String key, int defaultValue) {
        return convertIntegerProperty(PROPS.getProperty(key), defaultValue);
    }

    /**
     * Return the key property as an long
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static long getLongProperty(String key, long defaultValue) {
        return convertLongProperty(PROPS.getProperty(key), defaultValue);
    }

    /**
     * Return the key property as a float
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static float getFloatProperty(String key, float defaultValue) {
        return convertFloatProperty(PROPS.getProperty(key), defaultValue);
    }

    /**
     * Convert the value to a Float
     *
     * @param key
     * @param valueToConvert
     * @param defaultValue
     * @return
     */
    private static float convertFloatProperty(String valueToConvert, float defaultValue) {
        return NumberUtils.toFloat(StringUtils.trimToEmpty(valueToConvert), defaultValue);
    }

    /**
     * Convert the value to a Long
     *
     * @param key
     * @param valueToConvert
     * @param defaultValue
     * @return
     */
    private static long convertLongProperty(String valueToConvert, long defaultValue) {
        return NumberUtils.toLong(StringUtils.trimToEmpty(valueToConvert), defaultValue);
    }

    /**
     * Convert the value to a Integer
     *
     * @param key
     * @param valueToConvert
     * @param defaultValue
     * @return
     */
    private static int convertIntegerProperty(String valueToConvert, int defaultValue) {
        return NumberUtils.toInt(StringUtils.trimToEmpty(valueToConvert), defaultValue);
    }

    /**
     * Convert the value to a Boolean
     *
     * @param key
     * @param valueToConvert
     * @param defaultValue
     * @return
     */
    private static boolean convertBooleanProperty(String valueToConvert, boolean defaultValue) {
        boolean value = defaultValue;
        if (StringUtils.isNotBlank(valueToConvert)) {
            value = Boolean.parseBoolean(StringUtils.trimToEmpty(valueToConvert));
        }
        return value;
    }

    /**
     * Get the properties as an entry set for iteration<br>
     * Issue 309
     *
     * @return
     */
    public static Set<Entry<Object, Object>> getEntrySet() {
        // Shamelessly adapted from: http://stackoverflow.com/questions/54295/how-to-write-java-util-properties-to-xml-with-sorted-keys
        return new TreeMap<>(PROPS).entrySet();
    }

    /**
     * Set a property key to the string value
     *
     * @param key
     * @param value
     */
    public static void setProperty(String key, String value) {
        PROPS.setProperty(key, value);
    }

    /**
     * Set a property key to the boolean value
     *
     * @param key
     * @param value
     */
    public static void setProperty(String key, boolean value) {
        PROPS.setProperty(key, Boolean.toString(value));
    }

    /**
     * Set a property key to the integer value
     *
     * @param key
     * @param value
     */
    public static void setProperty(String key, int value) {
        PROPS.setProperty(key, Integer.toString(value));
    }

    /**
     * Set a property key to the long value
     *
     * @param key
     * @param value
     */
    public static void setProperty(String key, long value) {
        PROPS.setProperty(key, Long.toString(value));
    }

}
