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

import nl.armatiek.xmlindex.conf.Definitions;

public abstract class HierarchyNode extends Node {
  
  public Document doc;
  public long left;
  public long right;
  public long docLeft = -1;
  public long docRight = -1;
  public byte depth;
  public String value;
  
  public HierarchyNode(byte type, long parent) { 
    super(type, parent);
  }
  
  public HierarchyNode(byte type, Document doc) { 
    super(type, ((StoredField) doc.getField(Definitions.FIELDNAME_PARENT)).numericValue().longValue());
    this.doc = doc;
    this.left = ((StoredField) doc.getField(Definitions.FIELDNAME_LEFT)).numericValue().longValue();
    this.right = ((StoredField) doc.getField(Definitions.FIELDNAME_RIGHT)).numericValue().longValue();
    StoredField docLeftField = (StoredField) doc.getField(Definitions.FIELDNAME_DOCLEFT);
    if (docLeftField != null)
      this.docLeft = docLeftField.numericValue().longValue();
    StoredField docRightField = (StoredField) doc.getField(Definitions.FIELDNAME_DOCRIGHT);
    if (docRightField != null)
      this.docRight = docRightField.numericValue().longValue();
    this.depth = Byte.parseByte(doc.get(Definitions.FIELDNAME_DEPTH));
    this.value = doc.get(Definitions.FIELDNAME_VALUE);
  }
  
  @Override
  public int compareTo(Node otherNode) {    
    return Long.compare(left, ((HierarchyNode) otherNode).left);
  }
  
}