package nl.armatiek.xmlindex.saxon.axis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.iter.LookaheadIterator;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.node.Attribute;
import nl.armatiek.xmlindex.node.Element;
import nl.armatiek.xmlindex.node.Node;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexNodeInfo;

public class AttributeAxisIterator implements AxisIterator, LastPositionFinder, LookaheadIterator {
  
  protected Session context;
  protected XMLIndexNodeInfo contextNode;
  protected Node node;
  protected NodeTest nodeTest;
  protected List<XMLIndexNodeInfo> attrsList;
  protected Iterator<XMLIndexNodeInfo> attrsIter;
  
  public AttributeAxisIterator(Session context, XMLIndexNodeInfo contextNode, NodeTest nodeTest) {
    this.context = context;
    this.contextNode = contextNode;
    this.node = (Node) contextNode.getRealNode();
    this.nodeTest = nodeTest;
    this.attrsList = getAttrsList();
    if (this.attrsList != null)
      this.attrsIter = attrsList.iterator();
  }
  
  protected List<XMLIndexNodeInfo> getAttrsList() {
    if (((Element) node).attributes == null)
      return null;
    ArrayList<XMLIndexNodeInfo> attrs = new ArrayList<XMLIndexNodeInfo>();
    for (Attribute attr : ((Element) node).attributes) {
      XMLIndexNodeInfo attrNodeInfo = new XMLIndexNodeInfo(this.context, attr, contextNode);
       if (nodeTest.matchesNode(attrNodeInfo))
        attrs.add(attrNodeInfo);
    }
    return attrs;
  }
  
  @Override
  public void close() { }

  @Override
  public int getProperties() {
    return SequenceIterator.LOOKAHEAD | SequenceIterator.LAST_POSITION_FINDER;    
  }

  @Override
  public XMLIndexNodeInfo next() {
    if (attrsIter == null || !attrsIter.hasNext())
      return null;
    return attrsIter.next();
  }

  /* LastPositionFinder */
  @Override
  public int getLength() throws XPathException {
    return attrsList != null ? attrsList.size() : 0;
  }

  /* LookaheadIterator */
  @Override
  public boolean hasNext() {
    return attrsIter == null ? false : attrsIter.hasNext();        
  }
  
}