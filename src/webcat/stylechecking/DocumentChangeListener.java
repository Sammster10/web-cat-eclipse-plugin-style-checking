package webcat.stylechecking;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DocumentChangeListener implements IPartListener2 {

    private static final long THROTTLE_MS = 1000;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "StyleCheckThrottle");
        t.setDaemon(true);
        return t;
    });

    private final Map<IEditorPart, IDocumentListener> trackedEditors = new HashMap<>();
    private final Map<IFile, Long> lastRunTimes = new HashMap<>();
    private final Map<IFile, ScheduledFuture<?>> trailingChecks = new HashMap<>();

    @Override
    public void partOpened(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);
        if (part instanceof ITextEditor editor) {
            attachListener(editor);
        }
    }

    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);
        if (part instanceof ITextEditor editor) {
            detachListener(editor);
        }
    }

    @Override
    public void partActivated(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partDeactivated(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partVisible(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partInputChanged(IWorkbenchPartReference partRef) {
    }

    private void attachListener(ITextEditor editor) {
        if (trackedEditors.containsKey(editor)) {
            return;
        }
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        if (document == null) {
            return;
        }
        IFile file = Adapters.adapt(editor.getEditorInput(), IFile.class);
        if (file == null || !file.getName().endsWith(".java")) {
            return;
        }

        IDocumentListener listener = new IDocumentListener() {
            @Override
            public void documentAboutToBeChanged(DocumentEvent event) {
            }

            @Override
            public void documentChanged(DocumentEvent event) {
                throttleCheck(file, event.getDocument());
            }
        };
        document.addDocumentListener(listener);
        trackedEditors.put(editor, listener);

        StyleCheckRunner.getInstance().submit(file, document.get());
    }

    private void detachListener(ITextEditor editor) {
        IDocumentListener listener = trackedEditors.remove(editor);
        if (listener == null) {
            return;
        }
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        if (document != null) {
            document.removeDocumentListener(listener);
        }
    }

    private void throttleCheck(IFile file, IDocument document) {
        synchronized (this) {
            long now = System.currentTimeMillis();
            Long lastRun = lastRunTimes.get(file);
            long elapsed = lastRun == null ? THROTTLE_MS : now - lastRun;

            ScheduledFuture<?> existing = trailingChecks.remove(file);
            if (existing != null) {
                existing.cancel(false);
            }

            if (elapsed >= THROTTLE_MS) {
                lastRunTimes.put(file, now);
                String sourceCode = document.get();
                StyleCheckRunner.getInstance().submit(file, sourceCode);
            } else {
                long delay = THROTTLE_MS - elapsed;
                ScheduledFuture<?> future = scheduler.schedule(() -> {
                    synchronized (DocumentChangeListener.this) {
                        trailingChecks.remove(file);
                        lastRunTimes.put(file, System.currentTimeMillis());
                    }
                    String sourceCode = document.get();
                    StyleCheckRunner.getInstance().submit(file, sourceCode);
                }, delay, TimeUnit.MILLISECONDS);
                trailingChecks.put(file, future);
            }
        }
    }

    public void attachToExistingEditors(IWorkbenchPage page) {
        for (IEditorReference ref : page.getEditorReferences()) {
            IEditorPart editor = ref.getEditor(false);
            if (editor instanceof ITextEditor textEditor) {
                attachListener(textEditor);
            }
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}

