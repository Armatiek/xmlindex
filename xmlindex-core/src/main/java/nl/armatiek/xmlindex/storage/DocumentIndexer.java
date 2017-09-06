package nl.armatiek.xmlindex.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import net.sf.saxon.dom.DocumentWrapper;
import net.sf.saxon.dom.NodeOverNodeInfo;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmEmptySequence;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmMap;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.DateTimeValue;
import net.sf.saxon.value.DateValue;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.value.FloatValue;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.TimeValue;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.conf.TypedValueDef;
import nl.armatiek.xmlindex.conf.VirtualAttributeDef;
import nl.armatiek.xmlindex.conf.VirtualAttributeDefConfig;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.extensions.PluggableIndex;
import nl.armatiek.xmlindex.node.HierarchyNode;
import nl.armatiek.xmlindex.node.IndexRootElement;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexNodeInfo;
import nl.armatiek.xmlindex.saxon.util.SaxonUtils;

public class DocumentIndexer {
  
  private static final Logger logger = LoggerFactory.getLogger(DocumentIndexer.class);
  
  private static final String PROPNAME_BUFFER_SIZE = "http://apache.org/xml/properties/input-buffer-size";
  private static final Integer BUFFER_SIZE = new Integer(8 * 1024);
  
  private final StringField typeField = new StringField(Definitions.FIELDNAME_TYPE, "", Field.Store.YES);
  private final StoredField depthField = new StoredField(Definitions.FIELDNAME_DEPTH, "");
  
  private final LongPoint leftField_lp = new LongPoint(Definitions.FIELDNAME_LEFT, (long) 0);
  private final NumericDocValuesField leftField_dv = new NumericDocValuesField(Definitions.FIELDNAME_LEFT, (long) 0);
  private final StoredField leftField_sf = new StoredField(Definitions.FIELDNAME_LEFT, (long) 0);
  
  private final LongPoint rightField_lp = new LongPoint(Definitions.FIELDNAME_RIGHT, (long) 0);
  private final NumericDocValuesField rightField_dv = new NumericDocValuesField(Definitions.FIELDNAME_RIGHT, (long) 0);
  private final StoredField rightField_sf = new StoredField(Definitions.FIELDNAME_RIGHT, (long) 0);
  
  private final LongPoint parentField_lp = new LongPoint(Definitions.FIELDNAME_PARENT, (long) 0);
  private final StoredField parentField_sf = new StoredField(Definitions.FIELDNAME_PARENT, (long) 0);
  private final NumericDocValuesField parentField_dv = new NumericDocValuesField(Definitions.FIELDNAME_PARENT, (long) 0);
  
  private final Stack<StringField> availableFieldNameFields = new Stack<StringField>();
  private final Stack<StringField> usedFieldNameFields = new Stack<StringField>();
  private final StringField valueField = new StringField(Definitions.FIELDNAME_VALUE, "", Field.Store.NO);
  private final StoredField valueField_sf = new StoredField(Definitions.FIELDNAME_VALUE, "");
  private final StringField uriField = new StringField(Definitions.FIELDNAME_URI, "", Field.Store.YES);
  private final StoredField docLeftField_sf = new StoredField(Definitions.FIELDNAME_DOCLEFT, (long) 0);
  private final StoredField docRightField_sf = new StoredField(Definitions.FIELDNAME_DOCRIGHT, (long) 0);
  private final StringField baseUriField = new StringField(Definitions.FIELDNAME_BASEURI, "", Field.Store.NO);
  private final List<VirtualAttributeDef> vads = new ArrayList<VirtualAttributeDef>();
  
  private final String USERDATA_KEY = "u";
  private final XMLIndex index;
  private final int maxTermLength;
  private DocumentWrapper docWrapper;
  private long nodeCounter;
  
  public DocumentIndexer(XMLIndex index, long nodeCounter) {
    this.index = index;
    this.nodeCounter = nodeCounter;
    this.maxTermLength = index.getMaxTermLength();
  }
  
