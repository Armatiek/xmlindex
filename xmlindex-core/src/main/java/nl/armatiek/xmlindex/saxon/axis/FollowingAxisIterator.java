package nl.armatiek.xmlindex.saxon.axis;

import java.io.IOException;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery.Builder;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.pattern.NodeTest;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.node.HierarchyNode;
import nl.armatiek.xmlindex.node.Node;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexNodeInfo;

public class FollowingAxisIterator extends SearchResultsForwardAxisIterator  {
  
  public FollowingAxisIterator(Session context, XMLIndexNodeInfo contextNode, 
      NodeTest nodeTest, XPathContext xpathContext) {
    super(context, contextNode, nodeTest, false, xpathContext);
  }
  
  @Override
  protected void addAxisClauses(Builder queryBuilder) {
    try {
      long docRight = ((HierarchyNode) contextNode.getOwnerDocument().getRealNode()).right;
      queryBuilder.add(new BooleanClause(LongPoint.newRangeQuery(Definitions.FIELDNAME_LEFT, ((HierarchyNode) node).right, Definitions.MAX_LONG), BooleanClause.Occur.FILTER));
      queryBuilder.add(new BooleanClause(LongPoint.newRangeQuery(Definitions.FIELDNAME_RIGHT, 0, docRight), BooleanClause.Occur.FILTER));
    } catch (IOException ioe) {
      throw new XMLIndexException("Error adding query clauses for following axis", ioe);
    }
  }
    
  @Override
  protected XMLIndexNodeInfo getXMLIndexNodeInfo(Node node) {
    return new XMLIndexNodeInfo(session, node);
  }
  
}