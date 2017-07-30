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
/*
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

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.SequenceType;
import nl.armatiek.xmlindex.conf.Definitions;

/**
 * 
 * @author Maarten Kroon
 */
public class LocalVariableReferencer extends ExtensionFunctionDefinition {

  public static final StructuredQName qName = 
      new StructuredQName("", Definitions.NAMESPACE_EXT_FUNCTIONS, "local-variable-referencer");

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
    return 20;
  }

  @Override
  public SequenceType[] getArgumentTypes() {
    return new SequenceType[] { 
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE,
        SequenceType.ANY_SEQUENCE
    };
  }

  @Override
  public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
    return SequenceType.SINGLE_BOOLEAN;
  }

  @Override
  public ExtensionFunctionCall makeCallExpression() {
    return new LocalVariableReferencerCall();
  }
  
  @Override
  public boolean hasSideEffects() {
    return true;
  }
  
  public static class LocalVariableReferencerCall extends ExtensionFunctionCall {

    @Override
    public BooleanValue call(XPathContext context, Sequence[] arguments) throws XPathException {            
      return BooleanValue.TRUE;
    }
  }
  
}