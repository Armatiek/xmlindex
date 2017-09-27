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

import javax.xml.XMLConstants;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.saxon.util.SaxonUtils;
import nl.armatiek.xmlindex.utils.XMLUtils;

public class TypedValueDef {
 
  private final String name;
  private final XMLIndex index;
  private int nodeType;
  private QName nodeName;
  private ItemType itemType;
  
  /*
  public TypedValueDef(XMLIndex index, int nodeType, QName name, ItemType itemType) {
    this.index = index;
    this.nodeType = nodeType;
    this.nodeName = name;
    this.itemType = itemType;
  }
  */
  
  public TypedValueDef(Element typedValueDefElem) {
    this.index = null;
    this.name = XMLUtils.getValueOfChildElementByLocalName(typedValueDefElem, "name");
    nodeType = XMLUtils.toNodeType(XMLUtils.getValueOfChildElementByLocalName(typedValueDefElem, "node-type"));
    nodeName = SaxonUtils.getQName(XMLUtils.getChildElementByLocalName(typedValueDefElem, "node-name"));
    String type = XMLUtils.getValueOfChildElementByLocalName(typedValueDefElem, "item-type");
    itemType = Type.getBuiltInItemType(XMLConstants.W3C_XML_SCHEMA_NS_URI, StringUtils.substringAfter(type, ":"));
  }
  
  public String getName() {
    return name;
  }
  
  public String getKey() {
    return nodeType + nodeName.getNamespaceURI() + nodeName.getLocalName();
  }
  
  @Override
  public int hashCode() {
    String str = nodeType + "_" + nodeName.getClarkName() + "_" + itemType.getPrimitiveType();
    return str.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof TypedValueDef))
      return false;
    TypedValueDef def = (TypedValueDef) obj;
    return 
        def.getNodeType() == nodeType &&
        def.getItemType().getPrimitiveType() == itemType.getPrimitiveType() &&
        def.nodeName.equals(nodeName);
  }
    
  public int getNodeType() {
    return nodeType;
  }
  
  public QName getNodeName() {
    return nodeName;
  }

  public String getNamespaceUri() {
    return nodeName.getNamespaceURI();
  }

  public String getLocalPart() {
    return nodeName.getLocalName();
  }
  
  public ItemType getItemType() {
    return itemType;
  }
    
  public void reindex() throws IOException {
    Session session = index.aquireSession();
    try {  
      session.reindexTypedValueDef(this);
    } finally {
      index.returnSession(session);
    }
  }

}