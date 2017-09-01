/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.armatiek.xmlindex.saxon.tree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.AtomicSequence;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.CopyOptions;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.pattern.AnyChildNodeTest;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.NamespaceNode;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.tree.util.Navigator;
import net.sf.saxon.tree.wrapper.VirtualNode;
import net.sf.saxon.type.AnySimpleType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.Untyped;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.UntypedAtomicValue;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.lucene.query.HitIterator;
import nl.armatiek.xmlindex.lucene.query.HitQuery;
import nl.armatiek.xmlindex.node.Attribute;
import nl.armatiek.xmlindex.node.DocumentElement;
import nl.armatiek.xmlindex.node.Element;
import nl.armatiek.xmlindex.node.HierarchyNode;
import nl.armatiek.xmlindex.node.IndexRootElement;
import nl.armatiek.xmlindex.node.NamedNode;
import nl.armatiek.xmlindex.node.Namespace;
import nl.armatiek.xmlindex.node.Node;
import nl.armatiek.xmlindex.node.ProcessingInstruction;
import nl.armatiek.xmlindex.node.Text;
import nl.armatiek.xmlindex.node.ValueNode;
import nl.armatiek.xmlindex.saxon.axis.AncestorAxisIterator;
import nl.armatiek.xmlindex.saxon.axis.AttributeAxisIterator;
import nl.armatiek.xmlindex.saxon.axis.ChildAxisIterator;
import nl.armatiek.xmlindex.saxon.axis.DescendantAxisIterator;
import nl.armatiek.xmlindex.saxon.axis.FollowingAxisIterator;
import nl.armatiek.xmlindex.saxon.axis.FollowingSiblingAxisIterator;
import nl.armatiek.xmlindex.saxon.axis.ParentAxisIterator;
import nl.armatiek.xmlindex.saxon.axis.PrecedingAxisIterator;
import nl.armatiek.xmlindex.saxon.axis.PrecedingSiblingAxisIterator;

public class XMLIndexNodeInfo implements NodeInfo, VirtualNode /*, SteppingNode<XMLIndexNodeInfo>, SiblingCountingNode */ {
  
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(XMLIndexNodeInfo.class);
  
  protected Session session;
  protected Configuration config;
  protected XMLIndexTreeInfo tree;
  protected Node node;
  protected String systemId;  
  protected List<XMLIndexNodeInfo> attributes;
  protected XMLIndexNodeInfo parentNode;
  protected XMLIndexNodeInfo ownerDocument;
  protected String documentURI;
  
  protected static final Set<String> documentURIFieldsToLoad = new HashSet<String>(1);
  static {
    documentURIFieldsToLoad.add(Definitions.FIELDNAME_URI);
  }
  
  public XMLIndexNodeInfo(Session session, Node node) {
    this.session = session;
    this.node = node;
    this.tree = session.getTreeInfo();
    this.config = session.getConfiguration();
    if (node instanceof DocumentElement)
      documentURI = ((DocumentElement) node).uri;
  }
  
  public XMLIndexNodeInfo(Session context, Node node, XMLIndexNodeInfo parentNode) {
    this(context, node);
    this.parentNode = parentNode;
  }
    
  @Override
  public AtomicSequence atomize() throws XPathException {
    switch (getNodeKind()) {
      case Type.COMMENT:
      case Type.PROCESSING_INSTRUCTION:
        return new StringValue(getStringValueCS());
      default:
        return new UntypedAtomicValue(getStringValueCS());
    }
  }

  @Override
  public int compareOrder(NodeInfo other) {
    Node otherNode = (Node) ((XMLIndexNodeInfo) other).getRealNode();
    return Long.compare(((HierarchyNode) node).left, ((HierarchyNode) otherNode).left);
  }

