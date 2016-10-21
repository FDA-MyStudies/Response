/*
 * Copyright (c) 2015 LabKey Corporation
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

package org.labkey.mobileappsurvey;

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;

public class MobileAppSurveySchema
{
    private static final MobileAppSurveySchema _instance = new MobileAppSurveySchema();
    public static final String NAME = "mobileappsurvey";
    public static final String ENROLLMENT_TOKEN_BATCH_TABLE = "EnrollmentTokenBatch";
    public static final String ENROLLMENT_TOKEN_TABLE = "EnrollmentToken";
    public static final String STUDY_TABLE = "Study";
    public static final String PARTICIPANT_TABLE = "Participant";

    public static MobileAppSurveySchema getInstance()
    {
        return _instance;
    }

    private MobileAppSurveySchema()
    {
        // private constructor to prevent instantiation from
        // outside this class: this singleton should only be
        // accessed via org.labkey.mobileappsurvey.MobileAppSurveySchema.getInstance()
    }

    public DbSchema getSchema()
    {
        return DbSchema.get(NAME, DbSchemaType.Module);
    }

    public SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    public TableInfo getTableInfoEnrollmentTokenBatch()
    {
        return getSchema().getTable(ENROLLMENT_TOKEN_BATCH_TABLE);
    }

    public TableInfo getTableInfoEnrollmentToken()
    {
        return getSchema().getTable(ENROLLMENT_TOKEN_TABLE);
    }

    public TableInfo getTableInfoParticipant()
    {
        return getSchema().getTable(PARTICIPANT_TABLE);
    }

    public TableInfo getTableInfoStudy()
    {
        return getSchema().getTable(STUDY_TABLE);
    }
}
