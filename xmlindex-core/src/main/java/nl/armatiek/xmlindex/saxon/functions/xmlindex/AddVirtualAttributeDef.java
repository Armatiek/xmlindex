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

import org.apache.lucene.analysis.Analyzer;
import org.w3c.dom.Element;

import net.sf.saxon.dom.NodeOverNodeInfo;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.QNameValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.conf.VirtualAttributeDef;

/**
 * 
 * @author Maarten Kroon
 */
public class AddVirtualAttributeDef extends ExtensionFunctionDefinition {
  
  private static final StructuredQName qName = 
      new StructuredQName("", Definitions.NAMESPACE_EXT_FUNCTIONS, "add-virtual-attribute-def");

  @Override
  public StructuredQName getFunctionQName() {
    return qName;
  }

  @Override
  public int getMinimumNumberOfArguments() {
    return 4;
  }

  @Override
  public int getMaximumNumberOfArguments() {
    return 7;
  }

  @Override
  public SequenceType[] getArgumentTypes() {
    return new SequenceType[] { 
        SequenceType.SINGLE_QNAME, 
        SequenceType.SINGLE_STRING, 
        SequenceType.SINGLE_QNAME,
        SequenceType.SINGLE_QNAME, 
        SequenceType.OPTIONAL_NODE, 
        SequenceType.OPTIONAL_NODE,
        SequenceType.SINGLE_BOOLEAN };
  }

  @Override
  public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
    return SequenceType.SINGLE_BOOLEAN;
  }

  @Override
  public XMLIndexExtensionFunctionCall makeCallExpression() {
    return new AddVirtualAttributeDefCall();
  }
  
  @Override
  public boolean hasSideEffects() {
    return true;
  }
  
  private static class AddVirtualAttributeDefCall extends XMLIndexExtensionFunctionCall {

    @Override
    public BooleanValue call(XPathContext context, Sequence[] arguments) throws XPathException {            
      try {
        QName elemName = new QName(((QNameValue) arguments[0].head()).getStructuredQName());
        String virtAttrName = ((StringValue) arguments[1].head()).getStringValue();
        QName functionName = new QName(((QNameValue) arguments[2].head()).getStructuredQName());
        QName itemTypeName = new QName(((QNameValue) arguments[3].head()).getStructuredQName());
        ItemType itemType = Type.getBuiltInItemType(itemTypeName.getNamespaceURI(), itemTypeName.getLocalName());
        XMLIndex index = getSession(context).getIndex();
        Analyzer indexAnalyzer = null;
        if (arguments.length > 4)
          indexAnalyzer = VirtualAttributeDef.deserializeAnalyzer((Element) NodeOverNodeInfo.wrap(((NodeInfo) arguments[4].head())), 
              index.getConfiguration().getAnalyzerConfigPath());
        Analyzer queryAnalyzer= null;
        if (arguments.length > 5)
          queryAnalyzer = VirtualAttributeDef.deserializeAnalyzer((Element) NodeOverNodeInfo.wrap(((NodeInfo) arguments[5].head())), 
              index.getConfiguration().getAnalyzerConfigPath());
        boolean reindex = false;
        if (arguments.length > 6)
          reindex = ((BooleanValue) arguments[6].head()).getBooleanValue();
        VirtualAttributeDef virtualAttributeDef = new VirtualAttributeDef(index, elemName, virtAttrName, functionName, itemType, 
            indexAnalyzer, queryAnalyzer);
        index.getConfiguration().getVirtualAttributeConfig().add(virtualAttributeDef);
        if (reindex)
          virtualAttributeDef.reindex();
        return BooleanValue.TRUE;
      } catch (XPathException e) {
        throw e;
      } catch (Exception e) {
        throw new XPathException(e.getMessage(), e);
      }
    }
  }
  
}