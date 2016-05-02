package org.whitesource.bamboo.plugin.config;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.ExecutablePathUtils;

public class Maven2Config extends AbstractMavenConfig {

	private static final Logger log = Logger.getLogger(Maven2Config.class);
	public static final String M2_CAPABILITY_PREFIX = CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".mvn2";
	private static final String M2_EXECUTABLE_NAME = "mvn";

	public Maven2Config(@NotNull TaskContext taskContext, @NotNull CapabilityContext capabilityContext,
			@NotNull EnvironmentVariableAccessor environmentVariableAccessor) {
		super(taskContext, capabilityContext, environmentVariableAccessor, M2_CAPABILITY_PREFIX, getExecutableName());
		extraEnvironment.put("MAVEN2_HOME", builderPath);
		extraEnvironment.put("M2_HOME", builderPath);
	}

	public static String getExecutableName() {
		return ExecutablePathUtils.makeBatchIfOnWindows(M2_EXECUTABLE_NAME);
	}
}
