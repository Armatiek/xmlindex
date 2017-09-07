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

package nl.armatiek.xmlindex.extensions;

import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.value.SequenceType;
import nl.armatiek.xmlindex.plugins.index.PluggableIndexExtensionFunctionCall;

/**
 * 
 * @author Maarten Kroon
 */
public class SpatialIndexExtensionFunctionDefinition extends ExtensionFunctionDefinition {
  
  private static final String NAMESPACEURI_XMLINDEX_FX_SPATIAL = "http://www.armatiek.nl/xmlindex/functions/spatial";
  
  private static final StructuredQName qName = 
      new StructuredQName("", NAMESPACEURI_XMLINDEX_FX_SPATIAL, "spatial-query");

  @Override
  public StructuredQName getFunctionQName() {
    return qName;
  }

  @Override
  public int getMinimumNumberOfArguments() {
    return 3;
  }

  @Override
  public int getMaximumNumberOfArguments() {
    return 3;
  }

  @Override
  public SequenceType[] getArgumentTypes() {
    return new SequenceType[] { SequenceType.SINGLE_NODE, SequenceType.SINGLE_STRING, SequenceType.SINGLE_NODE };
  }

  @Override
  public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
    return SequenceType.OPTIONAL_BOOLEAN;
  }

  @Override
  public PluggableIndexExtensionFunctionCall makeCallExpression() {
    return new SpatialIndexExtensionFunctionCall();
  }
  
}