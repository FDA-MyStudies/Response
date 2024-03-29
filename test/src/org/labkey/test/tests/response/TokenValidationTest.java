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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.categories.Git;
import org.labkey.test.commands.response.EnrollParticipantCommand;
import org.labkey.test.commands.response.EnrollmentTokenValidationCommand;
import org.labkey.test.commands.response.ResolveEnrollmentTokenCommand;
import org.labkey.test.components.response.MyStudiesResponseServerTab;
import org.labkey.test.components.response.TokenBatchPopup;
import org.labkey.test.components.response.TokenBatchesWebPart;
import org.labkey.test.pages.response.SetupPage;
import org.labkey.test.pages.response.TokenListPage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test case to exercise the MobileAppStudy ValidateEnrollmentTokenAction and ResolveEnrollmentTokenAction
 */
@Category({Git.class})
public class TokenValidationTest extends BaseResponseTest
{
    static final String PROJECT_NAME01 = "TokenValidationTest Project 1";
    static final String STUDY_NAME01 = "TOKENVALIDATION";

    static final String PROJECT_NAME02 = "TokenValidationTest Project 2";
    static final String STUDY_NAME02 = "BLANKTOKENVALIDATION";

    static final String PROJECT_NAME03 = "TokenValidationTest Project 3";
    static final String STUDY_NAME03 = "RESOLVEVALIDATION";

