package nl.armatiek.xmlindex.saxon.axis;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.TinyTree;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaComponentVisitor;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.type.UType;
import net.sf.saxon.z.IntSet;
import nl.armatiek.xmlindex.query.QueryDef;

public class FilterNodeTest extends NodeTest {
  
  private NodeTest nodeTest;
  private QueryDef filterQueryDef;
  
  public FilterNodeTest(NodeTest nodeTest, QueryDef filterQueryDef) {
    this.nodeTest = nodeTest;
    this.filterQueryDef = filterQueryDef;
  }
  
  public NodeTest getNodeTest() {
    return nodeTest;
  }
  
  public QueryDef getFilterQueryDef() {
    return filterQueryDef;
  }

  @Override
  public String generateJavaScriptItemTypeTest(ItemType errorCode) throws XPathException {
    return nodeTest.generateJavaScriptItemTypeTest(errorCode);
  }
  
  @Override
  public UType getUType() {
    return nodeTest.getUType();
  }

  @Override
  public NodeTest copy() {
    return nodeTest.copy();
  }

  @Override
  public String generateJavaScriptItemTypeAcceptor(String errorCode) throws XPathException {
    return nodeTest.generateJavaScriptItemTypeAcceptor(errorCode);
  }

  @Override
  public AtomicType getAtomizedItemType() {
    return nodeTest.getAtomizedItemType();
  }

  @Override
  public SchemaType getContentType() {
    return nodeTest.getContentType();
  }

  @Override
  public double getDefaultPriority() {
    return nodeTest.getDefaultPriority();
  }

  @Override
  public int getFingerprint() {
    return nodeTest.getFingerprint();
  }

  @Override
  public StructuredQName getMatchingNodeName() {
    return nodeTest.getMatchingNodeName();
  }

  @Override
  public int getNodeKindMask() {
    return nodeTest.getNodeKindMask();
  }

  @Override
  public ItemType getPrimitiveItemType() {
    return nodeTest.getPrimitiveItemType();
  }

  @Override
  public int getPrimitiveType() {
    return nodeTest.getPrimitiveType();
  }

  @Override
  public IntSet getRequiredNodeNames() {
    return nodeTest.getRequiredNodeNames();
  }

  @Override
  public boolean isAtomicType() {
    return nodeTest.isAtomicType();
  }
  
  @Override
  public boolean isAtomizable() {
    return nodeTest.isAtomizable();
  }

  @Override
  public boolean isNillable() {
    return nodeTest.isNillable();
  }

  @Override
  public boolean isPlainType() {
    return nodeTest.isPlainType();
  }

  @Override
  public boolean matches(int nodeKind, NodeName name, SchemaType annotation) {
    return nodeTest.matches(nodeKind, name, annotation);
  }

  @Override
  public boolean matches(Item item, TypeHierarchy th) {
    return nodeTest.matches(item, th);
  }

  @Override
  public boolean matches(TinyTree tree, int nodeNr) {
    return nodeTest.matches(tree, nodeNr);
  }

  @Override
  public boolean matchesNode(NodeInfo node) {
    return nodeTest.matchesNode(node);
  }

  @Override
  public void visitNamedSchemaComponents(SchemaComponentVisitor visitor) throws XPathException {
    nodeTest.visitNamedSchemaComponents(visitor);
  }
  
}