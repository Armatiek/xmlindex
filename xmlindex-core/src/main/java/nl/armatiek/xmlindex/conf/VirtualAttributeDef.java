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

package nl.armatiek.xmlindex.conf;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer.Builder;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.Version;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.util.XMLUtils;

public class VirtualAttributeDef {
  
  private final XMLIndex index;
  private QName elemName;
  private String virtualAttrName;
  private QName functionName;
  private ItemType itemType;
  private Analyzer indexAnalyzer;
  private Analyzer queryAnalyzer;
  private XQueryEvaluator virtualAttrModule;
  
  public VirtualAttributeDef(XMLIndex index, QName elemName, String virtualAttrName,  QName functionName, ItemType itemType, 
      Analyzer indexAnalyzer, Analyzer queryAnalyzer) {
    this.index = index;
    this.elemName = elemName;
    this.itemType = itemType;
    this.virtualAttrName = virtualAttrName;
    this.functionName = functionName;
    this.queryAnalyzer = queryAnalyzer;
    this.indexAnalyzer = indexAnalyzer;
  }
  
  public VirtualAttributeDef(XMLIndex index, QName elemName, String virtualAttrName,  QName functionName, ItemType itemType) {
    this(index, elemName, virtualAttrName, functionName, itemType, null, null);
  }
  
  public VirtualAttributeDef(XMLIndex index, Document doc, Path analyzerConfigPath) {
    this.index = index; 
    elemName = new QName(doc.get(Definitions.FIELDNAME_NODENAMESPACEURI), doc.get(Definitions.FIELDNAME_NODELOCALNAME));
    itemType = Type.getBuiltInItemType(XMLConstants.W3C_XML_SCHEMA_NS_URI, doc.get(Definitions.FIELDNAME_ITEMTYPE));
    virtualAttrName = doc.get(Definitions.FIELDNAME_VIRTATTRNAME);
    functionName = new QName(doc.get(Definitions.FIELDNAME_FUNCNAMESPACEURI), doc.get(Definitions.FIELDNAME_FUNCLOCALNAME));
    queryAnalyzer = deserializeAnalyzer(doc.get(Definitions.FIELDNAME_ANALYZERQUERY), analyzerConfigPath);
    indexAnalyzer = deserializeAnalyzer(doc.get(Definitions.FIELDNAME_ANALYZERINDEX), analyzerConfigPath);
  }
  
  public QName getElemName() {
    return elemName;
  }
  
  public String getElemNamespaceUri() {
    return elemName.getNamespaceURI();
  }

  public String getElemLocalPart() {
    return elemName.getLocalName();
  }
  
  public String getVirtualAttributeName() {
    return virtualAttrName;
  }

  public QName getFunctionName() {
    return functionName;
  }
  
  public ItemType getItemType() {
    return itemType;
  }
  
  public Analyzer getQueryAnalyzer() {
    return queryAnalyzer;
  }
  
  public Analyzer getIndexAnalyzer() {
    return indexAnalyzer;
  }

  public XQueryEvaluator getVirtualAttributeModule() {
    return virtualAttrModule;
  }
  
  public void store() throws IOException {
    Document doc = new Document();
    doc.add(new NumericDocValuesField(Definitions.FIELDNAME_LEFT, Definitions.FIELVALUE_CONFIG_LEFT));
    doc.add(new StringField(Definitions.FIELDNAME_INDEXINFO, Definitions.FIELDVALUE_VIRTATTRDEF, Store.NO));
    doc.add(new StoredField(Definitions.FIELDNAME_NODENAMESPACEURI, elemName.getNamespaceURI()));
    doc.add(new StoredField(Definitions.FIELDNAME_NODELOCALNAME, elemName.getLocalName()));
    doc.add(new StoredField(Definitions.FIELDNAME_ITEMTYPE, ((BuiltInAtomicType) itemType).getName()));
    doc.add(new StoredField(Definitions.FIELDNAME_VIRTATTRNAME, virtualAttrName));
    doc.add(new StoredField(Definitions.FIELDNAME_FUNCNAMESPACEURI, functionName.getNamespaceURI()));
    doc.add(new StoredField(Definitions.FIELDNAME_FUNCLOCALNAME, functionName.getLocalName()));
    doc.add(new StoredField(Definitions.FIELDNAME_ANALYZERINDEX, indexAnalyzer != null ? serializeAnalyzer(indexAnalyzer) : ""));
    doc.add(new StoredField(Definitions.FIELDNAME_ANALYZERQUERY, queryAnalyzer != null ? serializeAnalyzer(queryAnalyzer) : ""));
    index.getNodeStore().updateLuceneDocument(new Term(Definitions.FIELDNAME_DEFNAME, virtualAttrName), doc);
    index.getNodeStore().commit(true);
  }
  
  public void reindex() throws IOException {
    Session session = index.aquireSession();
    try {  
      session.reindexVirtualAttributeDefTypedValueDef(this);
    } finally {
      index.returnSession(session);
    }
  }
  
  public static String getSPIName(Class<?> clazz, String[] suffixes) {
    String clazzName = clazz.getSimpleName();
    for (String suffix : suffixes) {
      if (clazzName.endsWith(suffix))
        return clazzName.substring(0, clazzName.length() - suffix.length()).toLowerCase(Locale.ROOT);
    }
    throw new XMLIndexException("Error determining SPI name for class \"" + clazzName + "\"");
  }
  
