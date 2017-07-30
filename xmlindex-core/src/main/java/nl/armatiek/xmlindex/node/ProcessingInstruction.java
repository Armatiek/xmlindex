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

import java.util.Iterator;

import org.apache.commons.lang3.CharUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.type.Type;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.error.XMLIndexException;

public class ProcessingInstruction extends HierarchyNode implements NamedNode, ValueNode {
  
  public StructuredQName name;
  
  public ProcessingInstruction(Document doc, Session session) {
    super((byte) Type.PROCESSING_INSTRUCTION, doc);
    
    Iterator<IndexableField> fields = doc.getFields().iterator();
    while (fields.hasNext()) {
      IndexableField field = fields.next();
      String name = field.name();
      if (CharUtils.isAsciiNumeric(name.charAt(0))) {
        String[] parts = name.split("_");
        int nodeType = Integer.parseInt(parts[0]);
        if (nodeType != Type.PROCESSING_INSTRUCTION)
          throw new XMLIndexException("Nodetype of processing instruction does not match");
        int nameCode = Integer.parseInt(parts[1]);
        this.name = session.getStructuredQName(nameCode, -1);
        this.value = field.stringValue();
      }
    }
  }
  
  @Override
  public String getValue() {
    return value;
  }

  @Override
  public StructuredQName getName() {
    return name;
  }
  
}