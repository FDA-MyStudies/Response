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

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;


public abstract class ResponseCommand
{
    public static final String CONTROLLER_NAME = "mobileappstudy";

    protected static final String MESSAGE_TAG = "Message";
    protected static final String ERRORS_TAG = "Errors";
    protected static final String EXCEPTION_MESSAGE_TAG = "exception";
    protected static final String SUCCESS_TAG = "success";

    private String _exceptionMessage;
    private boolean _success;

    public boolean getSuccess()
    {
        return _success;
    }
    public void setSuccess(boolean success)
    {
        _success = success;
    }

    public String getExceptionMessage()
    {
        return _exceptionMessage;
    }
    public void setExceptionMessage(String exceptionMessage)
    {
        _exceptionMessage = exceptionMessage;
    }

    protected boolean isExecuted = false;
    protected JSONObject _jsonResponse;

    public abstract HttpResponse execute(int expectedStatusCode);
    public abstract String getTargetURL();

    protected void parseResponse(String response)
    {
        try
        {
            _jsonResponse = new JSONObject(response);
        }
        catch (JSONException e)
        {
            throw new RuntimeException(e);
        }
        setSuccess((Boolean) _jsonResponse.get(SUCCESS_TAG));

        if (getSuccess())
            parseSuccessfulResponse(_jsonResponse);
        else
            parseErrorResponse(_jsonResponse);
    }

    protected void parseSuccessfulResponse(JSONObject response)
    {
        //do nothing here
    }

    protected void parseErrorResponse(JSONObject response)
    {
        setExceptionMessage(response.optString(EXCEPTION_MESSAGE_TAG, null));
    }

    protected HttpResponse execute(HttpUriRequest request, int expectedStatusCode)
    {
        setExceptionMessage(null); // Clear out previous exception message, in case we're reusing this Command

        try
        {
            TestLogger.log("Submitting request using url: " + request.getUri());
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }

        //CloseableHttpResponse response;
        try (CloseableHttpClient client = HttpClients.createDefault(); CloseableHttpResponse response = client.execute(request))
        {
            try
            {
                isExecuted = true;
                TestLogger.log("Post completed. Response body: " + getBody());

                int statusCode = response.getCode();
                String body = EntityUtils.toString(response.getEntity());
                parseResponse(body);

                if (expectedStatusCode < 400 && StringUtils.isNotBlank(getExceptionMessage()))
                    TestLogger.log("Unexpected error message: " + getExceptionMessage());

                assertEquals("Unexpected response status", expectedStatusCode, statusCode);
                return response;
            }
            finally
            {
                if (response != null)
                    EntityUtils.consumeQuietly(response.getEntity());
            }
        }
        catch (IOException | ParseException e)
        {
            throw new RuntimeException("Test failed requesting the URL,", e);
        }
    }

    public abstract String getBody();
}
