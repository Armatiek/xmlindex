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

import java.nio.file.Paths;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.saxon.functions.ExtensionFunctionCall;

/**
 * 
 * @author Maarten Kroon
 */
public class ImportDocuments extends ExtensionFunctionDefinition {
  
  private static final StructuredQName qName = 
      new StructuredQName("", Definitions.NAMESPACE_EXT_FUNCTIONS, "import-documents");

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
    return 3;
  }

  @Override
  public SequenceType[] getArgumentTypes() {
    return new SequenceType[] { SequenceType.SINGLE_STRING, SequenceType.SINGLE_INTEGER, SequenceType.SINGLE_STRING };
  }

  @Override
  public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
    return SequenceType.OPTIONAL_BOOLEAN;
  }

  @Override
  public ExtensionFunctionCall makeCallExpression() {
    return new ImportDocumentsCall();
  }
  
  @Override
  public boolean hasSideEffects() {
    return true;
  }
  
  private static class ImportDocumentsCall extends XMLIndexExtensionFunctionCall {

    @Override
    public ZeroOrOne<BooleanValue> call(XPathContext context, Sequence[] arguments) throws XPathException {            
      String path = ((StringValue) arguments[0].head()).getStringValue();
      int length = arguments.length;
      int maxDepth = Integer.MAX_VALUE;
      String pattern = null;
      if (length > 1)
        maxDepth = (int) ((Int64Value) arguments[1].head()).longValue();
      if (length > 2)
        pattern = ((StringValue) arguments[2].head()).getStringValue();
      try {
        getSession(context).addDocuments(Paths.get(path), maxDepth, pattern, null);
        return ZeroOrOne.empty();
      } catch (Exception e) {
        throw new XPathException("Error importing documents from \"" + path + "\"", e);
      }
    }
  }
  
}