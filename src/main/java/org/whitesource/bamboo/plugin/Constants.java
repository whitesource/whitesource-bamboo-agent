package org.whitesource.bamboo.plugin;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.atlassian.bamboo.utils.SystemProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class Constants {
	
	public static final String LOG_COMPONENT = "AgentTask";
	public static final String CONTACT_SUPPORT = "Encountered internal plugin error - please contact support!";
	
	public static final String API_KEY = "apiKey";
	public static final String CHECK_POLICIES = "checkPolicies";
	public static final String FORCE_UPDATE = "forceUpdate";

	public static final String PROJECT_TYPE = "projectType";
	public static final String PROJECT_TYPES = "projectTypes";
	public static final String PRODUCT_TOKEN = "productToken";
	public static final String PRODUCT_VERSION = "productVersion";
	public static final String PROJECT_TOKEN = "projectToken";
	public static final String MODULE_TOKENS = "moduleTokens";
	public static final String MODULES_INCLUDE_PATTERN = "modulesIncludePattern";
	public static final String MODULES_EXCLUDE_PATTERN = "modulesExcludePattern";
	public static final String FILES_INCLUDE_PATTERN = "filesIncludePattern";
	public static final String FILES_EXCLUDE_PATTERN = "filesExcludePattern";
	public static final String FAIL_ON_CONNECTION_ERROR = "failOnConnectionError";
	public static final String CONNECTION_RETRIES = "connectionRetries";
	public static final String PROXY_SETTINGS = "proxySettings";
	public static final String PROXY_HOST = "proxyHost";
	public static final String PROXY_PORT = "proxyPort";
	public static final String PROXY_USER_NAME = "proxyUserName";
	public static final String PROXY_PASSWORD = "proxyUserPassword";
	public static final String BAMBOO_SYSTEM_PROPERTIES = "bambooSystemProperties";

	public static final String IGNORE_POM = "ignorePOM";
	
	public static final String AGENT_TYPE = "bamboo";
    public static final String AGENT_VERSION = "2.0";
    public static final String SERVICE_URL_KEYWORD = "wss.url";
    public static final String DEFAULT_SERVICE_URL = "https://saas.whitesourcesoftware.com/agent";
    
	public static final Set<String> FIELD_COLLECTION = ImmutableSet
	        .<String> builder()
	        .add(API_KEY, CHECK_POLICIES, FORCE_UPDATE, PROJECT_TYPE, PRODUCT_TOKEN,PROJECT_TOKEN, PRODUCT_VERSION,MODULE_TOKENS, MODULES_INCLUDE_PATTERN,
	                MODULES_EXCLUDE_PATTERN, FILES_INCLUDE_PATTERN, FAIL_ON_CONNECTION_ERROR, CONNECTION_RETRIES, FILES_EXCLUDE_PATTERN, IGNORE_POM,SERVICE_URL_KEYWORD, PROXY_SETTINGS,
					PROXY_PORT, PROXY_HOST, PROXY_PASSWORD, PROXY_USER_NAME, BAMBOO_SYSTEM_PROPERTIES).build();
	public static final String GENERIC_TYPE = "Freestyle";
	public static final String MAVEN_TYPE = "Maven";
	public static final Map<String, String> TYPE_MAP = ImmutableMap.<String, String> builder()
	        .put(MAVEN_TYPE, MAVEN_TYPE).put(GENERIC_TYPE, GENERIC_TYPE).build();
	public static final String OPTION_TRUE = "True";
	public static final String OPTION_FALSE = "False";
	public static final String DEFAULT_TYPE = GENERIC_TYPE;
	public static final String DEFAULT_IGNORE_POM = OPTION_FALSE;
	public static final String CTX_PLAN = "plan";
	public static final String CTX_MAVEN_JOB = "mavenJob";
	
	public static final String BUILD_SUCCESSFUL_MARKER_UPDATE = SystemProperty.BUILD_SUCCESSFUL_MARKER.getValue("BUILD SUCCESS");
	public static final String BUILD_SUCCESSFUL_MARKER_CHECKPOLICIES = SystemProperty.BUILD_SUCCESSFUL_MARKER.getValue("All dependencies conform with the organization's policies.");
	public static final boolean SEARCH_BUILD_SUCCESS_FAIL_MESSAGE_EVERYWHERE = SystemProperty.SEARCH_BUILD_SUCCESS_FAIL_MESSAGE_EVERYWHERE.getValue(false);
	public static final int LINES_TO_PARSE_FOR_ERRORS = 200;
	public static final int FIND_SUCCESS_MESSAGE_IN_LAST = SystemProperty.FIND_SUCCESS_MESSAGE_IN_LAST.getValue(50);
	public static final Pattern PARAM_LIST_SPLIT_PATTERN = Pattern.compile(",|$", Pattern.MULTILINE);
	public static final Pattern KEY_VALUE_SPLIT_PATTERN = Pattern.compile("=");

	/* --- Errors --- */

	public static final String ERROR_CONNECTION_REFUSED = "Connection refused: ";
	


}
