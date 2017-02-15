package com.omertron.slackbot.utils;

import com.omertron.slackbot.Constants;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author stuar
 */
public class PropertyUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyUtils.class);

    /**
     * Read the properties file, or create it if not found
     *
     * @param props
     * @param filename
     * @throws java.io.FileNotFoundException
     */
    public static void initProperties(Properties props, String filename) throws FileNotFoundException {
        if (props.isEmpty()) {
            File f = new File(filename);

            if (f.exists()) {
                LOG.info("Loading properties from '{}'", filename);
                loadProperties(props, f);
            } else {
                LOG.info("Property file '{}' not found, creating dummy file.", filename);

                props.setProperty(Constants.BOT_TOKEN, "slack-bot-token-here");
                props.setProperty(Constants.PROXY_HOST, "");
                props.setProperty(Constants.PROXY_PORT, "");

                saveProperties(props, f, "Properties file for Slack Bot");
                throw new FileNotFoundException("Property file '" + f.getAbsolutePath() + "' was created, please edit it and re-run");
            }
        }
    }

    /**
     * Load properties from a file
     *
     * @param props
     * @param propertyFile
     */
    private static void loadProperties(Properties props, File propertyFile) {
        InputStream is = null;
        try {
            is = new FileInputStream(propertyFile);
            props.load(is);
        } catch (IOException ex) {
            LOG.warn("Failed to load properties file", ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    LOG.warn("Failed to close properties file", ex);
                }
            }
        }
    }

    /**
     * Save properties to a file
     *
     * @param props
     * @param propertyFile
     * @param headerText
     */
    private static void saveProperties(Properties props, File propertyFile, String headerText) {
        OutputStream out = null;

        try {
            out = new FileOutputStream(propertyFile);
            if (StringUtils.isNotBlank(headerText)) {
                props.store(out, headerText);
            }
        } catch (FileNotFoundException ex) {
            LOG.warn("Failed to find properties file", ex);
        } catch (IOException ex) {
            LOG.warn("Failed to read properties file", ex);
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException ex) {
                    LOG.warn("Failed to close properties file", ex);
                }
            }
        }
    }

}
