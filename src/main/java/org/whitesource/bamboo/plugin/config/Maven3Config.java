package org.whitesource.bamboo.plugin.config;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;

import java.util.Map;

public class Maven3Config extends AbstractMavenConfig {
	private static final Logger log = Logger.getLogger(Maven3Config.class);
	public static final String MVN3 = ".mvn3";
	public static final String M3_CAPABILITY_PREFIX = CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + MVN3;

	public Maven3Config(@NotNull TaskContext taskContext, @NotNull CapabilityContext capabilityContext,
						@NotNull EnvironmentVariableAccessor environmentVariableAccessor, Map<String, String> bambooSystemProperties) {
		super(taskContext, capabilityContext, environmentVariableAccessor, M3_CAPABILITY_PREFIX, MVN3, getExecutableName(), bambooSystemProperties);
		extraEnvironment.put("MAVEN2_HOME", builderPath);
		extraEnvironment.put("M2_HOME", builderPath);
	}

	public static String getExecutableName() {
		return Maven2Config.getExecutableName();
	}
}
