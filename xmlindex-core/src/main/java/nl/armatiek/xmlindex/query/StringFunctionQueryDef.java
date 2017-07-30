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

import net.sf.saxon.expr.Expression;

public final class StringFunctionQueryDef extends QueryDef {
  
  private String fieldName;
  private Expression valueExpression;
  private String functionName;
  
  public StringFunctionQueryDef(String fieldName, Expression valueExpression, String functionName) { 
    this.fieldName = fieldName;
    this.valueExpression = valueExpression;
    this.functionName = functionName;
  }
  
  public String getFieldName() {
    return fieldName;
  }
  
  public Expression getValueExpression() {
    return valueExpression;
  }
  
  public String getFunctionName() {
    return functionName;
  }
  
}