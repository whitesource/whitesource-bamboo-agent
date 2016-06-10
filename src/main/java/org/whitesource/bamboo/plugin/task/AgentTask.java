package org.whitesource.bamboo.plugin.task;

import static org.whitesource.bamboo.plugin.Constants.API_KEY;
import static org.whitesource.bamboo.plugin.Constants.BUILD_SUCCESSFUL_MARKER;
import static org.whitesource.bamboo.plugin.Constants.CHECK_POLICIES;
import static org.whitesource.bamboo.plugin.Constants.CONTACT_SUPPORT;
import static org.whitesource.bamboo.plugin.Constants.FAIL_CHECK_POLICIES;
import static org.whitesource.bamboo.plugin.Constants.FILES_EXCLUDE_PATTERN;
import static org.whitesource.bamboo.plugin.Constants.FILES_INCLUDE_PATTERN;
import static org.whitesource.bamboo.plugin.Constants.GENERIC_TYPE;
import static org.whitesource.bamboo.plugin.Constants.IGNORE_POM;
import static org.whitesource.bamboo.plugin.Constants.JUST_CHECK_POLICIES;
import static org.whitesource.bamboo.plugin.Constants.KEY_VALUE_SPLIT_PATTERN;
import static org.whitesource.bamboo.plugin.Constants.LINES_TO_PARSE_FOR_ERRORS;
import static org.whitesource.bamboo.plugin.Constants.LOG_COMPONENT;
import static org.whitesource.bamboo.plugin.Constants.MAVEN_TYPE;
import static org.whitesource.bamboo.plugin.Constants.MODULES_EXCLUDE_PATTERN;
import static org.whitesource.bamboo.plugin.Constants.MODULES_INCLUDE_PATTERN;
import static org.whitesource.bamboo.plugin.Constants.MODULE_TOKENS;
import static org.whitesource.bamboo.plugin.Constants.PARAM_LIST_SPLIT_PATTERN;
import static org.whitesource.bamboo.plugin.Constants.PRODUCT_TOKEN;
import static org.whitesource.bamboo.plugin.Constants.PRODUCT_VERSION;
import static org.whitesource.bamboo.plugin.Constants.PROJECT_TOKEN;
import static org.whitesource.bamboo.plugin.Constants.PROJECT_TYPE;
import static org.whitesource.bamboo.plugin.Constants.SEARCH_BUILD_SUCCESS_FAIL_MESSAGE_EVERYWHERE;
import static org.whitesource.bamboo.plugin.Constants.SERVICE_URL_KEYWORD;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.whitesource.agent.api.dispatch.CheckPolicyComplianceResult;
import org.whitesource.agent.api.dispatch.UpdateInventoryResult;
import org.whitesource.agent.api.model.AgentProjectInfo;
import org.whitesource.agent.client.WhitesourceService;
import org.whitesource.agent.client.WssServiceException;
import org.whitesource.agent.report.PolicyCheckReport;
import org.whitesource.bamboo.plugin.config.AbstractMavenConfig;
import org.whitesource.bamboo.plugin.config.Maven2Config;
import org.whitesource.bamboo.plugin.config.Maven3Config;
import org.whitesource.bamboo.plugin.freestyle.BaseOssInfoExtractor;
import org.whitesource.bamboo.plugin.freestyle.GenericOssInfoExtractor;
import org.whitesource.bamboo.plugin.freestyle.WssUtils;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.logger.interceptors.ErrorMemorisingInterceptor;
import com.atlassian.bamboo.build.logger.interceptors.LogMemorisingInterceptor;
import com.atlassian.bamboo.build.logger.interceptors.StringMatchingInterceptor;
import com.atlassian.bamboo.builder.MavenLogHelper;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContext;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContextImpl;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.process.ExternalProcessBuilder;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.security.SecureToken;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskState;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.utils.BambooPredicates;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.utils.process.ExternalProcess;
import com.google.common.collect.Iterables;

public class AgentTask implements TaskType {

	private final CapabilityContext capabilityContext;
	private final EnvironmentVariableAccessor environmentVariableAccessor;
	private final ProcessService processService;
	protected final Logger log = LoggerFactory.getLogger(BaseOssInfoExtractor.class);

	public AgentTask(final CapabilityContext capabilityContext,
			final EnvironmentVariableAccessor environmentVariableAccessor, final ProcessService processService) {
		this.capabilityContext = capabilityContext;
		this.environmentVariableAccessor = environmentVariableAccessor;
		this.processService = processService;
	}

