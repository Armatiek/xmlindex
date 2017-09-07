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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.BooleanClause.Occur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.AndExpression;
import net.sf.saxon.expr.AtomicSequenceConverter;
import net.sf.saxon.expr.Atomizer;
import net.sf.saxon.expr.AttributeGetter;
import net.sf.saxon.expr.AxisExpression;
import net.sf.saxon.expr.BooleanExpression;
import net.sf.saxon.expr.CardinalityChecker;
import net.sf.saxon.expr.CastExpression;
import net.sf.saxon.expr.CastingExpression;
import net.sf.saxon.expr.ComparisonExpression;
import net.sf.saxon.expr.ContextItemExpression;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.FilterExpression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.LocalVariableReference;
import net.sf.saxon.expr.Operand;
import net.sf.saxon.expr.OrExpression;
import net.sf.saxon.expr.SlashExpression;
import net.sf.saxon.expr.SystemFunctionCall;
import net.sf.saxon.expr.VariableReference;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.Optimizer;
import net.sf.saxon.expr.parser.RebindingMap;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.functions.Count;
import net.sf.saxon.functions.IntegratedFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.conf.TypedValueDef;
import nl.armatiek.xmlindex.conf.VirtualAttributeDef;
import nl.armatiek.xmlindex.error.OptimizationFailureException;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.plugins.index.PluggableIndex;
import nl.armatiek.xmlindex.plugins.index.PluggableIndexExtensionFunctionCall;
import nl.armatiek.xmlindex.query.BooleanQueryDef;
import nl.armatiek.xmlindex.query.ComparisonQueryDef;
import nl.armatiek.xmlindex.query.PluggableIndexQueryDef;
import nl.armatiek.xmlindex.query.ExistsQueryDef;
import nl.armatiek.xmlindex.query.FullTextQueryDef;
import nl.armatiek.xmlindex.query.QueryDef;
import nl.armatiek.xmlindex.query.QueryDefWithRelation;
import nl.armatiek.xmlindex.query.StringFunctionQueryDef;
import nl.armatiek.xmlindex.saxon.axis.FilterNodeTest;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.LocalVariableReferencer;

public class XMLIndexOptimizer extends Optimizer {
  
  private static final Logger logger = LoggerFactory.getLogger(XMLIndexOptimizer.class);

  private XMLIndex index;
  
  public XMLIndexOptimizer(XMLIndex index, Configuration config) {
    super(config);
    this.index = index;
  }
    
  private boolean isChildAxisExpressionWithElementNameTest(Expression expr) {
    AxisExpression ae;
    return (expr instanceof AxisExpression) && 
        ((ae = (AxisExpression) expr)).getAxis() == AxisInfo.CHILD && isElementNameTest(ae.getNodeTest());
  }
  
  private boolean isAttributeAxisExpressionWithAttributeNameTest(Expression expr) {
    AxisExpression ae;
    return (expr instanceof AxisExpression) && 
        ((ae = (AxisExpression) expr)).getAxis() == AxisInfo.ATTRIBUTE && isAttributeNameTest(ae.getNodeTest());
  }
  
  private boolean isElementNameTest(NodeTest nodeTest) {
    return (nodeTest instanceof NameTest) && ((NameTest) nodeTest).getNodeKind() == Type.ELEMENT;
  }
  
  private boolean isAttributeNameTest(NodeTest nodeTest) {
    return (nodeTest instanceof NameTest) && ((NameTest) nodeTest).getNodeKind() == Type.ATTRIBUTE;
  }
  
  private int getRelation(Expression expr) {
    if (isChildAxisExpressionWithElementNameTest(expr))
      return QueryDefWithRelation.RELATION_CHILDELEM;
    return QueryDefWithRelation.RELATION_ATTR;
  }
  
  /*
  private int getFieldNamePrefix(Expression expr) {
    if (isChildAxisExpressionWithElementNameTest(expr))
      return Type.ELEMENT;
    return Type.ATTRIBUTE;
  }
  */
  