  public static void serializeAnalyzer(Analyzer analyzer, XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement("analyzer");
    if (!(analyzer instanceof CustomAnalyzer)) {
      writer.writeAttribute("class", analyzer.getClass().getName());
    } else {
      CustomAnalyzer ca = (CustomAnalyzer) analyzer;
      
      /* Write character filters: */
      List<CharFilterFactory> cffs = ca.getCharFilterFactories();
      for (CharFilterFactory cff : cffs) {
        writer.writeStartElement("charFilter");
        writer.writeAttribute("class", getSPIName(cff.getClass(), new String[] { CharFilterFactory.class.getSimpleName() }));
        writeArgs(writer, cff.getOriginalArgs());
        writer.writeEndElement();
      }
      
      /* Write tokenizer */
      TokenizerFactory tf = ca.getTokenizerFactory();
      writer.writeStartElement("tokenizer");
      writer.writeAttribute("class", getSPIName(tf.getClass(), new String[] { TokenizerFactory.class.getSimpleName() }));
      writeArgs(writer, tf.getOriginalArgs());
      writer.writeEndElement();
      
      /* Write token filters: */
      List<TokenFilterFactory> tffs = ca.getTokenFilterFactories();
      for (TokenFilterFactory tff : tffs) {
        writer.writeStartElement("filter");
        writer.writeAttribute("class", getSPIName(tff.getClass(), new String[] { "TokenFilterFactory", "FilterFactory" }));
        writeArgs(writer, tff.getOriginalArgs());
        writer.writeEndElement();
      }
    }
    writer.writeEndElement();
  }
  
  private String serializeAnalyzer(Analyzer analyzer) {
    try {
      XMLOutputFactory output = XMLOutputFactory.newInstance();
      StringWriter sw = new StringWriter();
      XMLStreamWriter writer = output.createXMLStreamWriter(sw);
      writer.writeStartDocument();
      serializeAnalyzer(analyzer, writer);
      writer.writeEndDocument();
      writer.flush();
      return sw.toString();
    } catch (XMLStreamException xse) {
      throw new XMLIndexException("Error serializing analyzer", xse);
    }
  }
  
  private static void writeArgs(XMLStreamWriter writer, Map<String, String> args) throws XMLStreamException {
    Iterator<Map.Entry<String, String>> it = args.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, String> entry = it.next();
      if (entry.getKey().equals("luceneMatchVersion"))
        continue;
      writer.writeAttribute(entry.getKey(), entry.getValue());
    }
  }
  
  public static Analyzer deserializeAnalyzer(Element analyzerElem, Path analyzerConfigPath) {
    if (analyzerElem == null)
      return null;
    try {
      Analyzer analyzer;
      String analyzerClass = analyzerElem.getAttribute("class");
      if (StringUtils.isNotEmpty(analyzerClass)) {
        Class<?> clazz = Class.forName(analyzerClass);
        analyzer = (Analyzer) clazz.newInstance();
      } else {
        Builder builder = CustomAnalyzer.builder(analyzerConfigPath);
        Element childElem = XMLUtils.getFirstChildElement(analyzerElem);
        while (childElem != null) {
          String localName = childElem.getLocalName();
          switch (localName) {
          case "tokenizer":
            builder = builder.withTokenizer(childElem.getAttribute("class"), attrToParams(childElem));
            break;
          case "filter":
            builder = builder.addTokenFilter(childElem.getAttribute("class"), attrToParams(childElem));
            break;
          case "charFilter":
            builder = builder.addCharFilter(childElem.getAttribute("class"), attrToParams(childElem));
            break;
          }
          childElem = XMLUtils.getNextSiblingElement(childElem);
        }
        analyzer = builder.build();
      }
      return analyzer;
    } catch (Exception e) {
      throw new XMLIndexException("Error deserializing analyzer", e);
    }
  }
  
  private Analyzer deserializeAnalyzer(String xml, Path analyzerConfigPath) {
    if (StringUtils.isBlank(xml))
      return null;
    try {
      Element analyzerElem = XMLUtils.stringToDocument(xml).getDocumentElement();
      return deserializeAnalyzer(analyzerElem, analyzerConfigPath);
    } catch (Exception e) {
      throw new XMLIndexException("Error deserializing analyzer", e);
    }
  }
  
  private static Map<String, String> attrToParams(Element elem) {
    Map<String, String> params = new HashMap<String, String>();
    NamedNodeMap attrs = elem.getAttributes();
    for (int i=0; i<attrs.getLength(); i++) {
      Attr attr = (Attr) attrs.item(i);
      if (StringUtils.equals(attr.getNamespaceURI(), XMLConstants.XMLNS_ATTRIBUTE_NS_URI))
        continue;
      String localName = attr.getNamespaceURI() == null ? attr.getName() : attr.getLocalName();
      if (localName.equals("class"))
        continue;
      params.put(localName, attr.getValue());
    }
    params.put("luceneMatchVersion", Version.LATEST.toString());
    return params;
  }
   
}
