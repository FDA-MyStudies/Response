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
package org.labkey.response.view;

import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewContext;
import org.labkey.response.ResponseManager;
import org.labkey.response.data.MobileAppStudy;

/**
 * Web part for associating a study short name with a container.
 */
public class MyStudiesResponseServerWebPart extends JspView<MobileAppStudy>
{
    public MyStudiesResponseServerWebPart(ViewContext viewContext)
    {
        super("/org/labkey/response/view/MyStudiesResponseServer.jsp");
        setTitle("MyStudies Response Server");

        MobileAppStudy bean = ResponseManager.get().getStudy(viewContext.getContainer());
        bean = bean != null ? bean : new MobileAppStudy();

        bean.setEditable(!ResponseManager.get().hasStudyParticipants(viewContext.getContainer()));
        bean.setCanChangeCollection(viewContext.getContainer().hasPermission(viewContext.getUser(), AdminPermission.class));

        this.setModelBean(bean);
    }
}
