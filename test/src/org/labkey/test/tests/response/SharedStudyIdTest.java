/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Git;
import org.labkey.test.commands.response.EnrollParticipantCommand;
import org.labkey.test.commands.response.EnrollmentTokenValidationCommand;
import org.labkey.test.components.response.MyStudiesResponseServerTab;
import org.labkey.test.components.response.TokenBatchPopup;
import org.labkey.test.pages.response.SetupPage;
import org.labkey.test.pages.response.TokenListPage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@Category({Git.class})
public class SharedStudyIdTest extends BaseResponseTest
{
    private static final String CLIENT_1_PROJECT_NAME = "Shared Study Client 1";
    private static final String CLIENT_2_PROJECT_NAME = "Shared Study Client 2";
    private static final String PROJECT_NAME01 = "SharedStudyIdTest DataPartner1";
    private static final String PROJECT_NAME02 = "SharedStudyIdTest DataPartner2";
    private static final String STUDY_ID = "SharedStudy01";
    private static final String STUDY_SUBFOLDER = STUDY_ID + " Subfolder";
    private static final String CLIENT_1_TOKEN_STUDY = CLIENT_1_PROJECT_NAME + "/" + STUDY_SUBFOLDER;
    private static final String CLIENT_2_TOKEN_STUDY = CLIENT_2_PROJECT_NAME + "/" + STUDY_SUBFOLDER;

    private static final String NO_TOKEN_STUDY_ID = "NoTokenSharedStudy01";
    private static final String NO_TOKEN_STUDY_SUBFOLDER = NO_TOKEN_STUDY_ID + " Subfolder";

    @Override
    protected @Nullable String getProjectName()
    {
        return null;
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(CLIENT_1_PROJECT_NAME, false);
        _containerHelper.deleteProject(CLIENT_2_PROJECT_NAME, false);
        _containerHelper.deleteProject(PROJECT_NAME01, false);
        _containerHelper.deleteProject(PROJECT_NAME02, false);
    }

    void setUpProject(String project, String subfolder, String studyId, boolean addTokens)
    {
        if (!_containerHelper.getCreatedProjects().contains(project))
            _containerHelper.createProject(project, null);
        _containerHelper.createSubfolder(project, subfolder, FOLDER_TYPE);

        String projectPath = project + "/" + subfolder;

        MyStudiesResponseServerTab myStudiesResponseServerTab = MyStudiesResponseServerTab.beginAt(this);
        myStudiesResponseServerTab.setInputId(studyId);
        myStudiesResponseServerTab.saveAndExpectSuccess();

        SetupPage setupPage = SetupPage.beginAt(this, projectPath);
        if (addTokens)
        {
            log("Creating 10 tokens for " + projectPath);
            TokenBatchPopup tokenBatchPopup = setupPage.getTokenBatchesWebPart().openNewBatchPopup();
            tokenBatchPopup.selectOtherBatchSize();
            tokenBatchPopup.setOtherBatchSize("10");
            tokenBatchPopup.createNewBatch();
        }
    }

