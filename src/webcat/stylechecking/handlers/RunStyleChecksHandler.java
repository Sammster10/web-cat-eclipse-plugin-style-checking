package webcat.stylechecking.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import webcat.stylechecking.StyleCheckRunner;

public class RunStyleChecksHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection structured && !structured.isEmpty()) {
            for (Object element : structured) {
                IResource resource = Adapters.adapt(element, IResource.class);
                if (resource != null) {
                    StyleCheckRunner.getInstance().submit(resource);
                }
            }
            return null;
        }

        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        if (editor != null) {
            IEditorInput input = editor.getEditorInput();
            IResource resource = Adapters.adapt(input, IResource.class);
            if (resource != null) {
                StyleCheckRunner.getInstance().submit(resource);
            }
        }

        return null;
    }
}