  private void numberNode(Node node, byte depth) {
    if (isWhitespaceTextNode(node))
      return;
    UserData userData = new UserData(depth, nodeCounter++);
    node.setUserData(USERDATA_KEY, userData, null);
    depth++;
    for (Node childNode=node.getFirstChild(); childNode!=null; childNode=childNode.getNextSibling())
      numberNode(childNode, depth);
    userData.right = nodeCounter++;    
  }
    
  private boolean isTextOnlyElem(Node node) {
    if (node.getNodeType() != Node.ELEMENT_NODE)
      return false;
    NodeList childNodes = node.getChildNodes();
    return childNodes.getLength() == 1 && childNodes.item(0).getNodeType() == Node.TEXT_NODE;
  }
  
  private String getLocalPart(Node node) {
    return node.getNamespaceURI() == null ? node.getNodeName() : node.getLocalName();
  }
  
  private boolean isWhitespaceTextNode(Node node) {
    return node.getNodeType() == Node.TEXT_NODE && StringUtils.isWhitespace(node.getTextContent());
  }
  
  private String getFieldName(int nodeType, Node node) throws IOException {
    String uri = node.getNamespaceURI();
    int nameCode = index.getNameStore().putName(StringUtils.defaultString(uri), getLocalPart(node));
    return Integer.toString(nodeType) + "_" + Integer.toString(nameCode);    
  }
  
  private void addTypedValueField(org.apache.lucene.document.Document doc, Node node) throws IOException {
    IndexableField field = null;
    int nodeType = node.getNodeType();
    TypedValueDef tvd = index.getConfiguration().getTypedValueConfig().get(nodeType, 
        StringUtils.defaultString(node.getNamespaceURI()), getLocalPart(node));
    if (tvd == null)
      return;
    ItemType itemType = tvd.getItemType();
    String typeName = ((BuiltInAtomicType) itemType).getName();
    String fieldName = getFieldName(nodeType, node) + "_" + typeName;
    String value = node.getTextContent(); 
    try {
      if (itemType.equals(BuiltInAtomicType.BOOLEAN)) {
        BooleanValue bv = (BooleanValue) BooleanValue.fromString(value).asAtomic();
        String fieldValue = bv.getPrimitiveStringValue();
        field = new StringField(fieldName, fieldValue, Field.Store.NO);
      } else if (itemType.equals(BuiltInAtomicType.INT) || itemType.equals(BuiltInAtomicType.SHORT) || itemType.equals(BuiltInAtomicType.BYTE)) {
        IntegerValue iv = (IntegerValue) IntegerValue.stringToInteger(value).asAtomic();
        field = new IntPoint(fieldName, iv.asBigInteger().intValue());
      } else if (itemType.equals(BuiltInAtomicType.LONG)) {
        IntegerValue iv = (IntegerValue) IntegerValue.stringToInteger(value).asAtomic();
        field = new LongPoint(fieldName, iv.asBigInteger().longValue());
      } else if (itemType.equals(BuiltInAtomicType.DOUBLE)) {
        double d = DoubleValue.parseNumber(value).getDoubleValue();
        field = new DoublePoint(fieldName, d);
      } else if (itemType.equals(BuiltInAtomicType.FLOAT)) {
        float f = FloatValue.parseNumber(value).getFloatValue();
        field = new FloatPoint(fieldName, f);
      } else if (itemType.equals(BuiltInAtomicType.DATE_TIME)) {
        DateTimeValue dtv = (DateTimeValue) DateTimeValue.makeDateTimeValue(value, ConversionRules.DEFAULT);
        field = new LongPoint(fieldName, dtv.getCalendar().getTimeInMillis());
      } else if (itemType.equals(BuiltInAtomicType.DATE)) {
        DateValue dv = (DateValue) DateValue.makeDateValue(value, ConversionRules.DEFAULT);
        field = new LongPoint(fieldName, dv.getCalendar().getTimeInMillis());
      } else if (itemType.equals(BuiltInAtomicType.TIME)) {
        TimeValue tv = (TimeValue) TimeValue.makeTimeValue(value);
        field = new LongPoint(fieldName, tv.getCalendar().getTimeInMillis());
      }
    } catch (XPathException xe) {
      String location = new StructuredQName("", StringUtils.defaultString(node.getNamespaceURI()), getLocalPart(node)).getClarkName(); 
      logger.warn("Skipping indexing typed value of \"" + location + "\". Value \"" + value + "\" could not be converted to type xs:\"" + typeName + ".", xe);
    }
    
    if (field != null)
      doc.add(field);
  }
  
