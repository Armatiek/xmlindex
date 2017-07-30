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
import net.sf.saxon.type.ItemType;

public final class ComparisonQueryDef extends QueryDef {
  
  private String fieldName;
  private ItemType itemType;
  private int operator;
  private Expression valueExpression;
  
  public ComparisonQueryDef(String fieldName, ItemType itemType, 
      int operator, Expression valueExpression) {
    this.fieldName = fieldName;
    this.itemType = itemType;
    this.operator = operator;
    this.valueExpression = valueExpression;
  }
    
  public String getFieldName() {
    return fieldName;
  }
  
  public ItemType getItemType() {
    return itemType;
  }

  public int getOperator() {
    return operator;
  }
  
  public Expression getValueExpression() {
    return valueExpression;
  }
  
}