	@Override
	public TaskResult execute(TaskContext taskContext) throws TaskException {
		AbstractMavenConfig config = null;
		TaskResult result = null;

		final BuildLogger buildLogger = taskContext.getBuildLogger();
		final ConfigurationMap configurationMap = taskContext.getConfigurationMap();
		final String projectType = configurationMap.get(PROJECT_TYPE);
		TaskResultBuilder taskResultBuilder = TaskResultBuilder.newBuilder(taskContext);

		validateVariableSubstitution(buildLogger, taskResultBuilder, configurationMap);

		if (MAVEN_TYPE.equals(projectType)) {

			List<TaskDefinition> taskDefs = taskContext.getBuildContext().getBuildDefinition().getTaskDefinitions();

			if (Iterables.any(taskDefs, BambooPredicates
					.isTaskDefinitionPluginKeyEqual("com.atlassian.bamboo.plugins.maven:task.builder.mvn2"))) {
				config = new Maven2Config(taskContext, capabilityContext, environmentVariableAccessor);
			} else if (Iterables.any(taskDefs, BambooPredicates
					.isTaskDefinitionPluginKeyEqual("com.atlassian.bamboo.plugins.maven:task.builder.mvn3"))) {
				config = new Maven3Config(taskContext, capabilityContext, environmentVariableAccessor);
			}
			
			buildLogger.addBuildLogEntry("updating white source...");
			result = runMavenCommand(taskContext, config);
			if (result.getTaskState().equals(TaskState.SUCCESS)) {
				buildLogger.addBuildLogEntry("Successfully updated White Source.");
			} else if ((result.getTaskState().equals(TaskState.FAILED))) {
				buildLogger.addErrorLogEntry("Failed updated White Source.");
			} else if ((result.getTaskState().equals(TaskState.ERROR))) {
				buildLogger.addErrorLogEntry("Error while updating White Source.");
			}
			
		} else if (GENERIC_TYPE.equals(projectType)) {

			Collection<AgentProjectInfo> projectInfos = null;
			String fullPlanName = taskContext.getBuildContext().getPlanName();
			String planName = fullPlanName.substring(fullPlanName.indexOf("-")+1, fullPlanName.lastIndexOf("-")).trim();
			BaseOssInfoExtractor extractor = new GenericOssInfoExtractor(planName,
					configurationMap.get(PROJECT_TOKEN), configurationMap.get(FILES_INCLUDE_PATTERN),
					configurationMap.get(FILES_EXCLUDE_PATTERN), taskContext.getRootDirectory());
			projectInfos = extractor.extract();

			result = updateOssInventory(buildLogger, taskResultBuilder, configurationMap, taskContext.getBuildContext(),
					taskContext.getRootDirectory(), projectInfos);

		} else {

			buildLogger.addErrorLogEntry(CONTACT_SUPPORT);
			result = taskResultBuilder.failedWithError().build();
		}

		return result;
	}

	public static Map<String, String> splitParametersMap(String paramList) {

		Map<String, String> params = new HashMap<String, String>();

		List<String> kvps = splitParameters(paramList);
		if (kvps != null) {
			for (String kvp : kvps) {
				String[] split = KEY_VALUE_SPLIT_PATTERN.split(kvp);
				if (split.length == 2) {
					params.put(split[0], split[1]);
				}
			}
		}

		return params;
	}

	private boolean isSubstitutionValid(final String variable) {
		return !variable.contains("${");
	}

	private void validateVariableSubstitution(final BuildLogger buildLogger, final TaskResultBuilder taskResultBuilder,
			final Map<String, String> configurationMap) {
		buildLogger.addBuildLogEntry("White Source configuration:");
		for (Entry<String, String> variable : configurationMap.entrySet()) {
			final String value = variable.getKey().equals(API_KEY) ? "********" : variable.getValue();

			buildLogger.addBuildLogEntry("... " + variable.getKey() + " is '" + value + "'");
			if (!isSubstitutionValid(value)) {
				buildLogger.addErrorLogEntry("... " + variable.getKey()
						+ " contains unresolved variable substitutions - please add a matching global or plan variable.");
				taskResultBuilder.failed();
			}
		}
	}

	public static List<String> splitParameters(String paramList) {

		List<String> params = new ArrayList<String>();

		if (paramList != null) {
			String[] split = PARAM_LIST_SPLIT_PATTERN.split(paramList);
			if (split != null) {
				for (String param : split) {
					if (!(param == null || param.trim().length() == 0)) {
						params.add(param.trim());
					}
				}
			}
		}

		return params;
	}