  private QueryDef booleanExpression2QueryDef(AxisExpression base, BooleanExpression expr, 
      List<LocalVariableReference> localVars) throws XPathException {
    Occur occur;    
    if (expr instanceof AndExpression) {
      occur = Occur.FILTER;
    } else if (expr instanceof OrExpression) {
      occur = Occur.SHOULD;
    } else {
      throw new OptimizationFailureException("Could not convert BooleanExpression to Query. Unsupported BooleanExpression.");
    }    
    BooleanQueryDef booleanQuery = new BooleanQueryDef();
    booleanQuery.add(expression2QueryDef(base, expr.getLhsExpression(), localVars), occur);
    booleanQuery.add(expression2QueryDef(base, expr.getRhsExpression(), localVars), occur);
    return booleanQuery;      
  }
  
  private Expression getValueExpression(Expression operand) {
    if (operand instanceof Literal || operand instanceof VariableReference) {
      return operand;
    }
    return null;
  }
  
  private Expression getFieldExpression(Expression expr) {
    if (expr == null)
      return null;
    if (expr instanceof AttributeGetter) // E[@attr]
      return expr;
    if (expr instanceof AxisExpression && ((AxisExpression) expr).getAxis() == AxisInfo.ATTRIBUTE)  // E[@attr]
      return expr;
    if (expr instanceof ContextItemExpression) // E[.]
      return expr;
    if (isChildAxisExpressionWithElementNameTest(expr))  // E[elem]
      return expr;
    if (expr instanceof Atomizer || 
        expr instanceof CastingExpression || 
        expr instanceof CardinalityChecker || 
        expr instanceof AtomicSequenceConverter) {
      Operand op = expr.operands().iterator().next();
      if (op != null)
        return getFieldExpression(op.getChildExpression());
    }
    /*
    if (expr instanceof AtomicSequenceConverter)
      return getFieldExpression(((AtomicSequenceConverter) expr).getBaseExpression());
    */
    return null;        
  }
  
  private String getFieldName(int nodeType, String namespaceUri, String localPart, ItemType itemType) {
    int nameCode = index.getNameCode(namespaceUri, localPart);
    if (nameCode == -1)
      throw new OptimizationFailureException("Name \"{" + namespaceUri + "}" + localPart + "\" does not exist in NameStore.");
    String itemTypeSuffix = (itemType != null) ? "_" + ((BuiltInAtomicType) itemType).getName() : "";
    return Integer.toString(nodeType) + "_" + Integer.toString(nameCode) + itemTypeSuffix;
  }
  
