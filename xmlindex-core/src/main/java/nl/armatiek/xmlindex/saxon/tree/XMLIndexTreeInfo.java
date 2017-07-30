package nl.armatiek.xmlindex.saxon.tree;

import net.sf.saxon.om.GenericTreeInfo;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.node.IndexRootDocument;

public class XMLIndexTreeInfo extends GenericTreeInfo {

  private Session session;
  private XMLIndexNodeInfo rootNode;
  
  public XMLIndexTreeInfo(Session session) {
    super(session.getConfiguration());
    this.session = session;
  }
  
  @Override
  public XMLIndexNodeInfo getRootNode() {
    if (rootNode == null)
      rootNode = new XMLIndexNodeInfo(session, new IndexRootDocument());
    return rootNode;
  }
  
}