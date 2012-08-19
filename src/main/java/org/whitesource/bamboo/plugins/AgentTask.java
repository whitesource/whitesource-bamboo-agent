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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jetbrains.annotations.NotNull;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.variable.CustomVariableContextImpl;

public class AgentTask extends CustomVariableContextImpl implements TaskType
{
    @NotNull
    @java.lang.Override
    public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException
    {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final TaskResultBuilder taskResultBuilder = TaskResultBuilder.create(taskContext);
        final ConfigurationMap configurationMap = taskContext.getConfigurationMap();
        final Map<String, String> variableMap = new HashMap<String, String>();

        // @todo: convert to data loop.
        variableMap.put("organizationToken",
                substituteString(configurationMap.get(AgentTaskConfigurator.ORGANIZATION_TOKEN)));
        variableMap.put("projectToken", substituteString(configurationMap.get(AgentTaskConfigurator.PROJECT_TOKEN)));
        variableMap.put("includesPattern",
                substituteString(configurationMap.get(AgentTaskConfigurator.INCLUDES_PATTERN)));
        variableMap.put("excludesPattern",
                substituteString(configurationMap.get(AgentTaskConfigurator.EXCLUDES_PATTERN)));

        buildLogger.addBuildLogEntry("White Source configuration:");
        for (Entry<String, String> variable : variableMap.entrySet())
        {
            final String value = variable.getValue().contains("password") ? "********" : variable.getValue();
            buildLogger.addBuildLogEntry("... " + variable.getKey() + " is '" + value + "'");
            if (!isSubstitutionValid(variable.getValue()))
            {
                buildLogger
                        .addErrorLogEntry("... "
                                + variable.getKey()
                                + " contains unresolved variable substitutions - please add a matching global or plan variable.");
                taskResultBuilder.failed();
            }
        }

        return taskResultBuilder.build();
    }

    private boolean isSubstitutionValid(final String variable)
    {
        return !variable.contains("${");
    }
}