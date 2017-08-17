package nl.armatiek.xmlindex.saxon.axis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.join.JoinUtil;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.VariableReference;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.oper.OperandArray;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.pattern.AnyChildNodeTest;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.CombinedNodeTest;
import net.sf.saxon.pattern.LocalNameTest;
import net.sf.saxon.pattern.MultipleNodeKindTest;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.pattern.NamespaceTest;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.UType;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.DateTimeValue;
import net.sf.saxon.value.DateValue;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.value.FloatValue;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.TimeValue;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.conf.PluggableIndex;
import nl.armatiek.xmlindex.error.OptimizationFailureException;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.query.BooleanClauseDef;
import nl.armatiek.xmlindex.query.BooleanQueryDef;
import nl.armatiek.xmlindex.query.ComparisonQueryDef;
import nl.armatiek.xmlindex.query.CustomIndexQueryDef;
import nl.armatiek.xmlindex.query.ExistsQueryDef;
import nl.armatiek.xmlindex.query.FullTextQueryDef;
import nl.armatiek.xmlindex.query.QueryDef;
import nl.armatiek.xmlindex.query.QueryDefWithRelation;
import nl.armatiek.xmlindex.query.StringFunctionQueryDef;

public class QueryConstructor {
  
  private static final Logger logger = LoggerFactory.getLogger(QueryConstructor.class);
  
  public static Query getNodeTypeQuery(int nodeType) {
    // return IntPoint.newExactQuery("type", nodeType);
    return new TermQuery(new Term(Definitions.FIELDNAME_TYPE, Integer.toString(nodeType)));
  }
  
  public static Query getNameQuery(int nodeType, String uri, String localName, Session session) {
    int nameCode = session.getNameCode(uri, localName);
    return new TermQuery(new Term(Definitions.FIELDNAME_FIELDNAMES, Integer.toString(nodeType) + "_" + Integer.toString(nameCode)));
  }
  
  public static Query getLocalNameQuery(int nodeType, String localName) {
    throw new UnsupportedOperationException("LocalNameTest not supported");
  }
  
  public static Query getUriQuery(int nodeType, String uri) {
    throw new UnsupportedOperationException("NamespaceTest not supported");
  }
  
  public static void addBooleanClauseDefs(Builder queryBuilder, BooleanQueryDef booleanQueryDef,
      Session session, XPathContext xpathContext) throws XPathException {
    Iterator<BooleanClauseDef> clauses = booleanQueryDef.getClauses();
    while (clauses.hasNext()) {
      BooleanClauseDef clause = clauses.next();
      QueryDef queryDef = clause.getQueryDef();
      Occur occur = clause.getOccur();
      if (queryDef instanceof BooleanQueryDef) {
        Builder nestedQueryBuilder = new BooleanQuery.Builder();
        addBooleanClauseDefs(nestedQueryBuilder, (BooleanQueryDef) queryDef, session, xpathContext);
        queryBuilder.add(new BooleanClause(nestedQueryBuilder.build(), occur));
      } else if (queryDef instanceof QueryDefWithRelation && ((QueryDefWithRelation) queryDef).getRelation() == QueryDefWithRelation.RELATION_CHILDELEM) {
        Query query = childElementQueryDefToQuery((QueryDefWithRelation) queryDef, session, xpathContext);
        queryBuilder.add(new BooleanClause(query, occur));
      } else if (queryDef instanceof ComparisonQueryDef) {
        Query query = comparisonQueryDefToQuery((ComparisonQueryDef) queryDef, session, xpathContext);
        queryBuilder.add(new BooleanClause(query, occur));
      } else if (queryDef instanceof FullTextQueryDef) {
        Query query = fullTextQueryDefToQuery((FullTextQueryDef) queryDef, session, xpathContext);
        queryBuilder.add(new BooleanClause(query, occur));
      } else if (queryDef instanceof ExistsQueryDef) {
        Query query = existsQueryDefToQuery((ExistsQueryDef) queryDef);
        queryBuilder.add(new BooleanClause(query, occur));
      } else if (queryDef instanceof StringFunctionQueryDef) {
        Query query = stringFunctionQueryDefToQuery((StringFunctionQueryDef) queryDef, xpathContext);
        queryBuilder.add(new BooleanClause(query, occur));
      } else if (queryDef instanceof CustomIndexQueryDef) {
        Query query = customIndexQueryDefToQuery((CustomIndexQueryDef) queryDef, session, xpathContext);
        queryBuilder.add(new BooleanClause(query, occur));
      }
    }
  }
  
