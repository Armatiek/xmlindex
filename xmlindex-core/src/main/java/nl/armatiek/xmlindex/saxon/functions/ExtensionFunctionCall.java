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

package nl.armatiek.xmlindex.saxon.functions;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

import net.sf.saxon.Configuration;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.dom.DocumentWrapper;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.Type;

/**
 * Base class providing functionality used in extension function calls 
 * 
 * @author Maarten Kroon
 */
public abstract class ExtensionFunctionCall extends net.sf.saxon.lib.ExtensionFunctionCall {

  protected NodeInfo unwrapNodeInfo(NodeInfo nodeInfo) {
    if (nodeInfo != null && nodeInfo.getNodeKind() == Type.DOCUMENT) {
      nodeInfo = nodeInfo.iterateAxis(AxisInfo.CHILD, NodeKindTest.ELEMENT).next();
    }
    return nodeInfo;
  }
  
  protected void serialize(NodeInfo nodeInfo, Result result, Properties outputProperties) throws XPathException {
    try {
      TransformerFactory factory = new TransformerFactoryImpl();
      Transformer transformer = factory.newTransformer();
      if (outputProperties != null) {
        transformer.setOutputProperties(outputProperties);
      }
      transformer.transform(nodeInfo, result);
    } catch (Exception e) {
      throw new XPathException(e);
    }
  }
  
  protected void serialize(Sequence seq, Writer w, Properties outputProperties) throws XPathException {
    try {
      SequenceIterator iter = seq.iterate(); 
      Item item;
      while ((item = iter.next()) != null) {
        if (item instanceof NodeInfo) {
          serialize((NodeInfo) item, new StreamResult(w), outputProperties);
        } else {
          w.append(item.getStringValue());
        }
      }
    } catch (Exception e) {
      throw new XPathException(e);
    }
  }
  
  protected void serialize(Sequence seq, OutputStream os, Properties outputProperties) throws XPathException {
    String encoding = "UTF-8";
    if (outputProperties != null) {
      encoding = outputProperties.getProperty("encoding", encoding);
    }
    try {
      SequenceIterator iter = seq.iterate(); 
      Item item;
      while ((item = iter.next()) != null) {
        if (item instanceof NodeInfo) {
          serialize((NodeInfo) item, new StreamResult(os), outputProperties);
        } else {
          new OutputStreamWriter(os, encoding).append(item.getStringValue());
        }
      }
    } catch (Exception e) {
      throw new XPathException(e);
    }
  }

  protected String serialize(NodeInfo nodeInfo, Properties props) throws XPathException {
    StringWriter sw = new StringWriter();
    serialize(nodeInfo, new StreamResult(sw), props);
    return sw.toString();
  }
  
  protected String serialize(NodeInfo nodeInfo) throws XPathException {
    Properties props = new Properties();
    props.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    props.setProperty(OutputKeys.METHOD, "xml");
    props.setProperty(OutputKeys.INDENT, "no");
    return serialize(nodeInfo, props);
  }
  
  protected Properties getOutputProperties(NodeInfo paramsElem) {
    Properties props = new Properties();
    paramsElem = unwrapNodeInfo(paramsElem);
    AxisIterator iter = paramsElem.iterateAxis(AxisInfo.CHILD, NodeKindTest.ELEMENT);
    NodeInfo paramElem;
    while ((paramElem = iter.next()) != null) {
      props.put(paramElem.getLocalPart(), paramElem.getAttributeValue("", "value"));
    }  
    return props;
  }
    
  protected NodeInfo source2NodeInfo(Source source, Configuration configuration) {        
    Node node = ((DOMSource)source).getNode();
    String baseURI = source.getSystemId();
    DocumentWrapper documentWrapper = new DocumentWrapper(node.getOwnerDocument(), baseURI, configuration);
    return documentWrapper.wrap(node);            
  }
  
}