	private TaskResult updateOssInventory(final BuildLogger buildLogger, final TaskResultBuilder taskResultBuilder,
			final ConfigurationMap configurationMap, final BuildContext buildContext, final File buildDirectory,
			Collection<AgentProjectInfo> projectInfos) {

		TaskResult taskResult = taskResultBuilder.build();
		UpdateInventoryResult updateResult = null;
		
		if (CollectionUtils.isEmpty(projectInfos)) {
			buildLogger.addBuildLogEntry("No open source information found.");
		} else {

			final String wssUrl = configurationMap.get(SERVICE_URL_KEYWORD);

			WhitesourceService service = WssUtils.createServiceClient(wssUrl);
			try {
				final String apiKey = configurationMap.get(API_KEY);
				String productTokenOrName = configurationMap.get(PRODUCT_TOKEN);
				final String productVersion = configurationMap.get(PRODUCT_VERSION);
				
				if(StringUtils.isEmpty(productTokenOrName)){
					productTokenOrName = buildContext.getProjectName();
				}
				
				final String checkPoliciesType = configurationMap.get(CHECK_POLICIES);

				if(JUST_CHECK_POLICIES.equalsIgnoreCase(checkPoliciesType) || FAIL_CHECK_POLICIES.equalsIgnoreCase(checkPoliciesType)){
					
					buildLogger.addBuildLogEntry("Checking policies ...");
					CheckPolicyComplianceResult result = service.checkPolicyCompliance(apiKey, productTokenOrName, productVersion, projectInfos, true);
					reportCheckPoliciesResult(result, buildContext, buildDirectory, buildLogger);
					
					if (result.hasRejections()) {
						
						buildLogger.addErrorLogEntry("... open source rejected by organization policies.");
						if(FAIL_CHECK_POLICIES.equalsIgnoreCase(checkPoliciesType)){
							//check policies fail the build if any violations.
							taskResult = taskResultBuilder.failedWithError().build();
							return taskResult;
						}
						
					} 
					//just check policies case
					updateResult = service.update(apiKey, productTokenOrName, productVersion, projectInfos);
					
				} else {
					// no policy check
					buildLogger.addBuildLogEntry("Not checked any policies and updating results.");
					updateResult = service.update(apiKey, productTokenOrName, productVersion, projectInfos);
					
				}
				
				if(updateResult==null || updateResult.getCreatedProjects().isEmpty() && updateResult.getUpdatedProjects().isEmpty()){
					log.error(WssUtils.logMsg(LOG_COMPONENT, "Wss create/update failed"));
					buildLogger.addErrorLogEntry("White Source project create/update failed, please check your tokens or Contact Administrator.");
					taskResult = taskResultBuilder.failedWithError().build();
				}else{
					log.info(WssUtils.logMsg(LOG_COMPONENT, "Wss create/update success"));
					buildLogger.addBuildLogEntry("White Source project create/update sucessfully");
					logUpdateResult(updateResult, buildLogger);
				}
				
			} catch (WssServiceException e) {
				buildLogger.addErrorLogEntry("Communication with White Source failed.", e);
				taskResult = taskResultBuilder.failedWithError().build();
			} catch (IOException e) {
				buildLogger.addErrorLogEntry("Generating policy check report failed.", e);
				taskResult = taskResultBuilder.failedWithError().build();
			} finally {
				service.shutdown();
			}
		}
		
		return taskResult;
	}

