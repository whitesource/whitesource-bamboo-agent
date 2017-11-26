
package org.whitesource.bamboo.plugin.freestyle;

import org.apache.commons.lang.StringUtils;
import org.whitesource.agent.api.model.AgentProjectInfo;
import org.whitesource.agent.api.model.ChecksumType;
import org.whitesource.agent.api.model.Coordinates;
import org.whitesource.agent.api.model.DependencyInfo;
import org.whitesource.agent.hash.ChecksumUtils;
import org.whitesource.agent.hash.HashCalculator;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class GenericOssInfoExtractor extends BaseOssInfoExtractor {
	private static final String LOG_COMPONENT = "GenericExtractor";
	private static final String JAVA_SCRIPT_REGEX = ".*\\.js";

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
		
		//Path startingDir = Paths.get(root.getAbsolutePath());
		String glob;
		String startingDir;
		
		for (String pattern : includePatterns) {

			if(pattern.startsWith("*")){
				glob = "glob:"+pattern;
			}else{
				startingDir = root.getAbsolutePath();
				if(System.getProperty("os.name").startsWith("Windows")){
					startingDir = startingDir.replace("\\", "/");
				}
				glob = "glob:"+startingDir+"/"+pattern;
			}

			final PathMatcher includePathMatcher = FileSystems.getDefault().getPathMatcher(glob);

			try {
				Files.walkFileTree(root.toPath(), new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
						
						if (includePathMatcher.matches(path) && !checkForExclusions(path,root)) {
							log.debug("File :" + path.toFile());
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
	
	private boolean checkForExclusions(Path path,File root){
		
		String glob;
		boolean exFlag = false;
		String startingDir;
		
		for (String pattern : excludePatterns) {

			if(pattern.startsWith("*")){
				glob = "glob:"+pattern;
			}else{
				startingDir = root.getAbsolutePath();
				if(System.getProperty("os.name").startsWith("Windows")){
					startingDir = startingDir.replace("\\", "/");
				}
				glob = "glob:"+startingDir+"/"+pattern;
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
			if (file.getName().toLowerCase().matches(JAVA_SCRIPT_REGEX)) {
				Map<ChecksumType, String> javaScriptChecksums = new HashMap<>();
				try {
					javaScriptChecksums = new HashCalculator().calculateJavaScriptHashes(file);
				} catch (Exception e) {
					log.error("Failed to calculate javaScript file hash for :" + file.getName());
					log.debug("Failed to calculate javaScript file hash for :" + e.getMessage());
				}
				for (Map.Entry<ChecksumType, String> entry : javaScriptChecksums.entrySet()) {
					dependencyInfo.addChecksum(entry.getKey(), entry.getValue());
				}
			}

			// other platform SHA1
			ChecksumUtils.calculateOtherPlatformSha1(dependencyInfo, file);

			// super hash
			ChecksumUtils.calculateSuperHash(dependencyInfo, file);
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
