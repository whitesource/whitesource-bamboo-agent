/*
 * Copyright (C) 2012 White Source Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.whitesource.bamboo.agent;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whitesource.agent.api.model.AgentProjectInfo;
import org.whitesource.agent.api.model.DependencyInfo;

/**
 * Base class for extractors of open source usage information;
 * 
 * @author Edo.Shor
 */
public abstract class BaseOssInfoExtractor
{
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
        log.info("----------------- dumping projectInfos -----------------");
        log.info("Total number of projects : " + projectInfos.size());
        for (AgentProjectInfo projectInfo : projectInfos)
        {
            log.info("Project coordinates: " + projectInfo.getCoordinates());
            log.info("Project parent coordinates: " + projectInfo.getParentCoordinates());
            log.info("total # of dependencies: " + projectInfo.getDependencies().size());
            for (DependencyInfo info : projectInfo.getDependencies())
            {
                log.info(info + " SHA-1: " + info.getSha1());
            }
        }
        log.info("----------------- dump finished -----------------");
    }
}
