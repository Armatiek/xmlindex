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

package nl.armatiek.xmlindex.saxon.optim;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.VennExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexNodeInfo;

public class OptimizableExpression extends VennExpression {

  public OptimizableExpression(Expression origExpr, Expression optExpr) {
    super(origExpr, Token.UNION, optExpr);
  }

  @Override
  public SequenceIterator iterate(XPathContext context) throws XPathException {
    Item item = context.getContextItem();
    if (item instanceof XMLIndexNodeInfo)
      return getRhsExpression().iterate(context);
    return getLhsExpression().iterate(context);
  }
  
  @Override
  public String getExpressionName() {
    return "optimizable-expression";
  }
  
  @Override
  protected String tag() {
    return getExpressionName();
  }
  
  @Override
  public void export(ExpressionPresenter out) throws XPathException {
    out.startElement(tag(), this);
    explainExtraAttributes(out);
    getLhsExpression().export(out);
    getRhsExpression().export(out);
    out.endElement();
  }
  
}