  @Override
  public int comparePosition(NodeInfo other) {
    Node otherNode = ((Node) ((XMLIndexNodeInfo) other).getRealNode());
    
    if (node.type == Type.ATTRIBUTE || node.type == Type.NAMESPACE ||
        otherNode.type == Type.ATTRIBUTE || otherNode.type == Type.NAMESPACE) {
      throw new UnsupportedOperationException();
    }
    
    HierarchyNode hNode = (HierarchyNode) node;
    HierarchyNode hOtherNode = (HierarchyNode) otherNode;
    
    // are they the same node?
    if (hNode.left == hOtherNode.left)
      return AxisInfo.SELF;
    
    if (hNode.left == hOtherNode.parent)
      return AxisInfo.ANCESTOR;
    
    if (hNode.parent == hOtherNode.left)
      return AxisInfo.DESCENDANT;
   
    // do they have the same parent (common case)?
    if (hNode.parent == hOtherNode.parent) {
      if (compareOrder(other) < 0)
        return AxisInfo.PRECEDING;
      else
        return AxisInfo.FOLLOWING;
    }
    
    if (hNode.left < hOtherNode.left && hNode.right > hOtherNode.right)
      return AxisInfo.ANCESTOR;
    
    if (hNode.left > hOtherNode.left && hNode.right < hOtherNode.right)
      return AxisInfo.DESCENDANT;
    
    if (this.compareOrder(other) < 0)
      return AxisInfo.PRECEDING;
    
    return AxisInfo.FOLLOWING;
  }
     
  private void copyElementToReceiver(Receiver out, int copyOptions, Element elem) throws XPathException {
    StructuredQName name = elem.name;
    NodeName nodeName = new FingerprintedQName(name.getPrefix(), name.getURI(), name.getLocalPart());
    out.startElement(nodeName, Untyped.getInstance(), ExplicitLocation.UNKNOWN_LOCATION, copyOptions);

    if (elem.namespaces != null)
      for (Namespace ns : elem.namespaces)
        out.namespace(ns.binding, 0);
    
    if (elem.attributes != null)
      for (Attribute attr : elem.attributes) {
        StructuredQName attrName = attr.name;
        /* Do not copy/serialize virtual attributes: */
        if (attr.name.getURI().equals(Definitions.NAMESPACE_VIRTUALATTR))
          continue;
        NodeName attrNodeName = new FingerprintedQName(attrName.getPrefix(), attrName.getURI(), attrName.getLocalPart());
        out.attribute(attrNodeName, AnySimpleType.getInstance(), attr.value, ExplicitLocation.UNKNOWN_LOCATION, 0);
      }
    if (elem.left == elem.right-1)
      out.endElement();
    else
      out.startContent();  
  }
  
  private void copyTextToReceiver(Receiver out, ValueNode valueNode) throws XPathException {
    out.characters(valueNode.getValue(), ExplicitLocation.UNKNOWN_LOCATION, 0);
  }
  
  private void copyCommentToReceiver(Receiver out, ValueNode valueNode) throws XPathException {
    out.comment(valueNode.getValue(), ExplicitLocation.UNKNOWN_LOCATION, 0);
  }
  
  private void copyProcessingInstructionToReceiver(Receiver out, ProcessingInstruction piNode) throws XPathException {
    out.processingInstruction(((NamedNode) piNode).getName().getLocalPart(), piNode.value, ExplicitLocation.UNKNOWN_LOCATION, 0);
  }
  
