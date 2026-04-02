package webcat.stylechecking.checkers;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.lang.rule.RuleSetLoader;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.*;
import webcat.stylechecking.Activator;
import webcat.stylechecking.StyleChecker;
import webcat.stylechecking.StyleViolation;
import webcat.stylechecking.StyleViolationSource;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A style checker that uses PMD to analyze Java files for style violations.
 */
public class PmdStyleChecker implements StyleChecker {

    private static final ILog LOG = Platform.getLog(PmdStyleChecker.class);
    private static final String RULES_DIR = "config/pmd/rules";
    private static final String LIB_DIR = "config/pmd/lib";

    @Override
    public List<StyleViolation> check(IResource resource) throws Exception {
        List<IFile> javaFiles = collectJavaFiles(resource);
        if (javaFiles.isEmpty()) {
            return List.of();
        }

        Path rulesetsDir = resolvePluginPath(RULES_DIR);
        if (rulesetsDir == null) {
            LOG.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "No PMD rulesets directory found."));
            return List.of();
        }

        List<Path> rulesetPaths = findRulesetsInDir(rulesetsDir);
        if (rulesetPaths.isEmpty()) {
            LOG.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "No PMD rulesets found."));
            return List.of();
        }
        List<URL> customJarUrls;

        Path defaultLibDir = resolvePluginPath(LIB_DIR);
        if (defaultLibDir == null) {
            LOG.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "No custom PMD lib directory found."));
            customJarUrls = List.of();
        } else {
            customJarUrls = findCustomJarsInDir(defaultLibDir);
        }

        URLClassLoader customClassLoader = null;
        try {
            PMDConfiguration config = new PMDConfiguration();
            config.setIgnoreIncrementalAnalysis(true);

            if (!customJarUrls.isEmpty()) {
                customClassLoader = new URLClassLoader(customJarUrls.toArray(URL[]::new), getClass().getClassLoader());
                config.setClassLoader(customClassLoader);
            }

            List<RuleSet> ruleSets = new ArrayList<>();
            try (PmdAnalysis analysis = PmdAnalysis.create(config)) {
                RuleSetLoader ruleSetLoader = analysis.newRuleSetLoader();
                if (customClassLoader != null) {
                    ruleSetLoader.loadResourcesWith(customClassLoader);
                }

                for (Path rulesetPath : rulesetPaths) {
                    try {
                        ruleSets.add(ruleSetLoader.loadFromResource(rulesetPath.toAbsolutePath().toString()));
                    } catch (Exception e) {
                        LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to load PMD ruleset: %s".formatted(rulesetPath), e));
                    }
                }

                if (ruleSets.isEmpty()) {
                    LOG.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "All PMD rulesets failed to load"));
                    return List.of();
                }

                for (RuleSet ruleSet : ruleSets) {
                    analysis.addRuleSet(ruleSet);
                }
                for (IFile file : javaFiles) {
                    analysis.files().addFile(file.getLocation().toFile().toPath());
                }
                Report report = analysis.performAnalysisAndCollectReport();
                return toViolations(report);
            }
        } finally {
            if (customClassLoader != null) {
                customClassLoader.close();
            }
        }
    }

    private List<StyleViolation> toViolations(Report report) {
        List<StyleViolation> violations = new ArrayList<>();
        for (RuleViolation violation : report.getViolations()) {
            violations.add(new StyleViolation(
                    StyleViolationSource.PMD,
                    violation.getFileId().getAbsolutePath(),
                    violation.getBeginLine(),
                    violation.getEndLine(),
                    violation.getDescription()
            ));
        }
        return violations;
    }

    private List<IFile> collectJavaFiles(IResource resource) throws CoreException {
        List<IFile> files = new ArrayList<>();
        if (resource instanceof IFile file && file.getName().endsWith(".java")) {
            files.add(file);
        } else {
            resource.accept((IResourceVisitor) r -> {
                if (r instanceof IFile f && f.getName().endsWith(".java")) {
                    files.add(f);
                }
                return true;
            });
        }
        return files;
    }

    private List<Path> findRulesetsInDir(Path rulesDir) {
        List<Path> rulesets = new ArrayList<>();
        if (!Files.isDirectory(rulesDir)) {
            return rulesets;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(rulesDir, "*.xml")) {
            for (Path entry : stream) {
                rulesets.add(entry);
            }
        } catch (Exception e) {
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to scan rules directory: %s".formatted(rulesDir), e));
        }
        return rulesets;
    }

    private List<URL> findCustomJarsInDir(Path libDir) {
        List<URL> urls = new ArrayList<>();
        if (!Files.isDirectory(libDir)) {
            return urls;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(libDir, "*.jar")) {
            for (Path entry : stream) {
                urls.add(entry.toUri().toURL());
            }
        } catch (Exception e) {
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to scan lib directory: %s".formatted(libDir), e));
        }
        return urls;
    }

    private Path resolvePluginPath(String relativePath) {
        try {
            URL entry = Platform.getBundle(Activator.PLUGIN_ID).getEntry(relativePath);
            if (entry == null) {
                return null;
            }
            File file = new File(FileLocator.toFileURL(entry).getPath());
            return file.toPath();
        } catch (Exception e) {
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to resolve plugin path: %s".formatted(relativePath), e));
            return null;
        }
    }
}