  private void addVirtualAttributeFields(org.apache.lucene.document.Document doc, Element elem, XdmMap params) throws IOException, SaxonApiException {
    VirtualAttributeDefConfig vad = index.getConfiguration().getVirtualAttributeConfig();
    vads.clear();
    Map<String, VirtualAttributeDef> attrDefs = vad.getForElement(new QName(StringUtils.defaultString(elem.getNamespaceURI()), getLocalPart(elem)));  
    if (attrDefs != null)
      vads.addAll(attrDefs.values());
    if (elem.getParentNode().getNodeType() == Node.DOCUMENT_NODE) {
      attrDefs = vad.getForElement(Definitions.QNAME_VA_BINDING_DOCUMENT_ELEMENT);  
      if (attrDefs != null)
        vads.addAll(attrDefs.values());
    }
      
    if (vads.isEmpty())
      return;
    
    if (this.docWrapper == null)
      this.docWrapper = new DocumentWrapper(elem.getOwnerDocument(), elem.getOwnerDocument().getBaseURI(), index.getSaxonConfiguration());
    NodeInfo nodeInfo = docWrapper.wrap(elem);
    
    // XQueryEvaluator evaluator = index.getConfiguration().getVirtualAttributeConfig().getVirtualAttrsEvaluator();
    for (VirtualAttributeDef attrDef : vads) {
      XdmValue values = null;
      XdmValue[] args = null;
      if (attrDef.getFunctionName().getNamespaceURI().equals(Definitions.NAMESPACE_STD_VIRTUALATTR) || 
          vad.functionExists(attrDef.getFunctionName(), 2))
        args = new XdmValue[] { new XdmNode(nodeInfo), params == null ? XdmEmptySequence.getInstance() : params };
      else if (vad.functionExists(attrDef.getFunctionName(), 1))
        args = new XdmValue[] { new XdmNode(nodeInfo) };
      else {
        logger.warn("No virtual attribute function with name \"" + attrDef.getFunctionName().getEQName() + "\" found with one or two arguments");
        continue;
      }
      
      values = attrDef.getXQueryEvaluator().callFunction(attrDef.getFunctionName(), args);
        
      if (values instanceof XdmEmptySequence) 
        continue;
      
      int nameCode = index.getNameStore().putName(Definitions.NAMESPACE_VIRTUALATTR, attrDef.getVirtualAttributeName());
      String fieldName = Integer.toString(Type.ATTRIBUTE) + "_" + Integer.toString(nameCode);
      /*
      if (attr.getPrefix() != null)
        // TODO: ontdubbelen??
        doc.add(new StoredField(Definitions.FIELDNAME_PREFIXES, attrFieldName + ";" + index.putPrefix(attr.getPrefix())));
      */
      
      IndexableField field = null;
      
      XdmSequenceIterator valueIter = values.iterator();
      
      while (valueIter.hasNext()) {
        XdmItem item = valueIter.next();
        if (!(item instanceof XdmAtomicValue))
          throw new XMLIndexException("Error creating virtual attribute \"" + attrDef.getVirtualAttributeName() + "\". The result of the virtual attribute function must be a sequence of atomic values.");
        XdmAtomicValue value = (XdmAtomicValue) item;
        AtomicValue atomicVal = (AtomicValue) value.getUnderlyingValue();
        BuiltInAtomicType itemType = (BuiltInAtomicType) atomicVal.getItemType();
        
        try {
          if (!attrDef.getItemType().matches(atomicVal, index.getSaxonConfiguration().getTypeHierarchy()))
            throw new XMLIndexException("Error creating virtual attribute \"" + attrDef.getVirtualAttributeName() + "\". The itemtype of the virtual attribute definition does not match the type of results of the actual function call.");
          
          if (attrDef.getQueryAnalyzer() != null) {
            String text = value.getStringValue();
            field = new TextField(fieldName, new StringReader(text));
          } else if (itemType.equals(BuiltInAtomicType.STRING)) {
            field = new StringField(fieldName, value.getStringValue(), Field.Store.NO);
          } else if (itemType.equals(BuiltInAtomicType.BOOLEAN)) {
            field = new StringField(fieldName, Boolean.toString(value.getBooleanValue()), Field.Store.NO);
          } else if (itemType.equals(BuiltInAtomicType.INT) || itemType.equals(BuiltInAtomicType.SHORT) || itemType.equals(BuiltInAtomicType.BYTE)) {
            IntegerValue iv = (IntegerValue) atomicVal;
            field = new IntPoint(fieldName, iv.asBigInteger().intValue());
          } else if (itemType.equals(BuiltInAtomicType.LONG)) {
            IntegerValue iv = (IntegerValue) atomicVal;
            field = new LongPoint(fieldName, iv.asBigInteger().longValue());
          } else if (itemType.equals(BuiltInAtomicType.DOUBLE)) {
            DoubleValue dv = (DoubleValue) atomicVal;
            field = new DoublePoint(fieldName, dv.getDecimalValue().doubleValue());
          } else if (itemType.equals(BuiltInAtomicType.FLOAT)) {
            FloatValue fv = (FloatValue) atomicVal;
            field = new FloatPoint(fieldName, fv.getDecimalValue().floatValue());
          } else if (itemType.equals(BuiltInAtomicType.DATE_TIME)) {
            DateTimeValue dtv = (DateTimeValue) atomicVal;
            field = new LongPoint(fieldName, dtv.getCalendar().getTimeInMillis());
          } else if (itemType.equals(BuiltInAtomicType.DATE)) {
            DateValue dv = (DateValue) atomicVal;
            field = new LongPoint(fieldName, dv.getCalendar().getTimeInMillis());
          } else if (itemType.equals(BuiltInAtomicType.TIME)) {
            TimeValue tv = (TimeValue) atomicVal;
            field = new LongPoint(fieldName, tv.getCalendar().getTimeInMillis());
          } else {
            throw new XMLIndexException("Error indexing virtual attribute \"" + attrDef.getVirtualAttributeName() + "\". Type of virtual attribute function result \"" + value.getPrimitiveTypeName().toString() + "\" is not supported.");
          }
        } catch (XPathException xe) {
          logger.warn("Skipping indexing virtual attribute \"" + attrDef.getVirtualAttributeName() + "\". Value \"" + values.toString() + "\" could not be converted to type \"" + itemType.getDisplayName() + ".", xe);
        }
        
        if (field != null) {
          doc.add(field);
          addFieldNameField(doc, fieldName);
        }
        
      }
    }
  }
  
