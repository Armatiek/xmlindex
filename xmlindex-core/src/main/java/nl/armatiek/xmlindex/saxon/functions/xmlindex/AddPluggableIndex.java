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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.ma.map.KeyValuePair;
import net.sf.saxon.ma.map.MapType;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.conf.PluggableIndex;

/**
 * 
 * @author Maarten Kroon
 */
public class AddPluggableIndex extends ExtensionFunctionDefinition {
  
  private static final StructuredQName qName = 
      new StructuredQName("", Definitions.NAMESPACE_EXT_FUNCTIONS, "add-pluggable-index");

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
    return new SequenceType[] { 
        SequenceType.SINGLE_STRING, 
        SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE),
        SequenceType.SINGLE_BOOLEAN };
  }

  @Override
  public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
    return SequenceType.SINGLE_BOOLEAN;
  }

  @Override
  public XMLIndexExtensionFunctionCall makeCallExpression() {
    return new AddPluggableIndexCall();
  }
  
  @Override
  public boolean hasSideEffects() {
    return true;
  }
  
  private static class AddPluggableIndexCall extends XMLIndexExtensionFunctionCall {
    
    private Map<String, String> mapToParams(HashTrieMap map) throws XPathException {
      if (map == null)
        return null;
      HashMap<String, String> params = new HashMap<String, String>();
      Iterator<KeyValuePair> iter =  map.iterator();
      while (iter.hasNext()) {
        KeyValuePair pair = iter.next();
        params.put(((StringValue) pair.key).getStringValue(), SequenceTool.getStringValue(pair.value));
      }
      return params;
    }

    @Override
    public BooleanValue call(XPathContext context, Sequence[] arguments) throws XPathException {            
      try {
        String className = ((StringValue) arguments[0].head()).getStringValue();
        Map<String, String> params = null;
        if (arguments.length > 1)
          params = mapToParams((HashTrieMap) arguments[1].head());
        boolean reindex = false;
        if (arguments.length > 2)
          reindex = ((BooleanValue) arguments[2].head()).getBooleanValue();
        XMLIndex index = getSession(context).getIndex();
        PluggableIndex pluggableIndex = PluggableIndex.fromClassName(index, className, params);
        index.getConfiguration().getPluggableIndexConfig().add(pluggableIndex);
        if (reindex)
          pluggableIndex.reindex();
        return BooleanValue.TRUE;
      } catch (XPathException e) {
        throw e;
      } catch (Exception e) {
        throw new XPathException(e.getMessage(), e);
      }
    }
  }
  
}