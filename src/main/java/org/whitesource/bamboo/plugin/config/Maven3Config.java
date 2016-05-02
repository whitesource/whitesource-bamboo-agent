package org.whitesource.bamboo.plugin.config;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;

public class Maven3Config extends AbstractMavenConfig {
	private static final Logger log = Logger.getLogger(Maven3Config.class);
	public static final String M3_CAPABILITY_PREFIX = CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".mvn3";

	public Maven3Config(@NotNull TaskContext taskContext, @NotNull CapabilityContext capabilityContext,
			@NotNull EnvironmentVariableAccessor environmentVariableAccessor) {
		super(taskContext, capabilityContext, environmentVariableAccessor, M3_CAPABILITY_PREFIX, getExecutableName());
		extraEnvironment.put("MAVEN2_HOME", builderPath);
		extraEnvironment.put("M2_HOME", builderPath);
	}

	public static String getExecutableName() {
		return Maven2Config.getExecutableName();
	}
}
