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
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.QNameValue;
import net.sf.saxon.value.SequenceType;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.conf.TypedValueDef;

public class RemoveTypedValueDef extends ExtensionFunctionDefinition {
  
  private static final StructuredQName qName = 
      new StructuredQName("", Definitions.NAMESPACE_EXT_FUNCTIONS, "remove-typed-value-def");

  @Override
  public StructuredQName getFunctionQName() {
    return qName;
  }

  @Override
  public int getMinimumNumberOfArguments() {
    return 2;
  }

  @Override
  public int getMaximumNumberOfArguments() {
    return 3;
  }

  @Override
  public SequenceType[] getArgumentTypes() {
    return new SequenceType[] { SequenceType.SINGLE_INTEGER, SequenceType.SINGLE_QNAME,
        SequenceType.SINGLE_BOOLEAN };
  }

  @Override
  public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
    return SequenceType.SINGLE_BOOLEAN;
  }

  @Override
  public XMLIndexExtensionFunctionCall makeCallExpression() {
    return new RemoveTypedValueDefCall();
  }
  
  @Override
  public boolean hasSideEffects() {
    return true;
  }
  
  private static class RemoveTypedValueDefCall extends XMLIndexExtensionFunctionCall {
    
    @Override
    public BooleanValue call(XPathContext context, Sequence[] arguments) throws XPathException {            
      try {
        int nodeType = (int) ((Int64Value) arguments[0].head()).longValue();
        QName name = new QName(((QNameValue) arguments[1].head()).getStructuredQName());
        boolean reindex = false;
        if (arguments.length > 2)
          reindex = ((BooleanValue) arguments[2].head()).getBooleanValue();
        XMLIndex index = getSession(context).getIndex();
        TypedValueDef typedValueDef = index.getConfiguration().getTypedValueConfig().get(nodeType, name);
        if (typedValueDef == null)
          throw new XPathException("Typed value definition for node type \"" + nodeType + "\" and qualified name \"" + name.getClarkName() + "\" does not exists");
        index.getConfiguration().getTypedValueConfig().remove(typedValueDef);
        if (reindex)
          typedValueDef.reindex();
        return BooleanValue.TRUE;
      } catch (XPathException e) {
        throw e;
      } catch (Exception e) {
        throw new XPathException(e.getMessage(), e);
      }
    }
  }
  
}