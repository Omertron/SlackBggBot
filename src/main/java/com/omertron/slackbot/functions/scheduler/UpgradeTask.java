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
package com.omertron.slackbot.functions.scheduler;

import com.omertron.slackbot.Constants;
import com.omertron.slackbot.SlackBot;
import com.omertron.slackbot.enumeration.ExitCode;
import com.omertron.slackbot.utils.GitRepositoryState;
import com.omertron.slackbot.utils.HttpTools;
import com.omertron.slackbot.utils.PropertiesUtil;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.common.exception.ApiException;
import org.yamj.api.common.http.SimpleHttpClientBuilder;

/**
 * Class to compare the commit date of the latest code with the build date of
 * this instance.
 *
 * If there is a difference, then shutdown and restart the bot so the startup
 * script can get the latest build.
 *
 * @author Omertron
 */
public class UpgradeTask extends AbstractBotTask {

    private static final Logger LOG = LoggerFactory.getLogger(UpgradeTask.class);
    private static HttpTools httpTools;
    private static final long DIFFERENCE_THRESHOLD = 1l;
    private static final String PROP_BOT_RESTART_DAILY = "botRestartDaily";
    // URLs
    private static final String URL_GIT_MASTER = "https://api.github.com/repos/omertron/SlackBggBot/git/refs/heads/master";
    private static final String URL_GIT_COMMIT = "https://api.github.com/repos/Omertron/SlackBggBot/git";

    public UpgradeTask(ScheduledExecutorService executorService, String name, int targetHour, int targetMin, SlackSession session, SlackChannel channel) {
        super(executorService, name, targetHour, targetMin, session, channel);
        httpTools = new HttpTools(new SimpleHttpClientBuilder().build());
    }

    @Override
    public void doWork() {
        LOG.info("{} is running", getName());

        LocalDateTime ldtCommit = getLastCommitDate();
        if (ldtCommit == null) {
            LOG.warn("Failed to get commit date, not updating!");
            SlackBot.messageAdmins(getSession(), "Failed to get commit date, not updating!");
            return;
        }

        LocalDateTime ldtBuild = getBuildDate();

        long diff = calculateDifference(ldtBuild, ldtCommit);
        if (diff >= DIFFERENCE_THRESHOLD) {
            String message = String.format("%1$s: Difference between build and latest commit is %2$d minutes, which is greater than the threshold of %3$d minutes", getName(), diff, DIFFERENCE_THRESHOLD);
            LOG.info(message);
            SlackBot.messageAdmins(getSession(), message);
            if (PropertiesUtil.getBooleanProperty(Constants.BOT_TEST, false)) {
                SlackBot.messageAdmins(getSession(), "Skipping shutdown due to test instance");
            } else {
                SlackBot.shutdown(ExitCode.RESTART);
            }
        } else {
            if (PropertiesUtil.getBooleanProperty(PROP_BOT_RESTART_DAILY, false)) {
                LOG.info("Restart property '{}' is enabled, restarting bot", PROP_BOT_RESTART_DAILY);
                SlackBot.shutdown(ExitCode.RESTART);
            } else {
                LOG.info("Bot is running latest code.");
            }
        }
    }

    /**
     * Get the Build DateTime
     *
     * @return LocalDateTime
     */
    private LocalDateTime getBuildDate() {
        GitRepositoryState grs = new GitRepositoryState();
        LocalDateTime ldtBuild = LocalDateTime.parse(grs.getBuildTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss zzz"));
        LOG.info("Found build date: {}", ldtBuild.toString());
        return ldtBuild;
    }

    /**
     * Get the commit datetime
     *
     * @return LocalDateTime
     */
    private LocalDateTime getLastCommitDate() {
        // Get the main URL
        String page;
        try {
            page = httpTools.retrieveWebpage(HttpTools.createUrl(URL_GIT_MASTER));
        } catch (ApiException ex) {
            LOG.warn("Failed to retieve Git page", ex);
            return null;
        }

        int startLoc = page.indexOf("/commits/");
        int endLoc = page.indexOf("\"", startLoc);

        String urlString = page.substring(startLoc, endLoc);
        try {
            page = httpTools.retrieveWebpage(HttpTools.createUrl(URL_GIT_COMMIT + urlString));
        } catch (ApiException ex) {
            LOG.warn("Failed to retieve Git commit page", ex);
            return null;
        }

        startLoc = page.lastIndexOf("\"date\":") + 8;
        endLoc = page.indexOf("\"", startLoc);

        LocalDateTime ldtCommit = LocalDateTime.parse(page.subSequence(startLoc, endLoc - 1));

        LOG.info("Found commit date: {}", ldtCommit.toString());
        return ldtCommit;
    }

    /**
     * Calculate the difference between two date/times
     *
     * @param ldtBuild Build Date/Time
     * @param ldtCommit Commit Date/Time
     * @return Minutes between the dates
     */
    private long calculateDifference(LocalDateTime ldtBuild, LocalDateTime ldtCommit) {
        long days = ChronoUnit.DAYS.between(ldtBuild, ldtCommit);
        long hours = ChronoUnit.HOURS.between(ldtBuild, ldtCommit) - TimeUnit.DAYS.toHours(days);
        long minutes = ChronoUnit.MINUTES.between(ldtBuild, ldtCommit) - TimeUnit.HOURS.toMinutes(hours);
        long fullMinutes = ChronoUnit.MINUTES.between(ldtBuild, ldtCommit);
        LOG.info("Difference {}: {}d {}h {}m", fullMinutes, days, hours, minutes);

        return fullMinutes;
    }

}
