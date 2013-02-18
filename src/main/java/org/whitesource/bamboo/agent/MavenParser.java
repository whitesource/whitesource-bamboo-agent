package org.whitesource.bamboo.agent;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.project.MavenProject;

import com.atlassian.bamboo.maven.embedder.MavenConfiguration;
import com.atlassian.bamboo.maven.embedder.MavenEmbedderException;
import com.atlassian.bamboo.maven.embedder.MavenEmbedderService;
import com.atlassian.bamboo.maven.embedder.MavenEmbedderServiceImpl;
import com.google.common.collect.Sets;

public class MavenParser
{
    private static final Logger log = Logger.getLogger(MavenParser.class);
    public static final String DEFAULT_MAVEN_POM = "pom.xml";
    private MavenEmbedderService mavenEmbedderService;
    private MavenProject mavenProject;

    public MavenParser()
    {
        mavenEmbedderService = new MavenEmbedderServiceImpl();
    }

    public void parseProject(File file) throws MavenEmbedderException
    {
        MavenConfiguration mavenConfiguration = MavenConfiguration.builder().build();
        ModelBuildingResult modelBuildingResult = mavenEmbedderService.buildModel(file, mavenConfiguration);
        mavenProject = new MavenProject(modelBuildingResult.getEffectiveModel());
    }

    protected Set<MavenProject> getModules(MavenProject mavenProject)
    {
        Set<MavenProject> modules = Sets.newHashSet();

        // recursively add child modules
        for (String module : mavenProject.getModules())
        {
            File pom = new File(mavenProject.getModel().getPomFile().getParent(), module + File.separator
                    + DEFAULT_MAVEN_POM);
            try
            {
                MavenConfiguration mavenConfiguration = MavenConfiguration.builder().build();
                ModelBuildingResult modelBuildingResult = mavenEmbedderService.buildModel(pom, mavenConfiguration);
                MavenProject project = new MavenProject(modelBuildingResult.getEffectiveModel());
                modules.add(project);
                modules.addAll(getModules(project));

            }
            catch (MavenEmbedderException e)
            {
                log.warn("Can't read POM for module " + module, e);
            }
        }

        return modules;
    }

    protected Set<Artifact> getArtifacts(MavenProject mavenProject)
    {
        Set<Artifact> artifacts = Sets.newHashSet(mavenProject.getArtifact());
        artifacts.addAll(mavenProject.getArtifacts());

        return artifacts;
    }

    protected Set<Dependency> getDependencies(final MavenProject mavenProject)
    {
        Set<Dependency> dependencies = Sets.newHashSet();
        final List<Dependency> mavenProjectDependencies = mavenProject.getDependencies();
        if (mavenProjectDependencies != null)
        {
            dependencies.addAll(mavenProjectDependencies);
        }

        return dependencies;
    }

    protected Set<Artifact> getDependencyArtifacts(MavenProject mavenProject)
    {
        Set<Artifact> dependencyArtifacts = Sets.newHashSet();
        final Set<Artifact> mavenProjectDependencyArtifacts = mavenProject.getDependencyArtifacts();
        if (mavenProjectDependencyArtifacts != null)
        {
            dependencyArtifacts.addAll(mavenProjectDependencyArtifacts);
        }

        return dependencyArtifacts;
    }

    public MavenProject getMavenProject()
    {
        return mavenProject;
    }
}