  @Override
  public void copy(Receiver out, int copyOptions, Location locationId) throws XPathException {
    if (node.type == Type.ATTRIBUTE && ((Attribute) node).name.getURI().equals(Definitions.NAMESPACE_VIRTUALATTR))
      /* Do not copy/serialize virtual attributes: */
      return;
    
    if (node.type != Type.ELEMENT && node.type != Type.DOCUMENT) {
      /* Use Navigator to copy attributes, comments, processinginstructions and namespace nodes: */
      Navigator.copy(this, out, copyOptions, locationId);
      return;
    }
    
    long left = ((HierarchyNode) node).left;
    long right = ((HierarchyNode) node).right;
    
    try {
      if (node.type == Type.ELEMENT) {
        Element elem = (Element) node;
        if (elem.left == elem.right-1) {
          /* Special case: empty element: */
          copyElementToReceiver(out, copyOptions, elem);
          return;
        } else if (StringUtils.isNotEmpty(elem.value)) {
          /* Special case: element with single text child: */
          copyElementToReceiver(out, copyOptions, elem);
          out.characters(elem.value, ExplicitLocation.UNKNOWN_LOCATION, 0);
          out.endElement();
          return;
        }
      }
      
      if (this.node.type == Type.DOCUMENT)
        out.startDocument(CopyOptions.getStartDocumentProperties(copyOptions));
      
      IndexSearcher searcher = session.getIndexSearcher();
      
      /* Query all descendant nodes in document order: */
      Query query = LongPoint.newRangeQuery(Definitions.FIELDNAME_LEFT, left, right);
      HitQuery nodeQuery = new HitQuery(searcher, query);
      HitIterator iter = nodeQuery.execute();
      if (!iter.hasNext())
        return;
      
      HierarchyNode node = null;
      byte currentDepth = -1;
      while (iter.hasNext()) {
        Document doc = searcher.doc(iter.next().docId);
        node = (HierarchyNode) Node.createNode(doc, session);
        
        if (node.depth < currentDepth) {
          int diff = currentDepth - node.depth;
          for (int i=0; i<diff; i++)
            out.endElement();
        }
        currentDepth = node.depth;
        
        switch (node.type) {
          case Type.ELEMENT:
            copyElementToReceiver(out, copyOptions, ((Element) node));
            break;
          case Type.TEXT:
            copyTextToReceiver(out, (ValueNode) node);
            break;
          case Type.COMMENT:
            copyCommentToReceiver(out, (ValueNode) node);
            break;
          case Type.PROCESSING_INSTRUCTION:
            copyProcessingInstructionToReceiver(out, (ProcessingInstruction) node);
            break;
        }  
      }
      
      while (((HierarchyNode) this.node).depth < currentDepth--)
        if (currentDepth > 0)
          out.endElement();
      
      if (this.node.type == Type.DOCUMENT)
        out.endDocument();
      
      /*
      if (node.type == Type.ELEMENT) {
        for (int i=0; i<(currentDepth - node.depth); i++)
          out.endElement();
      } else if (node.type == Type.DOCUMENT)
        out.endDocument();
      */
      
    } catch (IOException ioe) {
      throw new XPathException("Error copying node to receiver", ioe);
    }
    
  }

  @Override
  public void generateId(FastStringBuffer buffer) {
    buffer.append(Long.toString(((HierarchyNode) node).left));
  }

  @Override
  public String getAttributeValue(String uri, String local) {
    if (node.type != Type.ELEMENT)
      return null;
    loadAttributes();
    for (XMLIndexNodeInfo attr : attributes)
      if (attr.getLocalPart().equals(local) && attr.getURI().equals(uri))
        return attr.getStringValue();
    return null;
  }

  @Override
  public String getBaseURI() { 
    try {
      return getDocumentURI();
    } catch (IOException ioe) {
      throw new XMLIndexException("Error getting base uri of node", ioe);
    }
  }

  @Override
  public int getColumnNumber() {    
    return -1;
  }

  @Override
  public Configuration getConfiguration() {
    return config;
  }

  @Override
  public NamespaceBinding[] getDeclaredNamespaces(NamespaceBinding[] buffer) {
    if (node.type != Type.ELEMENT)
      return null;
    if (((Element) node).namespaces == null)
      return NamespaceBinding.EMPTY_ARRAY;
    List<Namespace> namespaces = ((Element) node).namespaces;
    ArrayList<NamespaceBinding> bindings = new ArrayList<NamespaceBinding>();      
    for (Namespace namespace : namespaces)
      bindings.add(namespace.binding);
    if (bindings.size() == 0)
      return NamespaceBinding.EMPTY_ARRAY;
    return bindings.toArray(new NamespaceBinding[bindings.size()]); // TODO: optimize by using buffer
  }

  @Override
  public String getDisplayName() {
    String prefix = getPrefix();
    String local = getLocalPart();
    if (prefix.length() == 0) {
      return local;
    } else {
      return prefix + ":" + local;
    }
  }

