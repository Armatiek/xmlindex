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

package nl.armatiek.xmlindex.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.CharUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.type.Type;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.conf.Definitions;

public class Element extends HierarchyNode implements NamedNode, ValueNode {
 
  public StructuredQName name;
  public List<Attribute> attributes;
  public List<Namespace> namespaces;
  
  public Element(byte type, long parent) { 
    super(type, parent);
  }
  
  public Element(Document doc, Session session)  {
    super((byte) Type.ELEMENT, doc);
    
    String[] prefixes = doc.getValues(Definitions.FIELDNAME_PREFIXES);
    HashMap<String, Integer> nameCodeToPrefixCodeMap = null;
    if (prefixes.length > 0) {
      nameCodeToPrefixCodeMap = new HashMap<String, Integer>();
      for (String prefix : prefixes) {
        String[] parts = prefix.split(";");
        nameCodeToPrefixCodeMap.put(parts[0], Integer.parseInt(parts[1]));
      }
    }
    
    Iterator<IndexableField> fields = doc.getFields().iterator();
    long idCounter = Definitions.MAX_LONG;
    while (fields.hasNext()) {
      IndexableField field = fields.next();
      String name = field.name();
      if (CharUtils.isAsciiNumeric(name.charAt(0))) {
        String[] parts = name.split("_");
        int nodeType = Integer.parseInt(parts[0]);
        if (nodeType == Type.ELEMENT) {
          int nameCode = Integer.parseInt(parts[1]);
          int prefixCode = getPrefixCode(name, nameCodeToPrefixCodeMap);
          this.name = session.getStructuredQName(nameCode, prefixCode);
          this.value = field.stringValue();
        } else if (nodeType == Type.ATTRIBUTE) {
          if (attributes == null)
            attributes = new ArrayList<Attribute>();
          int nameCode = Integer.parseInt(parts[1]);
          int prefixCode = getPrefixCode(name, nameCodeToPrefixCodeMap);
          StructuredQName attrName = session.getStructuredQName(nameCode, prefixCode);
          Attribute attr = new Attribute(idCounter--, this.left, attrName, field.stringValue()); 
          attributes.add(attr);
        } else if (nodeType == Type.NAMESPACE) {
          if (namespaces == null)
            namespaces = new ArrayList<Namespace>();
          int nameCode = Integer.parseInt(parts[1]);
          StructuredQName nsName = session.getStructuredQName(nameCode, -1);
          Namespace namespace = new Namespace(idCounter--, this.left, nsName.getLocalPart(), field.stringValue()); 
          namespaces.add(namespace);
        }
      }
    }
  }
    
  private int getPrefixCode(String name, HashMap<String, Integer> nameCodeToPrefixCodeMap) {
    if (nameCodeToPrefixCodeMap == null)
      return -1;
    Integer prefixCode = nameCodeToPrefixCodeMap.get(name);
    if (prefixCode == null)
      return -1;
    return prefixCode;
  }
  
  @Override
  public StructuredQName getName() {
    return name;
  }
  
  @Override
  public String getValue() {
    return value;
  }
  
}