  private StringField getFieldNameField() {
    StringField field;
    if (!availableFieldNameFields.isEmpty())
      field = availableFieldNameFields.pop();
    else
      field = new StringField(Definitions.FIELDNAME_FIELDNAMES, "", Field.Store.NO);
    usedFieldNameFields.push(field);
    return field;
  }
  
  private void pushBackUsedFieldNameFields() {
    availableFieldNameFields.addAll(usedFieldNameFields);
    usedFieldNameFields.setSize(0);
  }
  
  private void addFieldNameField(org.apache.lucene.document.Document doc, String fieldName) {
    StringField field = getFieldNameField();
    field.setStringValue(fieldName);
    doc.add(field);
  }
  
  private void indexNode(Node node, long docLeft, long docRight, String uri, XdmMap params) 
      throws SaxonApiException, IOException  { 
    if (isWhitespaceTextNode(node))
      return;
    
    UserData userData = (UserData) node.getUserData(USERDATA_KEY);
    
    short nodeType = node.getNodeType();
    
    org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
    typeField.setStringValue(Integer.toString(nodeType));
    doc.add(typeField);
    depthField.setStringValue(Integer.toString(userData.depth));
    doc.add(depthField);
    leftField_lp.setLongValue(userData.left);
    leftField_dv.setLongValue(userData.left);
    leftField_sf.setLongValue(userData.left);
    doc.add(leftField_lp);
    doc.add(leftField_dv);
    doc.add(leftField_sf);
    rightField_lp.setLongValue(userData.right);
    rightField_dv.setLongValue(userData.right);
    rightField_sf.setLongValue(userData.right);
    doc.add(rightField_lp);
    doc.add(rightField_dv);
    doc.add(rightField_sf);
    
    Node parentNode = node.getParentNode();
    long parent;
    if (parentNode.getNodeType() == Node.DOCUMENT_NODE) {
      parent = IndexRootElement.LEFT;
      docLeft = userData.left;
      docRight = userData.right;
      uriField.setStringValue(uri);
      doc.add(uriField);
    } else
      parent = ((UserData) parentNode.getUserData(USERDATA_KEY)).left;
    parentField_lp.setLongValue(parent);
    doc.add(parentField_lp);
    parentField_sf.setLongValue(parent);
    doc.add(parentField_sf);
    parentField_dv.setLongValue(parent);
    doc.add(parentField_dv);
    
    docLeftField_sf.setLongValue(docLeft);
    doc.add(docLeftField_sf);
    
    docRightField_sf.setLongValue(docRight);
    doc.add(docRightField_sf);
    
    baseUriField.setStringValue(uri);
    doc.add(baseUriField);
    
    if (nodeType == Node.ELEMENT_NODE) {
      /* Element name and value */
      boolean isTextOnly = isTextOnlyElem(node);
      String elemValue = isTextOnly ? node.getTextContent() : "";
      String fieldName = getFieldName(node.getNodeType(), node);
      if (elemValue.length() <= maxTermLength)
        doc.add(new StringField(fieldName, elemValue, Field.Store.NO));
      doc.add(new StoredField(fieldName, elemValue));
      
      if (isTextOnly)
        addTypedValueField(doc, node);
      
      /* Prefix */
      if (node.getPrefix() != null)
        doc.add(new StoredField(Definitions.FIELDNAME_PREFIXES, fieldName + ";" + index.putPrefix(node.getPrefix())));
      
      addFieldNameField(doc, fieldName);
      
      addVirtualAttributeFields(doc, (Element) node, params);
      
      for (PluggableIndex pluggableIndex : index.getConfiguration().getPluggableIndexConfig().get())
        pluggableIndex.indexNode(doc, (Element) node);
       
      /* Attributes and namespace declarations: */
      NamedNodeMap attrs = node.getAttributes();
      for (int i=0; i<attrs.getLength(); i++) {
        Attr attr = (Attr) attrs.item(i);
        int attrType;
        if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())
            && (XMLConstants.XMLNS_ATTRIBUTE.equals(attr.getName()) || attr.getName().startsWith(XMLConstants.XMLNS_ATTRIBUTE))) {
          attrType = Type.NAMESPACE;
          String nsuri = attr.getValue();
          int nameCode = index.getNameStore().putName("", StringUtils.defaultIfBlank(StringUtils.substringAfter(attr.getName(), ":"), "_"));
          fieldName = Integer.toString(attrType) + "_" + Integer.toString(nameCode); 
          doc.add(new StringField(fieldName, nsuri, Field.Store.YES));
        } else {
          attrType = Type.ATTRIBUTE;
          String attrFieldName = getFieldName(attrType, attr);
          if (attr.getPrefix() != null)
            // TODO: ontdubbelen??
            doc.add(new StoredField(Definitions.FIELDNAME_PREFIXES, attrFieldName + ";" + index.putPrefix(attr.getPrefix())));
          doc.add(new StringField(attrFieldName, attr.getValue(), Field.Store.YES));
          addTypedValueField(doc, attr);
          addFieldNameField(doc, attrFieldName);
        }
      }
    } else if (nodeType == Node.TEXT_NODE || nodeType == Node.COMMENT_NODE) {
      String value = node.getTextContent();
      if (value.length() <= maxTermLength) {
        valueField.setStringValue(value);
        doc.add(valueField);
      }
      valueField_sf.setStringValue(value);
      doc.add(valueField_sf);
    } else if (nodeType == Node.PROCESSING_INSTRUCTION_NODE) {
      String value = ((ProcessingInstruction) node).getData();
      String fieldName = getFieldName(node.getNodeType(), node);
      doc.add(new StringField(fieldName, value, Field.Store.YES));
    }
    
    index.getNodeStore().addLuceneDocument(doc);
    
    pushBackUsedFieldNameFields();
      
    if (nodeType == Node.ELEMENT_NODE) {
      /* Childs: */
      Node child = node.getFirstChild();
      while (child != null) {
        indexNode(child, docLeft, docRight, uri, params);
        child = child.getNextSibling();
      }
    }
  }
  
  public void reindexNode(Session session, org.apache.lucene.document.Document doc, Map<Long, String> baseUriMap) throws IOException, SaxonApiException {
    nl.armatiek.xmlindex.node.Node indexNode = nl.armatiek.xmlindex.node.Node.createNode(doc, session);
    if (indexNode.type != Type.ELEMENT)
      throw new XMLIndexException("Reindexing of nodes other than element nodes is not supported");
    XMLIndexNodeInfo nodeInfo = new XMLIndexNodeInfo(session, indexNode);
    Node domNode = NodeOverNodeInfo.wrap(nodeInfo);
    org.apache.lucene.document.Document newDoc = new org.apache.lucene.document.Document();
    
    long docLeft = ((StoredField) doc.getField(Definitions.FIELDNAME_DOCLEFT)).numericValue().longValue();
    long docRight = ((StoredField) doc.getField(Definitions.FIELDNAME_DOCRIGHT)).numericValue().longValue();
    
    String baseUri = baseUriMap.get(docLeft);
    if (baseUri == null)
      baseUri = nodeInfo.getDocumentURI();
    baseUriField.setStringValue(baseUri);
    newDoc.add(baseUriField);
    baseUriMap.put(new Long(docLeft), baseUri);
  
    Iterator<IndexableField> fields = doc.iterator();
    while (fields.hasNext()) {
      IndexableField field = fields.next();
      String fieldName = field.name();
      
      if (CharUtils.isAsciiNumeric(fieldName.charAt(0)) && StringUtils.countMatches(fieldName, "_") == 1) {
        String value = field.stringValue();
        newDoc.add(new StoredField(fieldName, value));
        if (StringUtils.isNotBlank(value)) {
          if (value.length() <= maxTermLength)
            newDoc.add(new StringField(fieldName, value, Field.Store.NO));
          addTypedValueField(newDoc, domNode);
        }
        addFieldNameField(newDoc, fieldName);
      } else {
        switch (fieldName) {  
          case Definitions.FIELDNAME_TYPE:
            typeField.setStringValue(field.stringValue());
            newDoc.add(typeField);
            break;
          case Definitions.FIELDNAME_LEFT:
            long left = ((StoredField) field).numericValue().longValue();
            leftField_lp.setLongValue(left);
            leftField_dv.setLongValue(left);
            leftField_sf.setLongValue(left);
            newDoc.add(leftField_lp);
            newDoc.add(leftField_dv);
            newDoc.add(leftField_sf);
            break;
          case Definitions.FIELDNAME_RIGHT:
            long right = ((StoredField) field).numericValue().longValue();
            rightField_lp.setLongValue(right);
            rightField_dv.setLongValue(right);
            rightField_sf.setLongValue(right);
            newDoc.add(rightField_lp);
            newDoc.add(rightField_dv);
            newDoc.add(rightField_sf);
            break;
          case Definitions.FIELDNAME_PARENT:
            long parent = ((StoredField) field).numericValue().longValue();
            parentField_lp.setLongValue(parent);
            parentField_sf.setLongValue(parent);
            parentField_dv.setLongValue(parent);
            newDoc.add(parentField_lp);
            newDoc.add(parentField_sf);
            newDoc.add(parentField_dv);
            break;
          case Definitions.FIELDNAME_DEPTH:
            depthField.setStringValue(field.stringValue());
            newDoc.add(depthField);
            break;
          case Definitions.FIELDNAME_PREFIXES:
            newDoc.add(new StoredField(Definitions.FIELDNAME_PREFIXES, field.stringValue()));
            break;
          case Definitions.FIELDNAME_VALUE:
            String value = field.stringValue();
            if (value.length() <= maxTermLength) {
              valueField.setStringValue(value);
              newDoc.add(valueField);
            }
            valueField_sf.setStringValue(value);
            newDoc.add(valueField_sf);
            break;
          case Definitions.FIELDNAME_URI:
            uriField.setStringValue(field.stringValue());
            newDoc.add(uriField);
            break;
          case Definitions.FIELDNAME_DOCLEFT:
            docLeftField_sf.setLongValue(docLeft);
            newDoc.add(docLeftField_sf);
            break;
          case Definitions.FIELDNAME_DOCRIGHT:
            docRightField_sf.setLongValue(docRight);
            newDoc.add(docRightField_sf);
            break;
        }
      }
    }
    
    addVirtualAttributeFields(newDoc, (Element) domNode, null); // TODO: what to do with params when reindexing?
    
    for (PluggableIndex pluggableIndex : index.getConfiguration().getPluggableIndexConfig().get())
      pluggableIndex.indexNode(newDoc, (Element) domNode);
    
    // Term term = new Term(Definitions.FIELDNAME_LEFT, Long.toString(((HierarchyNode) indexNode).left));
    
    this.index.getNodeStore().deleteLuceneDocument(LongPoint.newExactQuery(Definitions.FIELDNAME_LEFT, ((HierarchyNode) indexNode).left));
    this.index.getNodeStore().addLuceneDocument(newDoc);
    
    pushBackUsedFieldNameFields();
  }
  
  private void indexDOMDocument(String uri, Document doc, Map<String, Object> params) throws Exception {   
    doc.setStrictErrorChecking(false);
    this.docWrapper = null;
    numberNode(doc.getDocumentElement(), (byte) 2);
    XdmMap map = null;
    if (params != null) {
      Map<XdmAtomicValue, XdmValue> paramMap = new HashMap<XdmAtomicValue, XdmValue>(); 
      Iterator<Entry<String, Object>> it = params.entrySet().iterator();
      while (it.hasNext()) {
        Entry<String, Object> entry = (Entry<String, Object>) it.next();
        paramMap.put(new XdmAtomicValue(entry.getKey()), SaxonUtils.convertObjectToXdmValue(entry.getValue()));
      }
      map = new XdmMap(paramMap);
    }
    indexNode(doc.getDocumentElement(), -1, -1, uri, map);
  }
  
  public void index(String uri, Document doc, Map<String, Object> params) throws Exception {
    logger.info(String.format("Adding document with uri \"%s\" ...", uri));
    indexDOMDocument(uri, doc, params);
  }
  
  public void index(String uri, InputStream is, String systemId, Map<String, Object> params) throws Exception {
    logger.info(String.format("Adding document with uri \"%s\" ...", uri));
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setAttribute(PROPNAME_BUFFER_SIZE, BUFFER_SIZE);
    dbf.setValidating(false);
    dbf.setXIncludeAware(true);
    dbf.setNamespaceAware(true);
    DocumentBuilder builder = dbf.newDocumentBuilder();
    Document doc = builder.parse(is, systemId);
    indexDOMDocument(uri, doc, params);
  }
    
  public long getNodeCounter() {
    return nodeCounter;
  }
  
  public void setNodeCounter(long counter) {
    nodeCounter = counter;
  }
  
  public static final class UserData {
    
    public byte depth;
    public long left;
    public long right;

    public UserData(byte depth, long left, long right) {
      this.depth = depth;
      this.left = left;
      this.right = right;
    }
    
    public UserData(byte depth, long left) {
      this(depth, left, 0);
    }
    
  }

}