package org.whitesource.bamboo.agent;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.util.Collection;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.whitesource.agent.api.model.AgentProjectInfo;

public class MavenOssInfoExtractorTest
{
    private static File testDirectory;
    protected static final String PROJECT_TOKEN = "TestProjectToken";
    protected static final String MODULE_TOKENS_EMPTY = "";
    protected static final String PATTERN_NONE = "";
    protected static final int NUM_ALL = 9; // REVIEW: maybe derive this number dynamically in setUp(), or maybe not?
    protected static final String PATTERN_WSS = "wss-*";
    protected static final int NUM_WSS = 3;
    protected static final String dependenciesMismatch = "Number of dependencies doesn't match, expected:<[%s]> but was:<[%s]> - have you added/removed any dependencies?";

    @Before
    public void setUp() throws Exception
    {
        testDirectory = new File(".");
    }

    @Test
    public void testExtractOssInfoIsDirectory()
    {
        assertThat(testDirectory).isDirectory();
    }

    @Test
    public void testExtractOssInfoDefault()
    {
        BaseOssInfoExtractor extractor = new MavenOssInfoExtractor(PROJECT_TOKEN, MODULE_TOKENS_EMPTY, PATTERN_NONE,
                PATTERN_NONE, false, testDirectory);
        Collection<AgentProjectInfo> projectInfos = extractor.extract();
        assertThat(projectInfos.size()).isEqualTo(1);
        int actual = projectInfos.iterator().next().getDependencies().size();
        assertThat(actual).overridingErrorMessage(dependenciesMismatch, NUM_ALL, actual).isEqualTo(NUM_ALL);
    }

    @Ignore("There is no multi module test project yet.")
    @Test
    public void testExtractOssInfoDefaultWithIncludes()
    {
        BaseOssInfoExtractor extractor = new MavenOssInfoExtractor(PROJECT_TOKEN, MODULE_TOKENS_EMPTY, PATTERN_WSS,
                PATTERN_NONE, false, testDirectory);
        Collection<AgentProjectInfo> projectInfos = extractor.extract();
        assertThat(projectInfos.size()).isEqualTo(1);
        int actual = projectInfos.iterator().next().getDependencies().size();
        assertThat(actual).overridingErrorMessage(dependenciesMismatch, NUM_WSS, actual).isEqualTo(NUM_WSS);
    }

    @Ignore("There is no multi module test project yet.")
    @Test
    public void testExtractOssInfoDefaultWithExcludes()
    {
        BaseOssInfoExtractor extractor = new MavenOssInfoExtractor(PROJECT_TOKEN, MODULE_TOKENS_EMPTY, PATTERN_NONE,
                PATTERN_WSS, false, testDirectory);
        Collection<AgentProjectInfo> projectInfos = extractor.extract();
        assertThat(projectInfos.size()).isEqualTo(1);
        int actual = projectInfos.iterator().next().getDependencies().size();
        assertThat(actual).overridingErrorMessage(dependenciesMismatch, NUM_ALL - NUM_WSS, actual).isEqualTo(
                NUM_ALL - NUM_WSS);
    }
}