  private QueryDef comparison2QueryDef(AxisExpression base, ComparisonExpression expr, 
      List<LocalVariableReference> localVars) throws XPathException {        
    int operator = expr.getSingletonOperator();
    switch (operator) {
      case Token.FEQ:
      case Token.EQUALS:
      case Token.FGT:
      case Token.GT:
      case Token.FGE:
      case Token.GE:
      case Token.FLT:  
      case Token.LT:
      case Token.FLE:
      case Token.LE:
        break;
      default:
        throw new OptimizationFailureException("Could not convert ComparisonExpression to Query. "
            + "Operator " + operator + " not supported.");
    }
    
    Expression operand1 = expr.getLhsExpression();
    Expression operand2 = expr.getRhsExpression();
    
    Expression fieldExpression;
    Expression valueExpression;
    
    if ((fieldExpression = getFieldExpression(operand1)) != null) {      
    } else if ((fieldExpression = getFieldExpression(operand2)) == null) {
      throw new OptimizationFailureException("Error determining field expression (" + operand1.toShortString() + " and " + operand2.toShortString() + ")");
    }    
    if ((valueExpression = getValueExpression(operand1)) != null) {      
    } else if ((valueExpression = getValueExpression(operand2)) == null) {
      throw new OptimizationFailureException("Error determining value expression (" + operand1.toShortString() + " and " + operand2.toShortString() + ")");
    }
    
    if (valueExpression instanceof LocalVariableReference)
      localVars.add((LocalVariableReference) valueExpression);
    
    int relation = getRelation(fieldExpression);
    
    String fieldName;
    ItemType itemType = null;
    
    NodeTest baseNodeTest = base.getNodeTest();
    if (!isElementNameTest(baseNodeTest))
      throw new OptimizationFailureException("The base axis expression does not select elements on fully qualified names");
      
    if (fieldExpression instanceof ContextItemExpression) { 
      /* E[.] */
      StructuredQName name = baseNodeTest.getMatchingNodeName();
      String namespaceUri = name.getURI();
      String localPart = name.getLocalPart();
      TypedValueDef tvd = index.getConfiguration().getTypedValueConfig().get(Type.ELEMENT, namespaceUri, localPart);
      if (tvd != null) {
        itemType = tvd.getItemType();
        ItemType valueType = valueExpression.getItemType();
        if (valueType == null || !Type.isSubType((AtomicType) itemType, (AtomicType) valueType))
          throw new XPathException(String.format("Item type of expression %s does not match defined item type for context item \"%s\"", 
              valueExpression.toShortString(), name.getDisplayName()));
      }
      fieldName = getFieldName(Node.ELEMENT_NODE, namespaceUri, localPart, itemType);
    } else if (isAttributeAxisExpressionWithAttributeNameTest(fieldExpression) || fieldExpression instanceof AttributeGetter) {
      /* E[@attr] */
      StructuredQName attrName;
      String namespaceUri;
      String localPart;
      if (fieldExpression instanceof AxisExpression) {
        AxisExpression ae = (AxisExpression) fieldExpression;
        NameTest nodeTest = (NameTest) ae.getNodeTest();
        attrName = nodeTest.getMatchingNodeName();
        namespaceUri = attrName.getURI();
        localPart = attrName.getLocalPart();
      } else {
        attrName = ((AttributeGetter) fieldExpression).getAttributeName().getStructuredQName();
        namespaceUri = attrName.getURI();
        localPart = attrName.getLocalPart();
      }
      
      if (namespaceUri.equals(Definitions.NAMESPACE_VIRTUALATTR)) {
        StructuredQName baseName = baseNodeTest.getMatchingNodeName();
        Map<String, VirtualAttributeDef> attrDefs = index.getConfiguration().getVirtualAttributeConfig().getForElement(new QName(baseName.getURI(), baseName.getLocalPart()));
        VirtualAttributeDef attrDef;
        if (attrDefs == null || (attrDef = attrDefs.get(localPart)) == null)
          throw new XMLIndexException("Virtual attribute definition \"" + localPart + "\" not "
              + "found for contextitem {" + baseName.getURI() + "}" + baseName.getLocalPart());
        // fieldName = attrDef.getVirtualAttributeName();
        fieldName = getFieldName(Node.ATTRIBUTE_NODE, Definitions.NAMESPACE_VIRTUALATTR, attrDef.getVirtualAttributeName(), itemType);
        if (attrDef.getQueryAnalyzer() != null) {
          if (operator != Token.FEQ && operator != Token.EQUALS)
            throw new XPathException(String.format("Only the operator \"=\" is supported in the "
                + "full text search expression \"%s\"", fieldExpression.toShortString()));
          return new FullTextQueryDef(index, fieldName, valueExpression, attrDef.getQueryAnalyzer(), relation);
        }
        itemType = attrDef.getItemType();
      } else {
        TypedValueDef tvd = index.getConfiguration().getTypedValueConfig().get(Type.ATTRIBUTE, attrName.getURI(), attrName.getLocalPart());
        if (tvd != null) {
          itemType = tvd.getItemType();
          ItemType valueType = valueExpression.getItemType();
          if (valueType == null || !Type.isSubType((AtomicType) itemType, (AtomicType) valueType))
            throw new XPathException(String.format("Item type of expression %s does not match defined item type for attribute \"%s\"", 
                valueExpression.toShortString(), attrName.getDisplayName()));
        }
        fieldName = getFieldName(Node.ATTRIBUTE_NODE, namespaceUri, localPart, itemType);
      }
    } else if (isChildAxisExpressionWithElementNameTest(fieldExpression)) {
      AxisExpression ae = (AxisExpression) fieldExpression;
      NameTest nodeTest = (NameTest) ae.getNodeTest();
      StructuredQName elemName = nodeTest.getMatchingNodeName();
      String namespaceUri = elemName.getURI();
      String localPart = elemName.getLocalPart();
      fieldName = getFieldName(Node.ELEMENT_NODE, namespaceUri, localPart, itemType);
      return new ComparisonQueryDef(index, fieldName, itemType, operator, valueExpression, QueryDefWithRelation.RELATION_CHILDELEM);
    } else
      throw new OptimizationFailureException("Could not convert ComparisonExpression \"" + fieldExpression.toString() + "\" to Query; ");
    
    if (itemType != null && !Type.isSubType((AtomicType) itemType, (AtomicType) valueExpression.getItemType())) {
      if (itemType.equals(Type.ITEM))
        logger.warn("A literal or variable in expression \"" + fieldExpression.toShortString() + "\" is untyped; "
            + "please use typed literals and variables to avoid runtime exceptions");
      else
        throw new XPathException("The item type of a typed value or virtual attribute definition in expression "
            + "\"" + fieldExpression.toShortString() + "\" does not match the type of the actual item");
    }
    
    return new ComparisonQueryDef(index, fieldName, itemType, operator, valueExpression, relation);
  }
  
