package webcat.stylechecking;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IStartup;

public class EarlyStartup implements IStartup {

    @Override
    public void earlyStartup() {
        ILog log = Platform.getLog(EarlyStartup.class);
        log.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "Early startup triggered"));
        Activator activator = Activator.getDefault();
        if (activator == null) {
            log.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
                    "Activator not yet started during earlyStartup, registering listener manually"));
            Activator.ensureListenerRegistered();
        }
    }
}

