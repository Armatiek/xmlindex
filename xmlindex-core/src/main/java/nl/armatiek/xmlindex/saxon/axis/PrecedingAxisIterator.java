package nl.armatiek.xmlindex.saxon.axis;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery.Builder;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.tree.iter.AxisIterator;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.node.HierarchyNode;
import nl.armatiek.xmlindex.node.Node;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexNodeInfo;

public class PrecedingAxisIterator extends SearchResultsReversedAxisIterator  {
  
  public PrecedingAxisIterator(Session context, XMLIndexNodeInfo contextNode, 
      NodeTest nodeTest, XPathContext xpathContext) {
    super(context, contextNode, nodeTest, false, xpathContext);
  }
  
  @Override
  protected void addAxisClauses(Builder queryBuilder) {
    long docLeft = ((HierarchyNode) node).docLeft;
    queryBuilder.add(new BooleanClause(LongPoint.newRangeQuery(Definitions.FIELDNAME_RIGHT, 0, ((HierarchyNode) node).left), BooleanClause.Occur.FILTER));
    queryBuilder.add(new BooleanClause(LongPoint.newRangeQuery(Definitions.FIELDNAME_LEFT, docLeft, Definitions.MAX_LONG), BooleanClause.Occur.FILTER));
  }
    
  @Override
  protected XMLIndexNodeInfo getXMLIndexNodeInfo(Node node) {
    return new XMLIndexNodeInfo(session, node);
  }
  
  @Override
  public AxisIterator getAnother() {    
    return new PrecedingAxisIterator(session, contextNode, nodeTest, xpathContext);
  }
  
}