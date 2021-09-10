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
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.json.simple.JSONObject;
import org.labkey.test.WebTestHelper;
import org.labkey.test.data.response.ParticipantPropertiesResponse;
import org.labkey.test.data.response.ParticipantProperty;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class EnrollmentTokenValidationCommand extends ResponseCommand
{
    protected static final String ACTION_NAME = "validateenrollmenttoken";
    public static final String INVALID_STUDYID_FORMAT = "Study with StudyId '%1$s' does not exist";
    public static final String INVALID_TOKEN_FORMAT = "Invalid token: '%1$s'";
    public static final String BLANK_STUDYID = "StudyId is required";
    public static final String TOKEN_REQUIRED = "Token is required";

    private String _batchToken;
    private String _studyName;
    private String _projectName;
    private String _language;

    private Collection<ParticipantProperty> _preEnrollmentParticipantProperties;

    public void setPreEnrollmentParticipantProperties(Collection<ParticipantProperty> preEnrollmentParticipantProperties)
    {
        _preEnrollmentParticipantProperties = preEnrollmentParticipantProperties;
    }

    public Collection<ParticipantProperty> getPreEnrollmentParticipantProperties()
    {
        return _preEnrollmentParticipantProperties;
    }

    public String getProjectName()
    {
        return _projectName;
    }
    public void setProjectName(String projectName)
    {
        _projectName = projectName;
    }

    public EnrollmentTokenValidationCommand(String project, String studyName, String batchToken)
    {
        _studyName = studyName;
        _batchToken = batchToken;
        _projectName = project;
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
        if (getLanguage() != null)
        {
            params.put("language", getLanguage());
        }
        return WebTestHelper.buildURL(CONTROLLER_NAME, getProjectName(), ACTION_NAME, params);
    }

    @Override
    public String getBody()
    {
        return "";
    }

    @Override
    protected void parseSuccessfulResponse(JSONObject response)
    {
        ObjectMapper mapper = new ObjectMapper();
        try
        {
            ParticipantPropertiesResponse ppResponse = mapper.readValue(response.toJSONString(), ParticipantPropertiesResponse.class);
            if (ppResponse != null)
                _preEnrollmentParticipantProperties = ppResponse.getPreEnrollmentParticipantProperties();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