    @BeforeClass
    public static void setupProject()
    {
        TokenValidationTest init = (TokenValidationTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.deleteProject(PROJECT_NAME01, false);
        _containerHelper.createProject(PROJECT_NAME01, FOLDER_TYPE);
        _containerHelper.deleteProject(PROJECT_NAME02, false);
        _containerHelper.createProject(PROJECT_NAME02, FOLDER_TYPE);
        _containerHelper.deleteProject(PROJECT_NAME03, false);
        _containerHelper.createProject(PROJECT_NAME03, FOLDER_TYPE);

        goToProjectHome(PROJECT_NAME01);
        SetupPage setupPage = new SetupPage(this);
        MyStudiesResponseServerTab myStudiesResponseServerTab = MyStudiesResponseServerTab.beginAt(this);
        myStudiesResponseServerTab.setInputId(STUDY_NAME01);
        myStudiesResponseServerTab.validateSaveButtonEnabled();
        myStudiesResponseServerTab.saveAndExpectSuccess();

        log("Create tokens.");
        SetupPage.beginAt(this, PROJECT_NAME01);
        TokenBatchPopup tokenBatchPopup = setupPage.getTokenBatchesWebPart().openNewBatchPopup();
        tokenBatchPopup.createNewBatch("100");

        goToProjectHome(PROJECT_NAME02);
        myStudiesResponseServerTab = MyStudiesResponseServerTab.beginAt(this);
        myStudiesResponseServerTab.setInputId(STUDY_NAME02);
        myStudiesResponseServerTab.validateSaveButtonEnabled();
        myStudiesResponseServerTab.saveAndExpectSuccess();

        // Third project to test resolving enrollment tokens in another study
        goToProjectHome(PROJECT_NAME03);
        myStudiesResponseServerTab = MyStudiesResponseServerTab.beginAt(this);
        myStudiesResponseServerTab.setInputId(STUDY_NAME03);
        myStudiesResponseServerTab.validateSaveButtonEnabled();
        myStudiesResponseServerTab.saveAndExpectSuccess();

        log("Create tokens.");
        SetupPage.beginAt(this, PROJECT_NAME03);
        tokenBatchPopup = setupPage.getTokenBatchesWebPart().openNewBatchPopup();
        tokenBatchPopup.createNewBatch("100");
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Test
    public void testSuccess()
    {
        TokenListPage tokenListPage = TokenListPage.beginAt(this, PROJECT_NAME01);
        String token = tokenListPage.getToken(0);

        log("Token validation action: successful token request");
        EnrollmentTokenValidationCommand cmd = new EnrollmentTokenValidationCommand(PROJECT_NAME01, STUDY_NAME01, token);
        cmd.execute(200);
        assertTrue("Enrollment token validation failed when it shouldn't have", cmd.getSuccess());
    }

    @Test
    public void testInvalidStudy()
    {
        TokenListPage tokenListPage = TokenListPage.beginAt(this, PROJECT_NAME01);
        String token = tokenListPage.getToken(0);

        log("Token validation action: invalid StudyId");
        EnrollmentTokenValidationCommand cmd = new EnrollmentTokenValidationCommand(PROJECT_NAME01, STUDY_NAME01 + "_INVALIDSTUDYNAME", token);
        cmd.execute(400);
        assertFalse("Enrollment token validation succeeded with an invalid studyId", cmd.getSuccess());
        assertEquals("Unexpected error message", String.format(EnrollmentTokenValidationCommand.INVALID_STUDYID_FORMAT, STUDY_NAME01 + "_INVALIDSTUDYNAME"), cmd.getExceptionMessage());
    }

    @Test
    public void testNoStudy()
    {
        TokenListPage tokenListPage = TokenListPage.beginAt(this, PROJECT_NAME01);
        String token = tokenListPage.getToken(0);

        log("Token validation action: no StudyId");
        EnrollmentTokenValidationCommand cmd = new EnrollmentTokenValidationCommand(PROJECT_NAME01, null, token);
        cmd.execute(400);
        assertFalse("Enrollment token validation succeeded without the studyId", cmd.getSuccess());
        assertEquals("Unexpected error message", EnrollmentTokenValidationCommand.BLANK_STUDYID, cmd.getExceptionMessage());
    }

    @Test
    public void testInvalidToken()
    {
        TokenListPage tokenListPage = TokenListPage.beginAt(this, PROJECT_NAME01);
        String token = tokenListPage.getToken(0);

        log("Token validation action: Invalid Token");
        EnrollmentTokenValidationCommand cmd = new EnrollmentTokenValidationCommand(PROJECT_NAME01, STUDY_NAME01, token + "thisIsAnInvalidToken");
        cmd.execute(400);
        assertFalse("Enrollment token validation succeeded when it shouldn't have", cmd.getSuccess());
        assertEquals("Unexpected error message", String.format(EnrollmentTokenValidationCommand.INVALID_TOKEN_FORMAT, token + "thisIsAnInvalidToken".toUpperCase()), cmd.getExceptionMessage());
    }

    @Test
    public void testBlankTokenWBatch()
    {
        log("Token validation action: Invalid Blank Token");
        EnrollmentTokenValidationCommand cmd = new EnrollmentTokenValidationCommand(PROJECT_NAME01, STUDY_NAME01, null);
        cmd.execute(400);
        assertFalse("Enrollment token validation succeeded when it shouldn't have", cmd.getSuccess());
        assertEquals("Unexpected error message", EnrollmentTokenValidationCommand.TOKEN_REQUIRED, cmd.getExceptionMessage());
    }

    @Test
    public void testBlankTokenNoBatch()
    {
        //Note: This uses the secondary project because once a batch is created blank tokens are no longer accepted
        log("Token validation action: successful blank token request");
        EnrollmentTokenValidationCommand cmd = new EnrollmentTokenValidationCommand(PROJECT_NAME02, STUDY_NAME02, null);
        cmd.execute(200);
        assertTrue("Blank Enrollment token validation failed when it shouldn't have", cmd.getSuccess());
    }

    @Test
    // test the ability to resolve enrollment tokens
    public void testResolveEnrollmentToken()
    {
        // Always resolve from the /Home project... folder shouldn't matter
        ResolveEnrollmentTokenCommand resolveCmd = new ResolveEnrollmentTokenCommand("home", null);

        log("Attempt resolving without specifying enrollment token");
        testInvalid(resolveCmd, null, EnrollmentTokenValidationCommand.TOKEN_REQUIRED);
        testInvalid(resolveCmd, "", EnrollmentTokenValidationCommand.TOKEN_REQUIRED);

        log("Attempt resolving a couple invalid tokens");
        String badToken = "ABCDEFGH"; // Wrong format - too short
        testInvalid(resolveCmd, badToken, String.format(EnrollmentTokenValidationCommand.INVALID_TOKEN_FORMAT, badToken));
        badToken = "ABCDEFGHZ"; // Wrong format - bad checksum
        testInvalid(resolveCmd, badToken, String.format(EnrollmentTokenValidationCommand.INVALID_TOKEN_FORMAT, badToken));

        log("Attempt resolving a non-existent token");
        badToken = "ABCDEFGHI"; // Valid token, but doesn't exist
        resolveCmd.setBatchToken(badToken);
        resolveCmd.execute(404);
        assertFalse(resolveCmd.getSuccess());
        assertNull(resolveCmd.getStudyId());
        assertEquals("Token is not associated with a study ID", resolveCmd.getMessage());

        TokenListPage tokenListPage = TokenListPage.beginAt(this, PROJECT_NAME01);
        String goodToken1 = tokenListPage.getToken(1);

        log("Resolve an existing token twice");
        // Resolve multiple times
        resolveCmd.setBatchToken(goodToken1);
        resolveCmd.execute(200);
        assertTrue(resolveCmd.getSuccess());
        assertEquals(STUDY_NAME01, resolveCmd.getStudyId());
        resolveCmd.execute(200);
        assertTrue(resolveCmd.getSuccess());
        assertEquals(STUDY_NAME01, resolveCmd.getStudyId());

        log("Enroll and resolve again");
        EnrollParticipantCommand enrollCmd = new EnrollParticipantCommand(PROJECT_NAME01, STUDY_NAME01, goodToken1, "true");
        enrollCmd.execute(200);
        resolveCmd.execute(200);
        assertTrue(resolveCmd.getSuccess());
        assertEquals(STUDY_NAME01, resolveCmd.getStudyId());

        log("Resolve token from a second study");
        tokenListPage = TokenListPage.beginAt(this, PROJECT_NAME03);
        String goodToken2 = tokenListPage.getToken(0);
        resolveCmd.setBatchToken(goodToken2);
        resolveCmd.execute(200);
        assertTrue(resolveCmd.getSuccess());
        assertEquals(STUDY_NAME03, resolveCmd.getStudyId());
    }

    @Test
    public void testMyStudiesCoordinatorRole()
    {
        goToProjectHome(PROJECT_NAME01);
        SetupPage setupPage = new SetupPage(this);
        TokenBatchesWebPart batchesWebPart = setupPage.getTokenBatchesWebPart();

        // Test for Administrator
        assertTrue(batchesWebPart.isNewBatchPresent());
        assertTrue(batchesWebPart.isNewBatchEnabled());
        MyStudiesResponseServerTab myStudiesResponseServerTab = MyStudiesResponseServerTab.beginAt(this);
        myStudiesResponseServerTab.validateSaveButtonDisabled();  // Save button should be present
        SetupPage.beginAt(this, PROJECT_NAME01);

        // Test for Reader
        impersonateRole("Reader");
        assertTrue(batchesWebPart.isNewBatchPresent());
        assertFalse(batchesWebPart.isNewBatchEnabled());
        MyStudiesResponseServerTab.beginAt(this);
        assertTextPresent("User does not have permission to perform this operation.");  // Not authorized
        stopImpersonating(false);

        // Test for MyStudies Coordinator
        impersonateRoles("Reader", "MyStudies Coordinator");
        assertTrue(batchesWebPart.isNewBatchPresent());
        assertTrue(batchesWebPart.isNewBatchEnabled());  // Should be able to create a new batch
        TokenBatchPopup tokenBatchPopup = batchesWebPart.openNewBatchPopup();
        TokenListPage tokenListPage = tokenBatchPopup.createNewBatch("100");
        assertEquals("Wrong number of tokens generated for MyStudies Coordinator", 100, tokenListPage.getNumTokens());
        stopImpersonating();
    }

    private void testInvalid(ResolveEnrollmentTokenCommand resolveCmd, String token, String expectedErrorMessage)
    {
        resolveCmd.setBatchToken(token);
        resolveCmd.execute(400);
        assertEquals(expectedErrorMessage, resolveCmd.getExceptionMessage());
    }
}
