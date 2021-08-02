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

import org.apache.logging.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.PropertyManager;
import org.labkey.response.participantproperties.ParticipantPropertiesDesign;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.labkey.response.ResponseController.ServerConfigurationAction.METADATA_DIRECTORY;
import static org.labkey.response.ResponseController.ServerConfigurationAction.RESPONSE_SERVER_CONFIGURATION;

/**
 * Get MobileAppStudy SurveySchema from a resource file
 */
public class FileSurveyDesignProvider extends AbstractSurveyDesignProviderImpl
{
    public FileSurveyDesignProvider(Container container, Logger logger)
    {
        super(container, logger);
    }

    @Override
    public SurveyDesign getSurveyDesign(Container c, String studyId, String activityId, String version) throws InvalidDesignException
    {
        try
        {
            //TODO: make this more flexible
            StringBuilder sb = new StringBuilder();
            Path filePath = Paths.get(getBasePath(), String.join("_", studyId, activityId, version) + ".json");
            Files.readAllLines(filePath).forEach(sb::append);

            return getSurveyDesign(sb.toString());
        }
        catch (IOException x)
        {
            throw new InvalidDesignException("Unable to read from SurveyDesign file", x);
        }
    }

    @Override
    public ParticipantPropertiesDesign getParticipantPropertiesDesign(Container c, String shortName) throws Exception
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            Path filePath = Paths.get(getBasePath(), String.join("_", shortName, "ParticipantProperties") + ".json");
            if (!Files.exists(filePath))
                return null; // No test file present

            Files.readAllLines(filePath).forEach(sb::append);
            return getParticipantPropertiesDesign(sb.toString());
        }
        catch (IOException x)
        {
            throw new InvalidDesignException("Invalid participant properties design file.");
        }
    }

    public static String getBasePath()
    {
        PropertyManager.PropertyMap props = PropertyManager.getEncryptedStore().getProperties(ContainerManager.getRoot(), RESPONSE_SERVER_CONFIGURATION);
        return props.get(METADATA_DIRECTORY);
    }
}
