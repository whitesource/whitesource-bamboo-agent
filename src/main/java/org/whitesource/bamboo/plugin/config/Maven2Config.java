package org.whitesource.bamboo.plugin.config;

import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.ExecutablePathUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Maven2Config extends AbstractMavenConfig {

	private static final Logger log = Logger.getLogger(Maven2Config.class);
	private static final String MVN2 = ".mvn2";
	private static final String M2_CAPABILITY_PREFIX = CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + MVN2;
	static final String M2_EXECUTABLE_NAME = "mvn";

	public Maven2Config(@NotNull TaskContext taskContext, @NotNull CapabilityContext capabilityContext,
						@NotNull EnvironmentVariableAccessor environmentVariableAccessor, Map<String, String> bambooSystemProperties) {
		super(taskContext, capabilityContext, environmentVariableAccessor, M2_CAPABILITY_PREFIX, MVN2, getExecutableName(), bambooSystemProperties);
		extraEnvironment.put("MAVEN2_HOME", builderPath);
		extraEnvironment.put("M2_HOME", builderPath);
	}

	public static String getExecutableName() {
		return ExecutablePathUtils.makeBatchIfOnWindows(M2_EXECUTABLE_NAME);
	}
}
