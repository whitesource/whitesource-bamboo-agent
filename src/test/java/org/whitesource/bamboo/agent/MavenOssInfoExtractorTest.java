package org.whitesource.bamboo.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Collection;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.whitesource.agent.api.model.AgentProjectInfo;

import com.atlassian.bamboo.build.logger.BuildLogger;

public class MavenOssInfoExtractorTest
{
    private static BuildLogger buildLogger;
    private static File testDirectory;
    protected static final String PROJECT_TOKEN = "TestProjectToken";
    protected static final String MODULE_TOKENS_EMPTY = "";
    protected static final String PATTERN_NONE = "";
    protected static final int NUM_ALL = 8; // REVIEW: maybe derive this number dynamically in setUp(), or maybe not?
    protected static final String PATTERN_WSS = "wss-*";
    protected static final int NUM_WSS = 3;
    protected static final String dependenciesMismatch = "Expected number of dependencies doesn't match actual one (%s vs. %s), have you added/removed any dependencies?";

    @Before
    public void setUp() throws Exception
    {
        buildLogger = mock(BuildLogger.class);
        testDirectory = new File(".");
    }

    @Test
    public void testExtractOssInfoIsDirectory()
    {
        assertTrue(testDirectory.isDirectory());
    }

    @Test
    public void testExtractOssInfoDefault()
    {
        BaseOssInfoExtractor extractor = new MavenOssInfoExtractor(PROJECT_TOKEN, MODULE_TOKENS_EMPTY, PATTERN_NONE,
                PATTERN_NONE, false, testDirectory, buildLogger);
        Collection<AgentProjectInfo> projectInfos = extractor.extract();
        assertEquals(1, projectInfos.size());
        int actual = projectInfos.iterator().next().getDependencies().size();
        assertEquals(String.format(dependenciesMismatch, NUM_ALL, actual), NUM_ALL, actual);
    }

    @Ignore("There is no multi module test project yet.")
    @Test
    public void testExtractOssInfoDefaultWithIncludes()
    {
        BaseOssInfoExtractor extractor = new MavenOssInfoExtractor(PROJECT_TOKEN, MODULE_TOKENS_EMPTY, PATTERN_WSS,
                PATTERN_NONE, false, testDirectory, buildLogger);
        Collection<AgentProjectInfo> projectInfos = extractor.extract();
        assertEquals(1, projectInfos.size());
        int actual = projectInfos.iterator().next().getDependencies().size();
        assertEquals(String.format(dependenciesMismatch, NUM_WSS, actual), NUM_WSS, actual);
    }

    @Ignore("There is no multi module test project yet.")
    @Test
    public void testExtractOssInfoDefaultWithExcludes()
    {
        BaseOssInfoExtractor extractor = new MavenOssInfoExtractor(PROJECT_TOKEN, MODULE_TOKENS_EMPTY, PATTERN_NONE,
                PATTERN_WSS, false, testDirectory, buildLogger);
        Collection<AgentProjectInfo> projectInfos = extractor.extract();
        assertEquals(1, projectInfos.size());
        int actual = projectInfos.iterator().next().getDependencies().size();
        assertEquals(String.format(dependenciesMismatch, NUM_ALL - NUM_WSS, actual), NUM_ALL - NUM_WSS, actual);
    }
}
