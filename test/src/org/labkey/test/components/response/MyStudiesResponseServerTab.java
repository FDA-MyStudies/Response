package org.labkey.test.components.response;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.Error;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.admin.FolderManagementTab;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MyStudiesResponseServerTab extends LabKeyPage<MyStudiesResponseServerTab.ElementCache> implements FolderManagementTab
{
    private WebDriver _driver;

    public MyStudiesResponseServerTab() {}

    public MyStudiesResponseServerTab(BaseWebDriverTest driver)
    {
        super(driver);
    }

    public static MyStudiesResponseServerTab beginAt(BaseWebDriverTest driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static MyStudiesResponseServerTab beginAt(BaseWebDriverTest driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("mobileappstudy", containerPath, "forwardingSettings"));
        return new MyStudiesResponseServerTab(driver);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    public void setDriver(WebDriver driver)
    {
        _driver = driver;
    }

    @Override
    public String getTabId()
    {
        return "forwarding";
    }

    public void setOauthCredentials(String tokenURL, String tokenField, String tokenHeader, String endpointURL)
    {
        //Enable oauth
        ElementCache ec = newElementCache();
        ec.oauthRadioButton.check();

        OAuthWebPart webPart = ec.oAuthWebPart;
        webPart.setTokenUrl(tokenURL);
        webPart.setTokenField(tokenField);
        webPart.setTokenHeader(tokenHeader);
        webPart.setEndpointURL(endpointURL);
    }

    public void setBasicAuthCredentials(String username, String password, String endpointURL)
    {
        //Enable oauth
        ElementCache ec = newElementCache();
        ec.basicRadioButton.check();

        BasicAuthWebPart webPart = ec.basicAuthWebPart;
        webPart.setUsername(username);
        webPart.setPassword(password);
        webPart.setBasicURL(endpointURL);
    }

    public String getInputId()
    {
        return elementCache().studyId.get();
    }

    public void setInputId(String value)
    {
        elementCache().studyId.setValue(value);
    }

    public void checkResponseCollection()
    {
        elementCache().responseCollection.check();
    }

    public void uncheckResponseCollection()
    {
        elementCache().responseCollection.uncheck();
    }

    public boolean isResponseCollectionChecked()
    {
        return elementCache().responseCollection.isChecked();
    }

    public boolean isSaveEnabled()
    {
        String classValue = elementCache().saveStudySetup.getAttribute("class");
        return !classValue.toLowerCase().contains("labkey-disabled-button");
    }

    public void validateSaveButtonEnabled()
    {
        log("Validate that the save button is now enabled.");
        assertTrue("Save button is not showing as enabled, it should be.", isSaveEnabled());
    }

    public void validateSaveButtonDisabled()
    {
        log("Validate that the save button is disabled.");
        assertFalse("Save button is showing as enabled, it should not be.", isSaveEnabled());
    }

    public void acceptCollectionWarning()
    {
        MyStudiesResponseServerWebPart.ResponseCollectionDialog warning = new MyStudiesResponseServerWebPart.ResponseCollectionDialog(getDriver());
        warning.clickOk();
    }

    public void saveStudySetup()
    {
        if (!isSaveEnabled())
            throw new IllegalStateException("Save button not enabled");

        boolean collectionEnabled = isResponseCollectionChecked();
        elementCache().saveStudySetup.click();

        if (!collectionEnabled)
            acceptCollectionWarning();
    }

    public Error saveAndExpectError()
    {
        saveStudySetup();
        return new Error(getDriver());
    }

    public void saveAndExpectSuccess()
    {
        saveStudySetup();
        shortWait().until(ExpectedConditions.visibilityOf(elementCache().successSaveMessage));
    }

    public void clickUpdateMetadata()
    {
        if (!isUpdateMetadataEnabled())
            throw new IllegalStateException("Update metadata button not enabled");

        elementCache().updateMetadata.click();

        shortWait().until(ExpectedConditions.visibilityOf(elementCache().successUpdateMessage));
    }

    private boolean isUpdateMetadataEnabled()
    {
        String classValue = elementCache().updateMetadata.getAttribute("class");
        return !classValue.toLowerCase().contains("labkey-disabled-button");
    }

    public void disableForwarding()
    {
        newElementCache().disableRadioButton.check();
    }

    public void submit()
    {
        shortWait().until(ExpectedConditions.elementToBeClickable(Locator.button("Submit")));
        clickButton("Submit");
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        RadioButton disableRadioButton = new RadioButton.RadioButtonFinder().withNameAndValue("forwardingType", "Disabled").findWhenNeeded(this);
        RadioButton basicRadioButton = new RadioButton.RadioButtonFinder().withNameAndValue("forwardingType", "Basic").findWhenNeeded(this);
        RadioButton oauthRadioButton = new RadioButton.RadioButtonFinder().withNameAndValue("forwardingType", "OAuth").findWhenNeeded(this);

        Input studyId = new Input(Locator.input("studyId").findWhenNeeded(this), getDriver());
        Checkbox responseCollection = new Checkbox(Locator.checkboxById("responseCollection").findWhenNeeded(getDriver()));
        WebElement saveStudySetup = new LazyWebElement(Locator.lkButton("Save"),this);
        WebElement updateMetadata = new LazyWebElement(Locator.lkButton("Update Metadata"),this);
        WebElement successSaveMessage = Locator.tagWithText("span", "Configuration Saved").findWhenNeeded(this);
        WebElement successUpdateMessage = Locator.tagWithText("span", "Metadata Updated").findWhenNeeded(this);

        OAuthWebPart oAuthWebPart = new OAuthWebPart(getDriver(), new LazyWebElement(Locator.id("oauthPanel"), this));
        BasicAuthWebPart basicAuthWebPart = new BasicAuthWebPart(getDriver(), new LazyWebElement(Locator.id("basicAuthPanel"), this));
    }
}
