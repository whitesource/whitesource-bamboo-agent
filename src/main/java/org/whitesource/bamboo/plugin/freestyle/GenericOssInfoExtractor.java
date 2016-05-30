
package org.whitesource.bamboo.plugin.freestyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.whitesource.agent.api.ChecksumUtils;
import org.whitesource.agent.api.model.AgentProjectInfo;
import org.whitesource.agent.api.model.Coordinates;
import org.whitesource.agent.api.model.DependencyInfo;

public class GenericOssInfoExtractor extends BaseOssInfoExtractor {
	private static final String LOG_COMPONENT = "GenericExtractor";

	protected String projectName;
	protected List<String> includePatterns;
	protected List<String> excludePatterns;
	protected java.io.File checkoutDirectory;

	/**
	 * Constructor
	 * 
	 * @param runner
	 */
	public GenericOssInfoExtractor(final String projectName, final String projectToken, final String includes,
			final String excludes, final java.io.File checkoutDirectory) {
		super(projectToken, includes, excludes);

		this.projectName = projectName;
		this.checkoutDirectory = checkoutDirectory;

		includePatterns = new ArrayList<String>();
		for (String pattern : this.includes) {
			includePatterns.add(pattern);

		}

		excludePatterns = new ArrayList<String>();
		for (String pattern : this.excludes) {
			excludePatterns.add(pattern);

		}

	}

	@Override
	public Collection<AgentProjectInfo> extract() {
		log.info(WssUtils.logMsg(LOG_COMPONENT, "Collection started"));

		// we send something anyhow, even when no OSS found.
		Collection<AgentProjectInfo> projectInfos = new ArrayList<AgentProjectInfo>();
		AgentProjectInfo projectInfo = new AgentProjectInfo();
		projectInfos.add(projectInfo);

		if (StringUtils.isBlank(projectToken)) {
			projectInfo.setCoordinates(new Coordinates(null, projectName, null));
		} else {
			projectInfo.setProjectToken(projectToken);
		}

		if (includePatterns.isEmpty()) {
			log.error(WssUtils.logMsg(LOG_COMPONENT, "No include patterns defined. Failing."));
		} else {
			log.info(WssUtils.logMsg(LOG_COMPONENT, "Including files matching:\r" + StringUtils.join(includes, "\r")));
			log.info(WssUtils.logMsg(LOG_COMPONENT, "Exluding files matching:\r" + StringUtils.join(excludes, "\r")));

			extractOssInfo(checkoutDirectory, projectInfo.getDependencies());
		}

		logAgentProjectInfos(projectInfos);

		return projectInfos;
	}

	public void extractOssInfo(final File root,
			final Collection<DependencyInfo> dependencyInfos) {
		
		Path startingDir = Paths.get(root.getAbsolutePath());
		String glob;
		
		for (String pattern : includePatterns) {

			if(pattern.startsWith("*")){
				glob = "glob:" + startingDir+pattern;
			}else{
				glob = "glob:" + startingDir+File.separatorChar+pattern;
			}

			final PathMatcher includePathMatcher = FileSystems.getDefault().getPathMatcher(glob);

			try {
				Files.walkFileTree(startingDir, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
						
						if (includePathMatcher.matches(path) && !checkForExclusions(path,startingDir)) {
							log.info("File :" + path.toFile());
							dependencyInfos.add(extractDepependencyInfo(path.toFile()));
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}
				});

			} catch (IOException e) {
				log.warn(WssUtils.logMsg(LOG_COMPONENT + "for finding matching files :" + pattern, e.getMessage()));
			}
		}
		
	}
	
	private boolean checkForExclusions(Path path,Path startingDir){
		
		String glob;
		boolean exFlag = false;
		
		for (String pattern : excludePatterns) {

			if(pattern.startsWith("*")){
				glob = "glob:" + startingDir+pattern;
			}else{
				glob = "glob:" + startingDir+File.separatorChar+pattern;
			}

			final PathMatcher excludePathMatcher = FileSystems.getDefault().getPathMatcher(glob);
				if (excludePathMatcher.matches(path)) {
					exFlag = true;
					break;
				}
			}
		
		return exFlag;
	}

	private DependencyInfo extractDepependencyInfo(File file) {
		DependencyInfo dependencyInfo = new DependencyInfo();

		dependencyInfo.setSystemPath(file.getAbsolutePath());
		dependencyInfo.setArtifactId(file.getName());

		try {
			dependencyInfo.setSha1(ChecksumUtils.calculateSHA1(file));
		} catch (IOException e) {
			log.warn(WssUtils.logMsg(LOG_COMPONENT, ERROR_SHA1 + "for " + file.getAbsolutePath()));
		}

		return dependencyInfo;
	}

	@Override
	protected String getLogComponent() {
		return LOG_COMPONENT;
	}
}
