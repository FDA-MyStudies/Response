package org.labkey.response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DeferredUpgrade;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.util.StringUtilsLabKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Base64.getDecoder;
import static org.labkey.response.ResponseController.ServerConfigurationAction.RESPONSE_SERVER_CONFIGURATION;
import static org.labkey.response.ResponseController.ServerConfigurationAction.WCP_SERVER;

public class ResponseUpgradeCode implements UpgradeCode
{
    private static final Logger LOG = LogManager.getLogger(ResponseUpgradeCode.class);
    private static final String SURVEY_METADATA_DIRECTORY = "SurveyMetadataDirectory";
    private static final String METADATA_SERVICE_BASE_URL = "MetadataServiceBaseUrl";
    private static final String METADATA_SERVICE_ACCESS_TOKEN = "MetadataServiceAccessToken";

    /**
     * Invoked within mobileappstudy-21.000-21.001.sql
     * Response Module Properties, which were formerly set in the Folder Management Module Properties tab, are here migrated
     * to the Admin Console Response Server Configuration page.
     */
    @DeferredUpgrade
    public static void migrateResponseServerConfig(ModuleContext context)
    {
        Module responseModule = ModuleLoader.getInstance().getModule("Response");
        if (responseModule == null)
            return;

        String surveyMetadataDirectoryValue = getResultingPropertyValue(SURVEY_METADATA_DIRECTORY, responseModule);
        String metadataServiceBaseUrl = getResultingPropertyValue(METADATA_SERVICE_BASE_URL, responseModule);
        String metadataServiceAccessToken =  getResultingPropertyValue(METADATA_SERVICE_ACCESS_TOKEN, responseModule);

        PropertyManager.PropertyMap props = PropertyManager.getEncryptedStore().getWritableProperties(ContainerManager.getRoot(), RESPONSE_SERVER_CONFIGURATION, true);
        if (props != null)
        {
            Map<String, String> valueMap = new HashMap<>();
            valueMap.put(ResponseController.ServerConfigurationAction.METADATA_LOAD_LOCATION, WCP_SERVER);
            valueMap.put(ResponseController.ServerConfigurationAction.METADATA_DIRECTORY, surveyMetadataDirectoryValue);
            valueMap.put(ResponseController.ServerConfigurationAction.WCP_BASE_URL, metadataServiceBaseUrl);

            if (metadataServiceAccessToken != null)
            {
                String decodedAccessToken = new String(getDecoder().decode(metadataServiceAccessToken), StringUtilsLabKey.DEFAULT_CHARSET);
                if (decodedAccessToken.split(":").length > 1)
                {
                    valueMap.put(ResponseController.ServerConfigurationAction.WCP_USERNAME, decodedAccessToken.split(":")[0]);
                    valueMap.put(ResponseController.ServerConfigurationAction.WCP_PASSWORD, decodedAccessToken.split(":")[1]);
                }
            }

            props.putAll(valueMap);
            props.save();
        }
    }

    private static String getResultingPropertyValue(String moduleProperty, Module responseModule)
    {
        ModuleProperty responseModuleProperty = new ModuleProperty(responseModule, moduleProperty);
        responseModuleProperty.setCanSetPerContainer(true);

        String siteDefaultValue = responseModuleProperty.getValueContainerSpecific(ContainerManager.getRoot());
        HashMap<String, Integer> projectLevelValues = new HashMap<>();
        HashMap<String, Integer> containerLevelValues = new HashMap<>();

        // collect up data structure of all names and their occurrence
        Set<Container> childrenWithModule = ContainerManager.getAllChildrenWithModule(ContainerManager.getRoot(), responseModule);
        for (Container c: childrenWithModule)
        {
            String projectValue = responseModuleProperty.getValueContainerSpecific(c.getProject());
            String containerValue = responseModuleProperty.getValueContainerSpecific(c);

            accumHashHelper(projectValue, projectLevelValues);
            accumHashHelper(containerValue, containerLevelValues);
        }

        // Find most popular values collected
        String projectLevelValue = projectLevelValues.size() > 0 ? projectLevelValues.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey() : null;
        String containerLevelValue = containerLevelValues.size() > 0 ? containerLevelValues.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey() : null;

        if (siteDefaultValue != null)
        {
            warnUnusedValues("Project", moduleProperty, projectLevelValues);
            warnUnusedValues("Container", moduleProperty, containerLevelValues);
            return siteDefaultValue;
        }

        if (projectLevelValue != null)
        {
            warnUnusedValues("Container", moduleProperty, containerLevelValues);
            return projectLevelValue;
        }

        return containerLevelValue;
    }

    public static void warnUnusedValues(String level, String moduleProperty, HashMap<String, Integer> levelValues)
    {
        if (levelValues.size() > 0)
        {
            String commaSeperatedProjectValues = levelValues.keySet().stream().map(Object::toString).collect(Collectors.joining(", "));
            LOG.warn(String.format("The following %s-level setting(s) for the Response Module Property '%s' will be discarded: %s", level, moduleProperty, commaSeperatedProjectValues));
        }
    }

    public static void accumHashHelper(String value, HashMap<String, Integer> levelValues)
    {
        if (value != null)
        {
            if (levelValues.containsKey(value))
                levelValues.put(value, levelValues.get(value) + 1);
            else
                levelValues.put(value, 1);
        }
    }
}
