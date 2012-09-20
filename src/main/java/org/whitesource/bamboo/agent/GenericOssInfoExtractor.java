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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.whitesource.agent.api.ChecksumUtils;
import org.whitesource.agent.api.model.AgentProjectInfo;
import org.whitesource.agent.api.model.Coordinates;
import org.whitesource.agent.api.model.DependencyInfo;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;

/**
 * Concrete implementation for generic job types. Based on user entered locations of open source libraries.
 * 
 * @author Edo.Shor
 */
public class GenericOssInfoExtractor extends BaseOssInfoExtractor
{
    private static final String LOG_COMPONENT = "GenericExtractor";

    protected String projectName;
    protected List<Pattern> includePatterns;
    protected List<Pattern> excludePatterns;
    protected java.io.File checkoutDirectory;
    protected BuildLogger buildLogger;

    /**
     * Constructor
     * 
     * @param runner
     */
    public GenericOssInfoExtractor(final String projectName, final String projectToken, final String includes,
            final String excludes, final java.io.File checkoutDirectory, final BuildLogger buildLogger)
    {
        super(projectToken, includes, excludes);

        this.projectName = projectName;
        this.checkoutDirectory = checkoutDirectory;
        this.buildLogger = buildLogger;

        includePatterns = new ArrayList<Pattern>();
        for (String pattern : this.includes)
        {
            includePatterns.add(Pattern.compile(FileUtil.convertAntToRegexp(pattern)));
        }

        excludePatterns = new ArrayList<Pattern>();
        for (String pattern : this.excludes)
        {
            excludePatterns.add(Pattern.compile(FileUtil.convertAntToRegexp(pattern)));
        }

    }

    @Override
    public Collection<AgentProjectInfo> extract()
    {
        log.info(WssUtils.logMsg(LOG_COMPONENT, "Collection started"));

        // we send something anyhow, even when no OSS found.
        Collection<AgentProjectInfo> projectInfos = new ArrayList<AgentProjectInfo>();
        AgentProjectInfo projectInfo = new AgentProjectInfo();
        projectInfos.add(projectInfo);

        projectInfo.setCoordinates(new Coordinates(null, projectName, null));
        projectInfo.setProjectToken(projectToken);

        if (includePatterns.isEmpty())
        {
            log.error(WssUtils.logMsg(LOG_COMPONENT, "No include patterns defined. Failing."));
            buildLogger.addErrorLogEntry("No include patterns defined. Can't look for open source information.");
        }
        else
        {
            buildLogger.addBuildLogEntry("Including files matching:");
            buildLogger.addBuildLogEntry(StringUtil.join(includes, "\r"));
            if (excludes.isEmpty())
            {
                buildLogger.addBuildLogEntry("Excluding none.");
            }
            else
            {
                buildLogger.addBuildLogEntry("Excluding files matching:");
                buildLogger.addBuildLogEntry(StringUtil.join(excludes, "\r"));
            }

            extractOssInfo(checkoutDirectory, projectInfo.getDependencies());
        }

        logAgentProjectInfos(projectInfos);

        return projectInfos;
    }

    private void extractOssInfo(final File root, final Collection<DependencyInfo> dependencyInfos)
    {
        extractOssInfo(root, root, dependencyInfos);
    }

    private void extractOssInfo(final File absoluteRoot, final File root,
            final Collection<DependencyInfo> dependencyInfos)
    {
        final File[] files = root.listFiles();
        if (files == null)
        {
            return;
        }

        for (File file : files)
        {
            if (file.isFile())
            {
                final String path = FileUtil.toSystemIndependentName(FileUtil.getRelativePath(absoluteRoot, file));

                boolean process = matchAny(path, includePatterns);
                if (process)
                {
                    process = !matchAny(path, excludePatterns);
                }

                if (process)
                {
                    dependencyInfos.add(extractDepependencyInfo(file));
                }
            }
            else
            {
                extractOssInfo(absoluteRoot, file, dependencyInfos);
            }
        }
    }

    private boolean matchAny(String value, List<Pattern> patterns)
    {
        boolean match = false;

        for (Pattern pattern : patterns)
        {
            if (pattern.matcher(value).matches())
            {
                match = true;
                break;
            }
        }

        return match;
    }

    private DependencyInfo extractDepependencyInfo(File file)
    {
        DependencyInfo info = new DependencyInfo();

        info.setSystemPath(file.getAbsolutePath());
        info.setArtifactId(file.getName());

        try
        {
            info.setSha1(ChecksumUtils.calculateSHA1(file));
        }
        catch (IOException e)
        {
            String msg = "Error calculating SHA-1 for " + file.getAbsolutePath();
            log.warn(WssUtils.logMsg(LOG_COMPONENT, msg));
            buildLogger.addBuildLogEntry(msg);
        }

        return info;
    }
}
