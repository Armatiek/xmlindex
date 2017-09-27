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
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.SequenceType;

import java.util.Map;

import org.apache.lucene.document.Document;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.node.HierarchyNode;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexNodeInfo;

/**
 * 
 * @author Maarten Kroon
 */
public class ReindexNode extends ExtensionFunctionDefinition {
  
  private static final StructuredQName qName = 
      new StructuredQName("", Definitions.NAMESPACE_EXT_FUNCTIONS, "reindex-node");

  @Override
  public StructuredQName getFunctionQName() {
    return qName;
  }

  @Override
  public int getMinimumNumberOfArguments() {
    return 1;
  }

  @Override
  public int getMaximumNumberOfArguments() {
    return 1;
  }

  @Override
  public SequenceType[] getArgumentTypes() {
    return new SequenceType[] { SequenceType.SINGLE_NODE };
  }

  @Override
  public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
    return SequenceType.OPTIONAL_BOOLEAN;
  }

  @Override
  public XMLIndexExtensionFunctionCall makeCallExpression() {
    return new ReindexNodeCall();
  }
  
  @Override
  public boolean hasSideEffects() {
    return true;
  }
  
  private static class ReindexNodeCall extends XMLIndexExtensionFunctionCall {

    @Override
    public ZeroOrOne<BooleanValue> call(XPathContext context, Sequence[] arguments) throws XPathException {            
      try {
        NodeInfo node = ((NodeInfo) arguments[0].head());
        if (!(node instanceof XMLIndexNodeInfo))
          throw new XPathException("Node is not a XMLIndexNodeInfo");
        Session session = getSession(context);
        Document doc = ((HierarchyNode) ((XMLIndexNodeInfo) node).getRealNode()).doc;
        @SuppressWarnings("unchecked")
        Map<Long, String> baseUriMap = (Map<Long, String>) ((ObjectValue<?>) context.getController().getParameter(Definitions.PARAM_BUM_SQN)).getObject();  
        session.getIndex().getNodeStore().reindexNode(session, doc, baseUriMap);
        return ZeroOrOne.empty();
      } catch (Exception e) {
        throw new XPathException("Error reindexing node", e);
      }
    }
  }
  
}