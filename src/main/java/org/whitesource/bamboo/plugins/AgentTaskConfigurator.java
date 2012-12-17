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

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.atlassian.bamboo.build.Job;
import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.util.Narrow;
import com.atlassian.bamboo.utils.BambooPredicates;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.opensymphony.xwork.TextProvider;

public class AgentTaskConfigurator extends AbstractTaskConfigurator
{
    public static final String API_KEY = "apiKey";
    public static final String CHECK_POLICIES = "checkPolicies";
    public static final String PROJECT_TYPE = "projectType";
    public static final String PROJECT_TYPES = "projectTypes";
    public static final String PROJECT_TOKEN = "projectToken";
    public static final String MODULE_TOKENS = "moduleTokens";
    public static final String MODULES_INCLUDE_PATTERN = "modulesIncludePattern";
    public static final String MODULES_EXCLUDE_PATTERN = "modulesExcludePattern";
    public static final String FILES_INCLUDE_PATTERN = "filesIncludePattern";
    public static final String FILES_EXCLUDE_PATTERN = "filesExcludePattern";
    public static final String IGNORE_POM = "ignorePOM";
    private static final Set<String> FIELD_COLLECTION = ImmutableSet
            .<String> builder()
            .add(API_KEY, CHECK_POLICIES, PROJECT_TYPE, PROJECT_TOKEN, MODULE_TOKENS, MODULES_INCLUDE_PATTERN,
                    MODULES_EXCLUDE_PATTERN, FILES_INCLUDE_PATTERN, FILES_EXCLUDE_PATTERN, IGNORE_POM).build();
    public static final String GENERIC_TYPE = "Freestyle"; // @todo: an enum would be helpful of course ...
    public static final String MAVEN_TYPE = "Maven"; // @todo: an enum would be helpful of course ...
    private static final Map<String, String> TYPE_MAP = ImmutableMap.<String, String> builder()
            .put(MAVEN_TYPE, MAVEN_TYPE).put(GENERIC_TYPE, GENERIC_TYPE).build();
    protected static final String OPTION_TRUE = "True";
    protected static final String OPTION_FALSE = "False";
    public static final String DEFAULT_TYPE = GENERIC_TYPE;
    private static final String DEFAULT_FILES_INCLUDES_PATTERN = "lib/*.jar";
    public static final String DEFAULT_IGNORE_POM = OPTION_FALSE;
    public static final String DEFAULT_CHECK_POLICIES = OPTION_FALSE;
    public static final String CTX_PLAN = "plan";
    public static final String CTX_MAVEN_JOB = "mavenJob";
    private Job mavenJob;
    private TextProvider textProvider; // KLUDGE: unused currently, see validate().

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params,
            @Nullable final TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(config, params,
                Iterables.concat(FIELD_COLLECTION, getFieldCollection()));

        if (previousTaskDefinition != null)
        {
            config.put(PROJECT_TYPE, previousTaskDefinition.getConfiguration().get(PROJECT_TYPE));
        }

        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context)
    {
        super.populateContextForCreate(context);
        context.put(CHECK_POLICIES, DEFAULT_CHECK_POLICIES);
        context.put(PROJECT_TYPES, TYPE_MAP);
        context.put(PROJECT_TYPE, detectProjectType(context));
        context.put(FILES_INCLUDE_PATTERN, DEFAULT_FILES_INCLUDES_PATTERN);
        context.put(IGNORE_POM, DEFAULT_IGNORE_POM);
        context.put("mode", "create");
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context,
            @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition,
                Iterables.concat(FIELD_COLLECTION, getFieldCollection()));
        context.put(PROJECT_TYPES, TYPE_MAP);
        context.put("mode", "edit");
    }

    @Override
    public void populateContextForView(@NotNull final Map<String, Object> context,
            @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForView(context, taskDefinition);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition,
                Iterables.concat(FIELD_COLLECTION, getFieldCollection()));
    }

    @Override
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection)
    {
        super.validate(params, errorCollection);

        // KLUDGE/REVIEW: the i18n text should be provided by
        // textProvider.getText() apparently as per the code generated
        // via 'atlas-create-bamboo-plugin', however, this does not work at all,
        // see https://answers.atlassian.com/questions/20566 for a discussion;
        // replacing the call with getI18nBean().getText() works fine though.

        // @todo: validate substitutions are populated already to avoid a build cycle!
        // @todo: validate API key via the API to avoid a build cycle!
        final String apiKeyValue = params.getString(API_KEY);
        if (StringUtils.isEmpty(apiKeyValue))
        {
            errorCollection.addError(API_KEY, getI18nBean().getText("org.whitesource.bamboo.plugins.apiKey.error"));
        }
        final String projectType = params.getString(PROJECT_TYPE);
        final String errorText = validateProjectType(projectType);
        if (StringUtils.isNotEmpty(errorText))
        {
            errorCollection.addError(PROJECT_TYPE, getI18nBean().getText(errorText));
        }
        final String includesPatternValue = params.getString(FILES_INCLUDE_PATTERN);
        if (StringUtils.isEmpty(includesPatternValue))
        {
            errorCollection.addError(FILES_INCLUDE_PATTERN,
                    getI18nBean().getText("org.whitesource.bamboo.plugins.includesPattern.error"));
        }
        // @todo: add regular expression based validations for map and pattern input fields!
    }

    private Object detectProjectType(@NotNull final Map<String, Object> context)
    {
        // This function should apply some heuristics to determine the project type at hand (e.g. find a Maven job, or a
        // pom.xml or else. Once done it should provide a flash message with the result to the user in order to
        // communicate the review need.
        String result = DEFAULT_TYPE;

        // REVIEW: this might be achievable with a more direct approach, alas ...
        Job defaultJob = (Job) context.get(CTX_PLAN);
        if (defaultJob != null)
        {
            Plan plan = defaultJob.getParent();
            Chain chain = Narrow.to(plan, Chain.class);
            if (chain != null)
            {
                mavenJob = findMavenJob(chain);
                if (mavenJob != null)
                {
                    context.put(CTX_MAVEN_JOB, mavenJob);
                    result = MAVEN_TYPE;
                }
            }
        }

        return result;
    }

    /**
     * @param projectType
     *            - one of 'Maven' or 'Freestyle' currently
     * @return errorMessageKey - null for validated, else key for error message retrieval.
     */
    private String validateProjectType(final String projectType)
    {
        String result = null;

        if (MAVEN_TYPE.equals(projectType) && null == mavenJob)
        {
            result = "org.whitesource.bamboo.plugins.projectType.maven.error";
        }

        return result;
    }

    @Nullable
    public static Job findMavenJob(@NotNull Chain chain)
    {
        for (Job job : chain.getAllJobs())
        {
            if (Iterables.any(job.getBuildDefinition().getTaskDefinitions(), BambooPredicates
                    .isTaskDefinitionPluginKeyEqual("com.atlassian.bamboo.plugins.maven:task.builder.mvn2")))
            {
                return job;
            }
            else if (Iterables.any(job.getBuildDefinition().getTaskDefinitions(), BambooPredicates
                    .isTaskDefinitionPluginKeyEqual("com.atlassian.bamboo.plugins.maven:task.builder.mvn3")))
            {
                return job;
            }
        }
        return null;
    }

    public void setTextProvider(final TextProvider textProvider)
    {
        this.textProvider = textProvider;
    }

    protected Set<String> getFieldCollection()
    {
        return FIELD_COLLECTION;
    }
}
