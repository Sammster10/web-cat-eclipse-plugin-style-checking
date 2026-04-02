package webcat.stylechecking;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

public class StyleCheckResourceListener implements IResourceChangeListener {

    private static final ILog LOG = Platform.getLog(StyleCheckResourceListener.class);

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta delta = event.getDelta();
        if (delta == null) {
            return;
        }

        try {
            delta.accept((IResourceDeltaVisitor) d -> {
                if (d.getResource().getType() == IResource.FILE
                        && d.getResource().getName().endsWith(".java")
                        && (d.getKind() == IResourceDelta.ADDED
                        || (d.getKind() == IResourceDelta.CHANGED
                        && (d.getFlags() & IResourceDelta.CONTENT) != 0))) {
                    StyleCheckRunner.getInstance().submit(d.getResource());
                }
                return true;
            });
        } catch (CoreException e) {
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to process resource change delta", e));
        }
    }
}

