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
import static org.labkey.response.ResponseModule.METADATA_SERVICE_ACCESS_TOKEN;
import static org.labkey.response.ResponseModule.METADATA_SERVICE_BASE_URL;
import static org.labkey.response.ResponseModule.SURVEY_METADATA_DIRECTORY;

public class ResponseUpgradeCode implements UpgradeCode
{
    private static final Logger LOG = LogManager.getLogger(ResponseUpgradeCode.class);

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

        ModuleProperty responseModulePropertyDirectory = responseModule.getModuleProperties().get(SURVEY_METADATA_DIRECTORY);
        ModuleProperty responseModulePropertyURL = responseModule.getModuleProperties().get(METADATA_SERVICE_BASE_URL);
        ModuleProperty responseModuleProperty = responseModule.getModuleProperties().get(METADATA_SERVICE_ACCESS_TOKEN);

        String surveyMetadataDirectoryValue = responseModulePropertyDirectory != null ? getResultingPropertyValue(SURVEY_METADATA_DIRECTORY, responseModulePropertyDirectory, responseModule) : "";
        String metadataServiceBaseUrl = responseModulePropertyURL != null ? getResultingPropertyValue(METADATA_SERVICE_BASE_URL, responseModulePropertyURL, responseModule) : "";
        String metadataServiceAccessToken = responseModuleProperty != null ? getResultingPropertyValue(METADATA_SERVICE_ACCESS_TOKEN, responseModuleProperty, responseModule) : "";

        PropertyManager.PropertyMap props = PropertyManager.getEncryptedStore().getWritableProperties(ContainerManager.getRoot(), RESPONSE_SERVER_CONFIGURATION, false);
        if (props != null)
        {
            Map<String, String> valueMap = new HashMap<>();
            valueMap.put(ResponseController.ServerConfigurationAction.METADATA_LOAD_LOCATION, WCP_SERVER);
            valueMap.put(ResponseController.ServerConfigurationAction.METADATA_DIRECTORY, surveyMetadataDirectoryValue);
            valueMap.put(ResponseController.ServerConfigurationAction.WCP_BASE_URL, metadataServiceBaseUrl);

            String decodedAccessToken = new String(getDecoder().decode(metadataServiceAccessToken), StringUtilsLabKey.DEFAULT_CHARSET);
            valueMap.put(ResponseController.ServerConfigurationAction.WCP_USERNAME, decodedAccessToken.split(":")[0]);
            valueMap.put(ResponseController.ServerConfigurationAction.WCP_PASSWORD, decodedAccessToken.split(":")[1]);

            props.putAll(valueMap);
            props.save();
        }
    }

    private static String getResultingPropertyValue(String moduleProperty, ModuleProperty responseModuleProperty, Module responseModule)
    {
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
        String projectLevelValue = projectLevelValues.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
        String containerLevelValue = containerLevelValues.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();

        if (siteDefaultValue != null)
        {
            warnUnusedValues("Project", moduleProperty, projectLevelValues);
            warnUnusedValues("Container", moduleProperty, containerLevelValues);
            return siteDefaultValue;
        }

        if (projectLevelValue != null && !projectLevelValue.isEmpty())
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
