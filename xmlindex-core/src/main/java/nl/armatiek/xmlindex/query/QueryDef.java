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

package nl.armatiek.xmlindex.query;

import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import nl.armatiek.xmlindex.XMLIndex;

public abstract class QueryDef {
  
  public abstract void export(ExpressionPresenter destination) throws XPathException;
  
  protected String getEQName(XMLIndex index, String fieldName) {
    int nameCode = Integer.parseInt(fieldName.split("_")[1]);
    StructuredQName name = index.getNameStore().getStructuredQName(nameCode);
    return name.getEQName();
  }
  
  protected String getNodeDisplayName(String fieldName) {
    int nodeType = Integer.parseInt(fieldName.split("_")[0]);
    switch (nodeType) {
      case 1: 
        return "element()";
      case 2:
        return "attribute()";
      default:
        return "unknown";
    }  
  }
  
}