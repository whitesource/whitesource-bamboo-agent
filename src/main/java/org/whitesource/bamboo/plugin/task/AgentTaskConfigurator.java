package org.whitesource.bamboo.plugin.task;

import static org.whitesource.bamboo.plugin.Constants.API_KEY;
import static org.whitesource.bamboo.plugin.Constants.CTX_MAVEN_JOB;
import static org.whitesource.bamboo.plugin.Constants.CTX_PLAN;
import static org.whitesource.bamboo.plugin.Constants.DEFAULT_IGNORE_POM;
import static org.whitesource.bamboo.plugin.Constants.DEFAULT_SERVICE_URL;
import static org.whitesource.bamboo.plugin.Constants.DEFAULT_TYPE;
import static org.whitesource.bamboo.plugin.Constants.FIELD_COLLECTION;
import static org.whitesource.bamboo.plugin.Constants.FILES_INCLUDE_PATTERN;
import static org.whitesource.bamboo.plugin.Constants.GENERIC_TYPE;
import static org.whitesource.bamboo.plugin.Constants.IGNORE_POM;
import static org.whitesource.bamboo.plugin.Constants.MAVEN_TYPE;
import static org.whitesource.bamboo.plugin.Constants.PROJECT_TYPE;
import static org.whitesource.bamboo.plugin.Constants.PROJECT_TYPES;
import static org.whitesource.bamboo.plugin.Constants.SERVICE_URL_KEYWORD;
import static org.whitesource.bamboo.plugin.Constants.TYPE_MAP;

import java.util.Collections;
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
import com.atlassian.bamboo.task.TaskRequirementSupport;
import com.atlassian.bamboo.util.Narrow;
import com.atlassian.bamboo.utils.BambooPredicates;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.agent.capability.Requirement;
import com.google.common.collect.Iterables;

public class AgentTaskConfigurator extends AbstractTaskConfigurator implements TaskRequirementSupport {
	
	private Job mavenJob;
	@NotNull
	@Override
	public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params,
			@Nullable final TaskDefinition previousTaskDefinition) {
		final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
		
		taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(config, params,
				Iterables.concat(FIELD_COLLECTION, getFieldCollection()));

		if (previousTaskDefinition != null) {
			config.put(PROJECT_TYPE, previousTaskDefinition.getConfiguration().get(PROJECT_TYPE));
		}

		return config;
	}
	
	@Override
	public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection){
	    super.validate(params, errorCollection);
	  
	    final String apiKey = params.getString(API_KEY);
	    final String includePattern = params.getString(FILES_INCLUDE_PATTERN);
	    if (StringUtils.isEmpty(apiKey)){
	        errorCollection.addError(API_KEY," Field can't be empty.");
	    }
	    
	    // for edit case we projectTypeForValidation, as there is a problem with the ui.bambooSection tag.
	    String projectType = params.getString(PROJECT_TYPE)!=null?params.getString(PROJECT_TYPE):params.getString("projectTypeForValidation");
	   
	    if (StringUtils.isEmpty(includePattern) && projectType!=null && projectType.equalsIgnoreCase(GENERIC_TYPE)){
	    	errorCollection.addError(FILES_INCLUDE_PATTERN, "Field can't be empty.");
	    }
	}
	
	@Override
	public void populateContextForCreate(@NotNull final Map<String, Object> context) {
		super.populateContextForCreate(context);
		context.put(PROJECT_TYPES, TYPE_MAP);
		context.put(PROJECT_TYPE, detectProjectType(context));
		context.put(IGNORE_POM, DEFAULT_IGNORE_POM);
		context.put(SERVICE_URL_KEYWORD, DEFAULT_SERVICE_URL);
		context.put("mode", "create");
	}

	@Override
	public void populateContextForEdit(@NotNull final Map<String, Object> context,
			@NotNull final TaskDefinition taskDefinition) {
		super.populateContextForEdit(context, taskDefinition);
		taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition,
				Iterables.concat(FIELD_COLLECTION, getFieldCollection()));
		context.put(PROJECT_TYPES, TYPE_MAP);
		context.put("mode", "edit");
	}

	@Override
	public void populateContextForView(@NotNull final Map<String, Object> context,
			@NotNull final TaskDefinition taskDefinition) {
		super.populateContextForView(context, taskDefinition);
		taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition,
				Iterables.concat(FIELD_COLLECTION, getFieldCollection()));
	}

	protected Set<String> getFieldCollection() {
		return FIELD_COLLECTION;
	}

	private Object detectProjectType(@NotNull final Map<String, Object> context) {
		String result = DEFAULT_TYPE;
		Job defaultJob = (Job) context.get(CTX_PLAN);
		if (defaultJob != null) {
			Plan plan = defaultJob.getParent();
			Chain chain = Narrow.to(plan, Chain.class);
			if (chain != null) {
				mavenJob = findMavenJob(chain);
				if (mavenJob != null) {
					context.put(CTX_MAVEN_JOB, mavenJob);
					result = MAVEN_TYPE;
				}
			}
		}

		return result;
	}

	@Nullable
	public static Job findMavenJob(@NotNull Chain chain) {
		for (Job job : chain.getAllJobs()) {
			if (Iterables.any(job.getBuildDefinition().getTaskDefinitions(), BambooPredicates
					.isTaskDefinitionPluginKeyEqual("com.atlassian.bamboo.plugins.maven:task.builder.mvn2"))) {
				return job;
			} else if (Iterables.any(job.getBuildDefinition().getTaskDefinitions(), BambooPredicates
					.isTaskDefinitionPluginKeyEqual("com.atlassian.bamboo.plugins.maven:task.builder.mvn3"))) {
				return job;
			}
		}
		return null;
	}

	@NotNull
    @Override
    public Set<Requirement> calculateRequirements(@NotNull TaskDefinition taskDefinition){
        return Collections.emptySet();
    }
}