    @BeforeClass
    public static void setupProject()
    {
        SharedStudyIdTest init = (SharedStudyIdTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.deleteProject(CLIENT_1_PROJECT_NAME, false);
        _containerHelper.deleteProject(CLIENT_2_PROJECT_NAME, false);
        setUpProject(CLIENT_1_PROJECT_NAME, STUDY_SUBFOLDER,  STUDY_ID,true);
        setUpProject(CLIENT_1_PROJECT_NAME, NO_TOKEN_STUDY_SUBFOLDER, NO_TOKEN_STUDY_ID, false);
        setUpProject(CLIENT_2_PROJECT_NAME, STUDY_SUBFOLDER, STUDY_ID, true);
        setUpProject(CLIENT_2_PROJECT_NAME, NO_TOKEN_STUDY_SUBFOLDER, NO_TOKEN_STUDY_ID, false);
    }

    @Test
    public void testStudyNameShared()
    {
        final String STUDY_FOLDER_NAME = "StudyFolder";
        final String SHORT_NAME = "Shared";

        _containerHelper.createProject(PROJECT_NAME01, "Collaboration");
        _containerHelper.createSubfolder(PROJECT_NAME01, STUDY_FOLDER_NAME,FOLDER_TYPE);

        MyStudiesResponseServerTab myStudiesResponseServerTab = MyStudiesResponseServerTab.beginAt(this);
        myStudiesResponseServerTab.setInputId(SHORT_NAME);
        myStudiesResponseServerTab.saveAndExpectSuccess();

        _containerHelper.createProject(PROJECT_NAME02, "Collaboration");
        _containerHelper.createSubfolder(PROJECT_NAME02, STUDY_FOLDER_NAME, FOLDER_TYPE);

        myStudiesResponseServerTab = MyStudiesResponseServerTab.beginAt(this);
        myStudiesResponseServerTab.setInputId(SHORT_NAME);
        myStudiesResponseServerTab.saveAndExpectSuccess();
        goToProjectHome(PROJECT_NAME02);
        clickFolder(STUDY_FOLDER_NAME);
        myStudiesResponseServerTab = MyStudiesResponseServerTab.beginAt(this);
        assertEquals("Study name not saved for second project", SHORT_NAME.toUpperCase(), myStudiesResponseServerTab.getInputId());

        log("Testing enrollment, which should fail without any tokens.");
        EnrollParticipantCommand enrollCmd = new EnrollParticipantCommand("home", SHORT_NAME, null, "NA");
        enrollCmd.execute(400);
        assertFalse("Enrollment should fail when two projects share a study id but have no enrollment tokens", enrollCmd.getSuccess());
    }


    @Test
    public void testValidateWithTokens()
    {
        // test validation for a token from each study
        // ensure that you get the appropriate errors when trying to enroll when a study id is shared but there are no tokens
        TokenListPage tokenListPage = TokenListPage.beginAt(this, CLIENT_1_TOKEN_STUDY);
        String token = tokenListPage.getToken(0);

        EnrollmentTokenValidationCommand cmd = new EnrollmentTokenValidationCommand("home", STUDY_ID, token);
        cmd.execute(200);
        assertTrue("Enrollment token validation for " + CLIENT_1_TOKEN_STUDY + " failed when it shouldn't have", cmd.getSuccess());

        tokenListPage = TokenListPage.beginAt(this, CLIENT_2_TOKEN_STUDY);
        token = tokenListPage.getToken(0);
        cmd = new EnrollmentTokenValidationCommand("home", STUDY_ID, token);
        cmd.execute(200);
        assertTrue("Enrollment token validation for " + CLIENT_2_TOKEN_STUDY + " failed when it shouldn't have", cmd.getSuccess());
    }

    @Test
    public void testEnrollWithTokens()
    {
        // test enrollment for a token from each study
        // then check validation using the same tokens (should see an error about token already in use, but no exception)
        TokenListPage tokenListPage = TokenListPage.beginAt(this, CLIENT_1_TOKEN_STUDY);
        String token = tokenListPage.getToken(0);

        EnrollParticipantCommand enrollCmd = new EnrollParticipantCommand("home", STUDY_ID, token, "true");
        enrollCmd.execute(200);
        assertTrue("Enrollment with token '" + token + "' for " + CLIENT_1_TOKEN_STUDY + " failed when it shouldn't have", enrollCmd.getSuccess());
        EnrollmentTokenValidationCommand validateCmd = new EnrollmentTokenValidationCommand("home", STUDY_ID, token);
        validateCmd.execute(400);
        assertFalse("Enrollment token validation for " + CLIENT_1_TOKEN_STUDY + " with token '" + token + "' should fail after enrollment succeeds", validateCmd.getSuccess());

        tokenListPage = TokenListPage.beginAt(this, CLIENT_2_TOKEN_STUDY);
        token = tokenListPage.getToken(0);
        enrollCmd = new EnrollParticipantCommand("home", STUDY_ID, token, "false");
        enrollCmd.execute(200);
        assertTrue("Enrollment with token '" + token + "' for  " + CLIENT_2_TOKEN_STUDY + " failed when it shouldn't have", enrollCmd.getSuccess());
        validateCmd = new EnrollmentTokenValidationCommand("home", STUDY_ID, token);
        validateCmd.execute(400);
        assertFalse("Enrollment token validation for " + CLIENT_2_TOKEN_STUDY + " with token '" + token + "' should fail after enrollment succeeds", validateCmd.getSuccess());
    }

    @Test
    // test validation of the "allowDataSharing" parameter at enrollment time
    public void testAllowDataSharingValidation() throws IOException, CommandException
    {
        TokenListPage tokenListPage = TokenListPage.beginAt(this, CLIENT_1_TOKEN_STUDY);
        String token1 = tokenListPage.getToken(1);
        String token2 = tokenListPage.getToken(2);
        String token3 = tokenListPage.getToken(3);

        // test null, blank, and invalid values - all should fail
        EnrollParticipantCommand enrollCmd = new EnrollParticipantCommand("home", STUDY_ID, token1, null);
        testRequired(enrollCmd, null);
        testRequired(enrollCmd, "");
        testRequired(enrollCmd, "%20%20%20");
        testInvalid(enrollCmd, "na");
        testInvalid(enrollCmd, "n/a");
        testInvalid(enrollCmd, "N/A");
        testInvalid(enrollCmd, "TRUE");
        testInvalid(enrollCmd, "True");
        testInvalid(enrollCmd, "T");
        testInvalid(enrollCmd, "t");
        testInvalid(enrollCmd, "yes");
        testInvalid(enrollCmd, "YES");
        testInvalid(enrollCmd, "1");
        testInvalid(enrollCmd, "FALSE");
        testInvalid(enrollCmd, "False");
        testInvalid(enrollCmd, "F");
        testInvalid(enrollCmd, "f");
        testInvalid(enrollCmd, "no");
        testInvalid(enrollCmd, "NO");
        testInvalid(enrollCmd, "0");
        testInvalid(enrollCmd, "Wombat");
        testInvalid(enrollCmd, "Mazipan");

        // test the three valid values - all should succeed
        testValid(enrollCmd, token1, "true");
        testValid(enrollCmd, token2, "false");
        testValid(enrollCmd, token3, "NA");
    }

    private void testValid(EnrollParticipantCommand enrollCmd, String token, String allowDataSharing) throws IOException, CommandException
    {
        enrollCmd.setBatchToken(token);
        enrollCmd.setAllowDataSharing(allowDataSharing);
        enrollCmd.execute(200);
        assertTrue("Enrollment with token '" + token + "' for " + CLIENT_1_TOKEN_STUDY + " failed when it shouldn't have", enrollCmd.getSuccess());

        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("mobileappstudy", "Participant");
        cmd.setColumns(List.of("AllowDataSharing", "Token"));
        cmd.addFilter("AppToken", enrollCmd.getAppToken(), Filter.Operator.EQUAL);
        SelectRowsResponse resp = cmd.execute(cn, CLIENT_1_TOKEN_STUDY);

        // Ensure that expected AllowDataSharing and Token values show up in the Participant table
        Map<String, Object> map = resp.getRows().get(0);
        assertEquals(allowDataSharing, map.get("AllowDataSharing"));
        assertEquals(token, map.get("Token"));
    }

    private void testRequired(EnrollParticipantCommand enrollCmd, String allowDataSharing)
    {
        test(enrollCmd, allowDataSharing, "allowDataSharing is required");
    }

    private void testInvalid(EnrollParticipantCommand enrollCmd, String allowDataSharing)
    {
        test(enrollCmd, allowDataSharing, "Invalid allowDataSharing value: '" + allowDataSharing + "'");
    }

    private void test(EnrollParticipantCommand enrollCmd, String allowDataSharing, String expectedMessage)
    {
        enrollCmd.setAllowDataSharing(allowDataSharing);
        enrollCmd.execute(400);
        assertEquals(expectedMessage, enrollCmd.getExceptionMessage());
    }
}