  private QueryDef integratedExtensionCall2QueryDef(IntegratedFunctionCall expr, List<LocalVariableReference> localVars) {
    PluggableIndexExtensionFunctionCall call = (PluggableIndexExtensionFunctionCall) expr.getFunction();
    PluggableIndex pluggableIndex = index.getConfiguration().getPluggableIndexConfig().get(call);
    if (pluggableIndex == null)
      throw new OptimizationFailureException("No pluggable index configured for extension function call \"" + call.getClass() + "\"");
    Iterator<Operand> operands = expr.operands().iterator();
    while (operands.hasNext()) {
      Operand operand = operands.next();
      if (operand.getChildExpression() instanceof LocalVariableReference)
        localVars.add((LocalVariableReference) operand.getChildExpression());
    }
    return new PluggableIndexQueryDef(pluggableIndex, expr.getOperanda());
  }
  
  private String getFieldName(AxisExpression base, Expression fieldExpression) {
    int nodeType;
    String namespaceUri;
    String localPart;
    if (fieldExpression instanceof ContextItemExpression) {
      if (!isElementNameTest(base.getNodeTest()))
        throw new OptimizationFailureException("Optimization of context item expression is only supported if the base axis expression refers to a fully qualified element name");
      NodeTest baseNodeTest = base.getNodeTest();
      StructuredQName name = baseNodeTest.getMatchingNodeName();
      nodeType = Type.ELEMENT;
      namespaceUri = name.getURI();
      localPart = name.getLocalPart();
    } else if (fieldExpression instanceof AttributeGetter) {
      nodeType = Type.ATTRIBUTE;
      FingerprintedQName attrName = ((AttributeGetter) fieldExpression).getAttributeName();
      namespaceUri = attrName.getURI();
      localPart = attrName.getLocalPart();
    } else if (fieldExpression instanceof AxisExpression) {
      AxisExpression ae = (AxisExpression) fieldExpression;
      NodeTest nodeTest = ae.getNodeTest();
      StructuredQName name;
      if (ae.getAxis() == AxisInfo.ATTRIBUTE) {
        if (!(nodeTest instanceof NameTest))
          throw new OptimizationFailureException("Attribute axis expression does contain fully qualified attribute name");  
        name = nodeTest.getMatchingNodeName();
        nodeType = Type.ATTRIBUTE;
      } else if (ae.getAxis() == AxisInfo.CHILD) {
        if (!(isElementNameTest(nodeTest)))
          throw new OptimizationFailureException("Child axis expression does not contain fully qualified element name"); 
        name = nodeTest.getMatchingNodeName();
        nodeType = Type.ELEMENT;
      } else 
        throw new OptimizationFailureException("Predicate expression is not an attribute or child axis expression");
      namespaceUri = name.getURI();
      localPart = name.getLocalPart();
    } else
      throw new OptimizationFailureException("Could not determine fieldname");
    return getFieldName(nodeType, namespaceUri, localPart, null);
  }
  
