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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.categories.Git;
import org.labkey.test.commands.response.SubmitResponseCommand;
import org.labkey.test.components.response.MyStudiesResponseServerTab;
import org.labkey.test.pages.response.ResponseQueryPage;
import org.labkey.test.util.ListHelper;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@Category({Git.class})
public class ResponseSubmissionTest extends BaseResponseTest
{
    //Create study
    private final static String STUDY_NAME01 = "Study01";  // Study names are case insensitive
    private final static String STUDY_NAME02 = "Study02";
    private final static String STUDY_NAME03 = "Study03";
    private final static String BASE_PROJECT_NAME = "Response Submission Test Project";
    private final static String PROJECT_NAME01 = BASE_PROJECT_NAME + " " + STUDY_NAME01;
    private final static String PROJECT_NAME02 = BASE_PROJECT_NAME + " " + STUDY_NAME02;
    private final static String PROJECT_NAME03 = BASE_PROJECT_NAME + " " + STUDY_NAME03;
    private final static String SURVEY_NAME = "FakeSurvey_1";

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @BeforeClass
    public static void setupProject()
    {
        ResponseSubmissionTest init = (ResponseSubmissionTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        doCleanup(false);

        setupProject(STUDY_NAME01, PROJECT_NAME01, SURVEY_NAME, false);
        setupProject(STUDY_NAME02, PROJECT_NAME02, SURVEY_NAME, true);
        setSurveyMetadataDropDir();
    }

    @Test
    public void testRequestBodyNotPresent()
    {
        //refresh the page
        goToProjectHome(PROJECT_NAME01);

        checkErrors();
        //Scenarios
        //        1. request body not present
        log("Testing bad request body");
        SubmitResponseCommand cmd = new SubmitResponseCommand();
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.METADATA_MISSING_MESSAGE, cmd.getExceptionMessage());
        checkExpectedErrors(1);
    }

    @Test
    public void testAppToken()
    {
        //refresh the page
        goToProjectHome(PROJECT_NAME01);

        checkErrors();
        int expectedErrorCount = 0;
        //        2. AppToken not present
        log("Testing AppToken not present");
        SubmitResponseCommand cmd = new SubmitResponseCommand(SURVEY_NAME, "1", null, "", BASE_RESULTS);
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.PARTICIPANTID_MISSING_MESSAGE, cmd.getExceptionMessage());
        expectedErrorCount++;

