package nl.armatiek.xmlindex.saxon.axis;

import java.io.IOException;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery.Builder;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.lucene.query.HitIterator;
import nl.armatiek.xmlindex.node.HierarchyNode;
import nl.armatiek.xmlindex.node.Node;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexNodeInfo;

public class ChildAxisIterator extends SearchResultsForwardAxisIterator  {
  
  public ChildAxisIterator(Session context, XMLIndexNodeInfo contextNode, 
      NodeTest nodeTest, XPathContext xpathContext) {
    super(context, contextNode, nodeTest, false, xpathContext);
  }
  
  @Override
  protected HitIterator getHits() throws IOException, XPathException {
    if (((HierarchyNode) node).right == ((HierarchyNode) node).left+1)
      return null;
    return super.getHits();
  }
  
  @Override
  protected void addAxisClauses(Builder queryBuilder) {
    queryBuilder.add(new BooleanClause(LongPoint.newExactQuery(Definitions.FIELDNAME_PARENT, 
        ((HierarchyNode) node).left), BooleanClause.Occur.FILTER));
  }
  
  @Override
  protected XMLIndexNodeInfo getXMLIndexNodeInfo(Node node) {
    return new XMLIndexNodeInfo(session, node, this.contextNode);
  }
  
}