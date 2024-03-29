/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.tests.response;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.categories.Git;
import org.labkey.test.components.response.MyStudiesResponseServerTab;
import org.labkey.test.components.response.TokenBatchPopup;
import org.labkey.test.pages.response.SetupPage;
import org.labkey.test.pages.response.TokenListPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@Category({Git.class})
public class ReadResponseTest extends BaseResponseTest
{
    public static final String PROJECT_NAME = "Read Response Test Project";
    public static final String PROJECT_STUDY_NAME = "TEST_READRESPONSE_STUDY";
    public static final String LIST_DIFF_DATATYPES = "TestListDiffDataTypes";
    public static final String LIST_SECOND = "SecondSimpleList";
    public static final String LIST_THIRD = "ThirdList";

    public static final String FIRST_STRING_FIELD_VALUE = "This is the string value for participant ";
    public static final String SECOND_STRING_FIELD_VALUE = "This is the second string value for participant ";
    public static final String THIRD_STRING_FIELD_VALUE = "This is the third string value for participant ";

    public static final String FIRST_MULTILINE_STRING_FIELD = "This is the \r\nfirst\r\nmulti-line for participant $\r\nand here is a new line.";
    public static final String SECOND_MULTILINE_STRING_FIELD = "This is the \r\nsecond\r\nmulti-line for participant $\r\nand here is a new line.";
    public static final String THIRD_MULTILINE_STRING_FIELD = "This is the \r\nthird\r\nmulti-line for participant $\r\nand here is a new line.";

    public static final String FIRST_FLAG_FIELD = "First flag ";
    public static final String SECOND_FLAG_FIELD = "Second flag ";
    public static final String THIRD_FLAG_FIELD = "Third flag ";

    public static final String FIRST_MANTISSA = ".0123456789";
    public static final String SECOND_MANTISSA = ".22222";
    public static final String THIRD_MANTISSA = ".3333";

    public static final String FIRST_DATE = "2017-03-17 11:11:11.000";
    public static final String SECOND_DATE = "2017-01-15 08:02:00.000";
    public static final String THIRD_DATE = "2016-11-20 14:25:00.000";

    public static final int FIRST_INT_OFFSET = 5;
    public static final int SECOND_INT_OFFSET = 7;
    public static final int THIRD_INT_OFFSET = 11;

    public static final String DESCRIPTION_VALUE_SECOND_LIST = "Description for ";
    public static final String DESCRIPTION_VALUE_THIRD_LIST = "This is a description in the third list ";