  public static Sequence expr2Sequence(Expression expr, XPathContext xpathContext) throws XPathException {
    if (expr instanceof Literal)
      return ((Literal) expr).getValue();
    else if (expr instanceof VariableReference)
      return ((VariableReference) expr).evaluateVariable(xpathContext);
    else
      throw new XMLIndexException("Could not convert Expression of type \"" + expr.getClass().getName() + "\" to Sequence, "
          + "only Literal or VariableReferences are supported");
  }
  
  public static Query comparisonQueryDefToQuery(ComparisonQueryDef queryDef, Session session, 
      XPathContext xpathContext) throws XPathException {
    Sequence value = expr2Sequence(queryDef.getValueExpression(), xpathContext);
    return constructQuery(session.getIndex(), queryDef, value);
  }
  
  public static Query childElementQueryDefToQuery(QueryDefWithRelation queryDef, Session session, 
      XPathContext xpathContext) throws XPathException {
    Query fromQuery = null;
    if (queryDef instanceof ComparisonQueryDef) 
      fromQuery = comparisonQueryDefToQuery((ComparisonQueryDef) queryDef, session, xpathContext);
    else if (queryDef instanceof ExistsQueryDef)
      fromQuery = existsQueryDefToQuery((ExistsQueryDef) queryDef);
    else if (queryDef instanceof FullTextQueryDef)
      fromQuery = fullTextQueryDefToQuery((FullTextQueryDef) queryDef, session, xpathContext);
    else if (queryDef instanceof StringFunctionQueryDef)
      fromQuery = stringFunctionQueryDefToQuery((StringFunctionQueryDef) queryDef, xpathContext);
    else 
      throw new XPathException("Unsupported query \"" + queryDef.getClass().toString() + "\"");
    Query joinQuery;
    try {
      joinQuery = JoinUtil.createJoinQuery(
          Definitions.FIELDNAME_PARENT, 
          false, 
          Definitions.FIELDNAME_LEFT, 
          Long.class,
          fromQuery, 
          session.getIndexSearcher(), 
          ScoreMode.None);
    } catch (IOException ioe) {
      throw new XPathException("Error creating join query", ioe);
    }
    return joinQuery;
  }
  
  /*
  public static Query elementComparisonQueryDefToQuery(ComparisonQueryDef queryDef, Session session, 
      XPathContext xpathContext) throws XPathException {
    Query joinQuery;
    try {
      joinQuery = JoinUtil.createJoinQuery(
          Definitions.FIELDNAME_PARENT, 
          false, 
          Definitions.FIELDNAME_LEFT, 
          Long.class,
          comparisonQueryDefToQuery(queryDef, session, xpathContext), 
          session.getIndexSearcher(), 
          ScoreMode.None);
    } catch (IOException ioe) {
      throw new XPathException("Error creating join query", ioe);
    }
    return joinQuery;
  }
  */
  
  public static Query fullTextQueryDefToQuery(FullTextQueryDef queryDef, Session session, 
      XPathContext xpathContext) throws XPathException {
    Sequence value = expr2Sequence(queryDef.getValueExpression(), xpathContext);
    String queryText = SequenceTool.getStringValue(value);
    try {
      QueryParser parser = new QueryParser(queryDef.getFieldName(), queryDef.getAnalyzer());
      return parser.parse(queryText);
    } catch (ParseException pe) {
      throw new XPathException("Error parsing full text query \"" + queryText + "\"", pe);
    }
  }
  
