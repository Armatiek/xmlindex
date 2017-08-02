package nl.armatiek.xmlindex.saxon.axis;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery.Builder;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.pattern.NodeTest;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.node.HierarchyNode;
import nl.armatiek.xmlindex.node.Node;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexNodeInfo;

public class PrecedingSiblingAxisIterator extends SearchResultsReversedAxisIterator  {
  
  public PrecedingSiblingAxisIterator(Session context, XMLIndexNodeInfo contextNode, 
      NodeTest nodeTest, XPathContext xpathContext) {
    super(context, contextNode, nodeTest, false, xpathContext);
  }
  
  @Override
  protected void addAxisClauses(Builder queryBuilder) {
    long left = Math.addExact(((HierarchyNode) node).left, -1);
    queryBuilder.add(new BooleanClause(LongPoint.newExactQuery(Definitions.FIELDNAME_PARENT, node.parent), BooleanClause.Occur.FILTER));
    queryBuilder.add(new BooleanClause(LongPoint.newRangeQuery(Definitions.FIELDNAME_LEFT, 0, left), BooleanClause.Occur.FILTER));
  }
    
  @Override
  protected XMLIndexNodeInfo getXMLIndexNodeInfo(Node node) {
    return new XMLIndexNodeInfo(session, node, contextNode.getCachedParent());
  }
  
}