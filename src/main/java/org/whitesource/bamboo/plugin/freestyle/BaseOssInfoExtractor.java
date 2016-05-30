
package org.whitesource.bamboo.plugin.freestyle;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whitesource.agent.api.model.AgentProjectInfo;
import org.whitesource.agent.api.model.DependencyInfo;

public abstract class BaseOssInfoExtractor
{
    protected static final String ERROR_SHA1 = "Error calculating SHA-1";
    protected String projectToken;
    protected List<String> includes;
    protected List<String> excludes;
    protected final Logger log = LoggerFactory.getLogger(BaseOssInfoExtractor.class);

    /**
     * Constructor
     * 
     * @param runner
     */
    protected BaseOssInfoExtractor(final String projectToken, final String includes, final String excludes)
    {
        this.projectToken = projectToken;
        this.includes = WssUtils.splitParameters(includes);
        this.excludes = WssUtils.splitParameters(excludes);
    }

    public abstract Collection<AgentProjectInfo> extract();

    public void logAgentProjectInfos(Collection<AgentProjectInfo> projectInfos)
    {
        log.info(WssUtils.logMsg(getLogComponent(), "----------------- dumping projectInfos -----------------"));
        log.info(WssUtils.logMsg(getLogComponent(), "Total number of projects : " + projectInfos.size()));
        for (AgentProjectInfo projectInfo : projectInfos)
        {
            log.info(WssUtils.logMsg(getLogComponent(), "Project coordinates: " + projectInfo.getCoordinates()));
            log.info(WssUtils.logMsg(getLogComponent(),
                    "Project parent coordinates: " + projectInfo.getParentCoordinates()));
            log.info(WssUtils.logMsg(getLogComponent(), "total # of dependencies: "
                    + projectInfo.getDependencies().size()));
            for (DependencyInfo info : projectInfo.getDependencies())
            {
                log.info(WssUtils.logMsg(getLogComponent(), info.getArtifactId() + " SHA-1: " + info.getSha1()));
            }
        }
        log.info(WssUtils.logMsg(getLogComponent(), "----------------- dump finished -----------------"));
    }

    protected abstract String getLogComponent();
}
