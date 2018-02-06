package org.whitesource.bamboo.plugin.task;

import com.atlassian.bamboo.build.Job;
import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plugin.BambooPluginUtils;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.TaskRequirementSupport;
import com.atlassian.bamboo.util.Narrow;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.agent.capability.Requirement;
import io.atlassian.fugue.Iterables;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.whitesource.bamboo.plugin.Constants.*;


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
	    super. validate(params, errorCollection);
	  
	    final String apiKey = params.getString(API_KEY);
	    final String includePattern = params.getString(FILES_INCLUDE_PATTERN);
	    if (StringUtils.isEmpty(apiKey)){
	        errorCollection.addError(API_KEY," Field can't be empty.");
	    }
	    
	    // for edit case we projectTypeForValidation, as there is a problem with the ui.bambooSection tag.
	    String projectType = params.getString(PROJECT_TYPE)!=null ? params.getString(PROJECT_TYPE): params.getString("projectTypeForValidation");
	   
	    if (StringUtils.isEmpty(includePattern) && projectType!=null && projectType.equalsIgnoreCase(GENERIC_TYPE)){
	    	errorCollection.addError(FILES_INCLUDE_PATTERN, "Field can't be empty.");
	    }
	    //Check proxy settings field by user
		if (params.getBoolean(PROXY_SETTINGS)) {
	    	if (params.getString(PROXY_PORT) == null || params.getString(PROXY_HOST) == null) {
				errorCollection.addError(PROXY_PORT, PROXY_HOST, "Proxy Port and Proxy Host must be not null");
			}
	    	try {
				int port = Integer.parseInt(params.getString(PROXY_PORT));
			} catch (Exception e) {
				errorCollection.addError(PROXY_PORT, "Proxy port must be integer");
			}
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
			if (Iterables.any(job.getBuildDefinition().getTaskDefinitions(), BambooPluginUtils.pluginKeyEquals("com.atlassian.bamboo.plugins.maven:task.builder.mvn2"))) {
				return job;
			} else if (Iterables.any(job.getBuildDefinition().getTaskDefinitions(), BambooPluginUtils.pluginKeyEquals("com.atlassian.bamboo.plugins.maven:task.builder.mvn3"))) {
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