        //        3. Invalid AppToken Participant
        log("Testing invalid apptoken");
        cmd = new SubmitResponseCommand(SURVEY_NAME, "1", null, "INVALIDPARTICIPANTID", BASE_RESULTS);
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.NO_PARTICIPANT_MESSAGE, cmd.getExceptionMessage());
        expectedErrorCount++;

        checkExpectedErrors(expectedErrorCount);
    }

    @Test
    public void testMetadata()
    {
        //refresh the page
        goToProjectHome(PROJECT_NAME01);
        //Capture a participant appToken
        String appToken = getNewAppToken(PROJECT_NAME01, STUDY_NAME01, null);

        checkErrors();
        int expectedErrorCount = 0;

        //        4. Invalid Metadata
        //            A. Metadata element missing
        log("Testing Metadata element not present");
        SubmitResponseCommand cmd = new SubmitResponseCommand(null, null, null, appToken, BASE_RESULTS);
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.METADATA_MISSING_MESSAGE, cmd.getExceptionMessage());
        expectedErrorCount++;

        //            B. Survey Version
        log("Testing SurveyVersion not present");
        cmd = new SubmitResponseCommand(SURVEY_NAME, null, null, appToken, BASE_RESULTS);
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.SURVEYVERSION_MISSING_MESSAGE, cmd.getExceptionMessage());
        expectedErrorCount++;

        //            C. Survey ActivityId
        log("Testing ActivityId not present");
        cmd = new SubmitResponseCommand(null, "1", null, appToken, BASE_RESULTS);
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.ACTIVITYID_MISSING_MESSAGE, cmd.getExceptionMessage());
        expectedErrorCount++;

        checkExpectedErrors(expectedErrorCount);
    }

    @Test
    public void testResponseNotPresent()
    {
        //refresh the page
        goToProjectHome(PROJECT_NAME01);
        String appToken = getNewAppToken(PROJECT_NAME01, STUDY_NAME01, null);
        checkErrors();
        //        5. Response not present
        log("Testing Response element not present");
        SubmitResponseCommand cmd = new SubmitResponseCommand();
        cmd.setBody(String.format(SubmitResponseCommand.MISSING_RESPONSE_JSON_FORMAT, SURVEY_NAME, "1", appToken));
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.RESPONSE_MISSING_MESSAGE, cmd.getExceptionMessage());
        checkExpectedErrors(1);
    }

    @Test
    public void testResponseCollection()
    {
        //refresh the page
        goToProjectHome(PROJECT_NAME01);
        String appToken = getNewAppToken(PROJECT_NAME01, STUDY_NAME01, null);
        MyStudiesResponseServerTab myStudiesResponseServerTab = MyStudiesResponseServerTab.beginAt(this);
        if (myStudiesResponseServerTab.isResponseCollectionChecked())
        {
            myStudiesResponseServerTab.uncheckResponseCollection();
            log("Disabling response collection for " + STUDY_NAME01);
            myStudiesResponseServerTab.saveAndExpectSuccess();
        }

        checkErrors();
        //        6. Study not collecting
        log("Testing Response Submission with Study collection turned off");
        SubmitResponseCommand cmd = new SubmitResponseCommand(SURVEY_NAME, "1", null, appToken, BASE_RESULTS);
        cmd.execute(400);
        assertTrue("Unexpected error message: " + cmd.getExceptionMessage(), String.format(SubmitResponseCommand.COLLECTION_DISABLED_MESSAGE_FORMAT, STUDY_NAME01)
                .equalsIgnoreCase(cmd.getExceptionMessage()));
        checkExpectedErrors(1);

        //        7. Success
        //Enable study collection
        log("Enabling response collection for " + STUDY_NAME01);
        goToProjectHome(PROJECT_NAME01);
        myStudiesResponseServerTab = MyStudiesResponseServerTab.beginAt(this);
        myStudiesResponseServerTab.checkResponseCollection();
        myStudiesResponseServerTab.saveAndExpectSuccess();
        goToProjectHome(PROJECT_NAME01);

        log("Testing Response Submission with Study collection turned on. Also testing language capture.");
        // Language: unspecified, which should result in English (US)
        cmd = new SubmitResponseCommand(SURVEY_NAME, "1", null, appToken, BASE_RESULTS);
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());

        // Language: English (US)
        cmd = new SubmitResponseCommand(SURVEY_NAME, "1", "en", appToken, BASE_RESULTS);
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());

        // Language: Spanish
        cmd = new SubmitResponseCommand(SURVEY_NAME, "1", "es", appToken, BASE_RESULTS);
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());

        // Language: French
        cmd = new SubmitResponseCommand(SURVEY_NAME, "1", "fr", appToken, BASE_RESULTS);
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());

        // Verify responses and language
        ResponseQueryPage responses = new ResponseQueryPage(this);
        responses.assertResponseErrorCounts(appToken, 4, 4);
        assertEquals(List.of("English (US)", "English (US)", "Spanish", "French"), responses.getLanguages());

        goToProjectHome(PROJECT_NAME01);
        log("Disabling response collection for " + STUDY_NAME01);
        myStudiesResponseServerTab = MyStudiesResponseServerTab.beginAt(this);
        myStudiesResponseServerTab.uncheckResponseCollection();
        myStudiesResponseServerTab.saveAndExpectSuccess();
        goToProjectHome(PROJECT_NAME01);

        //        8. Test submitting to a Study previously collecting, but not currently accepting results
        log("Testing Response Submission with Study collection turned off after previously being on");
        cmd = new SubmitResponseCommand(SURVEY_NAME, "1", null, appToken, BASE_RESULTS);
        cmd.execute(400);
        assertTrue("Unexpected error message: " + cmd.getExceptionMessage(), String.format(SubmitResponseCommand.COLLECTION_DISABLED_MESSAGE_FORMAT, STUDY_NAME01)
                .equalsIgnoreCase(cmd.getExceptionMessage()));
        checkExpectedErrors(1);
    }

    @Test
    public void testContainerSubmission()
    {
        //        8. Verify submission is container agnostic
        log("Testing Response Submission is container agnostic");
        goToProjectHome(PROJECT_NAME02);  //Setup Previously
        //Capture a participant appToken
        String appToken = getNewAppToken(PROJECT_NAME02, STUDY_NAME02, null);

        log("Verifying " + STUDY_NAME02 + " collecting responses.");
        SubmitResponseCommand cmd = new SubmitResponseCommand(SURVEY_NAME, "1", null, appToken, BASE_RESULTS);
        log("Testing submission to root container");
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());
        String originalUrl = cmd.getTargetURL();

        //Submit API call using apptoken associated to original study
        log("Submitting from " + PROJECT_NAME02 + " container");
        cmd = new SubmitResponseCommand(SURVEY_NAME, "1", null, appToken, BASE_RESULTS);
        cmd.changeProjectTarget(PROJECT_NAME02);
        assertNotEquals("Attempting to test container agnostic submission and URL targets are the same", originalUrl, cmd.getTargetURL());
        log("Testing submission to matching container");
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());

        //Submit API call using apptoken associated to original study
        log("Submitting from " + PROJECT_NAME01 + " container");
        cmd = new SubmitResponseCommand(SURVEY_NAME, "1", null, appToken, BASE_RESULTS);
        cmd.changeProjectTarget(PROJECT_NAME01);
        assertNotEquals("Attempting to test container agnostic submission and URL targets are the same", originalUrl, cmd.getTargetURL());
        log("Testing submission to appToken/container mismatch");
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());
    }

    @Test
    public void testSubmissionToDeletedProject()
    {
        //Setup a third study that we can delete
        _containerHelper.deleteProject(PROJECT_NAME03, false);
        _containerHelper.createProject(PROJECT_NAME03, FOLDER_TYPE);
        goToProjectHome(PROJECT_NAME03);
        MyStudiesResponseServerTab myStudiesResponseServerTab = MyStudiesResponseServerTab.beginAt(this);
        myStudiesResponseServerTab.checkResponseCollection();
        myStudiesResponseServerTab.setInputId(STUDY_NAME03);
        myStudiesResponseServerTab.validateSaveButtonEnabled();
        myStudiesResponseServerTab.saveAndExpectSuccess();
        _listHelper.createList(PROJECT_NAME03, SURVEY_NAME, ListHelper.ListColumnType.AutoInteger, "Key" );
        setSurveyMetadataDropDir();
        goToProjectHome(PROJECT_NAME03);

        //Capture a participant appToken
        String appToken = getNewAppToken(PROJECT_NAME03, STUDY_NAME03, null);

        log("Verifying Response Submission prior to study deletion");
        SubmitResponseCommand cmd = new SubmitResponseCommand(SURVEY_NAME, "1", null, appToken, BASE_RESULTS);
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());
        log("successful submission to " + STUDY_NAME03);

        _containerHelper.deleteProject(PROJECT_NAME03, false);

        //        9. Check submission to deleted project
        //Submit API call using appToken associated to deleted study
        checkErrors();
        log("Verifying Response Submission after study deletion");
        cmd = new SubmitResponseCommand(SURVEY_NAME, "1", null, appToken, BASE_RESULTS );
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.NO_PARTICIPANT_MESSAGE, cmd.getExceptionMessage());
        checkExpectedErrors(1);
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);

        _containerHelper.deleteProject(PROJECT_NAME01, false);
        _containerHelper.deleteProject(PROJECT_NAME02, false);
    }
}
