package webcat.stylechecking;

import org.eclipse.core.resources.IResource;

import java.util.List;

/**
 * A style checker that can analyze a resource and report any style violations found.
 */
public interface StyleChecker {

    /**
     * Checks the given resource for style violations and returns a list of any violations found.
     *
     * @param resource the resource to check
     * @return a list of style violations found in the resource
     * @throws Exception if an error occurs during style checking
     */
    List<StyleViolation> check(IResource resource) throws Exception;
}

