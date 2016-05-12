
package org.whitesource.bamboo.plugin.freestyle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.whitesource.agent.api.ChecksumUtils;
import org.whitesource.agent.api.model.AgentProjectInfo;
import org.whitesource.agent.api.model.Coordinates;
import org.whitesource.agent.api.model.DependencyInfo;

public class GenericOssInfoExtractor extends BaseOssInfoExtractor
{
    private static final String LOG_COMPONENT = "GenericExtractor";

    protected String projectName;
    protected List<Pattern> includePatterns;
    protected List<Pattern> excludePatterns;
    protected java.io.File checkoutDirectory;

    /**
     * Constructor
     * 
     * @param runner
     */
    public GenericOssInfoExtractor(final String projectName, final String projectToken, final String includes,
            final String excludes, final java.io.File checkoutDirectory)
    {
        super(projectToken, includes, excludes);

        this.projectName = projectName;
        this.checkoutDirectory = checkoutDirectory;

        includePatterns = new ArrayList<Pattern>();
        for (String pattern : this.includes)
        {
            includePatterns.add(Pattern.compile(convertGlobToRegEx(pattern)));
        }

        excludePatterns = new ArrayList<Pattern>();
        for (String pattern : this.excludes)
        {
            excludePatterns.add(Pattern.compile(convertGlobToRegEx(pattern)));
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

        if(StringUtils.isBlank(projectToken)){
        	 projectInfo.setCoordinates(new Coordinates(null, projectName, null));
        }else{
        	  projectInfo.setProjectToken(projectToken);
        }
        
        if (includePatterns.isEmpty())
        {
            log.error(WssUtils.logMsg(LOG_COMPONENT, "No include patterns defined. Failing."));
        }
        else
        {
            log.info(WssUtils.logMsg(LOG_COMPONENT, "Including files matching:\r" + StringUtils.join(includes, "\r")));
            log.info(WssUtils.logMsg(LOG_COMPONENT, "Exluding files matching:\r" + StringUtils.join(excludes, "\r")));

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
                final String path = FilenameUtils.normalize(
                        ResourceUtils.getRelativePath(file.getPath(), absoluteRoot.getPath(), File.separator));

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
        DependencyInfo dependencyInfo = new DependencyInfo();

        dependencyInfo.setSystemPath(file.getAbsolutePath());
        dependencyInfo.setArtifactId(file.getName());

        try
        {
            dependencyInfo.setSha1(ChecksumUtils.calculateSHA1(file));
        }
        catch (IOException e)
        {
            log.warn(WssUtils.logMsg(LOG_COMPONENT, ERROR_SHA1 + "for " + file.getAbsolutePath()));
        }

        return dependencyInfo;
    }

    // NOTE: derived from http://stackoverflow.com/a/1248627/45773.
    private String convertGlobToRegEx(String line)
    {
        log.debug(WssUtils.logMsg(LOG_COMPONENT, "Input glob expression: " + line));
        line = line.trim();
        int strLen = line.length();
        StringBuilder sb = new StringBuilder(strLen);
        // Remove beginning and ending * globs because they're useless
        if (line.startsWith("*"))
        {
            line = line.substring(1);
            strLen--;
        }
        if (line.endsWith("*"))
        {
            line = line.substring(0, strLen - 1);
            strLen--;
        }
        boolean escaping = false;
        int inCurlies = 0;
        for (char currentChar : line.toCharArray())
        {
            switch (currentChar)
            {
            case '*':
                if (escaping)
                    sb.append("\\*");
                else
                    sb.append(".*");
                escaping = false;
                break;
            case '?':
                if (escaping)
                    sb.append("\\?");
                else
                    sb.append('.');
                escaping = false;
                break;
            case '.':
            case '(':
            case ')':
            case '+':
            case '|':
            case '^':
            case '$':
            case '@':
            case '%':
                sb.append('\\');
                sb.append(currentChar);
                escaping = false;
                break;
            case '\\':
                if (escaping)
                {
                    sb.append("\\\\");
                    escaping = false;
                }
                else
                    escaping = true;
                break;
            case '{':
                if (escaping)
                {
                    sb.append("\\{");
                }
                else
                {
                    sb.append('(');
                    inCurlies++;
                }
                escaping = false;
                break;
            case '}':
                if (inCurlies > 0 && !escaping)
                {
                    sb.append(')');
                    inCurlies--;
                }
                else if (escaping)
                    sb.append("\\}");
                else
                    sb.append("}");
                escaping = false;
                break;
            case ',':
                if (inCurlies > 0 && !escaping)
                {
                    sb.append('|');
                }
                else if (escaping)
                    sb.append("\\,");
                else
                    sb.append(",");
                break;
            default:
                escaping = false;
                sb.append(currentChar);
            }
        }

        log.debug(WssUtils.logMsg(LOG_COMPONENT, "Output regular expression: " + sb.toString()));
        return sb.toString();
    }

    @Override
    protected String getLogComponent()
    {
        return LOG_COMPONENT;
    }
}