  @Override
  public int getLineNumber() {    
    return -1;
  }

  @Override
  public String getLocalPart() {
    if (node instanceof NamedNode) 
      return ((NamedNode) node).getName().getLocalPart();
    return "";          
  }

  @Override
  public int getNodeKind() {    
    return node.type;
  }
  
  @Override
  public int getFingerprint() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasFingerprint() {
    return false;
  }

  @Override
  public boolean isStreamed() {
    return false;
  }

  @Override
  public XMLIndexNodeInfo getParent() {
    if (parentNode != null)
      return parentNode;
    if (node.type == Type.DOCUMENT)
      return null;
    if (node.parent == IndexRootElement.LEFT)
      parentNode = new XMLIndexNodeInfo(session, Node.DOCUMENT_NODE);
    else if (node.parent == IndexRootElement.LEFT)
      parentNode = new XMLIndexNodeInfo(session, Node.ROOT_ELEMENT);
    else 
      parentNode = new ParentAxisIterator(session, this, null, null).next();
    return parentNode;
  }
  
  public XMLIndexNodeInfo getCachedParent() {
    return parentNode;
  }
  
  @Override
  public String getPublicId() {
    return null;
  }

  @Override
  public String getPrefix() {
    if (node instanceof NamedNode) 
      return ((NamedNode) node).getName().getPrefix();
    return "";  
  }

  @Override
  public NodeInfo getRoot() {
    return tree.getRootNode();
  }

  @Override
  public SchemaType getSchemaType() {     
    if (node.type == Type.ATTRIBUTE) {
      return BuiltInAtomicType.UNTYPED_ATOMIC;
    }
    return Untyped.getInstance();
  }

  @Override
  public String getStringValue() {
    return getStringValueCS().toString();
  }

  @Override
  public String getSystemId() {    
    return this.systemId;
  }
  
  @Override
  public XMLIndexTreeInfo getTreeInfo() {
    return tree;
  }

  @Override
  public String getURI() {
    if (node instanceof NamedNode) 
      return ((NamedNode) node).getName().getURI();
    return "";
  }

  @Override
  public boolean hasChildNodes() {
    if (node.type == Type.DOCUMENT)
      return true;
    if (node.type != Type.ELEMENT)
      return false;
    if (((Element) node).right == ((Element) node).left + 1)
      return false;
    return true;
  }

  @Override
  public boolean isId() {    
    return false;
  }

  @Override
  public boolean isIdref() {    
    return false;
  }

  @Override
  public boolean isNilled() {    
    return false;
  }

  @Override
  public boolean isSameNodeInfo(NodeInfo other) {
    Node otherNode = (Node) ((XMLIndexNodeInfo) other).getRealNode();
    return node.compareTo(otherNode) == 0;    
  }
  
  public AxisIterator iterateAxis(byte axisNumber, XPathContext xpathContext) {
    return iterateAxis(axisNumber, AnyNodeTest.getInstance(), xpathContext);
  }
  