  public static Query existsQueryDefToQuery(ExistsQueryDef queryDef) throws XPathException {
    String fieldName = queryDef.getFieldName();
    return new TermQuery(new Term(Definitions.FIELDNAME_FIELDNAMES, fieldName));
  }
  
  public static Query stringFunctionQueryDefToQuery(StringFunctionQueryDef queryDef, 
      XPathContext xpathContext) throws XPathException {
    Sequence value = expr2Sequence(queryDef.getValueExpression(), xpathContext);
    String queryText = SequenceTool.getStringValue(value);
    if (queryDef.getFunctionName().equals("starts-with"))
      return new PrefixQuery(new Term(queryDef.getFieldName(), queryText));
    queryText = queryText.replaceAll("\\*", "\\\\*");
    queryText = queryText.replaceAll("\\?", "\\\\?");
    switch (queryDef.getFunctionName()) {  
      case "ends-with":
        queryText = "*" + queryText;
        break;
      case "contains":
        queryText = "*" + queryText + "*";
        break;
    }
    return new WildcardQuery(new Term(queryDef.getFieldName(), queryText));
  }
  
  public static Query customIndexQueryDefToQuery(CustomIndexQueryDef queryDef, Session session, 
      XPathContext xpathContext) throws XPathException {
    PluggableIndex pluggableIndex = queryDef.getIndex();
    OperandArray params = queryDef.getParams();
    Sequence[] args = new Sequence[params.getNumberOfOperands()];
    for (int i=0; i<params.getNumberOfOperands(); i++) {
      Expression expr = params.getOperandExpression(i);
      if (expr instanceof VariableReference)
        args[i] = ((VariableReference) expr).evaluateVariable(xpathContext);
      else 
        args[i] = expr.evaluateItem(xpathContext);
    }
    return pluggableIndex.getQuery(args);
  }
  
