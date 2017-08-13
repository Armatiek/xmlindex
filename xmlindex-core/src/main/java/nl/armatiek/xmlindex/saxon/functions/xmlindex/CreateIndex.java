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

import java.io.File;
import java.io.IOException;
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
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.lucene.codec.XMLIndexStoredFieldsFormat.Mode;

/**
 * 
 * @author Maarten Kroon
 */
public class CreateIndex extends ExtensionFunctionDefinition {
  
  private static final StructuredQName qName = 
      new StructuredQName("", Definitions.NAMESPACE_EXT_FUNCTIONS, "create-index");

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
  public XMLIndexExtensionFunctionCall makeCallExpression() {
    return new CreateIndexCall();
  }
  
  @Override
  public boolean hasSideEffects() {
    return true;
  }
  
  private static class CreateIndexCall extends XMLIndexExtensionFunctionCall {

    @Override
    public ZeroOrOne<BooleanValue> call(XPathContext context, Sequence[] arguments) throws XPathException {            
      File indexDir = null;
      try {
        String indexPath = ((StringValue) arguments[0].head()).getStringValue();
        int maxTermLength = XMLIndex.DEFAULT_MAX_TERM_LENGTH;
        Mode compression = XMLIndex.DEFAULT_INDEX_COMPRESSION;
        if (arguments.length > 0)
          maxTermLength = (int) ((Int64Value) arguments[1].head()).longValue();
        if (arguments.length > 1)
          compression = Mode.valueOf(((StringValue) arguments[2].head()).getStringValue());
        indexDir = new File(indexPath);
        if (indexDir.isDirectory())
          throw new XPathException("Index directory \"" + indexDir.getAbsolutePath() + "\" already exists");
        XMLIndex index = new XMLIndex(indexDir.getName(), Paths.get(indexPath), maxTermLength, compression);
        index.close();
        return ZeroOrOne.empty();
      } catch (IOException ioe) {
        throw new XPathException("Could not create index \"" + indexDir.getAbsolutePath() + "\", " + ioe.getMessage(), ioe);
      }  
    }
  }
  
}