  private QueryDef existsCall2QueryDef(AxisExpression base, Expression functionExpr) {
    Expression predicate = functionExpr.operands().iterator().next().getChildExpression();
    String fieldName = getFieldName(base, predicate);
    return new ExistsQueryDef(index, fieldName, getRelation(predicate));
  }
  
  private QueryDef stringFunctionCall2QueryDef(AxisExpression base, Expression functionExpr, 
      String functionName, List<LocalVariableReference> localVars) {
    Iterator<Operand> operands = functionExpr.operands().iterator();
    Expression fieldExpression = getFieldExpression(operands.next().getChildExpression());
    Expression valueExpression = operands.next().getChildExpression();
    if (valueExpression instanceof LocalVariableReference)
      localVars.add((LocalVariableReference) valueExpression);
    String fieldName = getFieldName(base, fieldExpression);
    return new StringFunctionQueryDef(index, fieldName, valueExpression, functionName, getRelation(fieldExpression));
  }
  
  private QueryDef expression2QueryDef(AxisExpression base, Expression expr, 
      List<LocalVariableReference> localVars) throws XPathException {
    if (expr instanceof AndExpression || expr instanceof OrExpression) {
      return booleanExpression2QueryDef(base, (BooleanExpression) expr, localVars);          
    } else if (expr instanceof ComparisonExpression) {
      return comparison2QueryDef(base, (ComparisonExpression) expr, localVars);
    } else if (expr instanceof IntegratedFunctionCall) {
      return integratedExtensionCall2QueryDef((IntegratedFunctionCall) expr, localVars);
    } else if (expr.isCallOnSystemFunction("exists")) {
      return existsCall2QueryDef(base, expr);
    } else if (expr.isCallOnSystemFunction("starts-with")) {
      return stringFunctionCall2QueryDef(base, expr, "starts-with", localVars);
    } else if (expr.isCallOnSystemFunction("ends-with")) {
      return stringFunctionCall2QueryDef(base, expr, "ends-with", localVars);
    } else if (expr.isCallOnSystemFunction("contains")) {
      return stringFunctionCall2QueryDef(base, expr, "contains", localVars);
    }
    
    throw new OptimizationFailureException("Error converting Expression to Query; expression type \"" + expr.getClass() + "\" is not supported to be optimized");    
  }
  
  public boolean isAtomizer(Expression expr) {
    if (expr instanceof Atomizer)
      return true;
    if (expr instanceof CastExpression) {
      Expression castedExpression = ((CastExpression) expr).operands().iterator().next().getChildExpression();
      if (castedExpression instanceof Atomizer)
        return true;
    }
    return false;
  }
  
  private boolean partOfOptimizableExpression(Expression filter) {
    while ((filter = filter.getParentExpression()) != null) {
      if (filter instanceof OptimizableExpression)
        return true;
    }
    return false;
  }

  @Override
  public int isIndexableFilter(Expression filter) {  
    if (partOfOptimizableExpression(filter))
      return 0;
    else if (filter instanceof ComparisonExpression) {
      ComparisonExpression comp = (ComparisonExpression) filter;
      Expression lhs = comp.getLhsExpression();
      Expression rhs = comp.getRhsExpression();
      if (getFieldExpression(lhs) != null && getValueExpression(rhs) != null)
        return 1;
      else if (getFieldExpression(rhs) != null && getValueExpression(lhs) != null)
        return -1;
      return 0;
    } else if (filter instanceof IntegratedFunctionCall && 
        ((IntegratedFunctionCall) filter).getFunction() instanceof PluggableIndexExtensionFunctionCall) {
      return 1; 
    } else if (filter.isCallOnSystemFunction("exists") || filter.isCallOnSystemFunction("starts-with") || 
        filter.isCallOnSystemFunction("ends-with") || filter.isCallOnSystemFunction("contains") ) {
      Operand op = filter.operands().iterator().next();
      if (getFieldExpression(op.getChildExpression()) != null)
        return 1;
      return 0; 
    } else if (filter instanceof BooleanExpression) {
      BooleanExpression bool = (BooleanExpression) filter;
      Expression lhs = bool.getLhsExpression();
      Expression rhs = bool.getRhsExpression();
      if (isIndexableFilter(lhs) != 0 && isIndexableFilter(rhs) != 0)
        return 1;
      return 0;
    }  
    return 0;
  }

