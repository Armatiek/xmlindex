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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.error.XMLIndexException;

public class TypedValueDefConfig {
  
  private static final Logger logger = LoggerFactory.getLogger(TypedValueDefConfig.class);
  
  private final HashMap<String, TypedValueDef> typedValueDefs = new HashMap<String, TypedValueDef>(); 
  private final XMLIndex index;
  
  public TypedValueDefConfig(XMLIndex index) {
    try {
      this.index = index;
      List<TypedValueDef> tdefs = index.getNodeStore().getTypedValueDefs();
      for (TypedValueDef tdef : tdefs)
        typedValueDefs.put(tdef.getKey(), tdef);
    } catch (Exception e) {
      throw new XMLIndexException("Error initializing typed value definitions for index \"" + index.getIndexName() + "\"", e);
    }  
  }
  
  public void add(TypedValueDef typedValueDef) throws IOException {
    if (exists(typedValueDef.getNodeType(), typedValueDef.getQName())) 
      throw new IllegalArgumentException("Typed value definition for node type \"" + typedValueDef.getNodeType() + "\" and qualified name \"" + typedValueDef.getQName().getClarkName() + "\" already exists");
    int nodeType = typedValueDef.getNodeType();
    if (nodeType != Type.ELEMENT && nodeType != Type.ATTRIBUTE)
      throw new IllegalArgumentException("Error adding typed value definition, node type \"" + nodeType + "\" not supported");
    ItemType itemType = typedValueDef.getItemType();
    if (!(itemType.equals(BuiltInAtomicType.STRING) || itemType.equals(BuiltInAtomicType.BOOLEAN) || itemType.equals(BuiltInAtomicType.FLOAT) ||
        itemType.equals(BuiltInAtomicType.DOUBLE) || itemType.equals(BuiltInAtomicType.LONG) || itemType.equals(BuiltInAtomicType.INT) ||
        itemType.equals(BuiltInAtomicType.SHORT) || itemType.equals(BuiltInAtomicType.BYTE) || itemType.equals(BuiltInAtomicType.DATE_TIME) ||
        itemType.equals(BuiltInAtomicType.DATE) || itemType.equals(BuiltInAtomicType.TIME)))
      throw new IllegalArgumentException("Error adding typed value definition, item type \"" + itemType.toString() + "\" not supported");
    typedValueDef.store();
    typedValueDefs.put(typedValueDef.getKey(), typedValueDef);
    logger.info("Added typed value definition for node type \"" + typedValueDef.getNodeType() + "\", qualified name \"" + typedValueDef.getQName().getClarkName() + "\" and item type \"" + typedValueDef.getItemType() + "\"");
  }
  
  public void remove(TypedValueDef typedValueDef) throws IOException {
    String key = typedValueDef.getKey();
    if (!typedValueDefs.containsKey(key)) 
      throw new IllegalArgumentException("Typed value definition for node type \"" + typedValueDef.getNodeType() + "\" and qualified name \"" + typedValueDef.getQName().getClarkName() + "\" does not exists");
    index.getNodeStore().deleteLuceneDocument(new Term(Definitions.FIELDNAME_DEFNAME, key));
    index.getNodeStore().commit(true);
    typedValueDefs.remove(typedValueDef.getKey());
    logger.info("Removed typed value definition for node type \"" + typedValueDef.getNodeType() + "\", qualified name \"" + typedValueDef.getQName().getClarkName() + "\" and item type \"" + typedValueDef.getItemType() + "\"");
  }
  
  public Collection<TypedValueDef> get() {
    return typedValueDefs.values();
  }
  
  public TypedValueDef get(int nodeType, QName name) {
    String key = nodeType + name.getNamespaceURI() + name.getLocalName();
    return typedValueDefs.get(key);
  }
  
  public TypedValueDef get(int nodeType, String namespaceURI, String localName) {
    String key = nodeType + StringUtils.defaultString(namespaceURI) + localName;
    return typedValueDefs.get(key);
  }
  
  public boolean exists(int nodeType, QName name) {
    return get(nodeType, name) != null;
  }
  
}