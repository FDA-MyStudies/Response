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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.Action;
import org.labkey.api.action.ActionType;
import org.labkey.api.action.ApiQueryResponse;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.ApiUsageException;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.LabKeyError;
import org.labkey.api.action.Marshal;
import org.labkey.api.action.Marshaller;
import org.labkey.api.action.MutatingApiAction;
import org.labkey.api.action.ReadOnlyApiAction;
import org.labkey.api.action.ReportingApiQueryResponse;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.admin.AdminUrls;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.data.NormalContainerType;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.module.FolderTypeManager;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryForm;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.TempQuerySettings;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.CSRF;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.RequiresSiteAdmin;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.FolderManagement.FolderManagementViewPostAction;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.response.data.EnrollmentTokenBatch;
import org.labkey.response.data.Language;
import org.labkey.response.data.MobileAppStudy;
import org.labkey.response.data.Participant;
import org.labkey.response.data.SurveyMetadata;
import org.labkey.response.data.SurveyResponse;
import org.labkey.response.forwarder.ForwardingType;
import org.labkey.response.participantproperties.ParticipantProperty;
import org.labkey.response.query.ReadResponsesQuerySchema;
import org.labkey.response.security.GenerateEnrollmentTokensPermission;
import org.labkey.response.surveydesign.FileSurveyDesignProvider;
import org.labkey.response.surveydesign.InvalidDesignException;
import org.labkey.response.view.EnrollmentTokenBatchesWebPart;
import org.labkey.response.view.EnrollmentTokensWebPart;
import org.springframework.beans.PropertyValues;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.labkey.api.util.Result.failure;

@Marshal(Marshaller.Jackson)
public class ResponseController extends SpringActionController
{
    private static final Logger LOG = LogManager.getLogger(ResponseController.class);
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(ResponseController.class);

    public static final String NAME = "response";

    public ResponseController()
    {
        setActionResolver(_actionResolver);
    }

    public ActionURL getEnrollmentTokenBatchURL()
    {
        return new ActionURL(TokenBatchAction.class, getContainer());
    }

    /**
     * This action is used only for testing purposes.  It relies on the configuration of the SurveyMetadataDir module property.
     * It will read the file corresponding to the given query parameters and serve up a JSON response by reading the corresponding
     * file in the configured directory.
     */
    @RequiresNoPermission
    public class ActivityMetadataAction extends ReadOnlyApiAction<ActivityMetadataForm>
    {
        @Override
        public void validateForm(ActivityMetadataForm form, Errors errors)
        {
            if (FileSurveyDesignProvider.getBasePath() == null)
            {
                errors.reject(ERROR_REQUIRED, "No SurveyMetadataDirectory configured. Please set the appropriate Module Properties.");
                return;
            }

            if (form.getStudyId() == null)
                errors.reject(ERROR_REQUIRED, "studyId is a required parameter");
            if (form.getActivityId() == null)
                errors.reject(ERROR_REQUIRED, "activityId is a required parameter");
            if (form.getActivityVersion() == null)
                errors.reject(ERROR_REQUIRED, "activityVersion is a required parameter");
        }

        @Override
        public Object execute(ActivityMetadataForm form, BindException errors) throws InvalidDesignException
        {
            logger.info("Processing request with Authorization header: " + getViewContext().getRequest().getHeader("Authorization"));
            FileSurveyDesignProvider provider = new FileSurveyDesignProvider(getContainer(), logger);
            return provider.getSurveyDesign(getContainer(), form.getStudyId(), form.getActivityId(), form.getActivityVersion());
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class TokenBatchAction extends SimpleViewAction
    {
        @Override
        public void addNavTrail(NavTree root)
        {
        }

        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            setTitle("Enrollment Token Batches");
            return new EnrollmentTokenBatchesWebPart(getViewContext());
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class TokenListAction extends SimpleViewAction
    {
        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Token Batches", getEnrollmentTokenBatchURL());
            root.addChild("Enrollment Tokens");
        }

        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            setTitle("Enrollment Tokens");
            return new EnrollmentTokensWebPart(getViewContext());
        }
    }

    @RequiresPermission(GenerateEnrollmentTokensPermission.class)
    public class GenerateTokensAction extends MutatingApiAction<GenerateTokensForm>
    {
        @Override
        public void validateForm(GenerateTokensForm form, Errors errors)
        {
            if (form == null)
                errors.reject(ERROR_MSG, "Invalid input format. Please check the log for errors.");
            else if (form.getCount() == null || form.getCount() <= 0)
                errors.reject(ERROR_MSG, "Count must be provided and greater than 0.");
        }

