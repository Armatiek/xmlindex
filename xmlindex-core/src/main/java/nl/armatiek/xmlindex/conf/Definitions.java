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

import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.QName;

public class Definitions {
  
  public static final String FILENAME_NAMES             = "names.txt";
  public static final String FILENAME_PREFIXES          = "prefixes.txt";
  public static final String FILENAME_VIRTATTRMODULE    = "virtual-attributes.xqy";
  public static final String FILENAME_PROPERTIES        = "configuration.properties";
  public static final String FILENAME_INDEXCONFIG       = "index-configuration.xml";
  public static final String FILENAME_INDEXCONFIG_XSD   = "index-configuration.xsd";
  
  public static final String FOLDERNAME_CONF            = "conf";
  public static final String FOLDERNAME_XSD             = "xsd";
  public static final String FOLDERNAME_ANALYZER        = "analyzer";
  public static final String FOLDERNAME_NAMESTORE       = "namestore";
  public static final String FOLDERNAME_NODESTORE       = "nodestore";
  
  public static final String ELEMNAME_ROOT              = "xmlindex-root";
  
  public static final long MAX_LONG                     = Long.MAX_VALUE - 1024;
  
  public static final String FIELDNAME_TYPE             = "type";
  public static final String FIELDNAME_LEFT             = "left";
  public static final String FIELDNAME_RIGHT            = "right";
  public static final String FIELDNAME_PARENT           = "parent";
  public static final String FIELDNAME_DEPTH            = "depth";
  public static final String FIELDNAME_PREFIXES         = "prefixes";
  public static final String FIELDNAME_VALUE            = "value";
  public static final String FIELDNAME_FIELDNAMES       = "fieldnames";
  
  public static final String FIELDNAME_URI              = "uri";
  public static final String FIELDNAME_PATH             = "path";
  public static final String FIELDNAME_PARENTPATH       = "parentpath";
  public static final String FIELDNAME_DIRID            = "dirid";
  public static final String FIELDNAME_PARENTDIRID      = "parentdirid";
  
  public static final String FIELDNAME_BASEURI          = "baseuri";
  public static final String FIELDNAME_DOCLEFT          = "docleft";
  public static final String FIELDNAME_DOCRIGHT         = "docright";
  
  /* TypedValueDef, VirtualAttributeDef and CustomIndex fields: */
  /*
  public static final String FIELDNAME_DEFNAME          = "defname";
  public static final String FIELDNAME_ITEMTYPE         = "itemtype";
  public static final String FIELDNAME_NODETYPE         = "nodetype";
  public static final String FIELDNAME_NODENAMESPACEURI = "nodenamespaceuri";
  public static final String FIELDNAME_NODELOCALNAME    = "nodelocalname";
  public static final String FIELDNAME_VIRTATTRNAME     = "virtattrname";
  public static final String FIELDNAME_FUNCNAMESPACEURI = "funcnamespaceuri";
  public static final String FIELDNAME_FUNCLOCALNAME    = "funclocalname";
  public static final String FIELDNAME_ANALYZERINDEX    = "analyzerindex";
  public static final String FIELDNAME_ANALYZERQUERY    = "analyzerquery";
  public static final String FIELDNAME_CLASSNAME        = "classname";
  public static final String FIELDNAME_PARAMS           = "params";
  
  public static final String FIELDNAME_INDEXINFO        = "indexinfo";
  public static final String FIELDNAME_NODECOUNTER      = "nodecounter";
  
  public static final String FIELDVALUE_NODECOUNTER     = "nodecounter";
  public static final String FIELDVALUE_TYPEDVALDEF     = "typedvaldef";
  public static final String FIELDVALUE_VIRTATTRDEF     = "virtattrdef";
  public static final String FIELDVALUE_PLUGGABLEINDEX  = "pluggableindex";
  public static final String FIELDVALUE_PROPERTY        = "property";
  
  public static final long FIELVALUE_NODECOUNTER_LEFT   = MAX_LONG - 1;
  public static final long FIELVALUE_CONFIG_LEFT        = FIELVALUE_NODECOUNTER_LEFT - 1;
  */
  
  public static final String FIELDNAME_INDEXINFO        = "indexinfo";
  public static final String FIELDNAME_NODECOUNTER      = "nodecounter";
  
  public static final String FIELDVALUE_NODECOUNTER     = "nodecounter";
  public static final long FIELVALUE_NODECOUNTER_LEFT   = MAX_LONG - 1;
  public static final String PROPNAME_INDEX_COMPRESSION = "index-compression";
  public static final String PROPNAME_MAX_TERM_LENGTH   = "max-term-length";
  
  public static final String SCHEME_XMLINDEX            = "xmlindex";
  
  public static final String NAMESPACE_VIRTUALATTR      = "http://www.armatiek.nl/xmlindex/virtualattribute";
  public static final String NAMESPACE_EXT_FUNCTIONS    = "http://www.armatiek.nl/xmlindex/functions";
  
  public static final StructuredQName PARAM_SESSION_SQN = new StructuredQName("", "http://www.armatiek.nl/xmlindex/param/session", "session");
  public static final QName PARAM_SESSION_QN            = new QName("http://www.armatiek.nl/xmlindex/param/session", "session");
  
  public static final String EOL                        = System.getProperty("line.separator");
  
}