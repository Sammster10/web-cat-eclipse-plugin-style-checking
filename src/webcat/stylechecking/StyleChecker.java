package webcat.stylechecking;

import org.eclipse.core.resources.IResource;

import java.util.List;

public interface StyleChecker {

    List<StyleViolation> check(IResource resource) throws Exception;

    List<StyleViolation> check(IResource resource, String sourceCode) throws Exception;
}

