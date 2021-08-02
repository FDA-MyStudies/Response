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

package org.labkey.response;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.security.permissions.ApplicationAdminPermission;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.settings.AdminConsole;
import org.labkey.api.usageMetrics.UsageMetricsService;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.FolderManagement;
import org.labkey.api.view.SimpleWebPartFactory;
import org.labkey.api.view.WebPartFactory;
import org.labkey.response.query.MobileAppStudyQuerySchema;
import org.labkey.response.query.ReadResponsesQuerySchema;
import org.labkey.response.security.MyStudiesCoordinator;
import org.labkey.response.view.EnrollmentTokenBatchesWebPart;
import org.labkey.response.view.StudyConfigWebPart;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class ResponseModule extends DefaultModule
{
    public static final String NAME = "Response";
    public static final String SURVEY_METADATA_DIRECTORY = "SurveyMetadataDirectory";
    public static final String METADATA_SERVICE_BASE_URL = "MetadataServiceBaseUrl";
    public static final String METADATA_SERVICE_ACCESS_TOKEN = "MetadataServiceAccessToken";

    /**
     * Predicate that can be used to check if a container has this module active
     */
    public final Predicate<Container> IS_ACTIVE = container -> container.hasActiveModuleByName(getName());

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public @Nullable Double getSchemaVersion()
    {
        return 21.001;
    }

    @Override
    public boolean hasScripts()
    {
        return true;
    }

    @Override
    public @Nullable UpgradeCode getUpgradeCode()
    {
        return new ResponseUpgradeCode();
    }

    @Override
    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        SimpleWebPartFactory studySetupFactory = new SimpleWebPartFactory("MyStudies Response Server",
            WebPartFactory.LOCATION_BODY, StudyConfigWebPart.class, null);
        studySetupFactory.addLegacyNames("Mobile App Study Setup");
        return List.of(
            new SimpleWebPartFactory("Enrollment Token Batches", WebPartFactory.LOCATION_BODY, EnrollmentTokenBatchesWebPart.class, null),
            studySetupFactory
        );
    }

    @Override
    protected void init()
    {
        addController(ResponseController.NAME, ResponseController.class, "mobileappstudy");
    }

    @Override
    public void doStartup(ModuleContext moduleContext)
    {
        ContainerManager.addContainerListener(new ResponseContainerListener());
        MobileAppStudyQuerySchema.register(this);
        ReadResponsesQuerySchema.register(this);

        ActionURL serverConfigurationURL = new ActionURL(ResponseController.ServerConfigurationAction.class, ContainerManager.getRoot());
        AdminConsole.addLink(AdminConsole.SettingsLinkType.Configuration, "Response Server Configuration", serverConfigurationURL, ApplicationAdminPermission.class);
        FolderManagement.addTab(FolderManagement.TYPE.FolderManagement, "MyStudies Response Server", "forwarding",
                IS_ACTIVE, ResponseController.ForwardingSettingsAction.class);

        //Startup shredding and forwarder jobs
        ResponseManager.get().doStartup();

        RoleManager.registerRole(new MyStudiesCoordinator());

        UsageMetricsService svc = UsageMetricsService.get();
        if (null != svc)
        {
            svc.registerUsageMetrics(NAME, () -> {
                Map<String, Object> metric = new HashMap<>();
                metric.put("foldersWithStudyIdConfiguredCount", new SqlSelector(MobileAppStudyQuerySchema.getSchema(), "SELECT COUNT(*) FROM MOBILEAPPSTUDY.STUDY").getObject(Long.class));
                return metric;
            });
        }
    }

    @Override
    @NotNull
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return Collections.singleton(MobileAppStudySchema.NAME);
    }
}
