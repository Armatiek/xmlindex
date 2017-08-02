package nl.armatiek.xmlindex.restxq.adapter;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;

import net.sf.saxon.s9api.XdmExternalObject;

public class XdmDOMElementAdapter extends XdmExternalObject implements Element {

  private Element element;
  
  public XdmDOMElementAdapter(Element element) {
    super(element);
    this.element = element;
  }

  public String getTagName() {
    return element.getTagName();
  }

  public String getAttribute(String name) {
    return element.getAttribute(name);
  }

  public void setAttribute(String name, String value) throws DOMException {
    element.setAttribute(name, value);
  }

  public void removeAttribute(String name) throws DOMException {
    element.removeAttribute(name);
  }

  public Attr getAttributeNode(String name) {
    return element.getAttributeNode(name);
  }

  public Attr setAttributeNode(Attr newAttr) throws DOMException {
    return element.setAttributeNode(newAttr);
  }

  public Attr removeAttributeNode(Attr oldAttr) throws DOMException {
    return element.removeAttributeNode(oldAttr);
  }

  public String getNodeName() {
    return element.getNodeName();
  }

  public String getNodeValue() throws DOMException {
    return element.getNodeValue();
  }

  public NodeList getElementsByTagName(String name) {
    return element.getElementsByTagName(name);
  }

  public void setNodeValue(String nodeValue) throws DOMException {
    element.setNodeValue(nodeValue);
  }

  public String getAttributeNS(String namespaceURI, String localName) throws DOMException {
    return element.getAttributeNS(namespaceURI, localName);
  }

  public short getNodeType() {
    return element.getNodeType();
  }

  public Node getParentNode() {
    return element.getParentNode();
  }

  public NodeList getChildNodes() {
    return element.getChildNodes();
  }

  public void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException {
    element.setAttributeNS(namespaceURI, qualifiedName, value);
  }

  public Node getFirstChild() {
    return element.getFirstChild();
  }

  public Node getLastChild() {
    return element.getLastChild();
  }

  public Node getPreviousSibling() {
    return element.getPreviousSibling();
  }

  public Node getNextSibling() {
    return element.getNextSibling();
  }

  public NamedNodeMap getAttributes() {
    return element.getAttributes();
  }

  public Document getOwnerDocument() {
    return element.getOwnerDocument();
  }

  public Node insertBefore(Node newChild, Node refChild) throws DOMException {
    return element.insertBefore(newChild, refChild);
  }

  public void removeAttributeNS(String namespaceURI, String localName) throws DOMException {
    element.removeAttributeNS(namespaceURI, localName);
  }

  public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
    return element.replaceChild(newChild, oldChild);
  }

  public Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException {
    return element.getAttributeNodeNS(namespaceURI, localName);
  }

  public Node removeChild(Node oldChild) throws DOMException {
    return element.removeChild(oldChild);
  }

  public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
    return element.setAttributeNodeNS(newAttr);
  }

  public Node appendChild(Node newChild) throws DOMException {
    return element.appendChild(newChild);
  }

  public NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException {
    return element.getElementsByTagNameNS(namespaceURI, localName);
  }

  public boolean hasChildNodes() {
    return element.hasChildNodes();
  }

  public Node cloneNode(boolean deep) {
    return element.cloneNode(deep);
  }

  public boolean hasAttribute(String name) {
    return element.hasAttribute(name);
  }

  public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException {
    return element.hasAttributeNS(namespaceURI, localName);
  }

  public void normalize() {
    element.normalize();
  }

  public TypeInfo getSchemaTypeInfo() {
    return element.getSchemaTypeInfo();
  }

  public void setIdAttribute(String name, boolean isId) throws DOMException {
    element.setIdAttribute(name, isId);
  }

  public boolean isSupported(String feature, String version) {
    return element.isSupported(feature, version);
  }

  public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException {
    element.setIdAttributeNS(namespaceURI, localName, isId);
  }

  public String getNamespaceURI() {
    return element.getNamespaceURI();
  }

  public String getPrefix() {
    return element.getPrefix();
  }

  public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException {
    element.setIdAttributeNode(idAttr, isId);
  }

  public void setPrefix(String prefix) throws DOMException {
    element.setPrefix(prefix);
  }

  public String getLocalName() {
    return element.getLocalName();
  }

  public boolean hasAttributes() {
    return element.hasAttributes();
  }

  public String getBaseURI() {
    return element.getBaseURI();
  }

  public short compareDocumentPosition(Node other) throws DOMException {
    return element.compareDocumentPosition(other);
  }

  public String getTextContent() throws DOMException {
    return element.getTextContent();
  }

  public void setTextContent(String textContent) throws DOMException {
    element.setTextContent(textContent);
  }

  public boolean isSameNode(Node other) {
    return element.isSameNode(other);
  }

  public String lookupPrefix(String namespaceURI) {
    return element.lookupPrefix(namespaceURI);
  }

  public boolean isDefaultNamespace(String namespaceURI) {
    return element.isDefaultNamespace(namespaceURI);
  }

  public String lookupNamespaceURI(String prefix) {
    return element.lookupNamespaceURI(prefix);
  }

  public boolean isEqualNode(Node arg) {
    return element.isEqualNode(arg);
  }

  public Object getFeature(String feature, String version) {
    return element.getFeature(feature, version);
  }

  public Object setUserData(String key, Object data, UserDataHandler handler) {
    return element.setUserData(key, data, handler);
  }

  public Object getUserData(String key) {
    return element.getUserData(key);
  }

}
