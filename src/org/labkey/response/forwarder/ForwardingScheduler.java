/*
 * Copyright (c) 2019 LabKey Corporation
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
package org.labkey.response.forwarder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.collections.ConcurrentHashSet;
import org.labkey.api.data.Container;
import org.labkey.api.util.DateUtil;
import org.labkey.response.ResponseManager;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;


public class ForwardingScheduler
{
    private static JobDetail job = null;
    private static final Logger logger = LogManager.getLogger(ForwardingScheduler.class);
    private static final int INTERVAL_MINUTES = 5;
    private static final ForwardingScheduler instance = new ForwardingScheduler();
    private static final Set<String> enabledContainers = new ConcurrentHashSet<>();

    private TriggerKey triggerKey;

    private ForwardingScheduler()
    {
    }

    public static ForwardingScheduler get()
    {
        return instance;
    }

    public synchronized void schedule()
    {
        enabledContainers.clear();
        enabledContainers.addAll(refreshEnabledContainers());

        if (job == null)
        {
            job = JobBuilder.newJob(SurveyResponseForwardingJob.class)
                .withIdentity(ForwardingScheduler.class.getCanonicalName(), ForwardingScheduler.class.getCanonicalName())
                .usingJobData("surveyResponseForwarder", ForwardingScheduler.class.getCanonicalName())
                .build();
        }

        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(ForwardingScheduler.class.getCanonicalName(), ForwardingScheduler.class.getCanonicalName())
            .withSchedule(SimpleScheduleBuilder.repeatMinutelyForever(getIntervalMinutes()))
            .forJob(job)
            .build();

        this.triggerKey = trigger.getKey();

        try
        {
            StdSchedulerFactory.getDefaultScheduler().scheduleJob(job, trigger);
            logger.info(String.format("SurveyResponseForwarder scheduled to run every %1$S minutes. Next runtime %2$s", getIntervalMinutes(), DateUtil.formatDateTimeISO8601(trigger.getNextFireTime())));
        }
        catch (SchedulerException e)
        {
            logger.error("Failed to schedule SurveyResponseForwarder.", e);
        }
    }

    public synchronized void unschedule()
    {
        try
        {
            StdSchedulerFactory.getDefaultScheduler().unscheduleJob(triggerKey);
            logger.info("SurveyResponseForwarder has been unscheduled.");
        }
        catch (SchedulerException e)
        {
            logger.error("Failed to unschedule SurveyResponseForwarder.", e);
        }
    }

    private Set<String> refreshEnabledContainers()
    {
        return ResponseManager.get().getStudyContainers().stream()
            .filter(c -> ResponseManager.get().isForwardingEnabled(c))
            .map(Container::getId)
            .collect(Collectors.toSet());
    }

    public Collection<String> enabledContainers()
    {
        return Collections.unmodifiableSet(enabledContainers);
    }

    public void enableContainer(Container c, boolean enable)
    {
        if (enable)
            enabledContainers.add(c.getId());
        else
            enabledContainers.remove(c.getId());
    }

    public boolean forwardingIsEnabled(Container c)
    {
        if (null == c)
            return false;

        return enabledContainers.contains(c.getId());
    }

    protected int getIntervalMinutes()
    {
        return INTERVAL_MINUTES;
    }
}
