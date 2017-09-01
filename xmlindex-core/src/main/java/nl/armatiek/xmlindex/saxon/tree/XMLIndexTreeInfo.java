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

package nl.armatiek.xmlindex.saxon.tree;

import net.sf.saxon.om.GenericTreeInfo;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.node.IndexRootDocument;

public class XMLIndexTreeInfo extends GenericTreeInfo {

  private Session session;
  private XMLIndexNodeInfo rootNode;
  
  public XMLIndexTreeInfo(Session session) {
    super(session.getConfiguration());
    this.session = session;
  }
  
  @Override
  public XMLIndexNodeInfo getRootNode() {
    if (rootNode == null)
      rootNode = new XMLIndexNodeInfo(session, new IndexRootDocument());
    return rootNode;
  }
  
}