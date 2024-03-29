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
package org.labkey.test.commands.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.HttpResponse;
import org.json.JSONObject;
import org.labkey.test.WebTestHelper;
import org.labkey.test.data.response.ResolveEnrollmentTokenResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class ResolveEnrollmentTokenCommand extends ResponseCommand
{
    protected static final String ACTION_NAME = "resolveenrollmenttoken";

    private String _batchToken;
    private String _projectName;

    private String _studyId;
    private String _message;

    public String getProjectName()
    {
        return _projectName;
    }
    public void setProjectName(String projectName)
    {
        _projectName = projectName;
    }

    public ResolveEnrollmentTokenCommand(String project, String batchToken)
    {
        _batchToken = batchToken;
        _projectName = project;
    }

    public String getBatchToken()
    {
        return _batchToken;
    }
    public void setBatchToken(String batchToken)
    {
        _batchToken = batchToken;
    }

    public String getStudyId()
    {
        return _studyId;
    }

    public String getMessage()
    {
        return _message;
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
        params.put("token", getBatchToken());
        return WebTestHelper.buildURL(CONTROLLER_NAME, getProjectName(), ACTION_NAME, params);
    }

    @Override
    public String getBody()
    {
        return "";
    }

    @Override
    protected void parseErrorResponse(JSONObject response)
    {
        super.parseErrorResponse(response);
        _message = response.optString("message", null);
    }

    @Override
    protected void parseSuccessfulResponse(JSONObject response)
    {
        ObjectMapper mapper = new ObjectMapper();
        try
        {
            ResolveEnrollmentTokenResponse enrollmentTokenResponse = mapper.readValue(response.toString(), ResolveEnrollmentTokenResponse.class);
            if (null != enrollmentTokenResponse)
                _studyId = enrollmentTokenResponse.getStudyId();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
