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

public class AncestorAxisIterator extends SearchResultsReversedAxisIterator  {
  
  public AncestorAxisIterator(Session context, XMLIndexNodeInfo contextNode, 
      NodeTest nodeTest, boolean includeSelf, XPathContext xpathContext) {
    super(context, contextNode, nodeTest, includeSelf, xpathContext);
  }
  
  @Override
  protected void addAxisClauses(Builder queryBuilder) {
    long left = includeSelf ? ((HierarchyNode) node).left : Math.addExact(((HierarchyNode) node).left, -1);
    long right = includeSelf ? ((HierarchyNode) node).right : Math.addExact(((HierarchyNode) node).right, 1);
    queryBuilder.add(new BooleanClause(LongPoint.newRangeQuery(Definitions.FIELDNAME_LEFT, 0, left), BooleanClause.Occur.FILTER));
    queryBuilder.add(new BooleanClause(LongPoint.newRangeQuery(Definitions.FIELDNAME_RIGHT, right, Definitions.MAX_LONG), BooleanClause.Occur.FILTER));
  }
    
  @Override
  protected XMLIndexNodeInfo getXMLIndexNodeInfo(Node node) {
    return new XMLIndexNodeInfo(session, node);
  }
  
}