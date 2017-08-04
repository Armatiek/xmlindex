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

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.lucene.analysis.Analyzer;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.StreamWriterToReceiver;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.TinyBuilder;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.Untyped;
import net.sf.saxon.value.SequenceType;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.conf.PluggableIndex;
import nl.armatiek.xmlindex.conf.PluggableIndexConfig;
import nl.armatiek.xmlindex.conf.TypedValueDef;
import nl.armatiek.xmlindex.conf.TypedValueDefConfig;
import nl.armatiek.xmlindex.conf.VirtualAttributeDef;
import nl.armatiek.xmlindex.conf.VirtualAttributeDefConfig;

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

    private void writeElem(TinyBuilder builder, String localName, String content) throws XPathException {
      builder.startElement(new FingerprintedQName("", "", localName), 
          Untyped.getInstance(), ExplicitLocation.UNKNOWN_LOCATION, 0);
      builder.startContent();
      builder.characters(content, ExplicitLocation.UNKNOWN_LOCATION, 0);
      builder.endElement();
    }
    
    private void copyAnalyzerToBuilder(TinyBuilder builder, Analyzer analyzer, String elemName) throws XMLStreamException, XPathException {
      if (analyzer == null)
        return;
      builder.startElement(new FingerprintedQName("", "", elemName), 
          Untyped.getInstance(), ExplicitLocation.UNKNOWN_LOCATION, 0);
      XMLStreamWriter writer = new StreamWriterToReceiver(builder);
      VirtualAttributeDef.serializeAnalyzer(analyzer, writer);
      writer.flush();
      builder.endElement();
    }
    
    private void writeTypedValueConfig(TinyBuilder builder, XMLIndex index) throws XPathException {
      TypedValueDefConfig config = index.getConfiguration().getTypedValueConfig();
      builder.startElement(new FingerprintedQName("", "", "typed-value-config"), 
          Untyped.getInstance(), ExplicitLocation.UNKNOWN_LOCATION, 0);
      for (TypedValueDef tvd : config.get()) {
        builder.startElement(new FingerprintedQName("", "", "typed-value-def"), 
            Untyped.getInstance(), ExplicitLocation.UNKNOWN_LOCATION, 0);
        writeElem(builder, "node-type", Integer.toString(tvd.getNodeType()));
        writeElem(builder, "node-name", tvd.getQName().getEQName());
        writeElem(builder, "item-type", tvd.getItemType().toString());
        builder.endElement();
      }
      builder.endElement();
    }
    
    private void writeVirtualAttributeConfig(TinyBuilder builder, XMLIndex index) throws XPathException, XMLStreamException {
      VirtualAttributeDefConfig config = index.getConfiguration().getVirtualAttributeConfig();
      builder.startElement(new FingerprintedQName("", "", "virtual-attribute-config"), 
          Untyped.getInstance(), ExplicitLocation.UNKNOWN_LOCATION, 0);
      for (VirtualAttributeDef vad : config.get()) {
        builder.startElement(new FingerprintedQName("", "", "virtual-attribute-def"), 
            Untyped.getInstance(), ExplicitLocation.UNKNOWN_LOCATION, 0);
        writeElem(builder, "element-name", vad.getElemName().getEQName());
        writeElem(builder, "virtual-attribute-name", vad.getVirtualAttributeName());
        writeElem(builder, "function-name", vad.getFunctionName().getEQName());
        writeElem(builder, "item-type", vad.getItemType().toString());
        copyAnalyzerToBuilder(builder, vad.getIndexAnalyzer(), "index-analyzer");
        copyAnalyzerToBuilder(builder, vad.getQueryAnalyzer(), "query-analyzer");
        builder.endElement();
      }
      builder.endElement();
    }
    
    private void writePluggableIndexConfig(TinyBuilder builder, XMLIndex index) throws XPathException {
      PluggableIndexConfig config = index.getConfiguration().getPluggableIndexConfig();
      builder.startElement(new FingerprintedQName("", "", "pluggable-index-config"), 
          Untyped.getInstance(), ExplicitLocation.UNKNOWN_LOCATION, 0);
      for (PluggableIndex pi : config.get()) {
        writeElem(builder, "class-name", pi.getClass().getName());
        for (Map.Entry<String, String> entry : pi.getParams().entrySet()) {
          builder.startElement(new FingerprintedQName("", "", "param"), 
              Untyped.getInstance(), ExplicitLocation.UNKNOWN_LOCATION, 0);
          builder.attribute(new FingerprintedQName("", "", "name"), BuiltInAtomicType.UNTYPED_ATOMIC, entry.getKey(), ExplicitLocation.UNKNOWN_LOCATION, 0);
          builder.attribute(new FingerprintedQName("", "", "value"), BuiltInAtomicType.UNTYPED_ATOMIC, entry.getValue(), ExplicitLocation.UNKNOWN_LOCATION, 0);
          builder.endElement();
        }
      }
      builder.endElement();
    }
    
    @Override
    public NodeInfo call(XPathContext context, Sequence[] arguments) throws XPathException {            
      try {
        XMLIndex index = getSession(context).getIndex();
        PipelineConfiguration pipe = context.getConfiguration().makePipelineConfiguration();       
        TinyBuilder builder = new TinyBuilder(pipe);
        builder.open();
        builder.startElement(new FingerprintedQName("", "", "index-configuration"), 
            Untyped.getInstance(), ExplicitLocation.UNKNOWN_LOCATION, 0);
        writeElem(builder, "name", index.getIndexName());
        writeElem(builder, "path", index.getIndexPath().toString());
        writeElem(builder, "max-term-length", Integer.toString(index.getMaxTermLength()));
        writeElem(builder, "compression", index.getIndexCompression().toString().toLowerCase());
        writeTypedValueConfig(builder, index);
        writeVirtualAttributeConfig(builder, index);
        writePluggableIndexConfig(builder, index);
        builder.endElement();
        builder.close();
        return builder.getCurrentRoot();
      } catch (XPathException e) {
        throw e;
      } catch (Exception e) {
        throw new XPathException(e.getMessage(), e);
      }
    }
  }
  
}