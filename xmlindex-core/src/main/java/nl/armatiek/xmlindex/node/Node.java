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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;

import net.sf.saxon.type.Type;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.error.XMLIndexException;

public abstract class Node implements Comparable<Node> {
  
  public final static IndexRootDocument DOCUMENT_NODE = new IndexRootDocument();
  public final static IndexRootElement ROOT_ELEMENT = new IndexRootElement();
   
  public byte type;
  public long parent;
  
  public Node(byte type, long parent) { 
    this.type = type;
    this.parent = parent;
  }
  
  public static Node createNode(Document doc, Session session) {
    byte nodeType = Byte.parseByte(doc.get(Definitions.FIELDNAME_TYPE));
    long parent = ((StoredField) doc.getField(Definitions.FIELDNAME_PARENT)).numericValue().longValue();
    
    switch (nodeType) {
    case Type.ELEMENT:
      if (parent == Node.ROOT_ELEMENT.left)
        return new DocumentElement(doc, session);
      else
        return new Element(doc, session);
    case Type.TEXT:
      return new Text(doc);
    case Type.COMMENT:
      return new Comment(doc);
    case Type.PROCESSING_INSTRUCTION:
      return new ProcessingInstruction(doc, session);
    case Type.DOCUMENT:
      return DOCUMENT_NODE;
    default:
      throw new XMLIndexException("Error creating Node. Unsupported nodetype: " + nodeType);
    }
  }
   
  public abstract int compareTo(Node otherNode);
  
}