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

package nl.armatiek.xmlindex.conf;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Class containing all global string identifiers as static final String fields
 * 
 * @author Maarten Kroon
 */
public class WebDefinitions extends Definitions {
  
  public final static String PROJECT_NAME                      = "xmlindex";
  public final static String PROJECT_VERSION                   = "1.0.0-alpha1";
  public final static String FILENAME_PROPERTIES               = "xmlindex.properties";
  public final static String FILENAME_RESTXQ                   = "restxq.xqm";
  //public final static String FILENAME_QUARTZ                   = "xslweb-quartz.properties";
  //public final static String FILENAME_EHCACHE                  = "xslweb-ehcache.xml";
  
  public final static String XML_EXTENSION                     = "xml";
  public final static String XSL_EXTENSION                     = "xsl";
  public final static String XSD_EXTENSION                     = "xsd";
  public final static String STX_EXTENSION                     = "stx";
  public final static String TMP_EXTENSION                     = "tmp";
  public final static String[] XML_EXTENSIONS                  = new String[] {"xml", "xslt", "xsl", "xsd", "stx"};
  
  public static Set<String> xmlExtensions = new HashSet<String>();
  static {
    for (int i=0; i<XML_EXTENSIONS.length; i++) {
      xmlExtensions.add(XML_EXTENSIONS[i]);
    }
  }
  
  public final static String NAMESPACEURI_XML                   = "http://www.w3.org/XML/1998/namespace";
  public final static String NAMESPACEURI_XMLNS                 = "http://www.w3.org/2000/xmlns/";
  public final static String NAMESPACEURI_XSLT                  = "http://www.w3.org/1999/XSL/Transform";
  public final static String NAMESPACEURI_XMLSCHEMA_INSTANCE    = "http://www.w3.org/2001/XMLSchema-instance";
  public final static String NAMESPACEURI_XMLSCHEMA             = "http://www.w3.org/2001/XMLSchema";
  public final static String NAMESPACEURI_XHTML                 = "http://www.w3.org/1999/xhtml";
  public final static String NAMESPACEURI_XLINK                 = "http://www.w3.org/1999/xlink";
  public final static String NAMESPACEURI_XINCLUDE              = "http://www.w3.org/2001/XInclude";
  
  public final static String NAMESPACEURI_EXPATH_FILE           = "http://expath.org/ns/file";
  public final static String NAMESPACEURI_EXPATH_HTTP           = "http://expath.org/ns/http-client";
  
  public final static String NAMESPACE_RESTXQ                   = "http://exquery.org/ns/restxq";
  public final static String NAMESPACE_INPUT                    = "http://exquery.org/ns/restxq";
  public final static String NAMESPACE_OUTPUT                   = "http://www.w3.org/2010/xslt-xquery-serialization";
   
  public final static String MIMETYPE_XML                       = "text/xml";
  public final static String MIMETYPE_HTML                      = "text/html";
  public final static String MIMETYPE_MSWORD                    = "application/msword";
  public final static String MIMETYPE_MSEXCEL                   = "application/vnd.ms-excel";
  public final static String MIMETYPE_MSPOWERPOINT              = "application/vnd.ms-powerpoint";
  public final static String MIMETYPE_PDF                       = "application/pdf";
  public final static String MIMETYPE_ZIP                       = "application/zip";
  public final static String MIMETYPE_OO_TEXT                   = "application/vnd.oasis.opendocument.text";
  public final static String MIMETYPE_OO_TEXTTEMPLATE           = "application/vnd.oasis.opendocument.text-template";
  public final static String MIMETYPE_OO_TEXTWEB                = "application/vnd.oasis.opendocument.text-web";
  public final static String MIMETYPE_OO_TEXTMASTER             = "application/vnd.oasis.opendocument.text-master";
  public final static String MIMETYPE_OO_SPREADSHEET            = "application/vnd.oasis.opendocument.spreadsheet";
  public final static String MIMETYPE_OO_SPREADSHEETTEMPLATE    = "application/vnd.oasis.opendocument.spreadsheet-template";
  public final static String MIMETYPE_OO_PRESENTATION           = "application/vnd.oasis.opendocument.presentation";
  public final static String MIMETYPE_OO_PRESENTATIONTEMPLATE   = "application/vnd.oasis.opendocument.presentation-template";
  public final static String MIMETYPE_BINARY                    = "application/octet-stream";
  public final static String MIMETYPE_JPEG                      = "image/jpeg";
  public final static String MIMETYPE_GIF                       = "image/gif";
  public final static String MIMETYPE_PNG                       = "image/png";
  public final static String MIMETYPE_TEXTPLAIN                 = "text/plain";
  
  public final static String PROPERTYNAME_VERSION               = "xmlindex.version";
  public final static String PROPERTYNAME_TRUST_ALL_CERTS       = "xmlindex.trustallcerts";
  public final static String PROPERTYNAME_PARSER_HARDENING      = "xmlindex.parserhardening";
  public final static String PROPERTYNAME_INDEXPATH             = "xmlindex.indexpath";
  
  public final static String REGEX_PARAM                        = "\\{\\s*\\$(.+?)\\s*\\}";
  public final static Pattern PATTERN_PARAM                     = Pattern.compile(REGEX_PARAM);
  
}