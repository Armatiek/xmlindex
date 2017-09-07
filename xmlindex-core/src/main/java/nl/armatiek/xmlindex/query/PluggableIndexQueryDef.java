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

import net.sf.saxon.expr.oper.OperandArray;
import net.sf.saxon.trace.ExpressionPresenter;
import nl.armatiek.xmlindex.plugins.index.PluggableIndex;

public final class PluggableIndexQueryDef extends QueryDef {
  
  private PluggableIndex index;
  private OperandArray params;
  
  public PluggableIndexQueryDef(PluggableIndex index, OperandArray params) { 
    this.index = index;
    this.params = params;
  }
  
  public PluggableIndex getIndex() {
    return index;
  }
  
  public OperandArray getParams() {
    return params;
  }
  
  @Override
  public void export(ExpressionPresenter dest) {
    dest.startElement("pluggable-index-query");
    dest.emitAttribute("index", index.toString());
    if (params != null) {
      for (int i=0; i<params.getNumberOfOperands(); i++) {
        // TODO
      }
    }
    dest.endElement();
  }
  
}