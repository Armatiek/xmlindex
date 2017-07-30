package nl.armatiek.xmlindex.saxon;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Document;

import net.sf.saxon.lib.StandardOutputResolver;
import net.sf.saxon.trans.XPathException;
import nl.armatiek.xmlindex.Session;

public class XMLIndexOutputURIResolver extends StandardOutputResolver {
  
  private Session session;
  
  public XMLIndexOutputURIResolver(Session session) {
    this.session = session;
  } 

  @Override
  public Result resolve(String href, String base) throws XPathException {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setValidating(false);
      dbf.setNamespaceAware(true);
      DocumentBuilder builder = dbf.newDocumentBuilder();
      DOMResult result = new DOMResult(builder.newDocument(), href);
      return result;
    } catch (Exception e) {
      throw new XPathException("Error resolving document \"" + href + "\"", e);
    }
  }
  
  @Override
  public void close(Result result) throws XPathException {
    if (!(result instanceof DOMResult)) {
      super.close(result);
      return;
    }
    Document doc = (Document) ((DOMResult) result).getNode();
  }

  @Override
  public XMLIndexOutputURIResolver newInstance() {
    return new XMLIndexOutputURIResolver(session);
  }
  
}
