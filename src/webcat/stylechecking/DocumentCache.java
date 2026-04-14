package webcat.stylechecking;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.ITextEditor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

class DocumentCache {

    static IDocument getDocument(IFile file) {
        IDocument liveDoc = findLiveDocument(file);
        if (liveDoc != null) {
            return liveDoc;
        }
        return readFromDisk(file);
    }

    private static IDocument findLiveDocument(IFile file) {
        try {
            for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
                for (IWorkbenchPage page : window.getPages()) {
                    for (IEditorReference ref : page.getEditorReferences()) {
                        IEditorPart editor = ref.getEditor(false);
                        if (editor instanceof ITextEditor textEditor) {
                            IFile editorFile = Adapters.adapt(textEditor.getEditorInput(), IFile.class);
                            if (file.equals(editorFile)) {
                                IDocument doc = textEditor.getDocumentProvider()
                                        .getDocument(textEditor.getEditorInput());
                                if (doc != null) {
                                    return doc;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static IDocument readFromDisk(IFile file) {
        try (InputStream is = file.getContents();
             Reader reader = new InputStreamReader(is, Charset.forName(file.getCharset()))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int read;
            while ((read = reader.read(buf)) != -1) {
                sb.append(buf, 0, read);
            }
            return new Document(sb.toString());
        } catch (CoreException | java.io.IOException e) {
            return null;
        }
    }
}

