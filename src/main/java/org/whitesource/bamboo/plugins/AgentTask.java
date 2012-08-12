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

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import org.jetbrains.annotations.NotNull;

public class AgentTask implements TaskType
{
    @NotNull
    @java.lang.Override
    public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException
    {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final TaskResultBuilder taskResultBuilder = TaskResultBuilder.create(taskContext);
        final ConfigurationMap configurationMap = taskContext.getConfigurationMap();

        final String organizationToken = configurationMap.get(AgentTaskConfigurator.ORGANIZATION_TOKEN);
        final String projectToken = configurationMap.get(AgentTaskConfigurator.PROJECT_TOKEN);
        final String includesPattern = configurationMap.get(AgentTaskConfigurator.INCLUDES_PATTERN);
        final String excludesPattern = configurationMap.get(AgentTaskConfigurator.EXCLUDES_PATTERN);
        buildLogger.addBuildLogEntry("White Source configuration:" + "\r\tproject token is '" + projectToken
                + "'\r\tincludes pattern is '" + includesPattern + "'\r\texcludes pattern is '" + excludesPattern + "'");

        return taskResultBuilder.build();
    }
}