package webcat.stylechecking;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

class DocumentCache {

    static IDocument getDocument(IFile file) {
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