  public static void addNodeTestClauses(Builder queryBuilder, NodeTest nodeTest, 
      Occur occur, Session session, XPathContext xpathContext) throws XPathException {      
    if (nodeTest == null)
      return;
    if (nodeTest instanceof LocalNameTest) {      
      int nodeKind = ((LocalNameTest) nodeTest).getNodeKind();      
      String name = ((LocalNameTest) nodeTest).getLocalName();
      queryBuilder.add(getLocalNameQuery(nodeKind, name), occur);
    } else if (nodeTest instanceof NamespaceTest) {
      int nodeKind = ((NamespaceTest) nodeTest).getNodeKind();      
      String uri = ((NamespaceTest) nodeTest).getNamespaceURI();
      queryBuilder.add(getUriQuery(nodeKind, uri), occur);
    } else if (nodeTest instanceof NameTest) {
      int nodeKind = ((NameTest) nodeTest).getNodeKind();      
      String localPart = ((NameTest) nodeTest).getLocalPart();   
      String uri = ((NameTest) nodeTest).getNamespaceURI();
      queryBuilder.add(getNameQuery(nodeKind, uri, localPart, session), occur);
    } else if (nodeTest instanceof NodeKindTest)  {
      int nodeKind = ((NodeKindTest) nodeTest).getNodeKind();      
      queryBuilder.add(getNodeTypeQuery(nodeKind), occur);
    } else if (nodeTest instanceof AnyChildNodeTest) {
      Builder orQueryBuilder = new BooleanQuery.Builder();
      orQueryBuilder.add(new BooleanClause(getNodeTypeQuery(Type.ELEMENT), Occur.SHOULD));
      orQueryBuilder.add(new BooleanClause(getNodeTypeQuery(Type.TEXT), Occur.SHOULD));
      orQueryBuilder.add(new BooleanClause(getNodeTypeQuery(Type.COMMENT), Occur.SHOULD));
      orQueryBuilder.add(new BooleanClause(getNodeTypeQuery(Type.PROCESSING_INSTRUCTION), Occur.SHOULD));
      queryBuilder.add(orQueryBuilder.build(), occur);
    } else if (nodeTest instanceof AnyNodeTest) {
      /* No need to add clause */
    } else if (nodeTest instanceof CombinedNodeTest) {
      NodeTest[] nodeTests = ((CombinedNodeTest) nodeTest).getComponentNodeTests();
      int operator = ((CombinedNodeTest) nodeTest).getOperator();
      Builder combiQueryBuilder = new BooleanQuery.Builder();
      Occur occur0;
      Occur occur1;
      if (operator == Token.UNION) {
        occur0 = Occur.SHOULD;
        occur1 = Occur.SHOULD;
      } else if (operator == Token.INTERSECT) {
        occur0 = Occur.FILTER;
        occur1 = Occur.FILTER;
      } else if (operator == Token.EXCEPT) {
        occur0 = Occur.SHOULD;
        occur1 = Occur.MUST_NOT;
      } else
        throw new XMLIndexException("Unsupported operator : " + operator);
      addNodeTestClauses(combiQueryBuilder, nodeTests[0], occur0, session, xpathContext);
      addNodeTestClauses(combiQueryBuilder, nodeTests[1], occur1, session, xpathContext);
      queryBuilder.add(combiQueryBuilder.build(), occur);
    } else if (nodeTest instanceof MultipleNodeKindTest) {
      UType utype = ((MultipleNodeKindTest) nodeTest).getUType();
      ArrayList<Integer> typeList = new ArrayList<Integer>();
      if (utype.overlaps(UType.DOCUMENT))
        typeList.add((int) Type.DOCUMENT);
      if (utype.overlaps(UType.ELEMENT))
        typeList.add((int) Type.ELEMENT);
      if (utype.overlaps(UType.ATTRIBUTE))
        typeList.add((int) Type.ATTRIBUTE);
      if (utype.overlaps(UType.TEXT))
        typeList.add((int) Type.TEXT);
      if (utype.overlaps(UType.COMMENT))
        typeList.add((int) Type.COMMENT);
      if (utype.overlaps(UType.PI))
        typeList.add((int) Type.PROCESSING_INSTRUCTION);
      if (utype.overlaps(UType.NAMESPACE))
        typeList.add((int) Type.NAMESPACE);
      Builder orQueryBuilder = new BooleanQuery.Builder();
      for (int i=0; i<typeList.size(); i++)
        orQueryBuilder.add(new BooleanClause(getNodeTypeQuery(typeList.get(i)), Occur.SHOULD));      
      queryBuilder.add(orQueryBuilder.build(), occur);
    } else if (nodeTest instanceof FilterNodeTest) {
      NodeTest wrappedNodeTest = ((FilterNodeTest) nodeTest).getNodeTest();
      addNodeTestClauses(queryBuilder, wrappedNodeTest, Occur.FILTER, session, xpathContext); 
      Query query;
      QueryDef queryDef = ((FilterNodeTest) nodeTest).getFilterQueryDef();
      if (queryDef instanceof QueryDefWithRelation && ((QueryDefWithRelation) queryDef).getRelation() == QueryDefWithRelation.RELATION_CHILDELEM)
        query = childElementQueryDefToQuery((QueryDefWithRelation) queryDef, session, xpathContext);
      else if (queryDef instanceof ComparisonQueryDef)
        query = comparisonQueryDefToQuery((ComparisonQueryDef) queryDef, session, xpathContext);
      else if (queryDef instanceof FullTextQueryDef)
        query = fullTextQueryDefToQuery((FullTextQueryDef) queryDef, session, xpathContext);
      else if (queryDef instanceof CustomIndexQueryDef)
        query = customIndexQueryDefToQuery((CustomIndexQueryDef) queryDef, session, xpathContext);
      else if (queryDef instanceof ExistsQueryDef)
        query = existsQueryDefToQuery((ExistsQueryDef) queryDef);
      else if (queryDef instanceof StringFunctionQueryDef)
        query = stringFunctionQueryDefToQuery((StringFunctionQueryDef) queryDef, xpathContext);
      else {
        Builder filterQueryBuilder = new BooleanQuery.Builder();
        addBooleanClauseDefs(filterQueryBuilder, (BooleanQueryDef) queryDef, session, xpathContext);
        query = filterQueryBuilder.build();
      }
      queryBuilder.add(query, occur);
      // logger.info("Filter query : " + query.toString());
    } else {
      logger.error("Unsupported nodetest : " + nodeTest.toString());
    }
  }
  
