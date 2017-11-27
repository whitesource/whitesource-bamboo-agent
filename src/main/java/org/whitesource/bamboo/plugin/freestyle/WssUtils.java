package org.whitesource.bamboo.plugin.freestyle;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.whitesource.bamboo.plugin.Constants.*;

import com.atlassian.bamboo.configuration.ConfigurationMap;
import org.apache.commons.lang.StringUtils;
import org.whitesource.agent.client.WhitesourceService;
import org.whitesource.bamboo.plugin.Constants;
import org.whitesource.bamboo.plugin.task.AgentTask;

public final class WssUtils {
    /* --- Static members --- */

    //private static final Pattern PARAM_LIST_SPLIT_PATTERN = Pattern.compile(",|$", Pattern.MULTILINE);
    private static final Pattern PARAM_LIST_SPLIT_PATTERN = Pattern.compile(",|\\s+");
    private static final Pattern KEY_VALUE_SPLIT_PATTERN = Pattern.compile("=");
    private static final Logger log = LoggerFactory.getLogger(WssUtils.class);

    /* --- Public methods --- */

    public static WhitesourceService createServiceClient(String wssUrl, ConfigurationMap configurationMap) {
        WhitesourceService service;

        boolean isProxySettings = configurationMap.getAsBoolean(PROXY_SETTINGS);
        if (wssUrl != null) {
            service = new WhitesourceService(AGENT_TYPE, getResource(Constants.AGENT_VERSION),
                    getResource(Constants.VERSION), wssUrl, isProxySettings, Constants.DEFAULT_CONNECTION_TIMEOUT_MINUTES);
        } else {
            service = new WhitesourceService(AGENT_TYPE, getResource(Constants.AGENT_VERSION),
                    getResource(Constants.VERSION), DEFAULT_SERVICE_URL, isProxySettings, Constants.DEFAULT_CONNECTION_TIMEOUT_MINUTES);
        }

        // Fill proxy settings by user if set, else check if proxy settings are configured by default on the bamboo server.
        if (isProxySettings) {
            service.getClient().setProxy(configurationMap.get(PROXY_HOST), Integer.parseInt(configurationMap.get(PROXY_PORT)),
                    configurationMap.get(PROXY_USER_NAME), configurationMap.get(PROXY_PASSWORD));
        } else {
            // Reuse hosting application proxy settings, if any (see https://confluence.atlassian.com/x/nAFgDQ for the
            // rationale).
            final String httpProxyHost = System.getProperty("http.proxyHost");
            if (httpProxyHost != null) {
                final int proxyPort = Integer.parseInt(System.getProperty("http.proxyPort", "80"));
                final String proxyUser = System.getProperty("http.proxyUser");
                final String proxyPassword = System.getProperty("http.proxyPassword");
                service.getClient().setProxy("http://" + httpProxyHost, proxyPort, proxyUser, proxyPassword);
            } else {
                final String httpsProxyHost = System.getProperty("https.proxyHost");
                if (httpsProxyHost != null) {
                    final int proxyPort = Integer.parseInt(System.getProperty("https.proxyPort", "443"));
                    final String proxyUser = System.getProperty("http.proxyUser");
                    final String proxyPassword = System.getProperty("http.proxyPassword");
                    service.getClient().setProxy("https://" + httpsProxyHost, proxyPort, proxyUser, proxyPassword);
                }
            }
        }
        return service;
    }

    public static String logMsg(String component, String msg) {
        return "[whitesource]::" + component + ": " + msg;
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

    private static String getResource(String propertyName) {
        Properties properties = getProperties(propertyName);
        String val = (properties.getProperty(propertyName));
        if(StringUtils.isNotBlank(val)){
            return val;
        }
        return "";
    }

    private static Properties getProperties(String propertyName) {
        Properties properties = new Properties();
        try (InputStream stream = AgentTask.class.getResourceAsStream("/project.properties")) {
            properties.load(stream);
        } catch (IOException e) {
            log.error("Failed to get version "+ propertyName + ",error: "+ e);
        }
        return properties;
    }
}
