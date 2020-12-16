package com.vg.raiddataparser.googleservices;

import com.google.api.services.sheets.v4.model.*;
import com.vg.raiddataparser.googleservices.drive.GoogleDriveService;
import com.vg.raiddataparser.googleservices.sheets.GoogleSheetsService;
import com.vg.raiddataparser.model.Skill;
import com.vg.raiddataparser.model.champion.*;
import com.vg.raiddataparser.model.champion.attributes.ChampionAffinity;
import com.vg.raiddataparser.model.champion.attributes.ChampionFaction;
import com.vg.raiddataparser.model.champion.attributes.ChampionRarity;
import com.vg.raiddataparser.model.champion.attributes.ChampionRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SpreadsheetRaidData {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpreadsheetRaidData.class.getName());

    private final GoogleDriveService driveService = new GoogleDriveService();
    private final GoogleSheetsService sheetsService = new GoogleSheetsService();

    private final List<List<Object>> championValues = new ArrayList<>();
    private final List<List<Object>> skillValues = new ArrayList<>();

    private static final String SPREADSHEET_ID_FILE_NAME = "/spreadsheet_id.txt";
    private static final String RESOURCES_PATH = "src/main/resources";
    private static final String SHEET_CHAMPIONS_TITLE = "Champions";
    private static final String SHEET_SKILLS_TITLE = "Skills";


    public SpreadsheetRaidData() {
        initializeRaidData();
    }

    private void initializeRaidData() {

        File file = new File(RESOURCES_PATH + SPREADSHEET_ID_FILE_NAME);

        String title = "RSL - Champions' multipliers (last updated: " + getCurrentDateFormatyyyyMMdd() + ")";
        SpreadsheetProperties properties = new SpreadsheetProperties().setTitle(title);

        List<Sheet> sheets = new ArrayList<>();

        SheetProperties championSheetProperties = new SheetProperties();
        championSheetProperties.setTitle(SHEET_CHAMPIONS_TITLE)
                .setGridProperties(new GridProperties().setFrozenRowCount(1));

        SheetProperties skillSheetProperties = new SheetProperties();
        skillSheetProperties.setTitle(SHEET_SKILLS_TITLE)
                .setGridProperties(new GridProperties().setFrozenRowCount(1));

        Sheet championSheet = new Sheet().setProperties(championSheetProperties);
        Sheet skillSheet = new Sheet().setProperties(skillSheetProperties);

        sheets.add(championSheet);
        sheets.add(skillSheet);

        try {
            if (file.exists()) { // spreadsheet_id.txt exists
                String spreadsheetId = readSpreadsheetId(file);

                if (driveService.fileExists(spreadsheetId)) { // spreadsheet with corresponding ID exists on Drive
                    LOGGER.info("Spreadsheet already exists on Drive");
                    //TODO: Update Raid data
                    LOGGER.info("Updating Raid data (TO DO)");
                } else { // spreadsheet with corresponding ID doesn't exist on Drive
                    LOGGER.info("Spreadsheet does not exist");
                    Spreadsheet result = sheetsService.createSpreadsheet(properties, sheets);
                    writeSpreadsheetId(file, result.getSpreadsheetId());
                    addChampionHeaderRow();
                    addSkillHeaderRow();
                }
            } else { // spreadsheet_id.txt doesn't exist
                LOGGER.info("Creating new local file " + file.getAbsolutePath());
                if (file.createNewFile()) {
                    Spreadsheet result = sheetsService.createSpreadsheet(properties, sheets);
                    writeSpreadsheetId(file, result.getSpreadsheetId());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error occurred when creating spreadsheet. Operation will be aborted.", e);
        }
    }

    /**
     * Add the Champion values to the List<List<Object>> championValues
     *
     * @param c Champion to be added
     */
    public void addChampionValues(Champion c) {

        // Add champion only if:
        // - name is not empty
        // - name does not contains "hero"
        if (!c.getName().trim().isEmpty() && !c.getName().toLowerCase().contains("hero")) {
            LOGGER.info("Creating row for champion: " + c.getName() + "...");

            championValues.add(Arrays.asList(
                    c.getName(),
                    ChampionFaction.getName(c.getFaction()),
                    ChampionRarity.getName(c.getRarity()),
                    ChampionAffinity.getName(c.getAffinity()),
                    ChampionRole.getName(c.getRole()),
                    c.getHealth(),
                    c.getAttack(),
                    c.getDefense(),
                    c.getSpeed(),
                    c.getResistance(),
                    c.getAccuracy(),
                    c.getCriticalChance(),
                    c.getCriticalDamage(),
                    c.getCriticalHeal()
            ));
        }
    }

    /**
     * Populate the sheet with the Champion values via the Sheets service
     *
     * @throws IOException when updating values in sheet (Sheet service)
     */
    public void populateSheetChampion() throws IOException {
        LOGGER.info("Populating champion data...");

        String spreadsheetId = getSpreadsheetId();
        Spreadsheet spreadsheet = sheetsService.getSpreadsheet(spreadsheetId);
        ValueRange body = new ValueRange().setValues(championValues);
        sheetsService.writeValuesSingleRange(spreadsheet, body, SHEET_CHAMPIONS_TITLE);
    }

    private void addChampionHeaderRow() {
        LOGGER.info("Creating header row for champion sheet...");

        List<Object> headerRow = Arrays.asList(
                "Name",
                "Faction",
                "Rarity",
                "Affinity",
                "Role",
                "Health",
                "Attack",
                "Defense",
                "Speed",
                "Resistance",
                "Accuracy",
                "Critical Chance",
                "Critical Damage",
                "Critical Heal"
        );

        championValues.add(headerRow);
    }

    /**
     * Add the Skill values to the List<List<Object>> skillValues
     *
     * @param s Skill to be added
     */
    public void addSkillValues(Skill s) {

        // Add skill only if:
        // - name is not empty
        // - name does not contain "skill" and "name"
        // - description is not empty
        if (!s.getName().trim().isEmpty()
                && !(s.getName().toLowerCase().contains("skill") && s.getName().toLowerCase().contains("name"))
                && !s.getDescription().trim().isEmpty()) {
            LOGGER.info("Creating row for skill: " + s.getName() + "...");

            skillValues.add(Arrays.asList(
                    s.getName(),
                    s.getDescription(),
                    s.getCooldown(),
                    s.getMultiplierFormula() == null ? "" : s.getMultiplierFormula(),
                    s.getChampion().getName()
            ));
        }
    }

    public void populateSheetSkill() throws IOException {
        LOGGER.info("Populating skill data...");

        String spreadsheetId = getSpreadsheetId();
        Spreadsheet spreadsheet = sheetsService.getSpreadsheet(spreadsheetId);
        ValueRange body = new ValueRange().setValues(skillValues);
        sheetsService.writeValuesSingleRange(spreadsheet, body, SHEET_SKILLS_TITLE);
    }

    private void addSkillHeaderRow() {
        LOGGER.info("Creating header row for skill sheet...");

        List<Object> headerRow = Arrays.asList(
                "Name",
                "Description",
                "Cooldown",
                "Multiplier",
                "Champion"
        );

        skillValues.add(headerRow);
    }

    private static String getCurrentDateFormatyyyyMMdd() {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
        return dateFormatter.format(date);
    }

    private void writeSpreadsheetId(File f, String spreadsheetId) throws IOException {
        LOGGER.info("Writing spreadsheet ID to file");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(f, false))) {
            writer.write(spreadsheetId);
        } catch (IOException e) {
            throw new IOException("Error occurred when writing spreadsheet ID to file \""
                    + RESOURCES_PATH + SPREADSHEET_ID_FILE_NAME
                    + "\". Operation will be aborted.");
        }
    }

    private String getSpreadsheetId() throws IOException {
        File file = new File(RESOURCES_PATH + SPREADSHEET_ID_FILE_NAME);
        return readSpreadsheetId(file);
    }

    private String readSpreadsheetId(File f) throws IOException {
        LOGGER.info("Retrieving spreadsheet ID from file");
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            return reader.readLine();
        } catch (IOException e) {
            throw new IOException("Error occurred when retrieving spreadsheet ID from file \""
                    + RESOURCES_PATH + SPREADSHEET_ID_FILE_NAME
                    + "\". Operation will be aborted.");
        }
    }

}