	private void reportCheckPoliciesResult(CheckPolicyComplianceResult result, final BuildContext buildContext,
			final File buildDirectory, BuildLogger buildLogger) throws IOException {
		PolicyCheckReport report = new PolicyCheckReport(result, buildContext.getProjectName(),
				buildContext.getBuildResultKey());
		/*
		 * NOTE: if used as is, report.generate() yields an exception 'Velocity
		 * is not initialized correctly' due to a difficult to debug classpath
		 * issue, where 'ResourceManagerImpl instanceof ResourceManager'
		 * surprisingly yields false, see
		 * https://github.com/whitesource/whitesource-bamboo-agent/issues/9 for
		 * details.
		 * 
		 * It turns out that Velocity isn't very OSGi friendly in the first
		 * place (despite being 'OSGi ready' since version 1.7, see
		 * https://issues.apache.org/jira/browse/VELOCITY-694), for examples see
		 * e.g. https://developer.atlassian.com/display/PLUGINFRAMEWORK/
		 * Troubleshooting+Velocity+in+OSGi and
		 * http://stackoverflow.com/a/11437049/45773.
		 * 
		 * Even worse seems to be the reflection based dynamic class loading in
		 * place, which matches the issues outline in
		 * http://wiki.eclipse.org/index.php/Context_Class_Loader_Enhancements#
		 * Problem_Description (another remotely related issue is
		 * http://wiki.osgi.org/wiki/Avoid_Classloader_Hacks#
		 * Assumption_of_Global_Class_Visibility). Fortunately the former
		 * provides an easy workaround for the single call at hand though, which
		 * is used here accordingly (see
		 * http://wiki.eclipse.org/index.php/Context_Class_Loader_Enhancements#
		 * Context_Class_Loader_2).
		 */

		File reportArchive = null;
		Thread thread = Thread.currentThread();
		ClassLoader loader = thread.getContextClassLoader();
		thread.setContextClassLoader(this.getClass().getClassLoader());
		try {
			reportArchive = report.generate(buildDirectory, true);
		} finally {
			thread.setContextClassLoader(loader);
		}

		if (reportArchive != null) {
			// ArtifactDefinitionContext artifact = new
			// ArtifactDefinitionContextImpl();
			ArtifactDefinitionContext artifact = new ArtifactDefinitionContextImpl(reportArchive.getName(), false,
					SecureToken.create());
			artifact.setName(reportArchive.getName());
			artifact.setCopyPattern(reportArchive.getName());
			buildContext.getArtifactContext().getDefinitionContexts().add(artifact);
			log.info(WssUtils.logMsg(LOG_COMPONENT, "Defined artifact " + artifact));
		}
	}

	private void logUpdateResult(UpdateInventoryResult result, BuildLogger buildLogger) {
		
		log.info(WssUtils.logMsg(LOG_COMPONENT, "update success"));
		buildLogger.addBuildLogEntry("White Source update results: ");
		buildLogger.addBuildLogEntry("White Source organization: " + result.getOrganization());
		buildLogger.addBuildLogEntry(result.getCreatedProjects().size() + " Newly created projects:");
		StringUtils.join(result.getCreatedProjects(), ",");
		buildLogger.addBuildLogEntry(result.getUpdatedProjects().size() + " existing projects were updated:");
		StringUtils.join(result.getUpdatedProjects(), ",");
		
	}

	private List<String> populateaParams(TaskContext taskContext) {
		List<String> paramsList = new ArrayList<String>();
		final ConfigurationMap configurationMap = taskContext.getConfigurationMap();
		Map<String, String> moduleTokens;
		List<String> includes;
		List<String> exclude;
		
		final String productToken = configurationMap.get(PRODUCT_TOKEN);
		StringBuilder productTokenParam = new StringBuilder();

		if (StringUtils.isNotBlank(productToken)) {
			productTokenParam.append("-D").append("org.whitesource.product").append("=").append(productToken);
			paramsList.add(productTokenParam.toString());
		}
		
		final String productVersion = configurationMap.get(PRODUCT_VERSION);
		StringBuilder productVersionParam = new StringBuilder();

		if (StringUtils.isNotBlank(productVersion)) {
			productVersionParam.append("-D").append("org.whitesource.productVersion").append("=").append(productVersion);
			paramsList.add(productVersionParam.toString());
		}
		
		final String projectToken = configurationMap.get(PROJECT_TOKEN);
		StringBuilder projectTokenParam = new StringBuilder();

		if (StringUtils.isNotBlank(projectToken)) {
			projectTokenParam.append("-D").append("org.whitesource.projectToken").append("=").append(projectToken);
			paramsList.add(projectTokenParam.toString());
		}

		final String moduleTokensString = configurationMap.get(MODULE_TOKENS);
		StringBuilder moduleTokensParam = new StringBuilder();

		if (StringUtils.isNotBlank(moduleTokensString)) {

			moduleTokens = splitParametersMap(moduleTokensString);
			moduleTokensParam.append("-D").append("org.whitesource.moduleTokens").append("=").append(moduleTokens);
			// TODO convert into key1=value1,key2=value2
			paramsList.add(moduleTokensParam.toString());
		}

		final String moduleIncTokens = configurationMap.get(MODULES_INCLUDE_PATTERN);
		StringBuilder modulesIncludePatternParam = new StringBuilder();

		if (StringUtils.isNotBlank(moduleIncTokens)) {

			includes = splitParameters(moduleIncTokens);

			modulesIncludePatternParam.append("-D").append("org.whitesource.includes").append("=")
					.append(moduleIncTokens);
			// TODO convert into value1,value2,
			paramsList.add(modulesIncludePatternParam.toString());
		}

		final String moduleExcTokens = configurationMap.get(MODULES_EXCLUDE_PATTERN);
		StringBuilder modulesExcludePatternParam = new StringBuilder();

		if (StringUtils.isNotBlank(moduleExcTokens)) {

			exclude = splitParameters(moduleIncTokens);

			modulesExcludePatternParam.append("-D").append("org.whitesource.excludes").append("=")
					.append(moduleExcTokens);
			// TODO convert into value1,value2,
			paramsList.add(modulesExcludePatternParam.toString());
		}

		final String ignorePOM = configurationMap.get(IGNORE_POM);
		StringBuilder ignorePOMParam = new StringBuilder();

		if (StringUtils.isNotBlank(ignorePOM) && ignorePOM.equalsIgnoreCase("true")) {
			ignorePOMParam.append("-D").append("org.whitesource.ignorePOM").append("=").append(ignorePOM);
			paramsList.add(ignorePOMParam.toString());
		}

		return paramsList;
	}

