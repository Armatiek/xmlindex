package nl.armatiek.xmlindex.saxon;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;

import net.sf.saxon.lib.StandardURIResolver;
import net.sf.saxon.trans.XPathException;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.utils.URIUtils;

public class XMLIndexURIResolver extends StandardURIResolver {
  
  private Session session;
  
  public XMLIndexURIResolver(Session session) {
    this.session = session;
    setRecognizeQueryParameters(true);
  }
  
  @Override
  public Source resolve(String href, String base) throws XPathException {
    URI uri;
    try {
      uri = new URI(href);
    } catch (URISyntaxException e) {
      throw new XPathException("Syntax error in URI \"" + href + "\"", e);
    } 
    if (uri.isAbsolute() && uri.getScheme().equals(Definitions.SCHEME_XMLINDEX)) {  
      try {
        Map<String, List<String>> params = URIUtils.getQueryParams(uri);
        List<String> uris = params.get("uri");
        if (uris == null || uris.size() != 1)
          throw new XPathException("Missing required parameter \"uri\"");
        Source src = session.getDocument(uris.get(0));
        return src;
      } catch (IOException ioe) {
        throw new XPathException("Error resolving URI \"" + href + "\"", ioe);
      }
    } else {  
      return super.resolve(href, base);
    }
  }
}