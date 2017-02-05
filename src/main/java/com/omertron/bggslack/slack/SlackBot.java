package com.omertron.bggslack.slack;

import com.omertron.bgg.BggApi;
import com.omertron.bgg.BggException;
import com.omertron.bgg.model.SearchWrapper;
import com.omertron.bgg.model.Thing;
import me.ramswaroop.jbot.core.slack.Bot;
import me.ramswaroop.jbot.core.slack.Controller;
import me.ramswaroop.jbot.core.slack.EventType;
import me.ramswaroop.jbot.core.slack.models.Event;
import me.ramswaroop.jbot.core.slack.models.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.regex.Matcher;

/**
 * A Slack Bot sample. You can create multiple bots by just extending {@link Bot} class like this one.
 *
 * @author ramswaroop
 * @version 1.0.0, 05/06/2016
 */
@Component
public class SlackBot extends Bot {

    private static final Logger LOG = LoggerFactory.getLogger(SlackBot.class);
    private static final BggApi BGG = new BggApi();

    /**
     * Slack token from application.properties file. You can get your slack token next
     * <a href="https://my.slack.com/services/new/bot">creating a new bot</a>.
     */
    @Value("${slackBotToken}")
    private String slackToken;

    @Override
    public String getSlackToken() {
        return slackToken;
    }

    @Override
    public Bot getSlackBot() {
        return this;
    }

    /**
     * Invoked when the bot receives a direct mention (@botname: message) or a direct message. NOTE: These two event types are added
     * by jbot to make your task easier, Slack doesn't have any direct way to determine these type of events.
     *
     * @param session
     * @param event
     */
    @Controller(events = {EventType.DIRECT_MENTION, EventType.DIRECT_MESSAGE})
    public void onReceiveDM(WebSocketSession session, Event event) {
        reply(session, event, new Message("Hi, I am " + slackService.getCurrentUser().getName()));
    }

    /**
     * When Bot receives a command "game xxx"
     *
     * @param session
     * @param event
     * @param matcher
     */
    @Controller(events = EventType.MESSAGE, pattern = "^\\Qgame\\E(?:\\w?)(.*)$")
    public void test(WebSocketSession session, Event event, Matcher matcher) {
        String query = matcher.group(1);
        StringBuilder response;
        Message myReply;
        SearchWrapper results;

        try {
            results = BGG.searchBoardGame(query, false, false);
        } catch (BggException ex) {
            LOG.warn("Error getting BGG information: {}", ex.getMessage(), ex);
            myReply = new Message("Could not get information from BGG.\n Error: " + ex.getMessage());
            myReply.setThreadTs(event.getTs());
            reply(session, event, myReply);
            return;
        }

        response = new StringBuilder("Found ");
        response.append(results.getTotal())
                .append(" matches for *")
                .append(query)
                .append("*");

        for (Thing item : results.getItems()) {
            // Game {xxxx) ID
            response.append("\n")
                    .append(item.getPrimaryName())
                    .append(" (").append(item.getYearPublished()).append(") ")
                    .append("_*").append(item.getId()).append("*_");
        }

        myReply = new Message(response.toString());
        myReply.setThreadTs(event.getTs());
        reply(session, event, myReply);

    }

    /**
     * Invoked when an item is pinned in the channel.
     *
     * @param session
     * @param event
     */
    @Controller(events = EventType.PIN_ADDED)
    public void onPinAdded(WebSocketSession session, Event event) {
        reply(session, event, new Message("Thanks for the pin! You can find all pinned items under channel details."));
    }

    /**
     * Invoked when bot receives an event of type file shared. NOTE: You can't reply to this event as slack doesn't send a channel
     * id for this event type. You can learn more about
     * <a href="https://api.slack.com/events/file_shared">file_shared</a>
     * event from Slack's Api documentation.
     *
     * @param session
     * @param event
     */
    @Controller(events = EventType.FILE_SHARED)
    public void onFileShared(WebSocketSession session, Event event) {
        LOG.info("File shared: {}", event);
    }

}
