package webcat.stylechecking.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import webcat.stylechecking.StyleCheckRunner;

public class RunStyleChecksHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart editor = HandlerUtil.getActiveEditor(event);

        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection structured && !structured.isEmpty()) {
            for (Object element : structured) {
                IResource resource = Adapters.adapt(element, IResource.class);
                if (resource != null) {
                    String sourceCode = getLiveContent(editor, resource);
                    StyleCheckRunner.getInstance().submit(resource, sourceCode);
                }
            }
            return null;
        }

        if (editor != null) {
            IEditorInput input = editor.getEditorInput();
            IResource resource = Adapters.adapt(input, IResource.class);
            if (resource != null) {
                String sourceCode = getLiveContent(editor, resource);
                StyleCheckRunner.getInstance().submit(resource, sourceCode);
            }
        }

        return null;
    }

    private String getLiveContent(IEditorPart editor, IResource resource) {
        if (!(editor instanceof ITextEditor textEditor)) {
            return null;
        }
        IResource editorResource = Adapters.adapt(textEditor.getEditorInput(), IResource.class);
        if (editorResource == null || !editorResource.equals(resource)) {
            return null;
        }
        if (!textEditor.isDirty()) {
            return null;
        }
        IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
        if (document == null) {
            return null;
        }
        return document.get();
    }
}