  public static Object getIndexedValue(ItemType itemType, Sequence value) throws XPathException {
    if (itemType == null || itemType.equals(BuiltInAtomicType.STRING)) {
      return SequenceTool.getStringValue(value);
    } else if (itemType.equals(BuiltInAtomicType.BOOLEAN)) {
      return ((BooleanValue) value).getPrimitiveStringValue();
    } else if (itemType.equals(BuiltInAtomicType.INT) || itemType.equals(BuiltInAtomicType.SHORT) || itemType.equals(BuiltInAtomicType.BYTE)) {
      return ((IntegerValue) value).asBigInteger().intValue();
    } else if (itemType.equals(BuiltInAtomicType.LONG)) {
      return ((IntegerValue) value).longValue();
    } else if (itemType.equals(BuiltInAtomicType.DOUBLE)) {
      return ((DoubleValue) value).getDoubleValue();
    } else if (itemType.equals(BuiltInAtomicType.FLOAT)) {
      return ((FloatValue) value).getFloatValue();
    } else if (itemType.equals(BuiltInAtomicType.DATE_TIME)) {
      return Long.valueOf(((DateTimeValue) value).getCalendar().getTimeInMillis());
    } else if (itemType.equals(BuiltInAtomicType.DATE)) {
      return Long.valueOf(((DateValue) value).getCalendar().getTimeInMillis());
    } else if (itemType.equals(BuiltInAtomicType.TIME)) {
      return Long.valueOf(((TimeValue) value).getCalendar().getTimeInMillis());
    } else {
      throw new XMLIndexException("ItemType \"" + ((BuiltInAtomicType) itemType).getName() + "\" not supported");
    }
  }
  
  public static Query constructEqQuery(String fieldName, ItemType itemType, Object value) {
    if (itemType == null || itemType.equals(BuiltInAtomicType.STRING) || itemType.equals(BuiltInAtomicType.BOOLEAN)) {
      return new TermQuery(new Term(fieldName, (String) value));
    } else if (itemType.equals(BuiltInAtomicType.INT) || itemType.equals(BuiltInAtomicType.SHORT) || itemType.equals(BuiltInAtomicType.BYTE)) {
      return IntPoint.newExactQuery(fieldName, (int) value);
    } else if (itemType.equals(BuiltInAtomicType.LONG) || itemType.equals(BuiltInAtomicType.DATE_TIME) || itemType.equals(BuiltInAtomicType.DATE) || itemType.equals(BuiltInAtomicType.TIME)) {
      return LongPoint.newExactQuery(fieldName, (long) value);
    } else if (itemType.equals(BuiltInAtomicType.DOUBLE)) {
      return DoublePoint.newExactQuery(fieldName, (double) value);
    } else if (itemType.equals(BuiltInAtomicType.FLOAT)) {
      return FloatPoint.newExactQuery(fieldName, (float) value);
    } else {
      throw new XMLIndexException("ItemType \"" + ((BuiltInAtomicType) itemType).getName() + "\" not supported");
    }
  }
  
