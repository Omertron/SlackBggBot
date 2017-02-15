package com.omertron.slackbot.listeners;

import static com.omertron.slackbot.Constants.DELIM_LEFT;
import static com.omertron.slackbot.Constants.DELIM_RIGHT;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author stuar
 */
public class HelpListener implements SlackMessagePostedListener {

    private static final Logger LOG = LoggerFactory.getLogger(HelpListener.class);
    private static final List<HelpInfo> INFO = new ArrayList<>();
    private static final Pattern PAT_HELP;
    private static SlackAttachment helpMessage = null;

    static {
        String regex = new StringBuilder("(?i)")
                .append("\\").append(DELIM_LEFT).append("\\").append(DELIM_LEFT)
                .append("(help)(?:\\W*)(.*)")
                .append("\\").append(DELIM_RIGHT).append("\\").append(DELIM_RIGHT)
                .toString();

        LOG.info("Help Pattern: {}", regex);
        PAT_HELP = Pattern.compile(regex);
    }

    static class HelpInfo {

        String command;
        String param;
        String message;

        public HelpInfo(String command, String param, String message) {
            this.command = command;
            this.param = param;
            this.message = message;
        }
    }

    @Override
    public void onEvent(SlackMessagePosted event, SlackSession session) {
        // Channel On Which Message Was Posted
        SlackChannel msgChannel = event.getChannel();
        String msgContent = event.getMessageContent();

        // Filter out the bot's own messages
        if (session.sessionPersona().getId().equals(event.getSender().getId())) {
            return;
        }

        // Search for a user commnd pattern
        Matcher m = PAT_HELP.matcher(msgContent);
        if (m.matches()) {
            session.sendMessage(msgChannel, "", getHelpMessage());
        }
    }

    /**
     * Add a help message to the list
     *
     * @param command
     * @param message
     */
    public static void addHelpMessage(String command, String message) {
        addHelpMessage(command, "", message);
    }

    /**
     * Add a help message to the list with parameters
     *
     * @param command
     * @param param
     * @param message
     */
    public static void addHelpMessage(String command, String param, String message) {
        helpMessage = null;
        INFO.add(new HelpInfo(command, param, message));
    }

    /**
     * Format the help commands as an attachment
     * @return 
     */
    private SlackAttachment getHelpMessage() {
        if (helpMessage == null) {
            helpMessage = new SlackAttachment();

            helpMessage.setFallback("Help commads for the bot");
            helpMessage.setPretext("The following commands are available from the game bot");
            helpMessage.addMarkdownIn("fields");

            for (HelpInfo hi : INFO) {
                helpMessage.addField(String.format("%1$s <%2$s>", hi.command, hi.param), hi.message, false);
            }
        }

        return helpMessage;
    }
}
