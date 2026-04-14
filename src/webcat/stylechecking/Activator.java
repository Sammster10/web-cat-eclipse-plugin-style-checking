package webcat.stylechecking;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import webcat.stylechecking.checkers.PmdStyleChecker;

public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "Web-CAT_Style_Checking";
    private static final ILog LOG = Platform.getLog(Activator.class);

    private static Activator instance;
    private static volatile boolean listenerRegistered = false;
    private StyleCheckResourceListener resourceListener;
    private DocumentChangeListener documentChangeListener;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;

        StyleCheckRunner.getInstance().addChecker(new PmdStyleChecker());
        registerListener();
        registerDocumentChangeListener();

        LOG.log(new Status(IStatus.INFO, PLUGIN_ID, "Plugin started, resource listener registered"));
    }

    private synchronized void registerListener() {
        if (listenerRegistered) {
            return;
        }
        resourceListener = new StyleCheckResourceListener();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(
                resourceListener, IResourceChangeEvent.POST_CHANGE);
        listenerRegistered = true;
    }

    private void registerDocumentChangeListener() {
        documentChangeListener = new DocumentChangeListener();
        IWorkbench workbench = PlatformUI.getWorkbench();
        workbench.getDisplay().asyncExec(() -> {
            for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
                for (IWorkbenchPage page : window.getPages()) {
                    page.addPartListener(documentChangeListener);
                    documentChangeListener.attachToExistingEditors(page);
                }
            }
        });
    }

    static synchronized void ensureListenerRegistered() {
        if (listenerRegistered) {
            return;
        }
        StyleCheckRunner.getInstance().addChecker(new PmdStyleChecker());
        StyleCheckResourceListener listener = new StyleCheckResourceListener();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(
                listener, IResourceChangeEvent.POST_CHANGE);
        listenerRegistered = true;
        LOG.log(new Status(IStatus.INFO, PLUGIN_ID, "Resource listener registered from earlyStartup fallback"));
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (resourceListener != null) {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
        }
        if (documentChangeListener != null) {
            IWorkbench workbench = PlatformUI.getWorkbench();
            for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
                for (IWorkbenchPage page : window.getPages()) {
                    page.removePartListener(documentChangeListener);
                }
            }
            documentChangeListener.shutdown();
        }
        StyleCheckRunner.getInstance().shutdown();
        listenerRegistered = false;
        instance = null;
        super.stop(context);
    }

    public static Activator getDefault() {
        return instance;
    }
}