        @Override
        public Object execute(GenerateTokensForm form, BindException errors)
        {
            try
            {
                EnrollmentTokenBatch batch = ResponseManager
                    .get().createTokenBatch(form.getCount(), getUser(), getContainer());
                return success(PageFlowUtil.map("batchId", batch.getRowId()));
            }
            catch (Exception e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
                return failure(errors);
            }
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class StudyConfigAction extends MutatingApiAction<StudyConfigForm>
    {
        @Override
        public void validateForm(StudyConfigForm form, Errors errors)
        {
            MobileAppStudy study = ResponseManager.get().getStudy(getContainer());
            if (form == null)
                errors.reject(ERROR_MSG, "Invalid input format.  Please check the log for errors.");
            else if (StringUtils.isEmpty(form.getShortName()))
                errors.reject(ERROR_REQUIRED, "StudyId must be provided.");
            else if (ResponseManager.get().studyExistsAsSibling(form.getShortName(), getContainer()))
                errors.rejectValue("shortName", ERROR_MSG, "StudyId '" + form.getShortName() + "' is already associated with a different container within this folder. Each study can be associated with only one container per folder.");
            //Check if study exists, name has changed, and at least one participant has enrolled
            else if (study != null && !study.getShortName().equals(form.getShortName()) && ResponseManager.get().hasStudyParticipants(getContainer()))
                errors.rejectValue("shortName", ERROR_MSG, "This container already has a study with participant data associated with it.  Each container can be configured with only one study and cannot be reconfigured once participant data is present.");
        }

        @Override
        public Object execute(StudyConfigForm form, BindException errors)
        {
            // if submitting again with the same id in the same container, return the existing study object
            MobileAppStudy study = ResponseManager.get().getStudy(getContainer());
            if (study == null || !study.getShortName().equals(form.getShortName()) || study.getCollectionEnabled() != form.getCollectionEnabled())
                study = ResponseManager
                    .get().insertOrUpdateStudy(form.getShortName(), form.getCollectionEnabled(), getContainer(), getUser());

            return success(PageFlowUtil.map(
                "rowId", study.getRowId(),
                "studyId", study.getShortName(),
                "collectionEnabled", study.getCollectionEnabled()
            ));
        }
    }

    /**
     * Ignores the request container. Pulls container context from the appToken used in request
     */
    @RequiresNoPermission
    @CSRF(CSRF.Method.NONE) // No need for CSRF token; request includes a secret (the app token). Plus, mobile app has no ability to provide CSRF token.
    public class ProcessResponseAction extends MutatingApiAction<ResponseForm>
    {
        @Override
        public void validateForm(ResponseForm form, Errors errors)
        {
            //Check if form is valid
            if (form == null)
            {
                errors.reject(ERROR_MSG, "Please check the log for errors.");
                return;
            }

            form.validate(errors);
        }

        @Override
        public Object execute(ResponseForm form, BindException errors)
        {
            //Record response blob
            ResponseManager manager = ResponseManager.get();
            //Null checks are done in the validate method
            SurveyResponse resp = new SurveyResponse(
                    form.getParticipantId(),
                    form.getData().toString(),
                    form.getMetadata().getActivityId(),
                    form.getMetadata().getVersion(),
                    form.getMetadata().getLanguage()
            );
            resp = manager.insertResponse(resp);

            //Add a parsing job
            final Integer rowId = resp.getRowId();
            manager.enqueueSurveyResponse(() -> ResponseManager.get().shredSurveyResponse(rowId, getUser()));

            return success();
        }
    }

    /**
     * Ignores request container. Pulls container context from the appToken used in request
     */
    @RequiresNoPermission
    @CSRF(CSRF.Method.NONE) // No need for CSRF token; request includes a secret (the app token). Plus, mobile app has no ability to provide CSRF token.
    public class WithdrawFromStudyAction extends MutatingApiAction<WithdrawFromStudyForm>
    {
        @Override
        public void validateForm(WithdrawFromStudyForm form, Errors errors)
        {
            //Check if form is valid
            if (form == null)
            {
                errors.reject(ERROR_MSG, "Please check the log for errors.");
                return;
            }

            if (StringUtils.isBlank(form.getParticipantId()))
                errors.reject(ERROR_REQUIRED, "ParticipantId not included in request");
            else if(!ResponseManager.get().participantExists(form.getParticipantId()))
                errors.reject(ERROR_REQUIRED, "Invalid ParticipantId.");
        }

        @Override
        public Object execute(WithdrawFromStudyForm form, BindException errors) throws Exception
        {
            ResponseManager.get().withdrawFromStudy(form.getParticipantId(), form.isDelete());
            return success();
        }
    }

    private abstract class BaseEnrollmentAction extends MutatingApiAction<EnrollmentForm>
    {
        @Override
        public void validateForm(EnrollmentForm form, Errors errors)
        {
            if (form == null)
            {
                errors.reject(ERROR_MSG, "Invalid input format.");
            }
            else
            {
                if (StringUtils.isEmpty(form.getShortName()))
                    //StudyId typically refers to the Study.rowId, however in this context it is the Study.shortName.  Issue #28419
                    errors.reject(ERROR_REQUIRED, "StudyId is required");
                else if (!ResponseManager.get().studyExists(form.getShortName()))
                    errors.rejectValue("studyId", ERROR_MSG, "Study with StudyId '" + form.getShortName() + "' does not exist");
                else if (StringUtils.isNotEmpty(form.getToken()))
                {
                    if (ResponseManager.get().hasParticipant(form.getShortName(), form.getToken()))
                        errors.reject(ERROR_MSG, "Token already in use");
                    else if (!ResponseManager.get().isChecksumValid(form.getToken()))
                        errors.rejectValue("token", ERROR_MSG, "Invalid token: '" + form.getToken() + "'");
                    else if (!ResponseManager.get().isValidStudyToken(form.getToken(), form.getShortName()))
                        errors.rejectValue("token", ERROR_MSG, "Unknown token: '" + form.getToken() + "'");
                }
                // we allow for the possibility that someone can enroll without using an enrollment token
                else if (ResponseManager.get().enrollmentTokenRequired(form.getShortName()))
                {
                    // Return the "Token is required" error message in the requested language
                    Language lang = Language.getLanguage(form.getLanguage());
                    errors.reject(ERROR_REQUIRED, lang.getTokenIsRequiredErrorMessage());
                }
            }
        }
    }

    /**
     * Execute the validation steps for an enrollment token without enrolling
     */
    @RequiresNoPermission
    @CSRF(CSRF.Method.NONE) // No need for CSRF token; request includes a secret (the enrollment token). Plus, mobile app has no ability to provide CSRF token.
    public class ValidateEnrollmentTokenAction extends BaseEnrollmentAction
    {
        @Override
        public Object execute(EnrollmentForm enrollmentForm, BindException errors) throws InvalidKeyException
        {
            //If action passes validation then Token is valid for container
            Collection<ParticipantProperty> participantProperties = enrollmentForm.getParticipantProperties(User.getSearchUser(), getContainer());
            ApiSimpleResponse response = new ApiSimpleResponse();
            response.put("success", true);
            response.put("preEnrollmentParticipantProperties", participantProperties);
            return response;
        }
    }

    // These are the only valid values. Can't use an enum for this... true and false are keywords
    private static final Set<String> ALLOW_SHARING_VALUES = Set.of("true", "false", "NA");

    @RequiresNoPermission
    @CSRF(CSRF.Method.NONE) // No need for CSRF token; request includes a secret (the enrollment token). Plus, mobile app has no ability to provide CSRF token.
    public class EnrollAction extends BaseEnrollmentAction
    {
        @Override
        public void validateForm(EnrollmentForm form, Errors errors)
        {
            super.validateForm(form, errors);

            String allowDataSharing = form.getAllowDataSharing();

            if (StringUtils.isBlank(allowDataSharing))
            {
                errors.reject(ERROR_REQUIRED, "allowDataSharing is required");
            }
            else if (!ALLOW_SHARING_VALUES.contains(allowDataSharing))
            {
                errors.rejectValue("allowDataSharing", ERROR_MSG, "Invalid allowDataSharing value: '" + form.getAllowDataSharing() + "'");
            }
        }

        @Override
        public Object execute(EnrollmentForm enrollmentForm, BindException errors)
        {
            Participant participant = ResponseManager
                .get().enrollParticipant(enrollmentForm.getShortName(), enrollmentForm.getToken(), enrollmentForm.getAllowDataSharing());
            return success(PageFlowUtil.map("appToken", participant.getAppToken()));
        }
    }

    /**
     * Look up and return the studyId associated with the passed in enrollment token to support token search, #40743
     */
    @RequiresNoPermission
    @CSRF(CSRF.Method.NONE) // No need for CSRF token; request includes a secret (the enrollment token). Plus, mobile app has no ability to provide CSRF token.
    public class ResolveEnrollmentTokenAction extends MutatingApiAction<EnrollmentForm>
    {
        @Override
        public void validateForm(EnrollmentForm form, Errors errors)
        {
            if (form == null)
                errors.reject(ERROR_MSG, "Invalid input format.");
            else if (StringUtils.isEmpty(form.getToken()))
                errors.reject(ERROR_REQUIRED, "Token is required");
            else if (!ResponseManager.get().isChecksumValid(form.getToken()))
                errors.rejectValue("token", ERROR_MSG, "Invalid token: '" + form.getToken() + "'");
        }

        @Override
        public Object execute(EnrollmentForm enrollmentForm, BindException errors)
        {
            String studyId = ResponseManager.get().findStudyShortName(enrollmentForm.getToken());
            boolean success = null != studyId;

            ApiSimpleResponse response = new ApiSimpleResponse();
            response.put("success", success);

            if (success)
            {
                response.put("studyId", studyId);
            }
            else
            {
                response.put("message", "Token is not associated with a study ID");
                getViewContext().getResponse().setStatus(404);
            }

            return response;
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class ReprocessResponseAction extends MutatingApiAction<ReprocessResponseForm>
    {
        private Set<Integer> _ids;

        @Override
        public void validateForm(ReprocessResponseForm form, Errors errors)
        {
            if (form == null)
            {
                errors.reject(ERROR_MSG, "Invalid input format.");
                return;
            }

            Set<String> listIds = DataRegionSelection.getSelected(getViewContext(), form.getKey(), false);
            _ids = listIds.stream().map(Integer::valueOf).collect(Collectors.toSet());

            if (_ids.isEmpty())
                errors.reject(ERROR_REQUIRED, "No responses to reprocess");
        }

        @Override
        public Object execute(ReprocessResponseForm form, BindException errors)
        {
            Set<Integer> nonErrorIds = ResponseManager.get().getNonErrorResponses(_ids);
            int enqueued = ResponseManager.get().reprocessResponses(getUser(), _ids);

            return success(PageFlowUtil.map("countReprocessed", enqueued, "notReprocessed", nonErrorIds));
        }
    }

    private abstract class BaseQueryAction<FORM extends SelectRowsForm> extends ReadOnlyApiAction<FORM>
    {
        @Override
        public final @NotNull BindException defaultBindParameters(FORM form, PropertyValues params)
        {
            ParticipantForm participantForm = new ParticipantForm();
            BindException exception = defaultBindParameters(participantForm, getCommandName(), getPropertyValues());

            if (!exception.hasErrors())
                exception = super.defaultBindParameters(form, params);

            if (!exception.hasErrors())
                form.setParticipantForm(participantForm);

            return exception;
        }

        @Override
        public void validateForm(FORM form, Errors errors)
        {
            super.validateForm(form, errors);

            // Error when binding means null ParticipantForm, #33486
            if (!errors.hasErrors())
                form.getParticipantForm().validateForm(errors);
        }

        @Override
        public final Object execute(FORM form, BindException errors)
        {
            Participant participant = form.getParticipant();

            // ApiQueryResponse constructs a DataView that initializes its ViewContext from the root context, so we need
            // to modify the root with a read-everywhere user and the study container.
            ViewContext root = HttpView.getRootContext();

            // Shouldn't be null, but just in case
            if (null != root)
            {
                // Setting a ContextualRole would be cleaner, but HttpView.initViewContext() doesn't copy it over
                root.setUser(User.getSearchUser());
                root.setContainer(participant.getContainer());
            }

            return getResponse(form, errors);
        }

        @Override
        protected String getCommandClassMethodName()
        {
            return "getResponse";
        }

        abstract public ApiQueryResponse getResponse(FORM form, BindException errors);
    }

    @RequiresNoPermission
    @Action(ActionType.SelectData.class)
    public class SelectRowsAction extends BaseQueryAction<SelectRowsForm>
    {
        @Override
        public void validateForm(SelectRowsForm form, Errors errors)
        {
            super.validateForm(form, errors);

            if (!errors.hasErrors())
            {
                String queryName = StringUtils.trimToNull(form.getQueryName());

                if (null == queryName)
                    errors.reject(ERROR_REQUIRED, "No value was supplied for the required parameter 'queryName'");
                else if (null == form.getSchema().getTable(queryName))
                    errors.reject(ERROR_MSG, "Query '" + queryName + "' doesn't exist");
            }
        }

        @Override
        public ApiQueryResponse getResponse(SelectRowsForm form, BindException errors)
        {
            UserSchema schema = form.getSchema();

            // First parameter (ViewContext) is ignored, so just pass null
            QueryView view = QueryView.create(null, schema, form.getQuerySettings(), errors);

            return new ApiQueryResponse(view, false, true,
                    schema.getName(), form.getQueryName(), form.getQuerySettings().getOffset(), null,
                    false, false, false, false, false);
        }
    }

    @RequiresNoPermission
    @Action(ActionType.SelectData.class)
    public class ExecuteSqlAction extends BaseQueryAction<ExecuteSqlForm>
    {
        private String _sql;

        @Override
        public void validateForm(ExecuteSqlForm form, Errors errors)
        {
            super.validateForm(form, errors);

            if (!errors.hasErrors())
            {
                _sql = StringUtils.trimToNull(form.getSql());
                if (null == _sql)
                    errors.reject(ERROR_REQUIRED, "No value was supplied for the required parameter 'sql'");
            }
        }

        @Override
        public ApiQueryResponse getResponse(ExecuteSqlForm form, BindException errors)
        {
            //create a temp query settings object initialized with the posted LabKey SQL
            //this will provide a temporary QueryDefinition to Query
            QuerySettings settings = new TempQuerySettings(getViewContext(), _sql, form.getQuerySettings());

            //need to explicitly turn off various UI options that will try to refer to the
            //current URL and query string
            settings.setAllowChooseView(false);
            settings.setAllowCustomizeView(false);

            //build a query view using the schema and settings
            QueryView view = new QueryView(form.getSchema(), settings, errors);
            view.setShowRecordSelectors(false);
            view.setShowExportButtons(false);
            view.setButtonBarPosition(DataRegion.ButtonBarPosition.NONE);
            view.setShowPagination(false);

            return new ReportingApiQueryResponse(view, false, false, "sql", 0, null, false, false, false, false);
        }
    }

    public static class SelectRowsForm extends QueryForm
    {
        private ParticipantForm _participantForm = null;

        @Override
        protected @Nullable UserSchema createSchema()
        {
            // If this is being called then we've successfully validated and set the ParticipantForm
            // Return our special, filtered schema so getQuerySettings() works right
            return ReadResponsesQuerySchema.get(getParticipant());
        }

        Participant getParticipant()
        {
            return _participantForm.getParticipant();
        }

        ParticipantForm getParticipantForm()
        {
            return _participantForm;
        }

        void setParticipantForm(ParticipantForm participantForm)
        {
            _participantForm = participantForm;
        }
    }

    public static class ExecuteSqlForm extends SelectRowsForm
    {
        private String _sql;

        public String getSql()
        {
            return _sql;
        }

        public void setSql(String sql)
        {
            _sql = sql;
        }
    }

    public static class ReprocessResponseForm
    {
        private String _key;

        public String getKey()
        {
            return _key;
        }
        public void setKey(String key)
        {
            _key = key;
        }
    }


    public static class StudyConfigForm
    {
        private String _shortName;
        private boolean _collectionEnabled;

        public String getShortName()
        {
            return _shortName;
        }

        public void setShortName(String shortName)
        {
            _shortName = shortName;
        }

        public boolean getCollectionEnabled() {
            return _collectionEnabled;
        }
        public void setCollectionEnabled(boolean collectionEnabled) {
            _collectionEnabled = collectionEnabled;
        }

        //StudyId typically refers to the Study.rowId, however in this context it is the Study.shortName.  Issue #28419
        //Adding this since it could potentially be exposed
        public void setStudyId(String studyId)
        {
            setShortName(studyId);
        }

        public String getStudyId()
        {
            return _shortName;
        }
    }

    public static class EnrollmentForm
    {
        private String _token;
        private String _shortName;
        private String _allowDataSharing;
        private String _language;

        public String getToken()
        {
            return _token;
        }
        public void setToken(String token)
        {
            _token = isBlank(token) ? null : token.trim().toUpperCase();
        }

        public String getShortName()
        {
            return _shortName;
        }
        public void setShortName(String shortName)
        {
            _shortName = isBlank(shortName) ? null : shortName.trim().toUpperCase();
        }

        //StudyId typically refers to the Study.rowId, however in this context it is the Study.shortName.  Issue #28419
        public void setStudyId(String studyId)
        {
            setShortName(studyId);
        }

        public String getStudyId()
        {
            return _shortName;
        }

        public @NotNull Collection<ParticipantProperty> getParticipantProperties(User user, Container container) throws InvalidKeyException
        {
            return ResponseManager.get().getParticipantProperties(container, user, getToken(), getShortName(), true);
        }

        public String getAllowDataSharing()
        {
            return _allowDataSharing;
        }

        public void setAllowDataSharing(String allowDataSharing)
        {
            _allowDataSharing = allowDataSharing;
        }

        public String getLanguage()
        {
            return _language;
        }

        public void setLanguage(String language)
        {
            _language = language;
        }
    }

    public static class WithdrawFromStudyForm
    {
        private String _participantId;
        private boolean _delete;

        public boolean isDelete()
        {
            return _delete;
        }
        public void setDelete(boolean delete)
        {
            _delete = delete;
        }

        public String getParticipantId()
        {
            return _participantId;
        }
        public void setParticipantId(String participantId)
        {
            _participantId = participantId;
        }
    }

    public static class GenerateTokensForm
    {
        private Integer _count;

        public Integer getCount()
        {
            return _count;
        }

        public void setCount(Integer count)
        {
            _count = count;
        }
    }

    public static class ParticipantForm
    {
        private String _appToken;

        // Filled in by successful validate()
        protected Participant _participant;
        protected MobileAppStudy _study;

        //ParticipantId from JSON request is really the apptoken internally

        @JsonIgnore
        public String getAppToken()
        {
            return getParticipantId();
        }

        //ParticipantId from JSON request is really the apptoken internally
        public String getParticipantId()
        {
            return _appToken;
        }

        public void setParticipantId(String appToken)
        {
            _appToken = appToken;
        }

        public final void validate(Errors errors)
        {
            validateForm(errors);

            if (errors.hasErrors())
                LOG.error("Problem processing participant request: " + errors.getAllErrors().toString());
        }

        protected void validateForm(Errors errors)
        {
            String appToken = getAppToken();

            if (StringUtils.isBlank(appToken))
            {
                errors.reject(ERROR_REQUIRED, "ParticipantId not included in request");
            }
            else
            {
                //Check if there is an associated participant for the appToken
                _participant = ResponseManager.get().getParticipantFromAppToken(appToken);

                if (_participant == null)
                    errors.reject(ERROR_MSG, "Unable to identify participant");
                else if (Participant.ParticipantStatus.Withdrawn == _participant.getStatus())
                    errors.reject(ERROR_MSG, "Participant has withdrawn from study");
                else
                {
                    //Check if there is an associated study for the appToken
                    _study = ResponseManager.get().getStudyFromParticipant(_participant);
                    if (_study == null)
                        errors.reject(ERROR_MSG, "AppToken not associated with study");
                }
            }

            // If we have participant and study then we shouldn't have errors (and vice versa)
            assert (null != _participant && null != _study) == !errors.hasErrors();
        }

        @JsonIgnore
        public Participant getParticipant()
        {
            return _participant;
        }
    }

    public static class ResponseForm extends ParticipantForm
    {
        private String _type; // Unused, but don't delete... Jackson binding against our test responses goes crazy without it
        private JsonNode _data;
        private SurveyMetadata _metadata;

        public SurveyMetadata getMetadata()
        {
            return _metadata;
        }
        public void setMetadata(@NotNull SurveyMetadata metadata)
        {
            _metadata = metadata;
        }

        public JsonNode getData()
        {
            return _data;
        }
        public void setData(@NotNull JsonNode data)
        {
            _data = data;
        }

        public String getType()
        {
            return _type;
        }

        public void setType(String type)
        {
            _type = type;
        }

        @Override
        protected void validateForm(Errors errors)
        {
            // First, check if form's required fields are present
            SurveyMetadata info = getMetadata();

            if (info == null)
            {
                errors.reject(ERROR_REQUIRED, "Metadata not found");
            }
            else
            {
                super.validateForm(errors);

                if (!errors.hasErrors())
                {
                    if (isBlank(info.getActivityId()))
                        errors.reject(ERROR_REQUIRED, "ActivityId not included in request");
                    if (isBlank(info.getVersion()))
                        errors.reject(ERROR_REQUIRED, "SurveyVersion not included in request");
                    if (getData() == null)
                        errors.reject(ERROR_REQUIRED, "Response not included in request");
                    if (!_study.getCollectionEnabled())
                        errors.reject(ERROR_MSG, String.format("Response collection is not currently enabled for study [ %1s ]", _study.getShortName()));
                }
            }
        }
    }

    public static class ActivityMetadataForm
    {
        private String studyId;
        private String activityId;
        private String activityVersion;

        public String getStudyId()
        {
            return studyId;
        }

        public void setStudyId(String studyId)
        {
            this.studyId = studyId;
        }

        public String getActivityId()
        {
            return activityId;
        }

        public void setActivityId(String activityId)
        {
            this.activityId = activityId;
        }

        public String getActivityVersion()
        {
            return activityVersion;
        }

        public void setActivityVersion(String activityVersion)
        {
            this.activityVersion = activityVersion;
        }
    }

    @RequiresPermission(AdminPermission.class)
    public static class ForwardingSettingsAction extends FolderManagementViewPostAction<ForwardingSettingsForm>
    {
        @Override
        protected HttpView getTabView(ForwardingSettingsForm form, boolean reshow, BindException errors)
        {
            return new JspView<>("/org/labkey/response/view/MyStudiesResponseServerSettings.jsp", form, errors);
        }

        @Override
        public void validateCommand(ForwardingSettingsForm form, Errors errors)
        {
            form.getForwardingType().validateConfig(form, errors);
        }

        @Override
        public boolean handlePost(ForwardingSettingsForm form, BindException errors)
        {
            ResponseManager.get().setForwarderConfiguration(getContainer(), form);
            return true;
        }
    }

    public static class ForwardingSettingsForm
    {
        private ForwardingType forwardingType = ForwardingType.Disabled;
        private String basicURL;
        private String username;
        private String password;
        private String tokenRequestURL;
        private String tokenField;
        private String header;
        private String oauthURL;

        public ForwardingType getForwardingType ()
        {
            return forwardingType;
        }

        public void setForwardingType(ForwardingType forwardingType)
        {
            this.forwardingType = forwardingType;
        }

        public String getBasicURL()
        {
            return basicURL;
        }

        public void setBasicURL(String url)
        {
            this.basicURL = url;
        }

        public String getUsername()
        {
            return username;
        }

        public void setUsername(String username)
        {
            this.username = username;
        }

        public String getPassword()
        {
            return password;
        }

        public void setPassword(String password)
        {
            this.password = password;
        }

        public String getTokenRequestURL()
        {
            return tokenRequestURL;
        }

        public void setTokenRequestURL(String tokenRequestURL)
        {
            this.tokenRequestURL = tokenRequestURL;
        }

        public String getTokenField()
        {
            return tokenField;
        }

        public void setTokenField(String tokenField)
        {
            this.tokenField = tokenField;
        }

        public String getHeader()
        {
            return header;
        }

        public void setHeader(String header)
        {
            this.header = header;
        }

        public String getOauthURL()
        {
            return oauthURL;
        }

        public void setOauthURL(String oauthURL)
        {
            this.oauthURL = oauthURL;
        }

    }


    public static ActionURL getResponseForwardingSettingsURL(Container c)
    {
        return new ActionURL(ForwardingSettingsAction.class, c);
    }

    /**
     * Admin action to allow immediate updating of study metadata
     */
    @RequiresPermission(AdminPermission.class)
    public static class UpdateStudyMetadataAction extends MutatingApiAction<StudyMetadataForm>
    {
        @Override
        public void validateForm(StudyMetadataForm form, Errors errors)
        {
            form.validateForm(errors, getContainer());
        }

        @Override
        public Object execute(StudyMetadataForm studyMetadataForm, BindException errors)
        {
            try
            {
                ResponseManager.get().updateStudyDesign(getContainer(), getUser());
            }
            catch (Exception e)
            {
                logger.error("Unable to update study metadata: " + e.getMessage(), e);
                errors.reject(ERROR_MSG, e.getMessage());
            }

            return errors.hasErrors() ? failure(errors) : success();
        }
    }

    public static class StudyMetadataForm
    {
        private String studyId;

        public void setStudyId(String studyId)
        {
            this.studyId = studyId;
        }

        public String getStudyId()
        {
            return this.studyId;
        }

        public void validateForm(Errors errors, Container container)
        {
            if (StringUtils.isBlank(studyId))
                errors.reject(ERROR_REQUIRED, new String[] {studyId}, "StudyId required");
            else if (!ResponseManager.get().studyExists(studyId))
                errors.reject(ERROR_MSG, new String[] {studyId}, "StudyId provided does not match any known study");
            else if (!ResponseManager.get().getStudyContainers(studyId).contains(container))
                errors.reject(ERROR_MSG, new String[] {studyId}, "StudyId does not match study container requested");
        }
    }

    // This action was developed as sample code for BTC and as a starting point for an eventual API that will automate
    // the registration of new evaluators of the FDA MyStudies demo environment. See CreateFolderCommand.java as well.
    @RequiresSiteAdmin
    public static class CreateFolderAction extends MutatingApiAction<CreateFolderForm>
    {
        @Override
        public Object execute(CreateFolderForm form, BindException errors)
        {
            Container parent = getContainer();
            String folderName = form.getName();

            StringBuilder sb = new StringBuilder();
            if (!Container.isLegalName(folderName, parent.isRoot(), sb))
                throw new ApiUsageException(sb.toString());

            if (getContainer().hasChild(folderName))
                throw new ApiUsageException("Folder \"" + folderName + "\" already exists in the parent");

            // Create the folder and give it a specific folder type
            Container c = ContainerManager.createContainer(parent, folderName, form.getTitle(), form.getDescription(), NormalContainerType.NAME, getUser());
            // This clears out the default active modules set by createContainer(). My FolderType specifies the active modules that I want.
            // Or, if you prefer, you could put the modules you want in Set.of().
            c.setActiveModules(Set.of());
            c.setFolderType(FolderTypeManager.get().getFolderType("Mobile App Study"), getUser());

            return success();
        }
    }

    public static class CreateFolderForm
    {
        private String _name;
        private String _title;
        private String _description;

        public String getName()
        {
            return _name;
        }

        @SuppressWarnings("unused")
        public void setName(String name)
        {
            _name = name;
        }

        public String getTitle()
        {
            return _title;
        }

        @SuppressWarnings("unused")
        public void setTitle(String title)
        {
            _title = title;
        }

        public String getDescription()
        {
            return _description;
        }

        @SuppressWarnings("unused")
        public void setDescription(String description)
        {
            _description = description;
        }
    }

    @RequiresSiteAdmin
    public class ServerConfigurationAction extends FormViewAction<ServerConfigurationForm>
    {
        public static final String RESPONSE_SERVER_CONFIGURATION = "ResponseServerConfig";
        public static final String FILE = "file";
        public static final String WCP_SERVER = "wcpServer";

        public static final String  METADATA_LOAD_LOCATION = "metadataLoadLocation";
        public static final String METADATA_DIRECTORY = "metadataDirectory";
        public static final String WCP_BASE_URL = "wcpBaseURL";
        public static final String WCP_USERNAME = "wcpUsername";
        public static final String WCP_PASSWORD = "wcpPassword";

        @Override
        public ModelAndView getView(ServerConfigurationForm form, boolean reshow, BindException errors)
        {
            JspView<ServerConfigurationForm> view = new JspView<>("/org/labkey/response/view/responseServerConfiguration.jsp", form, errors);
            view.setTitle("Global Settings");
            return view;
        }

        @Override
        public boolean handlePost(ServerConfigurationForm form, BindException errors) throws Exception
        {
            if (form.getMetadataLoadLocation() != null && form.getMetadataLoadLocation().equals(FILE))
            {
                if (form.getMetadataDirectory() == null)
                    errors.addError(new LabKeyError("Metadata Directory path must not be blank"));
                else if (!Files.exists(Paths.get(form.getMetadataDirectory())))
                    errors.addError(new LabKeyError("Metadata Directory path is invalid"));
            }
            else if (form.getMetadataLoadLocation() != null && form.getMetadataLoadLocation().equals(WCP_SERVER))
            {
                if (form.getWcpUsername() == null || form.getWcpUsername().isBlank())
                    errors.addError(new LabKeyError("WCP username must not be blank"));

                if (form.getWcpPassword() == null || form.getWcpPassword().isBlank())
                    errors.addError(new LabKeyError("WCP password must not be blank"));

                if (form.getWcpBaseURL() == null || !form.getWcpBaseURL().toUpperCase().matches("^(HTTP|HTTPS)://.*$"))
                    errors.addError(new LabKeyError("WCP Base URL must begin with 'http://' or 'https://'"));

                if (form.getWcpBaseURL() == null || !form.getWcpBaseURL().endsWith("/StudyMetaData"))
                    errors.addError(new LabKeyError("WCP Base URL must end with '/StudyMetaData'"));
            }

            if (errors.hasErrors())
                return false;
            else
            {
                PropertyManager.PropertyMap props = PropertyManager.getEncryptedStore().getWritableProperties(getContainer(), RESPONSE_SERVER_CONFIGURATION, true);
                Map<String, String> valuesToPersist = form.getOptions();

                if (!valuesToPersist.isEmpty())
                {
                    props.putAll(valuesToPersist);
                    props.save();
                    return true;
                }
            }

            return false;
        }

        @Override
        public URLHelper getSuccessURL(ServerConfigurationForm serverConfigurationForm)
        {
            return urlProvider(AdminUrls.class).getAdminConsoleURL();
        }

        @Override
        public void validateCommand(ServerConfigurationForm target, Errors errors)
        {

        }

        @Override
        public void addNavTrail(NavTree root)
        {
            setHelpTopic(new MyStudiesHelpTopic("setup"));
            urlProvider(AdminUrls.class).addAdminNavTrail(root, "Response Server Configuration", getClass(), getContainer());
        }
    }

    public static class ServerConfigurationForm
    {
        private String _metadataDirectory;
        private String _wcpBaseURL;
        private String _wcpUsername;
        private String _wcpPassword;
        private String _metadataLoadLocation;

        public String getMetadataDirectory()
        {
            return _metadataDirectory;
        }

        public void setMetadataDirectory(String metadataDirectory)
        {
            _metadataDirectory = metadataDirectory;
        }

        public String getWcpBaseURL()
        {
            return _wcpBaseURL;
        }

        public void setWcpBaseURL(String wcpBaseURL)
        {
            _wcpBaseURL = wcpBaseURL;
        }

        public String getWcpUsername()
        {
            return _wcpUsername;
        }

        public void setWcpUsername(String wcpUsername)
        {
            _wcpUsername = wcpUsername;
        }

        public String getWcpPassword()
        {
            return _wcpPassword;
        }

        public void setWcpPassword(String wcpPassword)
        {
            _wcpPassword = wcpPassword;
        }

        public String getMetadataLoadLocation()
        {
            return _metadataLoadLocation;
        }

        public void setMetadataLoadLocation(String metadataLoadLocation)
        {
            _metadataLoadLocation = metadataLoadLocation;
        }

        public Map<String, String> getOptions()
        {
            Map<String, String> valueMap = new HashMap<>();

            valueMap.put(ServerConfigurationAction.METADATA_LOAD_LOCATION, _metadataLoadLocation);

            valueMap.put(ServerConfigurationAction.METADATA_DIRECTORY, _metadataDirectory);
            valueMap.put(ServerConfigurationAction.WCP_BASE_URL, _wcpBaseURL);
            valueMap.put(ServerConfigurationAction.WCP_USERNAME, _wcpUsername);
            valueMap.put(ServerConfigurationAction.WCP_PASSWORD, _wcpPassword);
            return valueMap;
        }
    }
}
