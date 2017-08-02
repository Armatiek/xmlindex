package nl.armatiek.xmlindex.saxon.axis;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery.Builder;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.pattern.NodeTest;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.node.Node;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexNodeInfo;

public class ParentAxisIterator extends SearchResultsForwardAxisIterator  {
    
  public ParentAxisIterator(Session context, XMLIndexNodeInfo contextNode, 
      NodeTest nodeTest, XPathContext xpathContext) {
    super(context, contextNode, nodeTest, false, xpathContext);
  }
  
  @Override
  protected void addAxisClauses(Builder queryBuilder) {
    queryBuilder.add(new BooleanClause(LongPoint.newExactQuery(Definitions.FIELDNAME_LEFT, node.parent), BooleanClause.Occur.FILTER));
  }
  
  @Override
  protected XMLIndexNodeInfo getXMLIndexNodeInfo(Node node) {
    return new XMLIndexNodeInfo(session, node);
  }
    
}