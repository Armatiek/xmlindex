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

package nl.armatiek.xmlindex.saxon.functions.xmlindex;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.ObjectValue;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.conf.Definitions;

/**
 * @author Maarten Kroon
 *
 */
public abstract class ExtensionFunctionCall extends net.sf.saxon.lib.ExtensionFunctionCall {
  
  protected Session getSession(XPathContext context) {
    return (Session) ((ObjectValue<?>) context.getController().getParameter(Definitions.PARAM_SESSION_SQN)).getObject();
  }
  
  protected NodeInfo unwrapNodeInfo(NodeInfo nodeInfo) {
    if (nodeInfo != null && nodeInfo.getNodeKind() == Type.DOCUMENT) {
      nodeInfo = nodeInfo.iterateAxis(AxisInfo.CHILD, NodeKindTest.ELEMENT).next();
    }
    return nodeInfo;
  }
  
}