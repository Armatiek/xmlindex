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

import java.nio.file.Path;

import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.TinyBuilder;
import net.sf.saxon.value.SequenceType;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.Definitions;

/**
 * 
 * @author Maarten Kroon
 */
public class GetConfiguration extends ExtensionFunctionDefinition {
  
  private static final StructuredQName qName = 
      new StructuredQName("", Definitions.NAMESPACE_EXT_FUNCTIONS, "get-configuration");

  @Override
  public StructuredQName getFunctionQName() {
    return qName;
  }

  @Override
  public int getMinimumNumberOfArguments() {
    return 0;
  }

  @Override
  public int getMaximumNumberOfArguments() {
    return 0;
  }

  @Override
  public SequenceType[] getArgumentTypes() {
    return new SequenceType[] { };
  }

  @Override
  public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
    return SequenceType.SINGLE_NODE;
  }

  @Override
  public XMLIndexExtensionFunctionCall makeCallExpression() {
    return new GetConfigurationCall();
  }
  
  private static class GetConfigurationCall extends XMLIndexExtensionFunctionCall {

    @Override
    public NodeInfo call(XPathContext context, Sequence[] arguments) throws XPathException {            
      try {
        XMLIndex index = getSession(context).getIndex();
        Path configPath = index.getConfigPath().resolve(Definitions.FILENAME_INDEXCONFIG);
        TransformerFactoryImpl tf = new net.sf.saxon.TransformerFactoryImpl(context.getConfiguration());     
        Transformer transformer = tf.newTransformer();    
        StreamSource source = new StreamSource(configPath.toFile());
        TinyBuilder builder = new TinyBuilder(new PipelineConfiguration(context.getConfiguration()));
        transformer.transform(source, builder);      
        return builder.getCurrentRoot();     
      } catch (XPathException e) {
        throw e;
      } catch (Exception e) {
        throw new XPathException(e.getMessage(), e);
      }
    }
  }
  
}