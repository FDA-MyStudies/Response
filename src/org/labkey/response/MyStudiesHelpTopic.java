package org.labkey.response;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.util.HelpTopic;

public class MyStudiesHelpTopic extends HelpTopic
{
    private static final String MY_STUDIES_HELP_LINK_PREFIX = "https://www.labkey.org/FDAMyStudiesHelp/wiki-page.view?name=";

    public MyStudiesHelpTopic(@NotNull String topic)
    {
        super(topic);
    }

    @Override
    public String getHelpTopicHref()
    {
        return MY_STUDIES_HELP_LINK_PREFIX + getTopic();
    }
}
