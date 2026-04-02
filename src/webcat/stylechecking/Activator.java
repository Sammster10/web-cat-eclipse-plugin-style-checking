package webcat.stylechecking;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import webcat.stylechecking.checkers.CheckstyleStyleChecker;
import webcat.stylechecking.checkers.PmdStyleChecker;

/**
 * Entry point for the plugin.
 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "Web-CAT_Style_Checking";
    private static final ILog LOG = Platform.getLog(Activator.class);

    private static Activator instance;
    private static volatile boolean listenerRegistered = false;
    private StyleCheckResourceListener resourceListener;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;

        StyleCheckRunner.getInstance().addChecker(new PmdStyleChecker());
        StyleCheckRunner.getInstance().addChecker(new CheckstyleStyleChecker());
        registerListener();

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

    static synchronized void ensureListenerRegistered() {
        if (listenerRegistered) {
            return;
        }
        StyleCheckRunner.getInstance().addChecker(new PmdStyleChecker());
        StyleCheckRunner.getInstance().addChecker(new CheckstyleStyleChecker());
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
        StyleCheckRunner.getInstance().shutdown();
        listenerRegistered = false;
        instance = null;
        super.stop(context);
    }

    public static Activator getDefault() {
        return instance;
    }
}

