<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.response.ResponseController.ForwardingSettingsAction" %>
<%@ page import="org.labkey.response.ResponseController.ForwardingSettingsForm" %>
<%@ page import="org.labkey.response.ResponseManager" %>
<%@ page import="org.labkey.response.forwarder.ForwarderProperties" %>
<%@ page import="org.labkey.response.forwarder.ForwardingType" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.response.data.MobileAppStudy" %>
<%@ page import="org.labkey.api.security.permissions.AdminPermission" %>
<%@ page import="org.labkey.response.ResponseController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<style>
    .lk-study-id {
        width: 300px;
    }

    .lk-response-collection {
        vertical-align: text-bottom;
        margin-top: 2px;
        margin-left: 7px;
    }

    .lk-response-collection-buttons {
        margin-top: 20px;
    }

    .lk-response-update-metadata {
        margin-left: 20px;
    }

    .lk-response-collection-enable-checkbox {
        display: flex;
    }

    .lk-response-update-metadata-success {
        color: #5cb85c;
        font-size: 12px;
        margin-left: 15px;
    }

    .lk-response-update-metadata-failure {
        color: red;
    }

    .text-field-error-state {
        border: 1px solid red !important;
    }

    .studysetup-prompt {
        margin-bottom: 28px;
    }
</style>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("internal/jQuery");
        dependencies.add("Ext4");
        dependencies.add("mobileAppStudy/panel/forwarderSettings.css");
        dependencies.add("mobileAppStudy/panel/forwarderSettings.js");
    }
%>
<%
    JspView<ForwardingSettingsForm> me = (JspView<ForwardingSettingsForm>) HttpView.currentView();
    ForwardingSettingsForm bean = me.getModelBean();

    ResponseManager manager = ResponseManager.get();

    Map<String, String> forwardingProperties = manager.getForwardingProperties(getContainer());
    ForwardingType authType = ForwarderProperties.getForwardingType(getContainer());

    String basicAuthURL = forwardingProperties.get(ForwarderProperties.URL_PROPERTY_NAME);
    String basicAuthUser = forwardingProperties.get(ForwarderProperties.USER_PROPERTY_NAME);
    String basicAuthPassword = StringUtils.isNotBlank(bean.getPassword()) ?
            ForwarderProperties.PASSWORD_PLACEHOLDER :
            "";

    String oauthRequestURL = forwardingProperties.get(ForwarderProperties.TOKEN_REQUEST_URL);
    String oauthTokenFieldPath = forwardingProperties.get(ForwarderProperties.TOKEN_FIELD);
    String oauthTokenHeader= forwardingProperties.get(ForwarderProperties.TOKEN_HEADER);
    String oauthURL = forwardingProperties.get(ForwarderProperties.OAUTH_URL);

    MobileAppStudy studySetupBean = ResponseManager.get().getStudy(getContainer());
    studySetupBean = studySetupBean != null ? studySetupBean : new MobileAppStudy();
    studySetupBean.setEditable(!ResponseManager.get().hasStudyParticipants(getContainer()));
    studySetupBean.setCanChangeCollection(getContainer().hasPermission(getUser(), AdminPermission.class));

    String shortName = studySetupBean.getShortName();
    boolean collectionEnabled = studySetupBean.getCollectionEnabled();
%>

<labkey:panel title="Study Setup">
    <labkey:form name="StudyConfigForm" action="<%=new ActionURL(ResponseController.StudyConfigAction.class, getContainer())%>" method="POST">
        <div class="studysetup-prompt"> Enter the StudyId to be associated with this folder. The StudyId should be the same as it appears in the study design interface. </div>

        <labkey:input type="text" className="form-control lk-study-id" name="studyId" id="studyId" placeholder="Enter StudyId" value="<%=shortName%>" /> <br/>
        <div class="lk-response-collection-enable-checkbox">
            <labkey:input type="checkbox" id="responseCollection" name="responseCollection" checked="<%=collectionEnabled%>" />
            <span class="lk-response-collection"> Enable Response Collection </span>
        </div>

        <div class="lk-response-collection-buttons">
            <%= button("Save").id("submitStudySetupButton").onClick("submitStudySetup();") %>
            <span class="lk-response-update-metadata"> <%= button("Update Metadata").id("updateMetadataButton").onClick("updateMetadata();") %> </span>
            <span class="lk-response-update-metadata-success"></span>
            <span class="lk-response-update-metadata-failure"></span>
        </div>
    </labkey:form>
</labkey:panel>

