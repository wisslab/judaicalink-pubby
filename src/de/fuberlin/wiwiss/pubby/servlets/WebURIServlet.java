package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.MappedResource;
import de.fuberlin.wiwiss.pubby.negotiation.ContentTypeNegotiator;
import de.fuberlin.wiwiss.pubby.negotiation.MediaRangeSpec;
import de.fuberlin.wiwiss.pubby.negotiation.PubbyNegotiator;

/**
 * Servlet that handles the public URIs of mapped resources.
 * It redirects either to the page URL or to the data URL,
 * based on content negotiation.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author Kai Eckert (kai@informatik.uni-mannheim.de)
 * @version $Id$
 */
public class WebURIServlet extends BaseServlet {

    private Logger log = Logger.getLogger(getClass().getName());

	public boolean doGet(String relativeURI, HttpServletRequest request,
			HttpServletResponse response, Configuration config) throws IOException, ServletException {
		MappedResource resource = config.getMappedResourceFromRelativeWebURI(relativeURI, true);
		if (resource == null) return false;

		response.addHeader("Vary", "Accept, User-Agent");
		ContentTypeNegotiator negotiator = PubbyNegotiator.getPubbyNegotiator();
		MediaRangeSpec bestMatch = negotiator.getBestMatch(
				request.getHeader("Accept"), request.getHeader("User-Agent"));
		if (bestMatch == null) {
			response.setStatus(406);
			response.setContentType("text/plain");
			response.getOutputStream().println(
					"406 Not Acceptable: The requested data format is not supported. " +
					"Only HTML and RDF are available.");
			return true;
		}
		
		String location;
        String message;
        if ("text/html".equals(bestMatch.getMediaType()) && request.getParameter("output")==null) {
		    location = resource.getPageURL();
            message = "For an HTML representation, see " + location;
            log.fine("HTML output, redirecting to " + location);
        } else if (resource.getDataset().redirectRDFRequestsToEndpoint()) {
			location = resource.getDataset().getDataSource().getResourceDescriptionURL(
					resource.getDatasetURI());
            message = "For a description of this item, see " + location;
        } else  if (resource.getDataset().isDataResource()) {
            log.fine("Data resource mode, forwarding to DataURLServlet.");
            getServletContext().getNamedDispatcher("DataURLServlet").forward(request, response);
            return true;
        } else {
            location = resource.getDataURL();
            message = "For a description of this item, see " + location;
        }
        log.fine("Redirect to: " + location);
        response.setStatus(303);
        response.setContentType("text/plain");
        response.addHeader("Location", location);
        response.getOutputStream().println(
                "303 See Other: " + message);
        return true;
	}
}