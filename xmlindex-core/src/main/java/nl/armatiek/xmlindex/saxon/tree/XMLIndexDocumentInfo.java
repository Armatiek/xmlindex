package nl.armatiek.xmlindex.saxon.tree;

import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.node.DocumentElement;


public class XMLIndexDocumentInfo extends XMLIndexNodeInfo {

  public XMLIndexDocumentInfo(Session session, DocumentElement docElem) {
    super(session, docElem);
  }
  
  public String getUri() {
    return ((DocumentElement) node).uri;
  }

}
