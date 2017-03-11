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

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.omertron.slackbot.Constants;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
     * Creates an authorised Credential object.<p>
     * Build and return an authorised Sheets API client service.
     *
     */
    public static void initialise() {
        if (credential == null) {
            LOG.info("Attempting to authorise");
            try {
                HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                credential = GoogleCredential.fromStream(new FileInputStream("SlackBggBot-7a8afe5ba1eb.json"))
                        .createScoped(Arrays.asList(SheetsScopes.SPREADSHEETS));
            } catch (IOException | GeneralSecurityException ex) {
                LOG.warn("Failed to authorise: {}", ex.getMessage(), ex);
            }
        }
        LOG.info("Authorised!");

        if (sheets == null) {
            LOG.info("Attempting to get sheet service");
            sheets = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(Constants.BOT_NAME)
                    .build();
        }

        LOG.info("Got sheet service");

    }

    public static boolean isAuthorised() {
        LOG.info("Authorised? {}", credential != null);
        return credential != null;
    }

    /**
     * Write data to the sheet
     *
     * @param sheetId The ID of the sheet to write to
     * @param cellRef The cell to write the data to
     * @param dataToWrite Data to write
     * @return True if successful, false otherwise
     */
    public static boolean writeValueToCell(final String sheetId, final String cellRef, final String dataToWrite) {
        LOG.info("Writing '{}' to cell {}", dataToWrite, cellRef);
        List<List<Object>> writeData = new ArrayList<>();
        List<Object> dataRow = new ArrayList<>();
        dataRow.add(dataToWrite == null ? "" : dataToWrite);
        writeData.add(dataRow);

        ValueRange vr = new ValueRange().setValues(writeData).setMajorDimension("ROWS");
        try {
            sheets.spreadsheets().values()
                    .update(sheetId, cellRef, vr)
                    .setValueInputOption("RAW")
                    .execute();
            return true;
        } catch (IOException ex) {
            LOG.warn("IO Exception writing to sheet: {}", ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Get a range from the spreadsheet
     *
     * @param sheetId The ID of the sheet to read
     * @param range The range of cells to read from
     * @return
     */
    public static ValueRange getSheetData(final String sheetId, final String range) {
        LOG.info("Getting information from range {}", range);
        try {
            return sheets.spreadsheets().values()
                    .get(sheetId, range)
                    .execute();
        } catch (IOException ex) {
            LOG.info("IO Exception: {}", ex.getMessage(), ex);
        }
        return null;
    }
}