  public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest, XPathContext xpathContext) {
    int nodeKind = getNodeKind();
    switch (axisNumber) {
    case AxisInfo.ANCESTOR:
      if (nodeKind == Type.DOCUMENT)
        return EmptyIterator.OfNodes.THE_INSTANCE;
      return new AncestorAxisIterator(session, this, nodeTest, false, xpathContext);
      // return new Navigator.AncestorEnumeration(this, false);
    case AxisInfo.ANCESTOR_OR_SELF:
      if (nodeKind == Type.DOCUMENT)
        return Navigator.filteredSingleton(this, nodeTest);
      return new AncestorAxisIterator(session, this, nodeTest, true, xpathContext);
      // return new Navigator.AncestorEnumeration(this, true);
    case AxisInfo.ATTRIBUTE:      
      if (nodeKind != Type.ELEMENT)
        return EmptyIterator.OfNodes.THE_INSTANCE;
      return new AttributeAxisIterator(session, this, nodeTest);
    case AxisInfo.CHILD:
      if (nodeKind == Type.ELEMENT && StringUtils.isNotEmpty(((Element) node).value)) {
        return Navigator.filteredSingleton(new XMLIndexNodeInfo(session, new Text((Element) node, ((Element) node).value)), nodeTest);
      } else if (nodeKind == Type.ELEMENT || nodeKind == Type.DOCUMENT)
        return new ChildAxisIterator(session, this, nodeTest, xpathContext);
      return EmptyIterator.OfNodes.THE_INSTANCE;
    case AxisInfo.DESCENDANT:
      if (nodeKind == Type.ELEMENT || nodeKind == Type.DOCUMENT)
        return new DescendantAxisIterator(session, this, nodeTest, false, xpathContext);
      return EmptyIterator.OfNodes.THE_INSTANCE;
    case AxisInfo.DESCENDANT_OR_SELF:
      if (nodeKind == Type.ELEMENT || nodeKind == Type.DOCUMENT)
        return new DescendantAxisIterator(session, this, nodeTest, true, xpathContext);
      return Navigator.filteredSingleton(this, nodeTest);
    case AxisInfo.FOLLOWING:
      return new FollowingAxisIterator(session, this, nodeTest, xpathContext);
    case AxisInfo.FOLLOWING_SIBLING:
      switch (nodeKind) {
        case Type.DOCUMENT:
        case Type.ATTRIBUTE:
        case Type.NAMESPACE:
          return EmptyIterator.OfNodes.THE_INSTANCE;
        default:
          return new FollowingSiblingAxisIterator(session, this, nodeTest, xpathContext);
      }
    case AxisInfo.NAMESPACE:
      if (nodeKind != Type.ELEMENT)
        return EmptyIterator.OfNodes.THE_INSTANCE;
      return NamespaceNode.makeIterator(this, nodeTest); // TODO: optimize
    case AxisInfo.PARENT:
      return Navigator.filteredSingleton(getParent(), nodeTest);
    case AxisInfo.PRECEDING:
      return new PrecedingAxisIterator(session, this, nodeTest, xpathContext);
    case AxisInfo.PRECEDING_SIBLING:
      switch (nodeKind) {
      case Type.DOCUMENT:
      case Type.ATTRIBUTE:
      case Type.NAMESPACE:
        return EmptyIterator.OfNodes.THE_INSTANCE;
      default:
        return new PrecedingSiblingAxisIterator(session, this, nodeTest, xpathContext);
      }
    case AxisInfo.SELF:
      return Navigator.filteredSingleton(this, nodeTest);
    case AxisInfo.PRECEDING_OR_ANCESTOR:
      return new Navigator.AxisFilter(new Navigator.PrecedingEnumeration(this, true), nodeTest);
    default:
      throw new IllegalArgumentException("Unknown axis number " + axisNumber);
    }
  }

  @Override
  public AxisIterator iterateAxis(byte axisNumber) {
    return iterateAxis(axisNumber, AnyNodeTest.getInstance(), null);
  }
  
  @Override
  public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {    
    return iterateAxis(axisNumber, nodeTest, null);
  }
    
  @Override
  public Location saveLocation() {
    return this;
  }
  
  @Override
  public void setSystemId(String systemId) {
    this.systemId = systemId;
  }

  @Override
  public CharSequence getStringValueCS() {
    switch (node.type) {
      case Type.DOCUMENT:
      case Type.ELEMENT:
        if (node.type == Type.ELEMENT && StringUtils.isNotEmpty(((Element) node).value))
          return ((Element) node).value;
        AxisIterator children = iterateAxis(AxisInfo.CHILD, AnyChildNodeTest.getInstance());        
        FastStringBuffer fsb = new FastStringBuffer(16);
        XMLIndexNodeInfo child;
        while ((child = (XMLIndexNodeInfo) children.next()) != null) {          
          fsb.append(child.getStringValueCS());
        }
        return fsb;
      case Type.ATTRIBUTE:        
      case Type.TEXT:                       
      case Type.COMMENT:
      case Type.PROCESSING_INSTRUCTION:
        return StringUtils.defaultString(((ValueNode) node).getValue());
      default:
        return "";
    }
  }

  @Override
  public Item head() {    
    return this;
  }

  @Override
  public SequenceIterator iterate() throws XPathException {
    return SingletonIterator.makeIterator(this);
  }

  @Override
  public Object getRealNode() {
    return node;
  }

  @Override
  public Object getUnderlyingNode() {
    return node;
  }
  
  public String nodeTypeToString(int type) {
    switch(type) {
    case Type.ELEMENT:
      return "Element";      
    case Type.ATTRIBUTE:
      return "Attribute";      
    case Type.TEXT:
      return "Text";         
    case Type.PROCESSING_INSTRUCTION:
      return "Processing Instruction";      
    case Type.COMMENT:
      return "Comment";      
    case Type.DOCUMENT:
      return "Document";
    default:
      return "Unknown type";
    }
  }
  
  public String axisToString(int axis) {
    switch(axis) {
    case AxisInfo.ANCESTOR:
      return "Ancestor";     
    case AxisInfo.ANCESTOR_OR_SELF:
      return "Ancestor or self";      
    case AxisInfo.ATTRIBUTE:
      return "Attribute";         
    case AxisInfo.CHILD:
      return "Child";      
    case AxisInfo.DESCENDANT:
      return "Descendant";      
    case AxisInfo.DESCENDANT_OR_SELF:
      return "Descendant or self";
    case AxisInfo.FOLLOWING:
      return "Following";
    case AxisInfo.FOLLOWING_SIBLING:
      return "Following sibling";
    case AxisInfo.NAMESPACE:
      return "Namespace";
    case AxisInfo.PARENT:
      return "Parent";
    case AxisInfo.PRECEDING:
      return "Preceding";
    case AxisInfo.PRECEDING_OR_ANCESTOR:
      return "Preceding or ancestor";
    case AxisInfo.PRECEDING_SIBLING:
      return "Preceding sibling";    
    default:
      return "Unknown axis";
    }
  }
  
  private void loadAttributes() {
    if (attributes != null)
      return;
    attributes = new ArrayList<XMLIndexNodeInfo>();
    AttributeAxisIterator attrs = new AttributeAxisIterator(session, this, AnyNodeTest.getInstance());
    XMLIndexNodeInfo nodeInfo;
    while ((nodeInfo = attrs.next()) != null)
      attributes.add(nodeInfo);
  }
  
  public XMLIndexNodeInfo getOwnerDocument() throws IOException {
    if (node instanceof DocumentElement)
      return this;
    if (ownerDocument != null)
      return ownerDocument;
    Node contextNode;
    if (getNodeKind() == Type.ATTRIBUTE)
      contextNode = (Node) getParent().getRealNode();
    else
      contextNode = node;
    IndexSearcher searcher = session.getIndexSearcher();
    Query query = LongPoint.newExactQuery(Definitions.FIELDNAME_LEFT, ((HierarchyNode) contextNode).docLeft);
    TopDocs topDocs = searcher.search(query, 1);
    if (topDocs.totalHits == 0)
      return null;
    Node ownerNode = Node.createNode(searcher.doc(topDocs.scoreDocs[0].doc), session);
    ownerDocument = new XMLIndexNodeInfo(session, ownerNode);
    return ownerDocument;
  }
  
  public String getDocumentURI() throws IOException {
    if (documentURI != null)
      return documentURI;
    Node contextNode;
    if (getNodeKind() == Type.ATTRIBUTE)
      contextNode = (Node) getParent().getRealNode();
    else
      contextNode = node;
    IndexSearcher searcher = session.getIndexSearcher();
    Query query = LongPoint.newExactQuery(Definitions.FIELDNAME_LEFT, ((HierarchyNode) contextNode).docLeft);
    TopDocs topDocs = searcher.search(query, 1);
    if (topDocs.totalHits == 0)
      return null;
    Document doc = searcher.doc(topDocs.scoreDocs[0].doc, documentURIFieldsToLoad);
    documentURI = doc.get(Definitions.FIELDNAME_URI);
    return documentURI;
  }

}