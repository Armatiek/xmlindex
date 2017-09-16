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

import java.io.StringReader;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.event.StreamWriterToReceiver;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.stax.XMLStreamWriterDestination;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.TinyBuilder;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.saxon.functions.ExtensionFunctionCall;

/**
 * 
 * @author Maarten Kroon
 */
public class ExplainTransformation extends ExtensionFunctionDefinition {
  
  private static final Logger logger = LoggerFactory.getLogger(ExplainTransformation.class);
    
  private static final StructuredQName qName = 
      new StructuredQName("", Definitions.NAMESPACE_EXT_FUNCTIONS, "explain-transformation");

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
    return 2;
  }

  @Override
  public SequenceType[] getArgumentTypes() {
    return new SequenceType[] { 
        SequenceType.SINGLE_STRING,
        SequenceType.SINGLE_BOOLEAN };
  }

  @Override
  public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
    return SequenceType.ANY_SEQUENCE;
  }

  @Override
  public ExtensionFunctionCall makeCallExpression() {
    return new ExplainTransformationCall();
  }
    
  private static class ExplainTransformationCall extends TransformOrQueryCall {

    @Override
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {            
      Session session = getSession(context);
      String xsl = ((StringValue) arguments[0].head()).getStringValue();
      boolean throwErrors = true;
      BooleanValue bool;
      if (arguments.length > 1 && (bool = (BooleanValue) arguments[1].head()) != null)
        throwErrors = bool.getBooleanValue();
      TinyBuilder builder = new TinyBuilder(context.getConfiguration().makePipelineConfiguration());
      try {
        try {
          XMLStreamWriter xsw = new StreamWriterToReceiver(builder);
          XMLStreamWriterDestination dest = new XMLStreamWriterDestination(xsw);
          ErrorListener errorListener = new ErrorListener("on index \"" + session.getIndex().getIndexName() + "\"");            
          try {
            session.transform(new StreamSource(new StringReader(xsl)), dest, null, errorListener, null, null, true);
          } catch (SaxonApiException sae) {
            XPathException xpe = this.getXPathException(errorListener, sae);
            if (xpe != null)
              throw xpe;
            else
              throw sae;
          }
          NodeInfo root = builder.getCurrentRoot();
          return (root != null) ? root : EmptySequence.getInstance();
        } catch (XPathException xpe) {
          throw xpe;
        } catch (SaxonApiException | XMLIndexException e) {
          if (e.getCause() instanceof XPathException)
            throw (XPathException) e.getCause();
          else 
            throw new XPathException(e.getMessage(), e);
        } catch (Exception e) {
          throw new XPathException(e.getMessage(), e);
        }
      } catch (Exception e) {
        if (throwErrors)
          throw e;
        logger.error("Error explaining transformation on index \"" + session.getIndex().getIndexName() + "\"", e);
        return getErrorResults(builder, e, false);
      }
    }
  }
  
}