  public static Query constructGtQuery(String fieldName, ItemType itemType, Object value) {
    if (itemType == null || itemType.equals(BuiltInAtomicType.STRING) || itemType.equals(BuiltInAtomicType.BOOLEAN)) {
      return new TermRangeQuery(fieldName, new BytesRef((String) value), null, false, true);
    } else if (itemType.equals(BuiltInAtomicType.INT) || itemType.equals(BuiltInAtomicType.SHORT) || itemType.equals(BuiltInAtomicType.BYTE)) {
      int intVal = Math.addExact((int) value, 1);
      return IntPoint.newRangeQuery(fieldName, intVal, Integer.MAX_VALUE); 
    } else if (itemType.equals(BuiltInAtomicType.LONG) || itemType.equals(BuiltInAtomicType.DATE_TIME) || itemType.equals(BuiltInAtomicType.DATE) || itemType.equals(BuiltInAtomicType.TIME)) {
      long longVal = Math.addExact((long) value, 1);
      return LongPoint.newRangeQuery(fieldName, longVal, Long.MAX_VALUE); 
    } else if (itemType.equals(BuiltInAtomicType.DOUBLE)) {
      double doubleVal = DoublePoint.nextUp((double) value);
      return DoublePoint.newRangeQuery(fieldName, doubleVal, Double.POSITIVE_INFINITY);
    } else if (itemType.equals(BuiltInAtomicType.FLOAT)) {
      float floatVal = FloatPoint.nextUp((float) value);
      return FloatPoint.newRangeQuery(fieldName, floatVal, Float.POSITIVE_INFINITY);
    } else {
      throw new XMLIndexException("ItemType \"" + ((BuiltInAtomicType) itemType).getName() + "\" not supported");
    }
  }
  
  public static Query constructGeQuery(String fieldName, ItemType itemType, Object value) {
    if (itemType == null || itemType.equals(BuiltInAtomicType.STRING) || itemType.equals(BuiltInAtomicType.BOOLEAN)) {
      return new TermRangeQuery(fieldName, new BytesRef((String) value), null, true, true);
    } else if (itemType.equals(BuiltInAtomicType.INT) || itemType.equals(BuiltInAtomicType.SHORT) || itemType.equals(BuiltInAtomicType.BYTE)) {
      return IntPoint.newRangeQuery(fieldName, (int) value, Integer.MAX_VALUE); 
    } else if (itemType.equals(BuiltInAtomicType.LONG) || itemType.equals(BuiltInAtomicType.DATE_TIME) || itemType.equals(BuiltInAtomicType.DATE) || itemType.equals(BuiltInAtomicType.TIME)) {
      return LongPoint.newRangeQuery(fieldName, (long) value, Long.MAX_VALUE); 
    } else if (itemType.equals(BuiltInAtomicType.DOUBLE)) {
      return DoublePoint.newRangeQuery(fieldName, (double) value, Double.POSITIVE_INFINITY);
    } else if (itemType.equals(BuiltInAtomicType.FLOAT)) {
      return FloatPoint.newRangeQuery(fieldName, (float) value, Float.POSITIVE_INFINITY);
    } else {
      throw new XMLIndexException("ItemType \"" + ((BuiltInAtomicType) itemType).getName() + "\" not supported");
    }
  }
  
  public static Query constructLtQuery(String fieldName, ItemType itemType, Object value) {
    if (itemType == null || itemType.equals(BuiltInAtomicType.STRING) || itemType.equals(BuiltInAtomicType.BOOLEAN)) {
      return new TermRangeQuery(fieldName, null, new BytesRef((String) value), true, false);
    } else if (itemType.equals(BuiltInAtomicType.INT) || itemType.equals(BuiltInAtomicType.SHORT) || itemType.equals(BuiltInAtomicType.BYTE)) {
      int intVal = Math.addExact((int) value, -1);
      return IntPoint.newRangeQuery(fieldName, Integer.MIN_VALUE, intVal); 
    } else if (itemType.equals(BuiltInAtomicType.LONG) || itemType.equals(BuiltInAtomicType.DATE_TIME) || itemType.equals(BuiltInAtomicType.DATE) || itemType.equals(BuiltInAtomicType.TIME)) {
      long longVal = Math.addExact((long) value, -1);
      return LongPoint.newRangeQuery(fieldName, Long.MIN_VALUE, longVal); 
    } else if (itemType.equals(BuiltInAtomicType.DOUBLE)) {
      double doubleVal = DoublePoint.nextDown((double) value);
      return DoublePoint.newRangeQuery(fieldName, Double.NEGATIVE_INFINITY, doubleVal);
    } else if (itemType.equals(BuiltInAtomicType.FLOAT)) {
      float floatVal = FloatPoint.nextDown((float) value);
      return FloatPoint.newRangeQuery(fieldName, Float.NEGATIVE_INFINITY, floatVal);
    } else {
      throw new XMLIndexException("ItemType \"" + ((BuiltInAtomicType) itemType).getName() + "\" not supported");
    }
  }
  