    private static ParticipantInfo participantToSkip, participantWithMultipleRow, participantWithOneRow, participantForSql;

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(PROJECT_NAME, false);
    }

    @BeforeClass
    public static void projectSetup() throws Exception
    {
        ReadResponseTest initTest = (ReadResponseTest) getCurrentTest();
        initTest.doProjectSetup();
    }

    private void doProjectSetup() throws IOException, CommandException
    {
        _containerHelper.createProject(PROJECT_NAME, FOLDER_TYPE);
        createBatchAndAssignTokens();
        setupList();
    }

    private void createBatchAndAssignTokens()
    {
        String batchId, tokenCount = "100";

        log("Creating the batch.");
        goToProjectHome(PROJECT_NAME);

        SetupPage setupPage = new SetupPage(this);
        MyStudiesResponseServerTab myStudiesResponseServerTab = MyStudiesResponseServerTab.beginAt(this);

        log("Set a study name.");
        myStudiesResponseServerTab.setInputId(PROJECT_STUDY_NAME);
        myStudiesResponseServerTab.saveAndExpectSuccess();

        log("Create " + tokenCount + " tokens.");
        SetupPage.beginAt(this, PROJECT_NAME);
        TokenBatchPopup tokenBatchPopup = setupPage.getTokenBatchesWebPart().openNewBatchPopup();
        TokenListPage tokenListPage = tokenBatchPopup.createNewBatch(tokenCount);

        batchId = tokenListPage.getBatchId();
        log("Batch Id: " + batchId);

        List<String> tokensToAssign = new ArrayList<>();
        tokensToAssign.add(tokenListPage.getToken(0));
        tokensToAssign.add(tokenListPage.getToken(1));
        tokensToAssign.add(tokenListPage.getToken(2));
        tokensToAssign.add(tokenListPage.getToken(3));
        tokensToAssign.add(tokenListPage.getToken(4));
        tokensToAssign.add(tokenListPage.getToken(5));
        tokensToAssign.add(tokenListPage.getToken(6));

        log("Validate that the correct info for the tokens is shown in the grid.");
        goToProjectHome();
        confirmBatchInfoCreated(setupPage, batchId, tokenCount, "0");

        log("Now assign some of the tokens from the batch.");

        assignTokens(tokensToAssign, PROJECT_NAME, PROJECT_STUDY_NAME);
    }

    private void setupList() throws IOException, CommandException
    {
        List<ParticipantInfo> participantsInfo = getTokens();

        // Flag special participants
        ReadResponseTest.participantToSkip = new ParticipantInfo(participantsInfo.get(participantsInfo.size()/2).getId(), participantsInfo.get(
            participantsInfo.size()/2).getAppToken());
        ReadResponseTest.participantWithMultipleRow = new ParticipantInfo(participantsInfo.get(0).getId(), participantsInfo.get(0).getAppToken());
        ReadResponseTest.participantWithOneRow = new ParticipantInfo(participantsInfo.get(1).getId(), participantsInfo.get(1).getAppToken());

        // Nothing particularly special about this participant, except that their integerField value will be the same as participantWithMultipleRow.
        ReadResponseTest.participantForSql = new ParticipantInfo(participantsInfo.get(participantsInfo.size() - 1).getId(), participantsInfo.get(participantsInfo.size() - 1).getAppToken());


        log("Create a list with columns for each of the basic data types.");
        {
            ListDefinition listDef = new IntListDefinition(LIST_DIFF_DATATYPES, "Key");
            listDef.setFields(List.of(new FieldDefinition("participantId", FieldDefinition.ColumnType.Integer), new FieldDefinition("stringField", FieldDefinition.ColumnType.String),
                new FieldDefinition("multiLineField", FieldDefinition.ColumnType.MultiLine), new FieldDefinition("booleanField", FieldDefinition.ColumnType.Boolean),
                new FieldDefinition("integerField", FieldDefinition.ColumnType.Integer), new FieldDefinition("doubleField", FieldDefinition.ColumnType.Decimal),
                new FieldDefinition("dateTimeField", FieldDefinition.ColumnType.DateAndTime), new FieldDefinition("flagField", FieldDefinition.ColumnType.Flag),
                new FieldDefinition("user", FieldDefinition.ColumnType.User)));
            listDef.getCreateCommand().execute(createDefaultConnection(), getProjectName());
            TestDataGenerator dataGenerator = listDef.getTestDataGenerator(getProjectName());

            Number userId = whoAmI().getUserId();

            log("Not going to put participant: " + ReadResponseTest.participantToSkip.getId() + " (" + ReadResponseTest.participantToSkip.getAppToken() + ") into the list.");
            for (ParticipantInfo participantInfo : participantsInfo)
            {
                if (participantInfo.getId() != ReadResponseTest.participantToSkip.getId())
                {
                    int participantId = participantInfo.getId();

                    Map<String, Object> rowData = new HashMap<>();

                    rowData.put("participantId", participantId);
                    rowData.put("stringField", FIRST_STRING_FIELD_VALUE + participantInfo.getId());
                    rowData.put("multiLineField", FIRST_MULTILINE_STRING_FIELD.replace("$", Integer.toString(participantId)));

                    if ((participantId & 1) == 0)
                        rowData.put("booleanField", "true");
                    else
                        rowData.put("booleanField", "false");

                    rowData.put("integerField", Integer.toString(participantId + FIRST_INT_OFFSET));

                    rowData.put("doubleField", participantInfo.getId() + FIRST_MANTISSA);

                    rowData.put("dateTimeField", FIRST_DATE);

                    rowData.put("flagField", FIRST_FLAG_FIELD + participantInfo.getId());

                    rowData.put("user", userId);

                    dataGenerator.addCustomRow(rowData);
                }
            }

            log("Now add a few more rows in the list for participant: " + ReadResponseTest.participantWithMultipleRow.getId() + " (" + ReadResponseTest.participantWithMultipleRow
                .getAppToken() + "). This is the only participant with multiple rows in the list.");
            int participantId = ReadResponseTest.participantWithMultipleRow.getId();

            dataGenerator.addCustomRow(Map.of("participantId", participantId, "stringField", SECOND_STRING_FIELD_VALUE + ReadResponseTest.participantWithMultipleRow.getId(),
                "multiLineField", SECOND_MULTILINE_STRING_FIELD.replace("$", Integer.toString(participantId)),
                "booleanField", true, "integerField", participantId + SECOND_INT_OFFSET, "doubleField", ReadResponseTest.participantWithMultipleRow.getId() + SECOND_MANTISSA,
                "dateTimeField", SECOND_DATE, "flagField", SECOND_FLAG_FIELD + ReadResponseTest.participantWithMultipleRow.getId(),
                "user", userId));

            dataGenerator.addCustomRow(Map.of("participantId", participantId, "stringField", THIRD_STRING_FIELD_VALUE + ReadResponseTest.participantWithMultipleRow.getId(),
                "multiLineField", THIRD_MULTILINE_STRING_FIELD.replace("$", Integer.toString(participantId)),
                "booleanField", false, "integerField", participantId + THIRD_INT_OFFSET, "doubleField", ReadResponseTest.participantWithMultipleRow.getId() + THIRD_MANTISSA,
                "dateTimeField", THIRD_DATE, "flagField", THIRD_FLAG_FIELD + ReadResponseTest.participantWithMultipleRow.getId(),
                "user", userId));
            dataGenerator.insertRows();
        }

        log("Create a simple second list that has no participantId but will work as a look-up.");
        {
            ListDefinition listDef = new IntListDefinition(LIST_SECOND, "Key");
            listDef.setFields(List.of(new FieldDefinition("integerField", FieldDefinition.ColumnType.Integer), new FieldDefinition("Description", FieldDefinition.ColumnType.String)));
            listDef.getCreateCommand().execute(createDefaultConnection(), getProjectName());

            TestDataGenerator dataGenerator = listDef.getTestDataGenerator(getProjectName());

            log("Now add two rows to " + LIST_SECOND);

            int id = participantWithMultipleRow.getId();

            dataGenerator.addCustomRow(Map.of(
                "Description", DESCRIPTION_VALUE_SECOND_LIST + (id + FIRST_INT_OFFSET),
                "integerField", Integer.toString(id + FIRST_INT_OFFSET)
            ));
            dataGenerator.addCustomRow(Map.of(
                "Description", DESCRIPTION_VALUE_SECOND_LIST + (id + SECOND_INT_OFFSET),
                "integerField", Integer.toString(id + SECOND_INT_OFFSET)
            ));
            dataGenerator.insertRows();
        }

        log("Create a third list that has a participantId column.");
        {
            ListDefinition listDef = new IntListDefinition(LIST_THIRD, "Key");
            listDef.setFields(List.of(new FieldDefinition("participantId", FieldDefinition.ColumnType.Integer), new FieldDefinition("integerField", FieldDefinition.ColumnType.Integer),
                new FieldDefinition("Description", FieldDefinition.ColumnType.String)));
            listDef.getCreateCommand().execute(createDefaultConnection(), getProjectName());

            TestDataGenerator dataGenerator = listDef.getTestDataGenerator(getProjectName());

            log("Now add a couple of rows to " + LIST_THIRD);

            int id = ReadResponseTest.participantWithMultipleRow.getId();

            dataGenerator.addCustomRow(Map.of(
                "participantId", id,
                "Description", DESCRIPTION_VALUE_THIRD_LIST + (id + SECOND_INT_OFFSET),
                "integerField", id + SECOND_INT_OFFSET
            ));

            id = ReadResponseTest.participantForSql.getId();
            dataGenerator.addCustomRow(Map.of(
                "participantId", id,
                "Description", DESCRIPTION_VALUE_THIRD_LIST + (id + FIRST_INT_OFFSET),
                "integerField", id + FIRST_INT_OFFSET
            ));

            id = ReadResponseTest.participantWithOneRow.getId();
            dataGenerator.addCustomRow(Map.of(
                "participantId", id,
                "Description", DESCRIPTION_VALUE_THIRD_LIST + (id + FIRST_INT_OFFSET),
                "integerField", id + FIRST_INT_OFFSET
            ));

            dataGenerator.insertRows();
        }

        log("Done creating and populating the lists.");

        // Connect as a normal admin user and test the row count in each list

        SelectRowsResponse response = getListInfo(LIST_DIFF_DATATYPES);
        log(LIST_DIFF_DATATYPES + " has " + response.getRowCount() + " rows");

        response = getListInfo(LIST_SECOND);
        log(LIST_SECOND + " has " + response.getRowCount() + " rows");

        response = getListInfo(LIST_THIRD);
        log(LIST_THIRD + " has " + response.getRowCount() + " rows");
    }

    private List<ParticipantInfo> getTokens() throws IOException, CommandException
    {
        List<ParticipantInfo> _participantInfos = new ArrayList<>();

        Connection cn = createDefaultConnection();
        SelectRowsCommand selectCmd = new SelectRowsCommand("mobileappstudy", "Participant");
        SelectRowsResponse rowsResponse = selectCmd.execute(cn, getProjectName());
        log("Row count: " + rowsResponse.getRows().size());

        for(Map<String, Object> row: rowsResponse.getRows())
        {
            _participantInfos.add(new ParticipantInfo((int)row.get("RowId"), row.get("AppToken").toString()));
        }

        log("Number of participants with AppTokens: " + _participantInfos.size());

        return _participantInfos;
    }

    @Test
    public void validateSelectRowsWithMultipleRows() throws CommandException, IOException
    {
        log("Call selectRows with participant " + ReadResponseTest.participantWithMultipleRow.getId() + " (" + ReadResponseTest.participantWithMultipleRow.getAppToken() + "). This participant should return multiple rows.");
        goToProjectHome();
        String participantAppToken = ReadResponseTest.participantWithMultipleRow.getAppToken();

        Map<String, Object> params = new HashMap<>();
        params.put("queryName", LIST_DIFF_DATATYPES);
        params.put("query.columns", "participantId, stringField, multiLineField, booleanField, integerField, doubleField, dateTimeField, flagField");
        params.put("participantId", participantAppToken);

        log("Columns parameter: " + params.get("query.columns"));

        CommandResponse rowsResponse = callSelectRows(params);

        log("Validate 3 rows were returned.");

        List<Map<String, Object>> rows = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + ReadResponseTest.participantWithMultipleRow.getId() + " (" + ReadResponseTest.participantWithMultipleRow.getAppToken() + ") not as expected.", 3, rows.size());

        log("Validate the first item returned in the json.");
        Map<String, Object> expectedValues = new HashMap<>();
        expectedValues.put("participantId", ReadResponseTest.participantWithMultipleRow.getId());
        expectedValues.put("stringField", FIRST_STRING_FIELD_VALUE + ReadResponseTest.participantWithMultipleRow.getId());
        expectedValues.put("multiLineField", FIRST_MULTILINE_STRING_FIELD.replace("$", Integer.toString(ReadResponseTest.participantWithMultipleRow.getId())));

        int id = ReadResponseTest.participantWithMultipleRow.getId();
        if ((id & 1) == 0)
            expectedValues.put("booleanField", true);
        else
            expectedValues.put("booleanField", false);

        expectedValues.put("integerField", id + FIRST_INT_OFFSET);
        expectedValues.put("doubleField", BigDecimal.valueOf(Double.parseDouble(ReadResponseTest.participantWithMultipleRow.getId() + FIRST_MANTISSA)));
        expectedValues.put("dateTimeField", FIRST_DATE);
        expectedValues.put("flagField", FIRST_FLAG_FIELD + ReadResponseTest.participantWithMultipleRow.getId());

        Map<String, Object> row = rows.get(0);
        checkJsonMapAgainstExpectedValues(expectedValues, row);

        log("Validate the second item returned in the json.");
        expectedValues = new HashMap<>();
        expectedValues.put("participantId", ReadResponseTest.participantWithMultipleRow.getId());
        expectedValues.put("stringField", SECOND_STRING_FIELD_VALUE + ReadResponseTest.participantWithMultipleRow.getId());
        expectedValues.put("multiLineField", SECOND_MULTILINE_STRING_FIELD.replace("$", Integer.toString(ReadResponseTest.participantWithMultipleRow.getId())));
        expectedValues.put("booleanField", true);
        expectedValues.put("integerField", id + SECOND_INT_OFFSET);
        expectedValues.put("doubleField", BigDecimal.valueOf(Double.parseDouble(ReadResponseTest.participantWithMultipleRow.getId() + SECOND_MANTISSA)));
        expectedValues.put("dateTimeField", SECOND_DATE);
        expectedValues.put("flagField", SECOND_FLAG_FIELD + ReadResponseTest.participantWithMultipleRow.getId());

        row = rows.get(1);
        checkJsonMapAgainstExpectedValues(expectedValues, row);

        log("Validate the third item returned in the json.");
        expectedValues = new HashMap<>();
        expectedValues.put("participantId", ReadResponseTest.participantWithMultipleRow.getId());
        expectedValues.put("stringField", THIRD_STRING_FIELD_VALUE + ReadResponseTest.participantWithMultipleRow.getId());
        expectedValues.put("multiLineField", THIRD_MULTILINE_STRING_FIELD.replace("$", Integer.toString(ReadResponseTest.participantWithMultipleRow.getId())));
        expectedValues.put("booleanField", false);
        expectedValues.put("integerField", id + THIRD_INT_OFFSET);
        expectedValues.put("doubleField", BigDecimal.valueOf(Double.parseDouble(ReadResponseTest.participantWithMultipleRow.getId() + THIRD_MANTISSA)));
        expectedValues.put("dateTimeField", THIRD_DATE);
        expectedValues.put("flagField", THIRD_FLAG_FIELD + ReadResponseTest.participantWithMultipleRow.getId());

        row = rows.get(2);
        checkJsonMapAgainstExpectedValues(expectedValues, row);

        log("Looks good. Go home.");
        goToHome();
    }

    @Test
    public void validateSelectRowsWithOneRow() throws CommandException, IOException
    {
        log("Call selectRows with participant " + ReadResponseTest.participantWithOneRow.getId() + " (" + ReadResponseTest.participantWithOneRow.getAppToken() + "). This participant should return one row.");
        goToProjectHome();
        String participantAppToken = ReadResponseTest.participantWithOneRow.getAppToken();

        Map<String, Object> params = new HashMap<>();
        params.put("query.queryName", LIST_DIFF_DATATYPES);
        params.put("query.columns", "participantId, stringField, multiLineField, booleanField, integerField, doubleField, dateTimeField, flagField");
        params.put("participantId", participantAppToken);

        log("Columns parameter: " + params.get("query.columns"));

        CommandResponse rowsResponse = callSelectRows(params);

        log("Validate that 1 row is returned.");

        List<Map<String, Object>> rows = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + ReadResponseTest.participantWithOneRow.getId() + " (" + ReadResponseTest.participantWithOneRow.getAppToken() + ") not as expected.", 1, rows.size());

        log("Validate the row returned.");
        Map<String, Object> expectedValues = new HashMap<>();
        expectedValues.put("participantId", ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("stringField", FIRST_STRING_FIELD_VALUE + ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("multiLineField", FIRST_MULTILINE_STRING_FIELD.replace("$", Integer.toString(ReadResponseTest.participantWithOneRow.getId())));

        int id = ReadResponseTest.participantWithOneRow.getId();
        if ((id & 1) == 0)
            expectedValues.put("booleanField", true);
        else
            expectedValues.put("booleanField", false);

        expectedValues.put("integerField", id + FIRST_INT_OFFSET);
        expectedValues.put("doubleField", BigDecimal.valueOf(Double.parseDouble(ReadResponseTest.participantWithOneRow.getId() + FIRST_MANTISSA)));
        expectedValues.put("dateTimeField", FIRST_DATE);
        expectedValues.put("flagField", FIRST_FLAG_FIELD + ReadResponseTest.participantWithOneRow.getId());

        Map<String, Object> row = rows.get(0);
        checkJsonMapAgainstExpectedValues(expectedValues, row);

        log("Looks good. Go home.");
        goToHome();
    }

    @Test
    public void validateSelectRowsWithNoRows() throws CommandException, IOException
    {
        log("Call selectRows with participant " + ReadResponseTest.participantToSkip.getId() + " (" + ReadResponseTest.participantToSkip.getAppToken() + "). This participant has no rows in the list.");
        goToProjectHome();
        String participantAppToken = ReadResponseTest.participantToSkip.getAppToken();

        Map<String, Object> params = new HashMap<>();
        params.put("queryName", LIST_DIFF_DATATYPES);
        params.put("query.columns", "participantId, stringField, multiLineField, booleanField, integerField, doubleField, dateTimeField, flagField");
        params.put("participantId", participantAppToken);

        log("Columns parameter: " + params.get("query.columns"));

        CommandResponse rowsResponse = callSelectRows(params);

        log("Validate that no rows are returned.");

        List<?> rows = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + ReadResponseTest.participantToSkip.getId() + " (" + ReadResponseTest.participantToSkip.getAppToken() + ") not as expected.", 0, rows.size());

        log("Looks good. Go home.");
        goToHome();
    }

    @Test
    public void validateSelectRowsColumnParameter() throws CommandException, IOException
    {
        String columnsToReturn = "integerField, participantId, stringField, dateTimeField, multiLineField";

        log("Call selectRows with participant " + ReadResponseTest.participantWithOneRow.getId() + " (" + ReadResponseTest.participantWithOneRow.getAppToken() + "). This participant should return one row.");
        log("Only these columns '" + columnsToReturn + "' should be returned.");

        goToProjectHome();
        String participantAppToken = ReadResponseTest.participantWithOneRow.getAppToken();

        Map<String, Object> params = new HashMap<>();
        params.put("query.queryName", LIST_DIFF_DATATYPES);
        params.put("query.columns", columnsToReturn);
        params.put("participantId", participantAppToken);

        log("Columns parameter: " + params.get("query.columns"));

        CommandResponse rowsResponse = callSelectRows(params);

        List<Map<String, Object>> rows = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + ReadResponseTest.participantWithOneRow.getId() + " (" + ReadResponseTest.participantWithOneRow.getAppToken() + ") not as expected.", 1, rows.size());

        log("Validate row returned. Verify that only the expected columns are returned.");

        Map<String, Object> expectedValues = new HashMap<>();
        expectedValues.put("participantId", ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("stringField", FIRST_STRING_FIELD_VALUE + ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("multiLineField", FIRST_MULTILINE_STRING_FIELD.replace("$", Integer.toString(ReadResponseTest.participantWithOneRow.getId())));

        int idAsInt = ReadResponseTest.participantWithOneRow.getId();
        expectedValues.put("integerField", idAsInt + FIRST_INT_OFFSET);

        expectedValues.put("dateTimeField", FIRST_DATE);

        Map<String, Object> row = rows.get(0);
        checkJsonMapAgainstExpectedValues(expectedValues, row);

        log("Call selectRows with no columns parameter, this should return all columns.");

        params = new HashMap<>();
        params.put("queryName", LIST_DIFF_DATATYPES);
        params.put("participantId", participantAppToken);

        rowsResponse = callSelectRows(params);

        log("Validate that 1 row.");

        rows = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + ReadResponseTest.participantWithOneRow.getId() + " (" + ReadResponseTest.participantWithOneRow.getAppToken() + ") not as expected.", 1, rows.size());

        log("Validate that the row returned has all of the columns.");

        expectedValues = new HashMap<>();
        expectedValues.put("participantId", ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("stringField", FIRST_STRING_FIELD_VALUE + ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("multiLineField", FIRST_MULTILINE_STRING_FIELD.replace("$", Integer.toString(ReadResponseTest.participantWithOneRow.getId())));

        if ((idAsInt & 1) == 0)
            expectedValues.put("booleanField", true);
        else
            expectedValues.put("booleanField", false);

        expectedValues.put("integerField", idAsInt + FIRST_INT_OFFSET);
        expectedValues.put("doubleField", BigDecimal.valueOf(Double.parseDouble(ReadResponseTest.participantWithOneRow.getId() + FIRST_MANTISSA)));
        expectedValues.put("dateTimeField", FIRST_DATE);
        expectedValues.put("flagField", FIRST_FLAG_FIELD + ReadResponseTest.participantWithOneRow.getId());

        APIUserHelper userHelper = new APIUserHelper(this);
        int userId = userHelper.getUserId(getCurrentUser());
        expectedValues.put("user", userId);

        row = rows.get(0);
        checkJsonMapAgainstExpectedValues(expectedValues, row);

        log("Now call selectRows with only bad column names.");
        params = new HashMap<>();
        params.put("query.queryName", LIST_DIFF_DATATYPES);
        params.put("query.columns", "foo, bar");
        params.put("participantId", participantAppToken);

        log("Columns parameter: " + params.get("query.columns"));

        rowsResponse = callSelectRows(params);

        log("Validate that 1 row is returned.");

        rows = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + ReadResponseTest.participantWithOneRow.getId() + " (" + ReadResponseTest.participantWithOneRow.getAppToken() + ") not as expected.", 1, rows.size());

        log("Since only invalid column names were passed no columns should be returned (other than the 'Key' column).");

        // If only invalid columns were provided no columns should be returned.
        expectedValues = new HashMap<>();

        row = rows.get(0);
        checkJsonMapAgainstExpectedValues(expectedValues, row);

        columnsToReturn = "integerField, participantId, foo, stringField, dateTimeField, bar, multiLineField";

        params.put("queryName", LIST_DIFF_DATATYPES);
        params.put("query.columns", columnsToReturn);
        params.put("participantId", participantAppToken);

        log("Now validate with a mix of valid and invalid columns. Column parameter: '" + params.get("query.columns") + "'.");

        rowsResponse = callSelectRows(params);

        log("Validate that 1 row is returned.");

        rows = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + ReadResponseTest.participantWithOneRow.getId() + " (" + ReadResponseTest.participantWithOneRow.getAppToken() + ") not as expected.", 1, rows.size());

        log("Validate row returned.");
        expectedValues = new HashMap<>();
        expectedValues.put("participantId", ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("stringField", FIRST_STRING_FIELD_VALUE + ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("multiLineField", FIRST_MULTILINE_STRING_FIELD.replace("$", Integer.toString(ReadResponseTest.participantWithOneRow.getId())));

        expectedValues.put("integerField", idAsInt + FIRST_INT_OFFSET);

        expectedValues.put("dateTimeField", FIRST_DATE);

        row = rows.get(0);
        checkJsonMapAgainstExpectedValues(expectedValues, row);

        log("Look at the 'special' columns. Specifically CreatedBy, ModifiedBy and Container. These are columns with FK into other lists outside of this project.");

        String containerId = getContainerId();
        userId = userHelper.getUserId(getCurrentUser());

        columnsToReturn = "participantId, CreatedBy, ModifiedBy, container";
        log("Column parameter: '" + columnsToReturn + "'.");

        params.put("query.queryName", LIST_DIFF_DATATYPES);
        params.put("query.columns", columnsToReturn);
        params.put("participantId", participantAppToken);

        rowsResponse = callSelectRows(params);

        log("Validate 1 row was returned.");

        rows = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + ReadResponseTest.participantWithOneRow.getId() + " (" + ReadResponseTest.participantWithOneRow.getAppToken() + ") not as expected.", 1, rows.size());

        log("Validate that the columns have the expected values.");
        expectedValues = new HashMap<>();
        expectedValues.put("participantId", ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("CreatedBy", userId);
        expectedValues.put("ModifiedBy", userId);
        expectedValues.put("container", containerId);

        row = rows.get(0);
        checkJsonMapAgainstExpectedValues(expectedValues, row);

        log("Looks good. Go home.");
        goToHome();
    }

    @Test
    public void validateSelectRowsErrorConditions() throws IOException
    {
        final String errorNoParticipantId = "ParticipantId not included in request";
        goToProjectHome();

        String task = "Call selectRows without a participantId.";
        log(task);
        Map<String, Object> params = new HashMap<>();
        params.put("queryName", LIST_DIFF_DATATYPES);
        params.put("query.columns", "participantId, stringField, multiLineField, booleanField, integerField, doubleField, dateTimeField, flagField");

        try
        {
            callSelectRows(params);
            fail("Should not have succeeded: " + task);
        }
        catch(CommandException ce)
        {
            assertEquals("Command exception did not include expected message. Exception was: " + ce.getMessage(), ce.getMessage(), errorNoParticipantId);
        }

        log("Looks good. Go home.");
        goToHome();
    }

    @Test
    public void validateExecuteSqlBasic() throws CommandException, IOException
    {
        int participantId = ReadResponseTest.participantWithMultipleRow.getId();
        String participantAppToken = ReadResponseTest.participantWithMultipleRow.getAppToken();
        String sql = "select * from TestListDiffDataTypes";

        log("Call the executeSql action with sql: '" + sql + "' and participant " + participantId + " (" + participantAppToken + ").");
        goToProjectHome();

        Map<String, Object> params = new HashMap<>();
        params.put("sql", sql);
        params.put("participantId", participantAppToken);

        CommandResponse rowsResponse = callExecuteSql(params);

        log("Validate 3 rows were returned.");

        List<Map<String, Object>> rows = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + participantId + " (" + participantAppToken + ") not as expected.", 3, rows.size());

        log("Validate the first item returned in the json.");
        Map<String, Object> expectedValues = new HashMap<>();
        expectedValues.put("participantId", participantId);
        expectedValues.put("stringField", FIRST_STRING_FIELD_VALUE + participantId);
        expectedValues.put("multiLineField", FIRST_MULTILINE_STRING_FIELD.replace("$", Integer.toString(participantId)));

        if ((participantId & 1) == 0)
            expectedValues.put("booleanField", true);
        else
            expectedValues.put("booleanField", false);

        expectedValues.put("integerField", participantId + FIRST_INT_OFFSET);
        expectedValues.put("doubleField", BigDecimal.valueOf(Double.parseDouble(participantId + FIRST_MANTISSA)));
        expectedValues.put("dateTimeField", FIRST_DATE);
        expectedValues.put("flagField", FIRST_FLAG_FIELD + participantId);

        String containerId = getContainerId();
        APIUserHelper userHelper = new APIUserHelper(this);
        int userId = userHelper.getUserId(getCurrentUser());

        expectedValues.put("CreatedBy", userId);
        expectedValues.put("user", userId);
        expectedValues.put("ModifiedBy", userId);
        expectedValues.put("container", containerId);

        Map<String, Object> row = rows.get(0);
        Map<String, Object> jsonData = (Map<String, Object>)row.get("data");

        checkJsonMapAgainstExpectedValues(expectedValues, jsonData);

        log("Looks good. Go home.");
        goToHome();
    }

    @Test
    public void validateExecuteSqlNoRows() throws CommandException, IOException
    {
        int participantId = ReadResponseTest.participantToSkip.getId();
        String participantAppToken = ReadResponseTest.participantToSkip.getAppToken();
        String sql = "select * from TestListDiffDataTypes";

        log("Call the executeSql action with sql: '" + sql + "' and participant " + participantId + " (" + participantAppToken + "). This participant has no rows in the list.");
        goToProjectHome();

        Map<String, Object> params = new HashMap<>();
        params.put("sql", sql);
        params.put("participantId", participantAppToken);

        CommandResponse rowsResponse = callExecuteSql(params);

        log("Validate no rows were returned.");

        List<?> rows = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + participantId + " (" + participantAppToken + ") not as expected.", 0, rows.size());

        log("Looks good. Go home.");
        goToHome();
    }

    @Test
    public void validateExecuteSqlJoinAndUnion() throws CommandException, IOException
    {
        goToProjectHome();

        int participantId = ReadResponseTest.participantWithMultipleRow.getId();
        String participantAppToken = ReadResponseTest.participantWithMultipleRow.getAppToken();

        log("First validate with a join clause.");
        String sql = "select SecondSimpleList.integerField, SecondSimpleList.Description, TestListDiffDataTypes.participantId from TestListDiffDataTypes inner join SecondSimpleList on SecondSimpleList.integerField = TestListDiffDataTypes.integerField order by integerField";
        log("Call the executeSql action with sql: '" + sql + "' and participant " + participantId + " (" + participantAppToken + ").");

        Map<String, Object> params = new HashMap<>();
        params.put("sql", sql);
        params.put("participantId", participantAppToken);

        CommandResponse rowsResponse = callExecuteSql(params);

        log("Validate that 2 rows were returned.");

        List<Map<String, Object>> rows = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + participantId + " (" + participantAppToken + ") not as expected.", 2, rows.size());

        log("Validate the first item returned in the json.");
        Map<String, Object> expectedValues = new HashMap<>();
        expectedValues.put("participantId", participantId);
        expectedValues.put("Description", DESCRIPTION_VALUE_SECOND_LIST + (participantId + FIRST_INT_OFFSET));
        expectedValues.put("integerField", participantId + FIRST_INT_OFFSET);

        Map<String, Object> row = rows.get(0);
        Map<String, Object> data = (Map<String, Object>)row.get("data");

        checkJsonMapAgainstExpectedValues(expectedValues, data);

        log("Validate the second item returned in the json.");
        expectedValues = new HashMap<>();
        expectedValues.put("participantId", participantId);
        expectedValues.put("Description", DESCRIPTION_VALUE_SECOND_LIST + (participantId + SECOND_INT_OFFSET));
        expectedValues.put("integerField", participantId + SECOND_INT_OFFSET);

        row = rows.get(1);
        data = (Map<String, Object>)row.get("data");

        checkJsonMapAgainstExpectedValues(expectedValues, data);

        log("Now call executeSql with a union clause.");

        participantId = ReadResponseTest.participantForSql.getId();
        participantAppToken = ReadResponseTest.participantForSql.getAppToken();

        sql = "select participantId, integerField, stringField from TestListDiffDataTypes UNION select participantId, integerField, Description from ThirdList order by integerField";
        log("Call the executeSql action with sql: '" + sql + "' and participant " + participantId + " (" + participantAppToken + ").");
        goToProjectHome();

        params = new HashMap<>();
        params.put("sql", sql);
        params.put("participantId", participantAppToken);

        rowsResponse = callExecuteSql(params);

        log("Validate 2 rows returned.");

        rows = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + participantId + " (" + participantAppToken + ") not as expected.", 2, rows.size());

        log("Validate the first item returned in the json.");
        expectedValues = new HashMap<>();
        expectedValues.put("participantId", participantId);
        expectedValues.put("stringField", DESCRIPTION_VALUE_THIRD_LIST + (participantId + FIRST_INT_OFFSET));
        expectedValues.put("integerField", participantId + FIRST_INT_OFFSET);

        row = rows.get(0);
        data = (Map<String, Object>)row.get("data");

        checkJsonMapAgainstExpectedValues(expectedValues, data);

        log("Validate the second item returned in the json.");
        expectedValues = new HashMap<>();
        expectedValues.put("participantId", participantId);
        expectedValues.put("stringField", FIRST_STRING_FIELD_VALUE + participantId);
        expectedValues.put("integerField", participantId + FIRST_INT_OFFSET);

        row = rows.get(1);
        data = (Map<String, Object>)row.get("data");

        checkJsonMapAgainstExpectedValues(expectedValues, data);

        log("Finally validate a join clause that includes all three lists.");

        participantId = ReadResponseTest.participantWithMultipleRow.getId();
        participantAppToken = ReadResponseTest.participantWithMultipleRow.getAppToken();

        sql = "select TestListDiffDataTypes.participantId, ThirdList.integerField, SecondSimpleList.Description " +
                "from ((TestListDiffDataTypes inner join ThirdList on ThirdList.participantId = TestListDiffDataTypes.participantId) " +
                "inner join SecondSImpleList on TestListDiffDataTypes.integerField = SecondSimpleList.integerField) order by integerField";
        log("Call the executeSql action with sql: '" + sql + "' and participant " + participantId + " (" + participantAppToken + "). This participant has no rows in the list.");
        goToProjectHome();

        params = new HashMap<>();
        params.put("sql", sql);
        params.put("participantId", participantAppToken);

        rowsResponse = callExecuteSql(params);

        log("Again validate that 2 rows are returned.");

        rows = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + participantId + " (" + participantAppToken + ") not as expected.", 2, rows.size());

        log("Validate the first item returned in the json.");
        expectedValues = new HashMap<>();
        expectedValues.put("participantId", participantId);
        expectedValues.put("Description", DESCRIPTION_VALUE_SECOND_LIST + (participantId + FIRST_INT_OFFSET));
        expectedValues.put("integerField", participantId + SECOND_INT_OFFSET);

        row = rows.get(0);
        data = (Map<String, Object>)row.get("data");

        checkJsonMapAgainstExpectedValues(expectedValues, data);

        log("Validate the second item returned in the json.");
        expectedValues = new HashMap<>();
        expectedValues.put("participantId", participantId);
        expectedValues.put("Description", DESCRIPTION_VALUE_SECOND_LIST + (participantId + SECOND_INT_OFFSET));
        expectedValues.put("integerField", participantId + SECOND_INT_OFFSET);

        row = rows.get(1);
        data = (Map<String, Object>)row.get("data");

        checkJsonMapAgainstExpectedValues(expectedValues, data);

        log("Looks good. Go home.");
        goToHome();
    }

    @Test
    public void validateExecuteSqlErrorConditions()
    {
        final String ERROR_NO_PARTICIPANTID = "ParticipantId not included in request";
        final String ERROR_TABLE_NOT_FOUND = "Query or table not found: core.Users";
        final String ERROR_BAD_SQL = "Syntax error near 'never'";

        goToProjectHome();

        int participantId = ReadResponseTest.participantWithMultipleRow.getId();
        String participantAppToken = ReadResponseTest.participantWithMultipleRow.getAppToken();

        testExecuteSql("without a participantId.",
            "select * from TestListDiffDataTypes",
            0, null,
            ERROR_NO_PARTICIPANTID);
        testExecuteSql("looking only at an 'external' table.",
            "select * from core.Users",
            participantId, participantAppToken,
            ERROR_TABLE_NOT_FOUND);
        testExecuteSql("while joining to an 'external' table.",
            "select TestListDiffDataTypes.participantId, TestListDiffDataTypes.user, core.Users.email from TestListDiffDataTypes inner join core.Users on TestListDiffDataTypes.user = core.Users.DisplayName",
            participantId, participantAppToken,
            ERROR_TABLE_NOT_FOUND);
        testExecuteSql("with garbage sql.",
            "select this should never ever ever work!",
            participantId, participantAppToken,
            ERROR_BAD_SQL);

        log("Looks good. Go home.");
        goToHome();
    }

    private void testExecuteSql(String task, String sql, int participantId, @Nullable String participantAppToken, String expectedErrorMessage)
    {
        String details = "Call executeSql with sql: '" + sql + "' and ";
        if (null == participantAppToken)
            testExecuteSql(task, Map.of("sql", sql), details + "no participant parameter.", expectedErrorMessage);
        else
            testExecuteSql(task, Map.of("sql", sql, "participantId", participantAppToken), details + "participant " + participantId + " (" + participantAppToken + ").", expectedErrorMessage);
    }

    private void testExecuteSql(String task, Map<String, Object> params, String detailsToLog, String expectedErrorMessage)
    {
        log("Call executeSql " + task);
        log(detailsToLog);

        try
        {
            // Command requires a mutable map :(
            callExecuteSql(new HashMap<>(params));
            fail("Should not have succeeded: " + task);
        }
        catch(CommandException | IOException ce)
        {
            Assert.assertTrue("Command exception did not include expected message. Exception was: " + ce.getMessage(), ce.getMessage().contains(expectedErrorMessage));
        }
    }

    private void confirmBatchInfoCreated(SetupPage setupPage, String batchId, String expectedTokenCount, String expectedUsedCount)
    {
        Map<String, String> batchData = setupPage.getTokenBatchesWebPart().getBatchData(batchId);

        assertEquals("BatchId not as expected.", batchId, batchData.get("RowId"));
        assertEquals("Expected number of tokens not created.", expectedTokenCount, batchData.get("Count"));
        assertEquals("Number of tokens in use not as expected.", expectedUsedCount, batchData.get("TokensInUse"));
    }

    private SelectRowsResponse getListInfo(String query) throws IOException, CommandException
    {
        Connection cn;
        SelectRowsCommand selectCmd = new SelectRowsCommand("lists", query);
        cn = createDefaultConnection();
        return selectCmd.execute(cn, getProjectName());
    }

    private static class ParticipantInfo
    {
        protected int _participantId;
        protected String _appToken;

        public ParticipantInfo()
        {
            // Do nothing constructor.
        }

        public ParticipantInfo(int participantId, String appToken)
        {
            _participantId = participantId;
            _appToken = appToken;
        }

        public void setId(int id)
        {
            _participantId = id;
        }

        public int getId()
        {
            return _participantId;
        }

        public void setAppToken(String appToken)
        {
            _appToken = appToken;
        }

        public String getAppToken()
        {
            return _appToken;
        }
    }
}
