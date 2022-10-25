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
package org.labkey.response.surveydesign;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.PropertyManager;
import org.labkey.response.participantproperties.ParticipantPropertiesDesign;

import java.net.URI;
import java.util.function.Function;

import static org.apache.commons.net.util.Base64.encodeBase64;
import static org.labkey.api.util.StringUtilsLabKey.DEFAULT_CHARSET;
import static org.labkey.response.ResponseController.ServerConfigurationAction.RESPONSE_SERVER_CONFIGURATION;
import static org.labkey.response.ResponseController.ServerConfigurationAction.WCP_BASE_URL;
import static org.labkey.response.ResponseController.ServerConfigurationAction.WCP_PASSWORD;
import static org.labkey.response.ResponseController.ServerConfigurationAction.WCP_USERNAME;

/**
 * Created by susanh on 3/10/17.
 */
public class ServiceSurveyDesignProvider extends AbstractSurveyDesignProviderImpl
{
    private static final String STUDY_ID_PARAM = "studyId";
    private static final String ACTIVITY_ID_PARAM = "activityId";
    private static final String VERSION_PARAM = "activityVersion";
    private static final String PARTICIPANT_PROPERTIES_ACTION = "participantProperties";
    private static final String ACTIVITY_ACTION = "activity";

    public ServiceSurveyDesignProvider(Container container, Logger logger)
    {
        super(container, logger);
    }

    @Override
    public SurveyDesign getSurveyDesign(Container c, String shortName, String activityId, String version) throws Exception
    {
        URIBuilder uriBuilder = new URIBuilder(String.join("/", getServiceUrl(), ACTIVITY_ACTION));
        uriBuilder.setParameter(STUDY_ID_PARAM, shortName);
        uriBuilder.setParameter(ACTIVITY_ID_PARAM, activityId);
        uriBuilder.setParameter(VERSION_PARAM, version);

        return getDesign(c, uriBuilder, this::getSurveyDesign);
    }

    @Override
    public ParticipantPropertiesDesign getParticipantPropertiesDesign(Container c, String shortName) throws Exception
    {
        URIBuilder uriBuilder = new URIBuilder(String.join("/", getServiceUrl(), PARTICIPANT_PROPERTIES_ACTION));
        uriBuilder.setParameter(STUDY_ID_PARAM, shortName);

        return getDesign(c, uriBuilder, this::getParticipantPropertiesDesign);
    }

    private <DESIGN> DESIGN getDesign(Container c, URIBuilder uriBuilder, Function<String, DESIGN> designProcessor) throws Exception
    {
        URI uri = uriBuilder.build();
        CredentialsProvider provider = new BasicCredentialsProvider();
        try (CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build())
        {
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("Authorization", "Basic " + getServiceToken());

            try (CloseableHttpResponse response = httpclient.execute(httpGet))
            {
                HttpClientResponseHandler<String> handler = new BasicHttpClientResponseHandler();

                if (response.getCode() == HttpStatus.SC_OK || response.getCode() == HttpStatus.SC_CREATED)
                {
                    return designProcessor.apply(handler.handleResponse(response));
                }
                else
                {
                    throw new Exception(String.format("Received response status %d using uri %s", response.getCode(), uri));
                }
            }
        }
    }

    private static String getServiceToken()
    {
        PropertyManager.PropertyMap props = PropertyManager.getEncryptedStore().getProperties(ContainerManager.getRoot(), RESPONSE_SERVER_CONFIGURATION);
        return new String(encodeBase64((props.get(WCP_USERNAME) + ":" + props.get(WCP_PASSWORD)).getBytes(DEFAULT_CHARSET)), DEFAULT_CHARSET);
    }

    private static String getServiceUrl()
    {
        PropertyManager.PropertyMap props = PropertyManager.getEncryptedStore().getProperties(ContainerManager.getRoot(), RESPONSE_SERVER_CONFIGURATION);
        String value = StringUtils.trimToNull(props.get(WCP_BASE_URL));

        //Allow backwards compatibility to baseUrl parameter, truncate /activity from the configured url if present Issue #39137
        return StringUtils.removeEndIgnoreCase(value, "/" + ACTIVITY_ACTION);
    }

    public static Boolean isConfigured()
    {
        return !StringUtils.isEmpty(getServiceToken()) && !StringUtils.isEmpty(getServiceUrl());
    }
}
