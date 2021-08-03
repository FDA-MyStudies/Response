<%
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
%>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.response.ResponseManager" %>
<%@ page import="org.labkey.response.data.MobileAppStudy" %>
<%@ page import="org.labkey.response.forwarder.ForwarderProperties" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="static org.labkey.response.ResponseController.getResponseForwardingSettingsURL" %>
<%@ page import="org.labkey.response.ResponseController" %>
<%@ page import="org.labkey.api.data.ContainerManager" %>
<%@ page
        import="static org.labkey.response.ResponseController.ServerConfigurationAction.RESPONSE_SERVER_CONFIGURATION" %>
<%@ page import="org.labkey.api.data.PropertyManager" %>
<%@ page import="static org.labkey.response.ResponseController.ServerConfigurationAction.WCP_BASE_URL" %>
<%@ page import="static org.labkey.response.ResponseController.ServerConfigurationAction.METADATA_DIRECTORY" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<style type="text/css">
    .labkey-warning  { color: red; }

    .study-configuration-values {
        margin-left: 20px;
        margin-top: 15px;
    }

    .study-configuration-row {
        margin-bottom: 7px;
    }

    .study-configuration-row-title {
        font-weight: bold;
        width: 240px;
        float: left;
    }

    .study-configuration-times-circle {
        color: red;
    }

    .study-configuration-check-circle {
        color: green;
        margin-left: 5px;
    }
</style>

<%
    JspView<MobileAppStudy> me = (JspView<MobileAppStudy>) HttpView.currentView();
    MobileAppStudy bean = me.getModelBean();

    String renderId = "labkey-mobileappstudy-studysetup";
    String shortName = bean.getShortName();
    boolean collectionEnabled = bean.getCollectionEnabled();

    Map<String, String> forwardingProperties = ResponseManager.get().getForwardingProperties(getContainer());
    boolean forwardingEnabled = Boolean.valueOf(forwardingProperties.get(ForwarderProperties.ENABLED_PROPERTY_NAME));
    ActionURL responseForwardingTab = getResponseForwardingSettingsURL(getContainer());
    ActionURL responseServerAdminConfigPage = new ActionURL(ResponseController.ServerConfigurationAction.class, ContainerManager.getRoot());
    PropertyManager.PropertyMap props = PropertyManager.getEncryptedStore().getProperties(ContainerManager.getRoot(), RESPONSE_SERVER_CONFIGURATION);
    String metadataDirectory = props.get(METADATA_DIRECTORY);
    String wcpBaseURL = props.get(WCP_BASE_URL);
    boolean responseServerAdminConfigured = (metadataDirectory != null && !metadataDirectory.isEmpty()) || (wcpBaseURL != null && !wcpBaseURL.isEmpty());
%>

<labkey:errors></labkey:errors>
<div id="<%= h(renderId)%>" class="requests-editor"></div>

<% if (!responseServerAdminConfigured) { %>
    <div>
        Response Server Site Settings are not configured. Click <a href="<%=h(responseServerAdminConfigPage)%>"> here </a> to configure them.
        <i class="fa fa-times-circle study-configuration-times-circle"></i>
    </div>
<% } %>

<h4>
    Study Configuration

    <% if (shortName == null || shortName.isEmpty()) { %>
        <i class="fa fa-times-circle study-configuration-times-circle"></i>
    <% } else { %>
        <i class="fa fa-check-circle study-configuration-check-circle"></i>
    <% } %>
</h4>

<div class="study-configuration-values">
    <div class="study-configuration-row">
        <span class="study-configuration-row-title"> StudyId associated with this folder: </span>
        <% if (shortName == null || shortName.isEmpty()) { %>
        Study Id is not set. Click <a href="<%=h(responseForwardingTab)%>"> here </a> to set Study Id.
        <% } else { %>
            <%= h(shortName)%>
        <% } %>
    </div>

    <div class="study-configuration-row">
        <span class="study-configuration-row-title"> Response Collection: </span>
        <% if (collectionEnabled) { %>
            Enabled
        <% } else { %>
            Disabled
        <% } %>
    </div>

    <div class="study-configuration-row">
        <span class="study-configuration-row-title"> Response Forwarding: </span>
        <% if (forwardingEnabled) { %>
            Enabled
        <% } else { %>
            Disabled
        <% } %>
    </div>
</div>
