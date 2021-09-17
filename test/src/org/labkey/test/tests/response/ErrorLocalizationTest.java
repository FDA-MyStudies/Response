package org.labkey.test.tests.response;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Git;
import org.labkey.test.commands.response.EnrollParticipantCommand;
import org.labkey.test.components.response.MyStudiesResponseServerTab;
import org.labkey.test.pages.response.SetupPage;

import java.util.Arrays;
import java.util.List;

import static org.labkey.test.commands.response.EnrollmentTokenValidationCommand.TOKEN_REQUIRED;

@Category({Git.class})
public class ErrorLocalizationTest extends BaseResponseTest
{
    private static final String STUDY_ID = "ErrorLocalizationTest";

    private static final String NO_TOKEN_EN = TOKEN_REQUIRED;
    private static final String NO_TOKEN_SP = "Se requiere un token";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        ErrorLocalizationTest init = (ErrorLocalizationTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), FOLDER_TYPE);
        MyStudiesResponseServerTab myStudiesResponseServerTab = MyStudiesResponseServerTab.beginAt(this, getProjectName());
        myStudiesResponseServerTab.setInputId(STUDY_ID);
        myStudiesResponseServerTab.saveAndExpectSuccess();
        SetupPage.beginAt(this, getProjectName())
                .getTokenBatchesWebPart()
                .openNewBatchPopup()
                .createNewBatch("10");
    }

    @Test
    public void testEnrollWithNoToken()
    {
        EnrollParticipantCommand enrollCmd = new EnrollParticipantCommand(getProjectName(), STUDY_ID, null, "NA");
        enrollCmd.execute(400);
        checker().verifyEquals("Error message with no language specified.", NO_TOKEN_EN, enrollCmd.getExceptionMessage());

        enrollCmd = new EnrollParticipantCommand(getProjectName(), STUDY_ID, null, "NA");
        enrollCmd.setLanguage("en");
        enrollCmd.execute(400);
        checker().verifyEquals("Error message with english specified.", NO_TOKEN_EN, enrollCmd.getExceptionMessage());

        enrollCmd = new EnrollParticipantCommand(getProjectName(), STUDY_ID, null, "NA");
        enrollCmd.setLanguage("es");
        enrollCmd.execute(400);
        checker().verifyEquals("Error message with spanish specified.", NO_TOKEN_SP, enrollCmd.getExceptionMessage());

        enrollCmd = new EnrollParticipantCommand(getProjectName(), STUDY_ID, null, "NA");
        enrollCmd.setLanguage("fr");
        enrollCmd.execute(400);
        checker().verifyEquals("Error message with unsupported language specified.", NO_TOKEN_EN, enrollCmd.getExceptionMessage());

        enrollCmd = new EnrollParticipantCommand(getProjectName(), STUDY_ID, null, "NA");
        enrollCmd.setLanguage("xyz");
        enrollCmd.execute(400);
        checker().verifyEquals("Error message with nonexistent language specified.", NO_TOKEN_EN, enrollCmd.getExceptionMessage());

    }

    @Test
    public void testValidateEnrollmentToken()
    {
        // Test code
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "ErrorLocalizationTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("Response");
    }
}