<labkey:panel title="Response Forwarding">
    <labkey:errors></labkey:errors>

    <labkey:form name="mobileAppStudyForwardingSettingsForm" action="<%=new ActionURL(ForwardingSettingsAction.class, getContainer())%>" method="POST" >
        <div id="authTypeSelector" class=" form-group" >
            <label>
                <input type="radio" name="forwardingType" value="<%=ForwardingType.Disabled%>"<%=checked(authType == ForwardingType.Disabled)%>/>
                Disabled
            </label><br>
            <label>
                <input type="radio" name="forwardingType" value="<%=ForwardingType.Basic%>"<%=checked(authType == ForwardingType.Basic)%>/>
                Basic Authorization
            </label><br>
            <label>
                <input type="radio" name="forwardingType" value="<%=ForwardingType.OAuth%>"<%=checked(authType == ForwardingType.OAuth)%>/>
                OAuth
            </label><br>
        </div>

        <div style="padding: 10px;" >
            <div id="basicAuthPanel" class=" form-group">
                <labkey:input type="text" className=" form-control lk-forwarder-input" label="User" name="username" value="<%=basicAuthUser%>" />
                <labkey:input type="password" className=" form-control lk-forwarder-input" label="Password" name="password" value="<%=basicAuthPassword%>"/>
                <labkey:input type="text" className=" form-control lk-forwarder-input lk-forwarder-url" label="Endpoint URL" name="basicURL" value="<%=basicAuthURL%>" />
            </div>
            <div id="oauthPanel" class=" form-group">
                <labkey:input type="text" className=" form-control lk-forwarder-input lk-forwarder-url" label="Token Request URL" name="tokenRequestURL" value="<%=oauthRequestURL%>" />
                <labkey:input type="text" className=" form-control lk-forwarder-input" label="Token Field" name="tokenField" value="<%=oauthTokenFieldPath%>"/>
                <labkey:input type="text" className=" form-control lk-forwarder-input" label="Header Name" name="header" value="<%=oauthTokenHeader%>" />
                <labkey:input type="text" className=" form-control lk-forwarder-input lk-forwarder-url" label="Endpoint URL" name="oauthURL" value="<%=oauthURL%>" />
            </div>
        </div>
        <div id="buttonBar">
            <button id="forwarderSubmitButton" type="submit" class="labkey-button primary" >Submit</button>
        </div>
    </labkey:form>
</labkey:panel>

<script type="text/javascript">
    let pulseSuccessMessage = () => {
        let successMessage = $('.lk-response-update-metadata-success');
        successMessage.text("Configuration Saved");
        successMessage.fadeIn(3000).delay(3000).fadeOut("slow");
    }

    function postStudySetup() {
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL("response", "studyConfig"),
            method: 'POST',
            jsonData: {
                collectionEnabled: $('#responseCollection').is(":checked"),
                studyId: $('#studyId').val()
            },
            success: (response) => {
                let result = JSON.parse(response.response);

                // Set panel values
                $('#studyId').val(result.data.studyId);
                $('#responseCollection').prop('checked', result.data.collectionEnabled);

                $('#submitStudySetupButton').addClass("labkey-disabled-button");
                $('#updateMetadataButton').removeClass("labkey-disabled-button");

                pulseSuccessMessage();
            },
            failure: (response) => {
                let errorMessage = "There was a problem.  Please check the logs or contact an administrator.";
                if ('responseText' in response && 'exception' in JSON.parse(response.responseText)) {
                    errorMessage = JSON.parse(response.responseText).exception;
                }
                LABKEY.Utils.alert("Error", errorMessage);
            }
        });
    }

    function submitStudySetup() {
        if (!$('#responseCollection').is(":checked")) {
            Ext4.Msg.show({
                title: 'Response collection stopped',
                msg: 'Response collection is disabled for this study. No data will be collected until it is enabled.',
                buttons: Ext4.Msg.OKCANCEL,
                icon: Ext4.Msg.WARNING,
                fn: function(val) {
                    if (val == 'ok'){
                        postStudySetup();
                    } else {
                        $('#submitStudySetupButton').removeClass("labkey-disabled-button");
                    }
                },
                scope: this
            });
        }
    }

    function updateMetadata() {
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL("response", "UpdateStudyMetadata"),
            method: 'POST',
            jsonData: { studyId: $('#studyId').val() },
            success: (r) => { pulseSuccessMessage(); },
            failure: (r) => {
                LABKEY.Utils.alert("Error", "There was a problem updating the study metadata.  Please check the logs or contact an administrator.");
            }
        })
    }

    function enableOrDisableStudySetupButtons () {
        if (($('#studyId').val() !== "<%=h(shortName)%>" || $('#responseCollection').prop('checked') !== <%=collectionEnabled%>) && $('#studyId').val().length > 0) {
            $('#submitStudySetupButton').removeClass("labkey-disabled-button");
            $('#studyId').removeClass("text-field-error-state");
        } else {
            $('#submitStudySetupButton').addClass("labkey-disabled-button");
            if ($('#studyId').val().length === 0) {
                $('#studyId').addClass("text-field-error-state");
            }
        }

        if (($('#studyId').val() !== "<%=h(shortName)%>")) {
            $('#updateMetadataButton').addClass("labkey-disabled-button");
        } else {
            $('#updateMetadataButton').removeClass("labkey-disabled-button");
        }
    }

    // Dirtiness listeners for enabling and disabling Study Setup buttons
    $('#studyId').change(() => {enableOrDisableStudySetupButtons()}); // Must be present for automation test purposes
    $('#studyId').on('input', function() {enableOrDisableStudySetupButtons()});
    $('#responseCollection').change(() => {enableOrDisableStudySetupButtons()});

    +function ($) {
        $('#authTypeSelector').change(LABKEY.MobileAppForwarderSettings.showAuthPanel);

        LABKEY.MobileAppForwarderSettings.showAuthPanel();
        enableOrDisableStudySetupButtons();
        $('#studyId').removeClass("text-field-error-state");
    } (jQuery);
</script>