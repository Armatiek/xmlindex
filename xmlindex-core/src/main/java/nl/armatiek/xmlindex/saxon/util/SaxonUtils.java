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

package nl.armatiek.xmlindex.saxon.util;

import java.nio.file.attribute.FileTime;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmEmptySequence;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.utils.XMLUtils;

public class SaxonUtils {
  
  public static String token2String(int operator) {
    switch (operator) {
      case Token.FEQ:
      case Token.EQUALS:
        return "=";
      case Token.FGT:
      case Token.GT:
        return ">";
      case Token.FGE:
      case Token.GE:
        return ">=";
      case Token.FLT:  
      case Token.LT:
        return "<";
      case Token.FLE:
      case Token.LE:
        return "<=";
      default:
        return "Unknown: " + Integer.toString(operator);
    }
  }
  
  public static XdmValue convertObjectToXdmValue(Object obj) throws XPathException {
    try {
      if (obj == null)
        return XdmEmptySequence.getInstance();
      if (obj instanceof Date)    
        return new XdmAtomicValue(XMLUtils.getDateTimeString((Date) obj), ItemType.DATE_TIME);
      if (obj instanceof FileTime)
        return new XdmAtomicValue(XMLUtils.getDateTimeString(new Date(((FileTime) obj).toMillis())), ItemType.DATE_TIME);
      return XdmValue.makeValue(obj);
    } catch (Exception e) {
      throw new XPathException("Error converting Java object to XdmValue", e);
    }
  }
  
  public static QName getQName(Node node) {
    String text = node.getTextContent();
    if (StringUtils.isBlank(text))
      return null;
    if (text.contains(":")) {
      String prefix = StringUtils.substringBefore(text, ":");
      String uri = node.lookupNamespaceURI(prefix);
      if (uri == null)
        throw new XMLIndexException("Prefix \"" + prefix + "\" not bound");
      String localName = StringUtils.substringAfter(text, ":");
      return new QName(uri, localName);
    }
    return new QName(text);
  }

}