  @Override
  public Expression tryIndexedFilter(FilterExpression f, ExpressionVisitor visitor, boolean indexFirstOperand, boolean contextIsDoc) {
    if (!(f.getBase() instanceof AxisExpression)) 
      return super.tryIndexedFilter(f, visitor, indexFirstOperand, contextIsDoc);
    String exprStr = f.toShortString();
    try {
      AxisExpression base = (AxisExpression) f.getBase();
      ArrayList<LocalVariableReference> localVars = new ArrayList<LocalVariableReference>(); 
      QueryDef filterQueryDef = expression2QueryDef(base, f.getFilter(), localVars);
      ContextPassingAxisExpression newBase = new ContextPassingAxisExpression(index, base.getAxis(), 
          new FilterNodeTest(base.getNodeTest(), filterQueryDef));
      newBase.setRetainedStaticContext(base.getRetainedStaticContext());
      newBase.setEvaluationMethod(base.getEvaluationMethod());
      newBase.setFiltered(false);
      newBase.setLocation(base.getLocation());
      Expression newExpr;
      if (!localVars.isEmpty()) {
        ArrayList<LocalVariableReference> localVarsCopies = new ArrayList<LocalVariableReference>();
        for (LocalVariableReference localVar : localVars)
          localVarsCopies.add((LocalVariableReference) localVar.copy(new RebindingMap()));
        
        LocalVariableReferencer function = new LocalVariableReferencer();
        ExtensionFunctionCall call = function.makeCallExpression();
        call.setDefinition(function);
        IntegratedFunctionCall newFilter = new IntegratedFunctionCall(LocalVariableReferencer.qName, call);
        newFilter.setArguments(localVarsCopies.toArray(new Expression[localVarsCopies.size()]));
        
        FilterExpression newFilterExpr = new FilterExpression(newBase, newFilter);
        newFilterExpr.setBase(newBase);
        newFilterExpr.setFilter(newFilter);
        newExpr = newFilterExpr;
      } else {
        newExpr = newBase;
      }
      logger.info("Successfully optimized filter expression '" + exprStr + "'");
      newExpr.setExtraProperty("isOptimized", Boolean.TRUE);
      return new OptimizableExpression(f, newExpr);
    } catch (OptimizationFailureException ofe) {
      logger.info("Unable to optimize filter expression '" + exprStr + "'. " + ofe.getMessage());
      return super.tryIndexedFilter(f, visitor, indexFirstOperand, contextIsDoc);
    } catch (XPathException xpe) {
      throw new XMLIndexException("Error optimizing filter expression '" + exprStr + "'", xpe);
    }
  }
  
  public static void optimize(Expression exp) {
    if (exp instanceof SystemFunctionCall && ((SystemFunctionCall) exp).getTargetFunction() instanceof Count) {
      SystemFunctionCall call = (SystemFunctionCall) exp;
      if (call.getOperanda().getOperandExpression(0) instanceof SlashExpression) {
        SlashExpression se = (SlashExpression) call.getOperanda().getOperandExpression(0);
        if (se.getActionExpression() instanceof OptimizableExpression) {
          // if (BooleanUtils.isTrue((Boolean) se.getActionExpression().getExtraProperty("isOptimized"))) {
          call.getOperanda().setOperand(0, se.getActionExpression());
          return;
        }
      }
    }
    for (Operand info : exp.operands()) {
      Expression childExpression = info.getChildExpression();
      optimize(childExpression);
    }
  }
  
}