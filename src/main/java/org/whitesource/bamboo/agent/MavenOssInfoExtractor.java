/**
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;
import org.whitesource.agent.api.ChecksumUtils;
import org.whitesource.agent.api.model.AgentProjectInfo;
import org.whitesource.agent.api.model.Coordinates;
import org.whitesource.agent.api.model.DependencyInfo;
import org.whitesource.agent.api.model.ExclusionInfo;

public class MavenOssInfoExtractor extends BaseOssInfoExtractor
{
    private static final String LOG_COMPONENT = "MavenExtractor";

    protected Map<String, String> moduleTokens;
    protected boolean ignorePomModules;
    private MavenParser mavenParser;
    protected java.io.File checkoutDirectory;

    /**
     * Constructor
     * 
     * @param runner
     */
    public MavenOssInfoExtractor(final String projectToken, final String moduleTokens, final String includes,
            final String excludes, final boolean ignorePomModules, final java.io.File checkoutDirectory)
    {
        super(projectToken, includes, excludes);

        this.ignorePomModules = ignorePomModules;
        this.moduleTokens = WssUtils.splitParametersMap(moduleTokens);
        this.checkoutDirectory = checkoutDirectory;
    }

    @Override
    public Collection<AgentProjectInfo> extract()
    {
        log.info(WssUtils.logMsg(LOG_COMPONENT, "Collection started"));

        mavenParser = configureMavenParser(checkoutDirectory);

        Collection<MavenProject> projects = new ArrayList<MavenProject>();
        MavenProject mavenProject = mavenParser.getMavenProject();
        projects.add(mavenProject);
        projects.addAll(mavenParser.getModules(mavenProject));

        Collection<AgentProjectInfo> projectInfos = new ArrayList<AgentProjectInfo>();
        // Collect OSS usage information
        for (MavenProject project : projects)
        {
            if (shouldProcess(project))
            {
                projectInfos.add(processProject(project));
            }
            else
            {
                log.info(WssUtils.logMsg(LOG_COMPONENT, "skipping " + project.getId()));
            }
        }
        logAgentProjectInfos(projectInfos);

        return projectInfos;
    }

    private MavenParser configureMavenParser(File workingDirectory)
    {
        MavenParser mavenParser = new MavenParser();

        File pom = new File(workingDirectory, MavenParser.DEFAULT_MAVEN_POM);
        log.info(WssUtils.logMsg(LOG_COMPONENT, "Parsing Maven POM " + pom.getPath()));
        mavenParser.parseProject(pom);

        return mavenParser;
    }

    private boolean shouldProcess(MavenProject project)
    {
        if (project == null)
        {
            return false;
        }

        boolean process = true;

        if (ignorePomModules && "pom".equals(project.getPackaging()))
        {
            process = false;
        }
        else if (!excludes.isEmpty() && matchAny(project.getArtifactId(), excludes))
        {
            process = false;
        }
        else if (!includes.isEmpty() && matchAny(project.getArtifactId(), includes))
        {
            process = true;
        }

        return process;
    }

    private boolean matchAny(String value, List<String> patterns)
    {
        boolean match = false;

        for (String pattern : patterns)
        {
            String regex = pattern.replace(".", "\\.").replace("*", ".*");
            if (value.matches(regex))
            {
                match = true;
                break;
            }
        }

        return match;
    }

    private AgentProjectInfo processProject(MavenProject project)
    {
        long startTime = System.currentTimeMillis();

        log.info(WssUtils.logMsg(LOG_COMPONENT, "processing Maven project " + project.getId()));

        AgentProjectInfo projectInfo = new AgentProjectInfo();

        // project token
        if (project.equals(mavenParser.getMavenProject()))
        {
            projectInfo.setProjectToken(projectToken);
        }
        else
        {
            projectInfo.setProjectToken(moduleTokens.get(project.getArtifactId()));
        }

        // project coordinates
        projectInfo.setCoordinates(extractCoordinates(project));

        Parent parent = project.getModel().getParent();
        // parent coordinates
        if (parent != null)
        {
            projectInfo.setParentCoordinates(extractParentCoordinates(parent));
        }

        // dependencies
        Map<Dependency, Artifact> lut = createLookupTable(project);
        for (Dependency dependency : mavenParser.getDependencies(project))
        {
            DependencyInfo dependencyInfo = getDependencyInfo(dependency);

            Artifact artifact = lut.get(dependency);
            if (artifact != null)
            {
                File artifactFile = artifact.getFile();
                if (artifactFile != null && artifactFile.exists())
                {
                    try
                    {
                        dependencyInfo.setSha1(ChecksumUtils.calculateSHA1(artifactFile));
                    }
                    catch (IOException e)
                    {
                        log.warn(WssUtils.logMsg(LOG_COMPONENT, ERROR_SHA1 + " for " + artifact.getId()));
                    }
                }
            }

            projectInfo.getDependencies().add(dependencyInfo);
        }

        log.info(WssUtils.logMsg(LOG_COMPONENT, "Total Maven project processing time is "
                + (System.currentTimeMillis() - startTime) + " [msec]"));

        return projectInfo;
    }

    private Coordinates extractCoordinates(MavenProject mavenProject)
    {
        return new Coordinates(mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion());
    }

    private Coordinates extractParentCoordinates(Parent parent)
    {
        return new Coordinates(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    private Map<Dependency, Artifact> createLookupTable(MavenProject project)
    {
        Map<Dependency, Artifact> lut = new HashMap<Dependency, Artifact>();

        for (Dependency dependency : mavenParser.getDependencies(project))
        {
            for (Artifact dependencyArtifact : mavenParser.getDependencyArtifacts(project))
            {
                if (match(dependency, dependencyArtifact))
                {
                    lut.put(dependency, dependencyArtifact);
                }
            }
        }

        return lut;
    }

    private boolean match(Dependency dependency, Artifact artifact)
    {
        boolean match = dependency.getGroupId().equals(artifact.getGroupId())
                && dependency.getArtifactId().equals(artifact.getArtifactId())
                && dependency.getVersion().equals(artifact.getVersion());

        if (match)
        {
            if (dependency.getClassifier() == null)
            {
                match = artifact.getClassifier() == null;
            }
            else
            {
                match = dependency.getClassifier().equals(artifact.getClassifier());
            }
        }

        if (match)
        {
            String type = artifact.getType();
            if (dependency.getType() == null)
            {
                match = type == null || type.equals("jar");
            }
            else
            {
                match = dependency.getType().equals(type);
            }
        }

        return match;
    }

    private DependencyInfo getDependencyInfo(Dependency dependency)
    {
        DependencyInfo info = new DependencyInfo();

        // dependency data
        info.setGroupId(dependency.getGroupId());
        info.setArtifactId(dependency.getArtifactId());
        info.setVersion(dependency.getVersion());
        info.setScope(dependency.getScope());
        info.setClassifier(dependency.getClassifier());
        info.setOptional(dependency.isOptional());
        info.setType(dependency.getType());
        info.setSystemPath(dependency.getSystemPath());

        // exclusions
        Collection<ExclusionInfo> exclusions = info.getExclusions();
        final List<Exclusion> mavenExclusions = dependency.getExclusions();
        for (Exclusion exclusion : mavenExclusions)
        {
            exclusions.add(new ExclusionInfo(exclusion.getGroupId(), exclusion.getArtifactId()));
        }

        return info;
    }

    @Override
    protected String getLogComponent()
    {
        return LOG_COMPONENT;
    }
}
