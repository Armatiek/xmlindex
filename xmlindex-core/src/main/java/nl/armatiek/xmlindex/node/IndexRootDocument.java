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

import net.sf.saxon.type.Type;
import nl.armatiek.xmlindex.conf.Definitions;

public class IndexRootDocument extends HierarchyNode {
  
  public final static long LEFT = 0;

  public IndexRootDocument() {
    super((byte) Type.DOCUMENT, -1);
    this.left = LEFT;
    this.right = Definitions.MAX_LONG;
    this.depth = 0;
    this.value = null;
  }
  
}