	private TaskResult runMavenCommand(TaskContext taskContext, AbstractMavenConfig config)
			throws TaskException {

		final BuildLogger buildLogger = taskContext.getBuildLogger();
		final ConfigurationMap configurationMap = taskContext.getConfigurationMap();
		final CurrentBuildResult currentBuildResult = taskContext.getBuildContext().getBuildResult();
		final String checkPoliciesType = configurationMap.get(CHECK_POLICIES);

		List<String> mavenCmd = new ArrayList<String>();
		mavenCmd.addAll(config.getCommandline());
		mavenCmd.add("-U");
		mavenCmd.add("org.whitesource:whitesource-maven-plugin:update");

		final String wssUrl = configurationMap.get(SERVICE_URL_KEYWORD);
		StringBuilder wssUrlParam = new StringBuilder();

		if (StringUtils.isNotBlank(wssUrl)) {
			wssUrlParam.append("-D").append(SERVICE_URL_KEYWORD).append("=").append(wssUrl);
			mavenCmd.add(wssUrlParam.toString());
		}
		
		final String apiKey = configurationMap.get(API_KEY);
		StringBuilder confParam = new StringBuilder();
		confParam.append("-D").append("org.whitesource.orgToken").append("=").append(apiKey);
		
		mavenCmd.add(confParam.toString());
		mavenCmd.addAll(populateaParams(taskContext));
		
		// we will do check policies only for FAIL_CHECK_POLICIES case.
		if( FAIL_CHECK_POLICIES.equalsIgnoreCase(checkPoliciesType)){
			
			StringBuilder checPolicyParam = new StringBuilder();
			checPolicyParam.append("-D").append("org.whitesource.checkPolicies").append("=").append(true);
			mavenCmd.add(checPolicyParam.toString());
			StringBuilder forceCheckParam = new StringBuilder();
			forceCheckParam.append("-D").append("org.whitesource.forceCheckAllDependencies").append("=").append(true);
			mavenCmd.add(forceCheckParam.toString());
		}

		buildLogger.addBuildLogEntry("Maven command to be executes ===> " + mavenCmd.toString());

		StringMatchingInterceptor buildSuccessMatcher = new StringMatchingInterceptor(BUILD_SUCCESSFUL_MARKER,
				SEARCH_BUILD_SUCCESS_FAIL_MESSAGE_EVERYWHERE);
		LogMemorisingInterceptor recentLogLines = new LogMemorisingInterceptor(LINES_TO_PARSE_FOR_ERRORS);
		ErrorMemorisingInterceptor errorLines = new ErrorMemorisingInterceptor();

		buildLogger.getInterceptorStack().add(buildSuccessMatcher);
		buildLogger.getInterceptorStack().add(recentLogLines);
		buildLogger.getInterceptorStack().add(errorLines);

		try {
			ExternalProcess externalProcess = processService.executeExternalProcess(taskContext,
					new ExternalProcessBuilder().workingDirectory(config.getWorkingDirectory())
							.env(config.getExtraEnvironment()).command(mavenCmd));

			if (externalProcess.getHandler().isComplete()) {
				TaskResultBuilder taskResultBuilder = TaskResultBuilder.newBuilder(taskContext)
						.checkReturnCode(externalProcess, 0);

				return taskResultBuilder.build();
			}
			throw new TaskException("Failed to execute command, external process not completed?");
		} catch (Exception e) {
			throw new TaskException("Failed to execute task", e);
		} finally {
			currentBuildResult.addBuildErrors(errorLines.getErrorStringList());
			currentBuildResult.addBuildErrors(MavenLogHelper.parseErrorOutput(recentLogLines.getLogEntries()));
		}
	}

}
