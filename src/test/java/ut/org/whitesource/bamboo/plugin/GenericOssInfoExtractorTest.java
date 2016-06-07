package ut.org.whitesource.bamboo.plugin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whitesource.agent.api.model.AgentProjectInfo;
import org.whitesource.bamboo.plugin.freestyle.GenericOssInfoExtractor;

@Ignore
public class GenericOssInfoExtractorTest {
	
	GenericOssInfoExtractor genericOssInfoExtractorTest;
    protected final Logger log = LoggerFactory.getLogger(GenericOssInfoExtractorTest.class);
    String startingDir = "/Users/gangadhar/Desktop/bahar/bamboo/v1/latest/git/openssl/openssl";

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	@Ignore
	public void simpleTest(){
		String pattern = "**/*.pl";
		String glob;
		String path="/Users/gangadhar/Desktop/bahar/bamboo/v1/latest/git/openssl/openssl/os2/backwardify.pl";
		if(pattern.startsWith("*")){
			//glob = "glob:" + startingDir+pattern;
			glob = "glob:"+pattern;
		}else{
			//glob = "glob:" + startingDir+File.separatorChar+pattern;
			glob = "glob:"+File.separatorChar+pattern;
		}
		final PathMatcher includePathMatcher = FileSystems.getDefault().getPathMatcher(glob);
		assertTrue(includePathMatcher.matches(new File(path).toPath()));
	}
	
	@Test
	public void extractOssInfoTest1(){
		genericOssInfoExtractorTest = new GenericOssInfoExtractor("test","test","ms/*.c","",new File(startingDir));
		Collection<AgentProjectInfo> projectInfos = genericOssInfoExtractorTest.extract();
		assertFalse(projectInfos.isEmpty());
		log.info(Integer.toString(projectInfos.iterator().next().getDependencies().size()));
		assertTrue(projectInfos.iterator().next().getDependencies().size()==2);
	}
	
	@Test
	public void extractOssInfoTest2(){
		genericOssInfoExtractorTest = new GenericOssInfoExtractor("test","test","**/ms/*.c","",new File(startingDir));
		Collection<AgentProjectInfo> projectInfos = genericOssInfoExtractorTest.extract();
		assertFalse(projectInfos.isEmpty());
		log.info(Integer.toString(projectInfos.iterator().next().getDependencies().size()));
		assertTrue(projectInfos.iterator().next().getDependencies().size()==2);
	}
	
	@Test
	public void extractOssInfoTest3(){
		genericOssInfoExtractorTest = new GenericOssInfoExtractor("test","test","**/*.c","",new File(startingDir));
		Collection<AgentProjectInfo> projectInfos = genericOssInfoExtractorTest.extract();
		assertFalse(projectInfos.isEmpty());
		log.info(Integer.toString(projectInfos.iterator().next().getDependencies().size()));
	}
	
	@Test
	public void extractOssInfoTest4(){
		genericOssInfoExtractorTest = new GenericOssInfoExtractor("test","test","doc/HOWTO/*.txt","doc/HOWTO/keys*.txt",new File(startingDir));
		Collection<AgentProjectInfo> projectInfos = genericOssInfoExtractorTest.extract();
		assertFalse(projectInfos.isEmpty());
		log.info(Integer.toString(projectInfos.iterator().next().getDependencies().size()));
		assertTrue(projectInfos.iterator().next().getDependencies().size()==2);
	}
	
	@Test
	public void extractOssInfoTest5(){
		genericOssInfoExtractorTest = new GenericOssInfoExtractor("test","test","MacOS/*.cpp,MacOS/*.hqx","",new File(startingDir));
		Collection<AgentProjectInfo> projectInfos = genericOssInfoExtractorTest.extract();
		assertFalse(projectInfos.isEmpty());
		log.info(Integer.toString(projectInfos.iterator().next().getDependencies().size()));
		assertTrue(projectInfos.iterator().next().getDependencies().size()==4);
	}
	
	//space and comma,
	@Test
	public void extractOssInfoTest6(){
		genericOssInfoExtractorTest = new GenericOssInfoExtractor("test","test","MacOS/*.hqx,MacOS/*.cpp,MacOS/*.h","MacOS/*.hqx MacOS/*.cpp",new File(startingDir));
		Collection<AgentProjectInfo> projectInfos = genericOssInfoExtractorTest.extract();
		assertFalse(projectInfos.isEmpty());
		log.info(Integer.toString(projectInfos.iterator().next().getDependencies().size()));
		assertTrue(projectInfos.iterator().next().getDependencies().size()==5);
	}

}
