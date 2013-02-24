/*
 * Copyright (C) 2012 WhiteSource Software Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.whitesource.bamboo.plugins;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.whitesource.agent.api.dispatch.CheckPoliciesResult;
import org.whitesource.agent.api.dispatch.UpdateInventoryResult;
import org.whitesource.agent.api.model.AgentProjectInfo;
import org.whitesource.agent.client.WhitesourceService;
import org.whitesource.agent.client.WssServiceException;
import org.whitesource.agent.report.PolicyCheckReport;
import org.whitesource.bamboo.agent.BaseOssInfoExtractor;
import org.whitesource.bamboo.agent.GenericOssInfoExtractor;
import org.whitesource.bamboo.agent.MavenOssInfoExtractor;
import org.whitesource.bamboo.agent.WssUtils;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContext;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContextImpl;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContextImpl;

public class AgentTask extends CustomVariableContextImpl implements TaskType
{
    private static final String LOG_COMPONENT = "AgentTask";
    private static final String CONTACT_SUPPORT = "Encountered internal plugin error - please contact support!";

    protected final Logger log = LoggerFactory.getLogger(BaseOssInfoExtractor.class);

    @NotNull
    @java.lang.Override
    public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException
    {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final TaskResultBuilder taskResultBuilder = TaskResultBuilder.create(taskContext);
        final ConfigurationMap configurationMap = taskContext.getConfigurationMap();

        validateVariableSubstitution(buildLogger, taskResultBuilder, configurationMap);

        Collection<AgentProjectInfo> projectInfos = collectOssUsageInformation(buildLogger, configurationMap,
                taskContext.getBuildContext().getProjectName(), taskContext.getRootDirectory(), taskContext,
                taskResultBuilder);

        updateOssInventory(buildLogger, taskResultBuilder, configurationMap, taskContext.getBuildContext(),
                taskContext.getRootDirectory(), projectInfos);

        return taskResultBuilder.build();
    }

    private void updateOssInventory(final BuildLogger buildLogger, final TaskResultBuilder taskResultBuilder,
            final ConfigurationMap configurationMap, final BuildContext buildContext, final File buildDirectory,
            Collection<AgentProjectInfo> projectInfos)
    {
        if (CollectionUtils.isEmpty(projectInfos))
        {
            buildLogger.addBuildLogEntry("No open source information found.");
        }
        else
        {
            WhitesourceService service = WssUtils.createServiceClient();
            try
            {
                final String apiKey = configurationMap.get(AgentTaskConfigurator.API_KEY);
                final Boolean checkPolicies = configurationMap.getAsBoolean(AgentTaskConfigurator.CHECK_POLICIES);

                if (checkPolicies)
                {
                    buildLogger.addBuildLogEntry("Checking policies ...");
                    CheckPoliciesResult result = service.checkPolicies(apiKey, projectInfos);
                    reportCheckPoliciesResult(result, buildContext, buildDirectory, buildLogger);
                    if (result.hasRejections())
                    {
                        buildLogger.addErrorLogEntry("... open source rejected by organization policies.");
                        taskResultBuilder.failedWithError();
                    }
                    else
                    {
                        buildLogger.addBuildLogEntry("... all dependencies conform with open source policies.");
                        final UpdateInventoryResult updateResult = service.update(apiKey, projectInfos);
                        logUpdateResult(updateResult, buildLogger);
                        buildLogger.addBuildLogEntry("Successfully updated White Source.");
                    }
                }
                else
                {
                    buildLogger.addBuildLogEntry("Ignoring policies ...");
                    final UpdateInventoryResult updateResult = service.update(apiKey, projectInfos);
                    logUpdateResult(updateResult, buildLogger);
                    buildLogger.addBuildLogEntry("Successfully updated White Source.");
                }
            }
            catch (WssServiceException e)
            {
                buildLogger.addErrorLogEntry("Communication with White Source failed.", e);
                taskResultBuilder.failedWithError();
            }
            catch (IOException e)
            {
                buildLogger.addErrorLogEntry("Generating policy check report failed.", e);
                taskResultBuilder.failedWithError();
            }
            finally
            {
                service.shutdown();
            }
        }
    }

    private void validateVariableSubstitution(final BuildLogger buildLogger, final TaskResultBuilder taskResultBuilder,
            final Map<String, String> configurationMap)
    {
        buildLogger.addBuildLogEntry("White Source configuration:");
        for (Entry<String, String> variable : configurationMap.entrySet())
        {
            final String value = variable.getKey().equals(AgentTaskConfigurator.API_KEY) ? "********" : variable
                    .getValue();

            buildLogger.addBuildLogEntry("... " + variable.getKey() + " is '" + value + "'");
            if (!isSubstitutionValid(value))
            {
                buildLogger
                        .addErrorLogEntry("... "
                                + variable.getKey()
                                + " contains unresolved variable substitutions - please add a matching global or plan variable.");
                taskResultBuilder.failed();
            }
        }
    }

    private Collection<AgentProjectInfo> collectOssUsageInformation(final BuildLogger buildLogger,
            final ConfigurationMap configurationMap, final String projectName, final java.io.File rootDirectory,
            TaskContext taskContext, TaskResultBuilder taskResultBuilder)
    {
        final String projectType = configurationMap.get(AgentTaskConfigurator.PROJECT_TYPE);

        Collection<AgentProjectInfo> projectInfos = null;
        if (AgentTaskConfigurator.GENERIC_TYPE.equals(projectType))
        {
            buildLogger.addBuildLogEntry("Collecting OSS usage information (Freestyle)");

            // REVIEW: the naming concerning 'includes' vs. 'includesPattern' is confusing down the call stack!
            BaseOssInfoExtractor extractor = new GenericOssInfoExtractor(projectName,
                    configurationMap.get(AgentTaskConfigurator.PROJECT_TOKEN),
                    configurationMap.get(AgentTaskConfigurator.FILES_INCLUDE_PATTERN),
                    configurationMap.get(AgentTaskConfigurator.FILES_EXCLUDE_PATTERN), rootDirectory);
            projectInfos = extractor.extract();
        }
        else if (AgentTaskConfigurator.MAVEN_TYPE.equals(projectType))
        {
            buildLogger.addBuildLogEntry("Collecting OSS usage information (Maven)");

            // REVIEW: the naming concerning 'includes' vs. 'includesPattern' is confusing down the call stack!
            BaseOssInfoExtractor extractor = new MavenOssInfoExtractor(
                    configurationMap.get(AgentTaskConfigurator.PROJECT_TOKEN),
                    configurationMap.get(AgentTaskConfigurator.MODULE_TOKENS),
                    configurationMap.get(AgentTaskConfigurator.MODULES_INCLUDE_PATTERN),
                    configurationMap.get(AgentTaskConfigurator.MODULES_EXCLUDE_PATTERN),
                    configurationMap.getAsBoolean(AgentTaskConfigurator.IGNORE_POM), taskContext.getWorkingDirectory());
            projectInfos = extractor.extract();
        }
        else
        {
            buildLogger.addErrorLogEntry(CONTACT_SUPPORT);
            taskResultBuilder.failedWithError();
        }

        return projectInfos;
    }

    private boolean isSubstitutionValid(final String variable)
    {
        return !variable.contains("${");
    }

    private void reportCheckPoliciesResult(CheckPoliciesResult result, final BuildContext buildContext,
            final File buildDirectory, BuildLogger buildLogger) throws IOException
    {
        PolicyCheckReport report = new PolicyCheckReport(result, buildContext.getProjectName(),
                buildContext.getBuildResultKey());
        /*
         * NOTE: if used as is, report.generate() yields an exception 'Velocity is not initialized correctly' due to a
         * difficult to debug classpath issue, where 'ResourceManagerImpl instanceof ResourceManager' surprisingly
         * yields false, see https://github.com/whitesource/whitesource-bamboo-agent/issues/9 for details.
         * 
         * It turns out that Velocity isn't very OSGi friendly in the first place (despite being 'OSGi ready' since
         * version 1.7, see https://issues.apache.org/jira/browse/VELOCITY-694), for examples see e.g.
         * https://developer.atlassian.com/display/PLUGINFRAMEWORK/Troubleshooting+Velocity+in+OSGi and
         * http://stackoverflow.com/a/11437049/45773.
         * 
         * Even worse seems to be the reflection based dynamic class loading in place, which matches the issues outline
         * in http://wiki.eclipse.org/index.php/Context_Class_Loader_Enhancements#Problem_Description (another remotely
         * related issue is http://wiki.osgi.org/wiki/Avoid_Classloader_Hacks#Assumption_of_Global_Class_Visibility).
         * Fortunately the former provides an easy workaround for the single call at hand though, which is used here
         * accordingly (see http://wiki.eclipse.org/index.php/Context_Class_Loader_Enhancements#Context_Class_Loader_2).
         */
        File reportArchive = null;
        Thread thread = Thread.currentThread();
        ClassLoader loader = thread.getContextClassLoader();
        thread.setContextClassLoader(this.getClass().getClassLoader());
        try
        {
            reportArchive = report.generate(buildDirectory, true);
        }
        finally
        {
            thread.setContextClassLoader(loader);
        }

        if (reportArchive != null)
        {
            ArtifactDefinitionContext artifact = new ArtifactDefinitionContextImpl();
            artifact.setName(reportArchive.getName());
            artifact.setCopyPattern(reportArchive.getName());
            buildContext.getArtifactContext().getDefinitionContexts().add(artifact);
            log.info(WssUtils.logMsg(LOG_COMPONENT, "Defined artifact " + artifact));
        }
    }

    private void logUpdateResult(UpdateInventoryResult result, BuildLogger buildLogger)
    {
        log.info(WssUtils.logMsg(LOG_COMPONENT, "update success"));

        buildLogger.addBuildLogEntry("White Source update results: ");
        buildLogger.addBuildLogEntry("White Source organization: " + result.getOrganization());
        buildLogger.addBuildLogEntry(result.getCreatedProjects().size() + " Newly created projects:");
        StringUtils.join(result.getCreatedProjects(), ",");
        buildLogger.addBuildLogEntry(result.getUpdatedProjects().size() + " existing projects were updated:");
        StringUtils.join(result.getUpdatedProjects(), ",");
    }
}