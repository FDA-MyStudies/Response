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
package org.labkey.test.commands.response;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.json.simple.JSONObject;
import org.labkey.test.WebTestHelper;

import java.util.HashMap;
import java.util.Map;


public class EnrollParticipantCommand extends ResponseCommand
{
    private static final String APP_TOKEN_JSON_FIELD = "appToken";
    protected static final String ACTION_NAME = "enroll";

    private String _batchToken;
    private String _studyName;
    private String _appToken;
    private String _projectName;
    private String _allowDataSharing;
    private String _language;

    public String getProjectName()
    {
        return _projectName;
    }
    public void setProjectName(String projectName)
    {
        _projectName = projectName;
    }

    public EnrollParticipantCommand(String project, String studyName, String batchToken, String allowDataSharing)
    {
        _studyName = studyName;
        _batchToken = batchToken;
        _projectName = project;
        _allowDataSharing = allowDataSharing;
    }

    public String getStudyName()
    {
        return _studyName;
    }
    public void setStudyName(String studyName)
    {
        _studyName = studyName;
    }

    public String getBatchToken()
    {
        return _batchToken;
    }
    public void setBatchToken(String batchToken)
    {
        _batchToken = batchToken;
    }

    public String getAllowDataSharing()
    {
        return _allowDataSharing;
    }

    public void setAllowDataSharing(String allowDataSharing)
    {
        _allowDataSharing = allowDataSharing;
    }

    public String getLanguage()
    {
        return _language;
    }

    public void setLanguage(String language)
    {
        _language = language;
    }

    @Override
    public HttpResponse execute(int expectedStatusCode)
    {
        HttpPost post = new HttpPost(getTargetURL());
        return execute(post, expectedStatusCode);
    }

    @Override
    public String getTargetURL()
    {
        Map<String, String> params = new HashMap<>();
        params.put("studyId", getStudyName());
        params.put("token", getBatchToken());
        params.put("allowDataSharing", getAllowDataSharing());
        if (getLanguage() != null)
        {
            params.put("language", getLanguage());
        }
        return WebTestHelper.buildURL(CONTROLLER_NAME, getProjectName(), ACTION_NAME, params);
    }

    public String getAppToken()
    {
        if (!isExecuted)
            throw new IllegalStateException("Enroll command has not been executed yet");

        return _appToken;
    }

    @Override
    protected void parseSuccessfulResponse(JSONObject response)
    {
        _appToken = (String) ((JSONObject) response.get("data")).get(APP_TOKEN_JSON_FIELD);
    }

    @Override
    public String getBody()
    {
        return "";
    }
}
