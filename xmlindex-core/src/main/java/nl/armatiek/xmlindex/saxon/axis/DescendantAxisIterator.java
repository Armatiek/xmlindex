package nl.armatiek.xmlindex.saxon.axis;

import java.io.IOException;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery.Builder;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.lucene.query.HitIterator;
import nl.armatiek.xmlindex.node.HierarchyNode;
import nl.armatiek.xmlindex.node.Node;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexNodeInfo;

public class DescendantAxisIterator extends SearchResultsForwardAxisIterator  {
  
  public DescendantAxisIterator(Session context, XMLIndexNodeInfo contextNode, 
      NodeTest nodeTest, boolean includeSelf, XPathContext xpathContext) {
    super(context, contextNode, nodeTest, includeSelf, xpathContext);
  }
  
  @Override
  protected HitIterator getHits() throws IOException, XPathException {
    if (((HierarchyNode) node).right == ((HierarchyNode) node).left+1)
      return null;
    return super.getHits();
  }
    
  @Override
  protected void addAxisClauses(Builder queryBuilder) {
    long left = (includeSelf) ? ((HierarchyNode) node).left : Math.addExact(((HierarchyNode) node).left, 1);
    long right = (includeSelf) ? ((HierarchyNode) node).right : Math.addExact(((HierarchyNode) node).right, -1);
    queryBuilder.add(new BooleanClause(LongPoint.newRangeQuery(Definitions.FIELDNAME_LEFT, left, right), BooleanClause.Occur.FILTER));
  }
    
  @Override
  protected XMLIndexNodeInfo getXMLIndexNodeInfo(Node node) {
    return new XMLIndexNodeInfo(session, node);
  }
  
  @Override
  public AxisIterator getAnother() {    
    return new DescendantAxisIterator(session, contextNode, nodeTest, includeSelf, xpathContext);
  }
  
}