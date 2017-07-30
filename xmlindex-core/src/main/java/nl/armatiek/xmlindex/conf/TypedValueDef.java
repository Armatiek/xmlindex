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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;

public class TypedValueDef {
 
  private final XMLIndex index;
  private int nodeType;
  private QName name;
  private ItemType itemType;
  
  public TypedValueDef(XMLIndex index, int nodeType, QName name, ItemType itemType) {
    this.index = index;
    this.nodeType = nodeType;
    this.name = name;
    this.itemType = itemType;
  }
  
  public TypedValueDef(XMLIndex index, Document doc) {
    this.index = index;
    nodeType = ((StoredField) doc.getField(Definitions.FIELDNAME_NODETYPE)).numericValue().intValue();
    name = new QName(doc.get(Definitions.FIELDNAME_NODENAMESPACEURI), doc.get(Definitions.FIELDNAME_NODELOCALNAME));
    itemType = Type.getBuiltInItemType(XMLConstants.W3C_XML_SCHEMA_NS_URI, doc.get(Definitions.FIELDNAME_ITEMTYPE));
  }
  
  public String getKey() {
    return nodeType + name.getNamespaceURI() + name.getLocalName();
  }
  
  @Override
  public int hashCode() {
    String str = nodeType + "_" + name.getClarkName() + "_" + itemType.getPrimitiveType();
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
        def.name.equals(name);
  }
    
  public int getNodeType() {
    return nodeType;
  }
  
  public QName getQName() {
    return name;
  }

  public String getNamespaceUri() {
    return name.getNamespaceURI();
  }

  public String getLocalPart() {
    return name.getLocalName();
  }
  
  public ItemType getItemType() {
    return itemType;
  }
  
  public void store() throws IOException {
    Document doc = new Document();
    doc.add(new NumericDocValuesField(Definitions.FIELDNAME_LEFT, Definitions.FIELVALUE_CONFIG_LEFT));
    doc.add(new StringField(Definitions.FIELDNAME_INDEXINFO, Definitions.FIELDVALUE_TYPEDVALDEF, Store.NO));
    doc.add(new StoredField(Definitions.FIELDNAME_NODETYPE, nodeType));
    doc.add(new StoredField(Definitions.FIELDNAME_NODENAMESPACEURI, getNamespaceUri()));
    doc.add(new StoredField(Definitions.FIELDNAME_NODELOCALNAME, getLocalPart()));
    doc.add(new StoredField(Definitions.FIELDNAME_ITEMTYPE, ((BuiltInAtomicType) itemType).getName()));
    index.getNodeStore().updateLuceneDocument(new Term(Definitions.FIELDNAME_DEFNAME, getKey()), doc);
    index.getNodeStore().commit(true);
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