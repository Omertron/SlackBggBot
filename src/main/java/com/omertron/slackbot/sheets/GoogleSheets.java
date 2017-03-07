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
package com.omertron.slackbot.sheets;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.omertron.slackbot.Constants;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleSheets {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleSheets.class);
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport HTTP_TRANSPORT;
    /**
     * Google credentials
     */
    private static GoogleCredential credential = null;
    /**
     * Static instance of the sheet
     */
    private static Sheets sheets = null;

    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorise() {
        if (credential == null) {
            LOG.info("Attempting to authorise");
            try {
                HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                credential = GoogleCredential.fromStream(new FileInputStream("SlackBggBot-7a8afe5ba1eb.json"))
                        .createScoped(Arrays.asList(SheetsScopes.SPREADSHEETS_READONLY));
            } catch (IOException | GeneralSecurityException ex) {
                LOG.warn("Failed to authorise: {}", ex.getMessage(), ex);
            }
        }

        LOG.info("Authorised!");
        return credential;
    }

    public static boolean isAuthorised() {
        LOG.info("Authorised? {}", credential != null);
        return credential != null;
    }

    /**
     * Build and return an authorized Sheets API client service.
     *
     * @return an authorized Sheets API client service
     * @throws IOException
     */
    public static Sheets getSheetsService() {
        if (sheets == null) {
            LOG.info("Attempting to get sheet service");
            sheets = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(Constants.BOT_NAME)
                    .build();
        }

        LOG.info("Got sheet service");
        return sheets;
    }

}
