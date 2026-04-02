package webcat.stylechecking;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class StyleCheckRunner {

    public static final String MARKER_TYPE = "Web-CAT_Style_Checking.styleViolation";
    private static final ILog LOG = Platform.getLog(StyleCheckRunner.class);
    private static final StyleCheckRunner INSTANCE = new StyleCheckRunner();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "StyleCheckRunner");
        t.setDaemon(true);
        return t;
    });

    private final AtomicReference<IResource> pendingRequest = new AtomicReference<>();
    private volatile boolean running;
    private final List<StyleChecker> checkers = new ArrayList<>();

    private StyleCheckRunner() {
    }

    public static StyleCheckRunner getInstance() {
        return INSTANCE;
    }

    public void addChecker(StyleChecker checker) {
        checkers.add(checker);
    }

    public synchronized void submit(IResource resource) {
        if (running) {
            pendingRequest.set(resource);
            return;
        }
        startCheck(resource);
    }

    private synchronized void startCheck(IResource resource) {
        running = true;
        executor.submit(() -> {
            try {
                List<StyleViolation> allViolations = new ArrayList<>();
                for (StyleChecker checker : checkers) {
                    try {
                        allViolations.addAll(checker.check(resource));
                    } catch (Exception e) {
                        LOG.log(new Status(
                                IStatus.ERROR,
                                Activator.PLUGIN_ID,
                                "Checker %s failed for: %s".formatted(checker.getClass().getSimpleName(), resource.getFullPath()),
                                e
                        ));
                    }
                }

                resource.deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
                applyMarkers(allViolations, resource);

                LOG.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "Style check complete for %s (%d violations)".formatted(resource.getFullPath(), allViolations.size())));
            } catch (Exception e) {
                LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Style check failed for: %s".formatted(resource.getFullPath()), e));
            } finally {
                onCheckComplete();
            }
        });
    }

    private void applyMarkers(List<StyleViolation> violations, IResource resource) {
        for (StyleViolation violation : violations) {
            IFile[] files = resource.getWorkspace().getRoot()
                    .findFilesForLocationURI(new File(violation.filePath()).toURI());
            if (files.length == 0) {
                continue;
            }
            IFile file = files[0];
            try {
                IMarker marker = file.createMarker(MARKER_TYPE);
                marker.setAttribute(IMarker.MESSAGE, "[%s] %s".formatted(violation.source().getDisplayName(), violation.message()));
                marker.setAttribute(IMarker.LINE_NUMBER, violation.beginLine());
                marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
                marker.setAttribute(IMarker.SOURCE_ID, Activator.PLUGIN_ID);
                setCharRange(marker, file, violation.beginLine(), violation.endLine());
            } catch (CoreException e) {
                LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to create marker on %s".formatted(file.getFullPath()), e));
            }
        }
    }

    private void setCharRange(IMarker marker, IFile file, int beginLine, int endLine) {
        try {
            IDocument document = DocumentCache.getDocument(file);
            if (document == null) {
                return;
            }
            int startOffset = document.getLineOffset(beginLine - 1);
            String startLineText = document.get(startOffset, document.getLineLength(beginLine - 1));
            int leadingWhitespace = startLineText.length() - startLineText.stripLeading().length();
            int charStart = startOffset + leadingWhitespace;

            int endLineIndex = Math.min(endLine, document.getNumberOfLines()) - 1;
            int endOffset = document.getLineOffset(endLineIndex);
            String endLineText = document.get(endOffset, document.getLineLength(endLineIndex));
            int charEnd = endOffset + endLineText.stripTrailing().length();

            if (charEnd > charStart) {
                marker.setAttribute(IMarker.CHAR_START, charStart);
                marker.setAttribute(IMarker.CHAR_END, charEnd);
            }
        } catch (CoreException | BadLocationException e) {
            // Line info is still set; char range is best-effort
        }
    }

    private synchronized void onCheckComplete() {
        running = false;
        IResource next = pendingRequest.getAndSet(null);
        if (next != null) {
            startCheck(next);
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}

