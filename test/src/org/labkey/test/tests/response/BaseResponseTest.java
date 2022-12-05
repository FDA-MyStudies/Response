/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.GuestCredentialsProvider;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.commands.response.EnrollParticipantCommand;
import org.labkey.test.commands.response.SubmitResponseCommand;
import org.labkey.test.components.response.MyStudiesResponseServerTab;
import org.labkey.test.data.response.InitialSurvey;
import org.labkey.test.data.response.QuestionResponse;
import org.labkey.test.data.response.Survey;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PostgresOnlyTest;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpClassCallback;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockserver.model.HttpRequest.request;

/**
 * Created by iansigmon on 12/9/16.
 */
public abstract class BaseResponseTest extends BaseWebDriverTest implements PostgresOnlyTest
{
    protected static final String MOBILEAPP_SCHEMA = "mobileappstudy";
    protected static final String LIST_SCHEMA = "lists";
    protected static final String FOLDER_TYPE = "MyStudies Response";
    protected final static String BASE_RESULTS = "{\n" +
            "\t\t\"start\": \"2016-09-06T15:48:13.000+0000\",\n" +
            "\t\t\"end\": \"2016-09-06T15:48:45.000+0000\",\n" +
            "\t\t\"results\": []\n" +
            "}";

    @Override
    protected @Nullable String getProjectName()
    {
        return null;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.singletonList("Response");
    }

    /**
     * Get apptoken associated to a participant and study via the API
     * @param project study container folder
     * @param studyShortName study parameter
     * @param batchToken get
     * @return the appToken string
     */
    String getNewAppToken(String project, String studyShortName, String batchToken)
    {
        log("Requesting app token for project [" + project +"] and study [" + studyShortName + "]");
        EnrollParticipantCommand cmd = new EnrollParticipantCommand(project, studyShortName, batchToken, "true");

        cmd.execute(200);
        String appToken = cmd.getAppToken();
        assertNotNull("AppToken was null", appToken);
        log("AppToken received: " + appToken);

        return appToken;
    }

    protected boolean mobileAppTableExists(String table, String schema) throws CommandException, IOException
    {
        Connection cn = createDefaultConnection();
        SelectRowsCommand selectCmd = new SelectRowsCommand(schema, table);
        selectCmd.setColumns(Arrays.asList("*"));

        try
        {
            selectCmd.execute(cn, getCurrentContainerPath());
            return true;
        }
        catch (CommandException e)
        {
            if (e.getStatusCode() == 404)
                return false;
            else
                throw e;
        }
    }

    protected SelectRowsResponse getMobileAppData(String table, String schema)
    {
        Connection cn = createDefaultConnection();
        SelectRowsCommand selectCmd = new SelectRowsCommand(schema, table);
        selectCmd.setColumns(Arrays.asList("*"));

        SelectRowsResponse selectResp;
        try
        {
            selectResp = selectCmd.execute(cn, getCurrentContainerPath());
        }
        catch (CommandException | IOException e)
        {
            log(e.getMessage());
            throw new RuntimeException(e);
        }

        return selectResp;
    }

    protected SelectRowsResponse getMobileAppDataWithRetry(String table, String schema)
    {
        int waitTime = 1000;
        while (waitTime < 45000)
        {
            try
            {
                return getMobileAppData(table, schema);
            }
            catch (RuntimeException e)
            {
                if (waitTime > 30000)
                    throw e;

                log("Waiting " + waitTime + " before retrying");
                sleep(waitTime);
                waitTime *= 2;
            }
        }

        return null;
    }

