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

import net.sf.saxon.type.Type;

public class Text extends HierarchyNode implements ValueNode {

  public Text(Document doc) {
    super((byte) Type.TEXT, doc);
  }
  
  public Text(Element parentElem, String value) {
    super((byte) Type.TEXT, parentElem.left);
    this.left = parentElem.left + 1;
    this.right = parentElem.right - 1;
    this.depth = (byte) (parentElem.depth + 1);
    this.docLeft = parentElem.docLeft;
    this.value = value;
  }
  
  @Override
  public String getValue() {
    return value;
  }
  
}