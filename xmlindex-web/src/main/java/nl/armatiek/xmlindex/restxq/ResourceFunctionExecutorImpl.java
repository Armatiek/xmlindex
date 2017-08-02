package nl.armatiek.xmlindex.restxq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.exquery.http.HttpRequest;
import org.exquery.restxq.Namespace;
import org.exquery.restxq.ResourceFunction;
import org.exquery.restxq.ResourceFunctionExecuter;
import org.exquery.restxq.RestXqServiceException;
import org.exquery.xquery.Sequence;
import org.exquery.xquery.TypedArgumentValue;
import org.exquery.xquery.TypedValue;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.instruct.UserFunctionParameter;
import net.sf.saxon.query.XQueryFunction;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmEmptySequence;
import net.sf.saxon.s9api.XdmValue;
import nl.armatiek.xmlindex.restxq.adapter.FunctionSignatureAdapter;
import nl.armatiek.xmlindex.restxq.adapter.SequenceAdapter;

public class ResourceFunctionExecutorImpl implements ResourceFunctionExecuter {

  public final static QName XQ_VAR_BASE_URI = new QName("base-uri", Namespace.ANNOTATION_NS);
  public final static QName XQ_VAR_URI = new QName("uri", Namespace.ANNOTATION_NS);
  
  private final XQueryExecutable restXQuery;
  private final Configuration config;
  private final String uri;
  private final String baseUri;
  
  public ResourceFunctionExecutorImpl(final Configuration config, final XQueryExecutable restXQuery, 
      final String baseUri, final String uri) {
    this.config = config;
    this.restXQuery = restXQuery;
    this.baseUri = baseUri;
    this.uri = uri;
  }
  
  @Override
  public Sequence execute(final ResourceFunction resourceFunction, final Iterable<TypedArgumentValue> arguments, 
      final HttpRequest request) throws RestXqServiceException {
    try {
      XQueryFunction func = ((FunctionSignatureAdapter) resourceFunction.getFunctionSignature()).getXQueryFunction();
      XQueryEvaluator eval = restXQuery.load();
      eval.setExternalVariable(XQ_VAR_BASE_URI, new XdmAtomicValue(baseUri));
      eval.setExternalVariable(XQ_VAR_URI, new XdmAtomicValue(uri));
      // TODO: errorListener    
      /*
      FunctionSignature sign = resourceFunction.getFunctionSignature();
      QName funcName = new QName(sign.getName());
      ArrayList<XdmValue> argList = new ArrayList<XdmValue>();
      Iterator<TypedArgumentValue> args = arguments.iterator();
      while (args.hasNext()) {
        TypedArgumentValue tav = args.next();
        Sequence typedValue = tav.getTypedValue();
        argList.add(convertTypedValueToXdmValue(tav.getTypedValue()));
      }
      */
      XdmValue value = eval.callFunction(new QName(func.getFunctionName()), convertToSaxonFunctionArguments(arguments, func));
      return new SequenceAdapter(value, config);
    } catch (SaxonApiException e) {
      throw new RestXqServiceException(e.getMessage(), e);
    }
  }
  
  private XdmValue[] convertToSaxonFunctionArguments(final Iterable<TypedArgumentValue> arguments, final XQueryFunction func) throws  RestXqServiceException {
    final List<XdmValue> argList = new ArrayList<XdmValue>();
    for (final UserFunctionParameter paramDef : func.getParameterDefinitions()) {
      XdmValue arg = null;
      for (final TypedArgumentValue argument : arguments) {
        final String argumentName = argument.getArgumentName();
        if (argumentName != null && argumentName.equals(paramDef.getVariableQName().getLocalPart())) {
          arg = convertTypedValueToXdmValue(argument.getTypedValue());    
          break;
        }
      }
      if (arg != null)
        arg = XdmEmptySequence.getInstance();
      argList.add(arg);
    }
    return argList.toArray(new XdmValue[argList.size()]);
  }
  
  private XdmValue convertTypedValueToXdmValue(Sequence sequence) throws RestXqServiceException {
    ArrayList<XdmValue> valueList = new ArrayList<XdmValue>();
    Iterator<TypedValue> iter = sequence.iterator();
    while (iter.hasNext()) {
      TypedValue tv = iter.next();
      switch (tv.getType()) {
      case ITEM:
        return null;
      case DOCUMENT:
        return null;
      default:
        try {
          return XdmAtomicValue.makeAtomicValue(tv.getValue());
        } catch (IllegalArgumentException e) {
          throw new RestXqServiceException("Type \"" + tv.getType() + "\" not supported", e);
        }
      }
    }
    return XdmValue.makeValue(valueList);
  }
  
  /*
  private XdmValue convertTypedValueToXdmValue(TypedValue typedValue) {
    
    
    
    switch (typedValue.getType()) {
    
    case ITEM:
        destinationClass = Item.class;
        break;
        
    case Type.DOCUMENT:
        destinationClass = DocumentImpl.class;  //TODO test this
        break;
    
    case STRING:
    case LANGUAGE:
    case NM_TOKEN:
    case NAME:
    case NC_NAME:
    case ENTITY:
    case ID_REF:
    case IDREFS:
    case NORMALIZED_STRING:
      return new XdmAtomicValue((String) typedValue.getValue());
    case INT:
    case INTEGER:
    case UNSIGNED_SHORT:
      return new XdmAtomicValue((Integer) typedValue.getValue());
    case LONG:
    case UNSIGNED_LONG:
    case UNSIGNED_INT:
      return new XdmAtomicValue((Long) typedValue.getValue());
    case UNSIGNED_BYTE:
      return new XdmAtomicValue((Short) typedValue.getValue());
    case FLOAT:
      return new XdmAtomicValue((Float) typedValue.getValue());
    case DOUBLE:
      return new XdmAtomicValue((Double) typedValue.getValue());
    case BOOLEAN:
      return new XdmAtomicValue((Boolean) typedValue.getValue());
    case DECIMAL:
      return new XdmAtomicValue((String) typedValue.getValue());
    case ANY_URI:
      return new XdmAtomicValue((URI) typedValue.getValue()));
    case QNAME:
      return new XdmAtomicValue((QName) typedValue.getValue()));
    case NON_NEGATIVE_INTEGER:
      return new XdmAtomicValue(new BigDecimal((BigInteger) typedValue.getValue());
    
      
      
      
    case Type.DATE:
        destinationClass = DateValue.class;
        break;
        
    case Type.DATE_TIME:
        destinationClass = DateTimeValue.class;
        break;
        
    case Type.TIME:
        destinationClass = TimeValue.class;
        break;
        
    
    
    
    
    
    
    default:
        destinationClass = Item.class;
}
    
    
  }
  */

}
