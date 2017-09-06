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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer.Builder;
import org.apache.lucene.util.Version;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.saxon.util.SaxonUtils;
import nl.armatiek.xmlindex.util.XMLUtils;

public class VirtualAttributeDef {
  
  private final XMLIndex index;
  private final List<QName> elemNames;
  private final String virtualAttrName;
  private final QName functionName;
  private final ItemType itemType;
  private final Analyzer indexAnalyzer;
  private final Analyzer queryAnalyzer;
  private final boolean storeValue;
  private final XQueryEvaluator eval;
  
  public VirtualAttributeDef(XMLIndex index, String virtualAttrName, QName functionName, ItemType itemType, 
      boolean storeValue, XQueryEvaluator eval) {
    this.index = index;
    this.elemNames = new ArrayList<QName>();
    this.elemNames.add(Definitions.QNAME_VA_BINDING_DOCUMENT_ELEMENT);
    this.virtualAttrName = virtualAttrName;
    this.functionName = functionName;
    this.itemType = itemType;
    this.storeValue = storeValue;
    this.indexAnalyzer = null;
    this.queryAnalyzer = null;
    this.eval = eval;
  }
  
  public VirtualAttributeDef(XMLIndex index, Element virtualAttributeDefElem, Path analyzerConfigPath,
      XQueryEvaluator eval) {
    this.index = index;
    this.elemNames = new ArrayList<QName>();
    Element bindingsElement = XMLUtils.getChildElementByLocalName(virtualAttributeDefElem, "bindings");
    if (XMLUtils.getChildElementByLocalName(bindingsElement, "document-element") != null)
      elemNames.add(Definitions.QNAME_VA_BINDING_DOCUMENT_ELEMENT);
    NodeList elemNameNodes = bindingsElement.getElementsByTagName("element-name");
    for (int i=0; i<elemNameNodes.getLength(); i++)
      elemNames.add(SaxonUtils.getQName(elemNameNodes.item(i)));
    virtualAttrName = XMLUtils.getValueOfChildElementByLocalName(virtualAttributeDefElem, "virtual-attribute-name");
    functionName = SaxonUtils.getQName(XMLUtils.getChildElementByLocalName(virtualAttributeDefElem, "function-name"));
    String type = XMLUtils.getValueOfChildElementByLocalName(virtualAttributeDefElem, "item-type");
    itemType = Type.getBuiltInItemType(XMLConstants.W3C_XML_SCHEMA_NS_URI, StringUtils.substringAfter(type, ":"));
    queryAnalyzer = getAnalyzer(virtualAttributeDefElem, "index-analyzer", analyzerConfigPath);
    indexAnalyzer = getAnalyzer(virtualAttributeDefElem, "query-analyzer", analyzerConfigPath);
    storeValue = XMLUtils.getBooleanValue(XMLUtils.getValueOfChildElementByLocalName(virtualAttributeDefElem, "store-value"), false);
    this.eval = eval; 
  }
  
  public List<QName> getElemNames() {
    return elemNames;
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
  
  public boolean getStoreValue() {
    return storeValue;
  }
  
  public XQueryEvaluator getXQueryEvaluator() {
    return eval;
  }

  public void reindex() throws IOException {
    Session session = index.aquireSession();
    try {  
      session.reindexVirtualAttributeDefTypedValueDef(this);
    } finally {
      index.returnSession(session);
    }
  }
  
  private Analyzer getAnalyzer(Element parentElem, String elemName, Path analyzerConfigPath) {
    Element analyzerElem = XMLUtils.getChildElementByLocalName(parentElem, elemName);
    if (analyzerElem != null)
      return deserializeAnalyzer(XMLUtils.getFirstChildElement(analyzerElem), analyzerConfigPath);
    return null;
  }

  private Analyzer deserializeAnalyzer(Element analyzerElem, Path analyzerConfigPath) {
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
          case "char-filter":
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
  
  private Map<String, String> attrToParams(Element elem) {
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