  public static Query constructLeQuery(String fieldName, ItemType itemType, Object value) {
    if (itemType == null || itemType.equals(BuiltInAtomicType.STRING) || itemType.equals(BuiltInAtomicType.BOOLEAN)) {
      return new TermRangeQuery(fieldName, null, new BytesRef((String) value), true, true);
    } else if (itemType.equals(BuiltInAtomicType.INT) || itemType.equals(BuiltInAtomicType.SHORT) || itemType.equals(BuiltInAtomicType.BYTE)) {
      return IntPoint.newRangeQuery(fieldName, Integer.MIN_VALUE, (int) value); 
    } else if (itemType.equals(BuiltInAtomicType.LONG) || itemType.equals(BuiltInAtomicType.DATE_TIME) || itemType.equals(BuiltInAtomicType.DATE) || itemType.equals(BuiltInAtomicType.TIME)) {
      return LongPoint.newRangeQuery(fieldName, Long.MIN_VALUE, (long) value); 
    } else if (itemType.equals(BuiltInAtomicType.DOUBLE)) {
      return DoublePoint.newRangeQuery(fieldName, Double.NEGATIVE_INFINITY, (double) value);
    } else if (itemType.equals(BuiltInAtomicType.FLOAT)) {
      return FloatPoint.newRangeQuery(fieldName, Float.NEGATIVE_INFINITY, (float) value);
    } else {
      throw new XMLIndexException("ItemType \"" + ((BuiltInAtomicType) itemType).getName() + "\" not supported");
    }
  }
    
  public static Query constructQuery(XMLIndex index, ComparisonQueryDef queryDef, Sequence value) throws XPathException {
    // TODO: multivalued GroundedValue value = ((Literal) valueExpression).getValue();
    
    // Minimax.minimax(value.iterate(), true, new GenericAtomicComparer(new CodepointCollator(), ), true, context);
    
    // String stringValue = SequenceTool.getStringValue(value);
    ItemType itemType = queryDef.getItemType();
    Object indexedValue = getIndexedValue(itemType, value);
    int cardinality = SequenceTool.getCardinality(value);
    int operator = queryDef.getOperator();
    if (cardinality != StaticProperty.EXACTLY_ONE && (!(operator == Token.FEQ || operator == Token.EQUALS)))
      throw new XPathException("Comparison query on sequences with length > 1 only supported for \"=\" operator");
    
    String fieldName = queryDef.getFieldName();
    switch (operator) {
      case Token.FEQ:
      case Token.EQUALS:
        if (cardinality == StaticProperty.EXACTLY_ONE)
          return constructEqQuery(fieldName, itemType, indexedValue);
        else {
          BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
          SequenceIterator iter = value.iterate();
          Item item;
          while ((item = iter.next()) != null)
            queryBuilder.add(constructEqQuery(fieldName, itemType, item.getStringValue()), Occur.SHOULD);
          return queryBuilder.build();
        }
      case Token.FGT:
      case Token.GT:
        return constructGtQuery(fieldName, itemType, indexedValue);
      case Token.FGE:
      case Token.GE:
        return constructGeQuery(fieldName, itemType, indexedValue);
      case Token.FLT:  
      case Token.LT:
        return constructLtQuery(fieldName, itemType, indexedValue);
      case Token.FLE:
      case Token.LE:
        return constructLeQuery(fieldName, itemType, indexedValue);
      default:
        throw new OptimizationFailureException("Could not create Query. Operator " + queryDef.getOperator() + " not supported.");
    }
  }

}