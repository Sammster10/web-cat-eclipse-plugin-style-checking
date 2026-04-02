package webcat.stylechecking.checkers;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.Configuration;
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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CheckstyleStyleChecker implements StyleChecker {

    private static final ILog LOG = Platform.getLog(CheckstyleStyleChecker.class);
    private static final String CONFIG_DIR = "config/checkstyle";

    @Override
    public List<StyleViolation> check(IResource resource) throws Exception {
        List<IFile> javaFiles = collectJavaFiles(resource);
        if (javaFiles.isEmpty()) {
            return List.of();
        }

        Path configDir = resolvePluginPath(CONFIG_DIR);
        if (configDir == null) {
            LOG.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "No Checkstyle config directory found."));
            return List.of();
        }

        List<Path> configPaths = findConfigsInDir(configDir);
        if (configPaths.isEmpty()) {
            LOG.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "No Checkstyle config files found."));
            return List.of();
        }

        List<File> filesToCheck = new ArrayList<>();
        for (IFile file : javaFiles) {
            filesToCheck.add(file.getLocation().toFile());
        }

        List<StyleViolation> allViolations = new ArrayList<>();

        for (Path configPath : configPaths) {
            try {
                Properties properties = new Properties();
                Configuration configuration = ConfigurationLoader.loadConfiguration(
                        configPath.toAbsolutePath().toString(),
                        new PropertiesExpander(properties)
                );

                Checker checker = new Checker();
                checker.setModuleClassLoader(getClass().getClassLoader());
                checker.configure(configuration);

                CollectingAuditListener listener = new CollectingAuditListener();
                checker.addListener(listener);

                checker.process(filesToCheck);
                checker.destroy();

                allViolations.addAll(listener.getViolations());
            } catch (Exception e) {
                LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        String.format("Failed to run Checkstyle with config: %s", configPath), e));
            }
        }

        return allViolations;
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

    private List<Path> findConfigsInDir(Path configDir) {
        List<Path> configs = new ArrayList<>();
        if (!Files.isDirectory(configDir)) {
            return configs;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(configDir, "*.xml")) {
            for (Path entry : stream) {
                configs.add(entry);
            }
        } catch (Exception e) {
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    String.format("Failed to scan Checkstyle config directory: %s", configDir), e));
        }
        return configs;
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
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    String.format("Failed to resolve plugin path: %s", relativePath), e));
            return null;
        }
    }

    private static class CollectingAuditListener implements AuditListener {

        private final List<StyleViolation> violations = new ArrayList<>();

        @Override
        public void auditStarted(AuditEvent event) {
        }

        @Override
        public void auditFinished(AuditEvent event) {
        }

        @Override
        public void fileStarted(AuditEvent event) {
        }

        @Override
        public void fileFinished(AuditEvent event) {
        }

        @Override
        public void addError(AuditEvent event) {
            violations.add(new StyleViolation(
                    StyleViolationSource.CHECKSTYLE,
                    event.getFileName(),
                    event.getLine(),
                    event.getLine(),
                    event.getMessage()
            ));
        }

        @Override
        public void addException(AuditEvent event, Throwable throwable) {
            Platform.getLog(CheckstyleStyleChecker.class).log(new Status(
                    IStatus.ERROR, Activator.PLUGIN_ID,
                    String.format("Checkstyle exception in file: %s", event.getFileName()), throwable));
        }

        public List<StyleViolation> getViolations() {
            return violations;
        }
    }
}

