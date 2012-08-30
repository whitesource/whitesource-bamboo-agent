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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.whitesource.agent.api.dispatch.UpdateInventoryResult;
import org.whitesource.agent.api.model.AgentProjectInfo;
import org.whitesource.api.client.WhitesourceService;
import org.whitesource.api.client.WssServiceException;
import org.whitesource.bamboo.agent.BaseOssInfoExtractor;
import org.whitesource.bamboo.agent.GenericOssInfoExtractor;
import org.whitesource.bamboo.agent.WssUtils;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.variable.CustomVariableContextImpl;
import com.intellij.openapi.util.text.StringUtil;

public class AgentTask extends CustomVariableContextImpl implements TaskType
{
    private static final String LOG_COMPONENT = "AgentTask";

    protected final Logger log = LoggerFactory.getLogger(BaseOssInfoExtractor.class);

    @NotNull
    @java.lang.Override
    public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException
    {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final TaskResultBuilder taskResultBuilder = TaskResultBuilder.create(taskContext);
        final ConfigurationMap configurationMap = taskContext.getConfigurationMap();
        final Map<String, String> variableMap = new HashMap<String, String>();

        // @todo: convert to data loop.
        variableMap.put("organizationToken", configurationMap.get(AgentTaskConfigurator.ORGANIZATION_TOKEN));
        variableMap.put("projectToken", configurationMap.get(AgentTaskConfigurator.PROJECT_TOKEN));
        variableMap.put("includesPattern", configurationMap.get(AgentTaskConfigurator.INCLUDES_PATTERN));
        variableMap.put("excludesPattern", configurationMap.get(AgentTaskConfigurator.EXCLUDES_PATTERN));

        validateVariableSubstitution(buildLogger, taskResultBuilder, variableMap);

        Collection<AgentProjectInfo> projectInfos = collectOssUsageInformation(buildLogger, variableMap, taskContext
                .getBuildContext().getProjectName(), taskContext.getRootDirectory());

        updateOssInventory(buildLogger, taskResultBuilder, variableMap, projectInfos);

        return taskResultBuilder.build();
    }

    private void updateOssInventory(final BuildLogger buildLogger, final TaskResultBuilder taskResultBuilder,
            final Map<String, String> variableMap, Collection<AgentProjectInfo> projectInfos)
    {
        if (CollectionUtils.isEmpty(projectInfos))
        {
            buildLogger.addBuildLogEntry("No open source information found.");
        }
        else
        {
            buildLogger.addBuildLogEntry("Sending to White Source:");
            WhitesourceService service = WssUtils.createServiceClient();
            try
            {
                final String token = variableMap.get("organizationToken");
                final UpdateInventoryResult updateResult = service.update(token, projectInfos);
                logUpdateResult(updateResult, buildLogger);
                buildLogger.addBuildLogEntry("Successfully updated White Source.");
            }
            catch (WssServiceException e)
            {
                taskResultBuilder.failedWithError();
                buildLogger.addErrorLogEntry("Communication with White Source failed.", e);
            }
            finally
            {
                service.shutdown();
            }
        }
    }

    private void validateVariableSubstitution(final BuildLogger buildLogger, final TaskResultBuilder taskResultBuilder,
            final Map<String, String> variableMap)
    {
        buildLogger.addBuildLogEntry("White Source configuration:");
        for (Entry<String, String> variable : variableMap.entrySet())
        {
            final String value = variable.getKey().equals("organizationToken") ? "********" : variable.getValue();

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
            final Map<String, String> variableMap, final String projectName, final java.io.File rootDirectory)
    {
        buildLogger.addBuildLogEntry("Collecting OSS usage information");
        // REVIEW: the naming concerning 'includes' vs. 'includesPattern' is confusing down the call stack!
        BaseOssInfoExtractor extractor = new GenericOssInfoExtractor(projectName, variableMap.get("projectToken"),
                variableMap.get("includesPattern"), variableMap.get("excludesPattern"), rootDirectory, buildLogger);
        Collection<AgentProjectInfo> projectInfos = extractor.extract();

        return projectInfos;
    }

    private boolean isSubstitutionValid(final String variable)
    {
        return !variable.contains("${");
    }

    private void logUpdateResult(UpdateInventoryResult result, BuildLogger buildLogger)
    {
        log.info(WssUtils.logMsg(LOG_COMPONENT, "update success"));

        buildLogger.addBuildLogEntry("White Source update results: ");
        buildLogger.addBuildLogEntry("White Source organization: " + result.getOrganization());
        buildLogger.addBuildLogEntry(result.getCreatedProjects().size() + " Newly created projects:");
        StringUtil.join(result.getCreatedProjects(), ",");
        buildLogger.addBuildLogEntry(result.getUpdatedProjects().size() + " existing projects were updated:");
        StringUtil.join(result.getUpdatedProjects(), ",");
    }
}