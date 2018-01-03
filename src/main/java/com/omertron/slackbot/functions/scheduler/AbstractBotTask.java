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
import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the task that does the work
 *
 * @author Omertron
 */
public abstract class AbstractBotTask implements BotTaskInterface {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBotTask.class);
    private static final ZoneId TIMEZONE = ZoneId.of("Europe/London");

    private final ScheduledExecutorService executorService;
    private volatile ScheduledFuture<?> scheduledTask = null;

    private final String name;
    private final int targetHour;
    private final int targetMin;
    private static final int TARGET_SEC = 0;
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private volatile boolean isBusy = false;

    private final SlackSession session;
    private final SlackChannel channel;

    public AbstractBotTask(ScheduledExecutorService executorService,
            String name,
            int targetHour,
            int targetMin,
            SlackSession session,
            SlackChannel channel) {
        this.executorService = executorService;
        this.name = "Executor [" + name + "]";
        this.targetHour = targetHour;
        this.targetMin = targetMin;
        this.session = session;
        this.channel = channel;

        LOG.info("{} scheduled for {}:{} on channel '{}'", name, targetHour, targetMin, channel.getName());
    }

    @Override
    public final String getName() {
        return name;
    }

    public final int getTargetHour() {
        return targetHour;
    }

    public final int getTargetMin() {
        return targetMin;
    }

    public final SlackSession getSession() {
        return session;
    }

    public final SlackChannel getChannel() {
        return channel;
    }

    public final ScheduledFuture getScheduledTask() {
        return scheduledTask;
    }

    public final AtomicInteger getCompletedTasks() {
        return completedTasks;
    }

    /**
     * Return an attachment with the status of the BotTask
     *
     * @return
     */
    @Override
    public final SlackAttachment getStatus() {
        SlackAttachment sa = new SlackAttachment();

        sa.setTitle(name);

        StringBuilder time = new StringBuilder();
        time.append(StringUtils.leftPad(Integer.toString(targetHour), 2, '0'))
                .append(':')
                .append(StringUtils.leftPad(Integer.toString(targetMin), 2, '0'));

        sa.addField("Target Time", time.toString(), true);
        sa.addField("Remaining Time", formatSeconds(scheduledTask.getDelay(TimeUnit.SECONDS)), true);
        sa.addField("Channel", channel.getName(), true);
        sa.addField("Executions", Integer.toString(completedTasks.get()), true);
        sa.setColor(Constants.ATTACH_COLOUR_GOOD);

        return sa;
    }

    @Override
    public final void start() {
        scheduleNextTask(doTaskWork());
    }

    @Override
    public final void stop() {
        LOG.info("{} is stopping.", name);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        executorService.shutdown();
        LOG.info("{} stopped.", name);
        try {
            LOG.info("{} awaitTermination, start: isBusy [{}]", name, isBusy);
            // wait one minute to termination if busy
            if (isBusy) {
                executorService.awaitTermination(1, TimeUnit.MINUTES);
            }
        } catch (InterruptedException ex) {
            LOG.error("{} awaitTermination exception", name, ex);
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        } finally {
            LOG.info("{} awaitTermination, finished", name);
        }
    }

    private Runnable doTaskWork() {
        return () -> {
            LOG.info("{} [{}] started at {}", name, completedTasks.get(), formattedDateTime());
            try {
                isBusy = true;
                doWork();
                LOG.info("{} finished work at {}", name, formattedDateTime());
            } catch (Exception ex) {
                LOG.error("{} threw exception at {}", name, formattedDateTime(), ex);
            } finally {
                isBusy = false;
            }
            scheduleNextTask(doTaskWork());
            LOG.info("{} [{}] finished at {}", name, completedTasks.getAndIncrement(), formattedDateTime());
        };
    }

    /**
     * Schedule the task for the next occurrence of the hour/minute combination
     *
     * @param task
     */
    private void scheduleNextTask(Runnable task) {
        LOG.info("{} creating next schedule at {}", name, formattedDateTime());
        long delay = computeNextDelay(targetHour, targetMin, TARGET_SEC);
        LOG.info("{} is next scheduled in {}", name, formatSeconds(delay));
        scheduledTask = executorService.schedule(task, delay, TimeUnit.SECONDS);
    }

    /**
     * Convert seconds to Hours, Minutes and Seconds
     *
     * @param seconds
     * @return
     */
    protected final String formatSeconds(long seconds) {
        int day = (int) TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds)
                - TimeUnit.DAYS.toHours(day);
        long minute = TimeUnit.SECONDS.toMinutes(seconds)
                - TimeUnit.DAYS.toMinutes(day)
                - TimeUnit.HOURS.toMinutes(hours);
        long second = TimeUnit.SECONDS.toSeconds(seconds)
                - TimeUnit.DAYS.toSeconds(day)
                - TimeUnit.HOURS.toSeconds(hours)
                - TimeUnit.MINUTES.toSeconds(minute);

        return String.format("%1$dh %2$dm %3$ds", hours, minute, second);
    }

    /**
     * Calculate the time between "now" and the execution time.
     *
     * @param targetHour
     * @param targetMin
     * @param targetSec
     * @return
     */
    private long computeNextDelay(int targetHour, int targetMin, int targetSec) {
        ZonedDateTime zonedNow = localeDateTime();
        ZonedDateTime zonedNextTarget = zonedNow.withHour(targetHour)
                .withMinute(targetMin)
                .withSecond(targetSec)
                .withNano(0);

        if (zonedNow.compareTo(zonedNextTarget) >= 0) {
            zonedNextTarget = zonedNextTarget.plusDays(1);
        }

        Duration duration = Duration.between(zonedNow, zonedNextTarget);

        // If we are scheduled within the next minute, then skip a day as we probably just ran fast
        if (duration.getSeconds() <= 60l) {
            zonedNextTarget = zonedNextTarget.plusDays(1);
            duration = Duration.between(zonedNow, zonedNextTarget);
        }

        return duration.getSeconds();
    }

    /**
     * Get the current date/time in the local time zone
     *
     * @return
     */
    private static ZonedDateTime localeDateTime() {
        return ZonedDateTime.now(TIMEZONE);
    }

    /**
     * Return the current date/time formatted for printing
     *
     * @return
     */
    protected static final String formattedDateTime() {
        return localeDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
