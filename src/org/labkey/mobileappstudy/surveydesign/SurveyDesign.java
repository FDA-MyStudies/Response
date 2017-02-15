package org.labkey.mobileappstudy.surveydesign;

import org.labkey.mobileappstudy.data.SurveyMetadata;

import java.util.List;

/**
 * Created by iansigmon on 2/2/17.
 */
public class SurveyDesign
{
    private SurveyMetadata _metadata;
    private List<SurveyStep> steps;

    public List<SurveyStep> getSteps()
    {
        return steps;
    }
    public void setSteps(List<SurveyStep> steps)
    {
        this.steps = steps;
    }

    public SurveyMetadata getMetadata()
    {
        return _metadata;
    }
    public void setMetadata(SurveyMetadata surveyMetadata)
    {
        _metadata = surveyMetadata;
    }

    public String getSurveyName()
    {
        return _metadata.getActivityId();
    }

}
