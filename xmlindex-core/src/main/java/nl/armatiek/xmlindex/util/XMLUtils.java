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

package nl.armatiek.xmlindex.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.sf.saxon.type.Type;

/**
 * Helper class containing several XML/DOM/JAXP related methods.  
 * 
 * @author Maarten Kroon
 */
public class XMLUtils {
  
  public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance(); 
    docFactory.setNamespaceAware(true);
    docFactory.setValidating(false);
    return docFactory.newDocumentBuilder();
  }
  
  
  public static Document stringToDocument(String xml) throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilder builder = getDocumentBuilder();
    return builder.parse(new InputSource(new StringReader(xml)));
  }
  
  public static Element createElemWithText(Document ownerDoc, String tagName, String text) {
    Element newElem = ownerDoc.createElement(tagName);
    newElem.appendChild(ownerDoc.createTextNode(text));
    return newElem;
  }
  
  public static String nodeToString(Node node) throws Exception {
    if (node == null) {
      return null;
    }
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer = factory.newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.setOutputProperty(OutputKeys.INDENT, "no");
    DOMSource source = new DOMSource(node);
    StringWriter sw = new StringWriter();
    StreamResult result = new StreamResult(sw);
    transformer.transform(source, result);
    return sw.toString();
  }
  
  
  public static Document getEmptyDOM() throws ParserConfigurationException {
    DocumentBuilder builder = getDocumentBuilder();
    return builder.newDocument();
  }
  
  protected static void getTextFromNode(Node node, StringBuffer buffer, boolean addSpace) {
    switch (node.getNodeType()) {
      case Node.CDATA_SECTION_NODE: 
      case Node.TEXT_NODE:
        buffer.append(node.getNodeValue());
        if (addSpace)
          buffer.append(" ");
    }
    Node child = node.getFirstChild();
    while (child != null) {
      getTextFromNode(child, buffer, addSpace);
      child = child.getNextSibling();
    }
  }
  
  public static String getTextFromNode(Node node, String defaultValue) {
    if (node == null) {
      return defaultValue;
    }
    if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
      return ((Attr)node).getValue();
    } else if (node.getNodeType() == Node.TEXT_NODE) {
      return node.getNodeValue();
    } else {
      StringBuffer text = new StringBuffer();
      getTextFromNode(node, text, true);
      return text.toString().trim();
    }
  }

  public static String getTextFromNode(Node node) {
    return getTextFromNode(node, null);
  }
  
  
  public static String getLocalName(Node node) {
    if (node.getPrefix() == null)
      return node.getNodeName();
    else
      return node.getLocalName();
  }
  
  public static Element getChildElementByLocalName(Element parentElem, String localName) {
    Node child = parentElem.getFirstChild();
    while (child != null) {
      if ((child.getNodeType() == Node.ELEMENT_NODE) && getLocalName(child).equals(localName)) {
        return (Element) child;
      }
      child = child.getNextSibling();
    }
    return null;
  }
    
  public static Element getFirstChildElement(Element parentElem) {
    if (parentElem == null) {
      return null;
    }
    Node child = parentElem.getFirstChild();
    while (child != null) {
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        return (Element) child;
      }
      child = child.getNextSibling();
    }
    return null;
  }
 
  public static String getValueOfChildElementByLocalName(Element parentElem, String localName) {
    Element childElem = getChildElementByLocalName(parentElem, localName);
    return (childElem != null) ? getTextFromNode(childElem) : null;
  }
  
  public static Node getPreviousSiblingElement(Node node) {
    if (node == null)
      return null;
    Node prevSibling = node.getPreviousSibling();
    while ((prevSibling != null) && (prevSibling.getNodeType() != Node.ELEMENT_NODE))
      prevSibling = prevSibling.getPreviousSibling();
    if ((prevSibling != null) && (prevSibling.getNodeType() == Node.ELEMENT_NODE))
      return prevSibling;
    return null;
  }
  
  public static Element getNextSiblingElement(Node node) {
    if (node == null)
      return null;
    Node nextSibling = node.getNextSibling();
    while ((nextSibling != null) && (nextSibling.getNodeType() != Node.ELEMENT_NODE))
      nextSibling = nextSibling.getNextSibling();
    if ((nextSibling != null) && (nextSibling.getNodeType() == Node.ELEMENT_NODE))
      return (Element) nextSibling;
    return null;
  }
  
  public static Element getFirstChildElementByLocalName(Element parentElem, String localName) {
    Node child = parentElem.getFirstChild();
    while (child != null) {
      if ((child.getNodeType() == Node.ELEMENT_NODE) && getLocalName(child).equals(localName)) {
        return (Element) child;
      }
      child = child.getNextSibling();
    }
    return null;
  }
  
  public static int getNodePosition(Node node) {
    int index = 0;
    Node tmp = node;
    while (true) {
      tmp = tmp.getPreviousSibling();
      if (tmp == null)
        break;
      ++index;
    }
    return index;
  }
  
  public static boolean containsNode(Node parent, Node child) {
    Node tmp = parent.getFirstChild();
    while (tmp != null) {
      if (tmp.equals(child))
        return true; 
      tmp = tmp.getNextSibling();
    }
    return false;
  }
  
  public static boolean getBooleanValue(String value, boolean defaultValue) {
    if (StringUtils.isBlank(value)) {
      return defaultValue;
    }
    return (value.equals("true") || value.equals("1"));
  }
  
  public static int getIntegerValue(String value, int defaultValue) {
    if (StringUtils.isBlank(value)) {
      return defaultValue;
    }
    return Integer.parseInt(value);
  }
  
  public static String getDateTimeString(Date dateTime) { 
    Calendar cal = Calendar.getInstance();
    if (dateTime != null) {
      cal.setTime(dateTime);
    } 
    return DatatypeConverter.printDateTime(cal);   
  }
  
  public static String getDateTimeString() {
    return getDateTimeString(new Date());           
  }
  
  public static int toNodeType(String type) {
    switch (type) {
    case "document-node()":
      return Type.DOCUMENT;
    case "element()":
      return Type.ELEMENT;
    case "attribute()":
      return Type.ATTRIBUTE;
    case "comment()":
      return Type.COMMENT;
    case "text()":
      return Type.TEXT;
    case "processing-instruction()":
      return Type.PROCESSING_INSTRUCTION;
    case "namespace-node()":
      return Type.NAMESPACE;
    default:
      return 0;
    }
  }
  
}