    private void goToResponseServerConfiguration()
    {
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("Response Server Configuration"));
    }

    protected void setResponseServerConfigurations(LinkedHashMap<String, String> props)
    {
        log("setting response server configuration");
        goToResponseServerConfiguration();

        for (String prop: props.keySet())
        {
            String val = props.get(prop);
            log("setting property: " + prop + " to value: " + val);

            if (prop.equals("metadataLoadLocation"))
                checkRadioButton(Locator.radioButtonByNameAndValue("metadataLoadLocation", val));
            else
                setFormElement(Locator.name(prop), val);
        }
        clickButton("Save and Finish");

        assertElementNotPresent(Locators.labkeyError);
    }

    @LogMethod
    protected void assignTokens(List<String> tokensToAssign, String projectName, String studyName)
    {
        Connection connection = createGuestConnection();  // No credentials, just the token -- mimic the mobile app
        for(String token : tokensToAssign)
        {
            try
            {
                CommandResponse response = assignToken(connection, token, projectName, studyName);
                assertEquals(true, response.getProperty("success"));
                log("Token assigned.");
            }
            catch (IOException | CommandException e)
            {
                throw new RuntimeException("Failed to assign token", e);
            }
        }
    }

    @LogMethod
    protected CommandResponse assignToken(Connection connection, @LoggedParam String token, @LoggedParam String projectName, @LoggedParam String studyName) throws IOException, CommandException
    {
        Command<?> command = new PostCommand<>("mobileappstudy", "enroll");
        HashMap<String, Object> params = new HashMap<>(Maps.of("shortName", studyName, "token", token, "allowDataSharing", "true"));
        command.setParameters(params);
        log("Assigning token: " + token);
        return command.execute(connection, projectName);
    }

    /**
     * Wrap question response and submit to server via the API
     *
     * @param qr to send to server
     * @param appToken to use in request
     * @return error message of request if there is one.
     */
    protected String submitQuestion(QuestionResponse qr, String appToken, String surveyName, String surveyVersion, int expectedStatusCode)
    {
        Survey survey = new InitialSurvey(appToken, surveyName, surveyVersion, new Date(), new Date());
        survey.addResponse(qr);

        return submitSurvey(survey, expectedStatusCode);
    }

    /**
     * Submit the survey to server via API
     *
     * @param survey to submit
     * @param expectedStatusCode status code to expect from server
     * @return error message from response (if it exists)
     */
    protected String submitSurvey(Survey survey, int expectedStatusCode)
    {
        SubmitResponseCommand cmd = new SubmitResponseCommand(this::log, survey);
        cmd.execute(expectedStatusCode);

        return cmd.getExceptionMessage();
    }

    protected void setSurveyMetadataDropDir()
    {
        LinkedHashMap<String, String> props = new LinkedHashMap<>()
        {{
            put("metadataLoadLocation", "file");
            put("metadataDirectory", TestFileUtils.getSampleData("SurveyMetadata").getAbsolutePath());
        }};

        setResponseServerConfigurations(props);
    }

    protected void setupProject(String studyName, String projectName, String surveyName, boolean enableResponseCollection)
    {
        _containerHelper.createProject(projectName, FOLDER_TYPE);
        log("Set a study name.");
        goToProjectHome(projectName);

        MyStudiesResponseServerTab myStudiesResponseServerTab = MyStudiesResponseServerTab.beginAt(this);
        myStudiesResponseServerTab.setInputId(studyName);
        if (enableResponseCollection)
            myStudiesResponseServerTab.checkResponseCollection();
        myStudiesResponseServerTab.validateSaveButtonEnabled();
        myStudiesResponseServerTab.saveAndExpectSuccess();
        if (StringUtils.isNotBlank(surveyName))
            _listHelper.createList(projectName, surveyName, ListHelper.ListColumnType.AutoInteger, "Key");
    }

    /**
     * Adds a Request matcher to the mockserver
     * @param mockServer to add matcher to
     * @param requestPath to add matcher for
     * @param log logging method
     * @param method HTTP request type, e.g., GET, POST, etc.
     * @param matcher Fully qualified class name String to request handler that implements ExpectationResponseCallback
     */
    protected static void addRequestMatcher(ClientAndServer mockServer, String requestPath, Consumer<String> log, String method, String matcher )
    {
        log.accept(String.format("Adding a response for %1$s requests.", requestPath));
        mockServer.when(
                request()
                        .withMethod(method)
                        .withPath("/" + requestPath)
        ).respond(HttpClassCallback.callback(matcher));
    }

    protected CommandResponse callSelectRows(Map<String, Object> params) throws IOException, CommandException
    {
        return callCommand("selectRows", params);
    }

    protected CommandResponse callExecuteSql(Map<String, Object> params) throws IOException, CommandException
    {
        return callCommand("executeSql", params);
    }

    protected CommandResponse callCommand(String action, Map<String, Object> params)  throws IOException, CommandException
    {
        Command<?> selectCmd = new Command<>("mobileAppStudy", action);
        selectCmd.setParameters(params);

        return selectCmd.execute(createGuestConnection(), getProjectName());
    }

    /**
     * Returns a remoteapi Connection that uses no credentials and no cookies -- to mimic the mobile app that uses
     * participantId or enrollment token to authenticate
     */
    protected Connection createGuestConnection()
    {
        return new Connection(WebTestHelper.getBaseURL(), new GuestCredentialsProvider());
    }

    protected void checkJsonMapAgainstExpectedValues(Map<String, Object> expectedValues, Map<String, Object> actualValues)
    {
        List<String> ignoredColumns = List.of(
                "Key",
                "Created",
                "Modified",
                "lastIndexed",
                "diImportHash",
                "EntityId",
                "_labkeyurl_user");

        Map<String, Object> normalizedValues = new HashMap<>();
        for (String column : actualValues.keySet())
        {
            if (expectedValues.containsKey(column) || !ignoredColumns.contains(column))
            {
                Object value;
                if (actualValues.get(column) instanceof Map columnMap)
                {
                    // Need to do this if the object that is being compared came from an executeSql call.
                    value = columnMap.get("value");
                }
                else
                {
                    value = actualValues.get(column);
                }
                normalizedValues.put(column, value);
            }
        }
        Assertions.assertThat(normalizedValues).as("Row data").containsExactlyInAnyOrderEntriesOf(expectedValues);
    }

    protected String getResponseFromFile(String dir, String filename)
    {
        return TestFileUtils.getFileContents(TestFileUtils.getSampleData(String.join("/", dir, filename)));
    }
}
