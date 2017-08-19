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
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.type.ItemType;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.saxon.util.SaxonUtils;

public class ComparisonQueryDef extends QueryDefWithRelation {
  
  private XMLIndex index;
  private String fieldName;
  private ItemType itemType;
  private int operator;
  private Expression valueExpression;
  
  public ComparisonQueryDef(XMLIndex index, String fieldName, ItemType itemType, 
      int operator, Expression valueExpression, int relation) {
    super(relation);
    this.index = index;
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
  
  @Override
  public void export(ExpressionPresenter destination) {
    destination.startElement("comparison-query");
    destination.emitAttribute("name-code", fieldName);
    destination.emitAttribute("field-name", getEQName(index, fieldName));
    destination.emitAttribute("node-type", getNodeDisplayName(fieldName));
    if (itemType != null)
      destination.emitAttribute("item-type", itemType.toString());
    destination.emitAttribute("operator",SaxonUtils.token2String(operator));
    destination.emitAttribute("value", valueExpression.toString());
    destination.emitAttribute("relation", (getRelation() == RELATION_ATTR) ? "attribute" : "child-element");
    destination.endElement();
  }
  
}