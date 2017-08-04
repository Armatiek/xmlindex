package nl.armatiek.xmlindex.restxq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.exquery.http.HttpRequest;
import org.exquery.restxq.ResourceFunction;
import org.exquery.restxq.ResourceFunctionExecuter;
import org.exquery.restxq.RestXqServiceException;
import org.exquery.xquery.Sequence;
import org.exquery.xquery.TypedArgumentValue;
import org.exquery.xquery.TypedValue;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.instruct.UserFunctionParameter;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.query.XQueryFunction;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmEmptySequence;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.StringConverter;
import net.sf.saxon.type.ValidationException;
import net.sf.saxon.value.AtomicValue;
import nl.armatiek.xmlindex.conf.WebDefinitions;
import nl.armatiek.xmlindex.restxq.adapter.FunctionSignatureAdapter;
import nl.armatiek.xmlindex.restxq.adapter.SequenceAdapter;

public class ResourceFunctionExecutorImpl implements ResourceFunctionExecuter {

  public final static QName XQ_VAR_BASE_URI  = new QName(WebDefinitions.NAMESPACE_RESTXQ, "base-uri");
  public final static QName XQ_VAR_URI       = new QName(WebDefinitions.NAMESPACE_RESTXQ, "uri");
  public final static QName XQ_VAR_INDEXNAME = new QName(WebDefinitions.NAMESPACE_RESTXQ, "index-name");
  
  private final XQueryExecutable restXQuery;
  private final Configuration config;
  private final String uri;
  private final String baseUri;
  private final String indexName;
  
  public ResourceFunctionExecutorImpl(final Configuration config, final XQueryExecutable restXQuery, 
      final String baseUri, final String uri, final String indexName) {
    this.config = config;
    this.restXQuery = restXQuery;
    this.baseUri = baseUri;
    this.uri = uri;
    this.indexName = indexName;
    
  }
  
  @Override
  public Sequence<?> execute(final ResourceFunction resourceFunction, final Iterable<TypedArgumentValue> arguments, 
      final HttpRequest request) throws RestXqServiceException {
    try {
      XQueryFunction func = ((FunctionSignatureAdapter) resourceFunction.getFunctionSignature()).getXQueryFunction();
      XQueryEvaluator eval = restXQuery.load();
      eval.setExternalVariable(XQ_VAR_BASE_URI, new XdmAtomicValue(baseUri));
      eval.setExternalVariable(XQ_VAR_URI, new XdmAtomicValue(uri));
      eval.setExternalVariable(XQ_VAR_INDEXNAME, new XdmAtomicValue(indexName));
      // TODO: errorListener    
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
      for (final TypedArgumentValue<?> argument : arguments) {
        final String argumentName = argument.getArgumentName();
        if (argumentName != null && argumentName.equals(paramDef.getVariableQName().getLocalPart())) {
          arg = convertTypedValueToXdmValue(argument.getTypedValue(), paramDef.getRequiredType().getPrimaryType());    
          break;
        }
      }
      if (arg == null)
        arg = XdmEmptySequence.getInstance();
      argList.add(arg);
    }
    return argList.toArray(new XdmValue[argList.size()]);
  }
  
  private XdmValue convertTypedValueToXdmValue(Sequence<?> sequence, ItemType itemType) throws RestXqServiceException {
    ArrayList<XdmValue> valueList = new ArrayList<XdmValue>();
    Iterator<?> iter = sequence.iterator();
    while (iter.hasNext()) {
      TypedValue<?> tv = (TypedValue<?>) iter.next();
      try {
        if (tv.getValue() instanceof NodeInfo) {
          return new XdmNode((NodeInfo) tv.getValue());
        } else if (tv.getValue() instanceof String && itemType.equals(BuiltInAtomicType.STRING)) {
          // Saxon function is expecting type String and EXQuery TypedValue is already of type String:
          return new XdmAtomicValue((String) tv.getValue());
        } else if (tv.getValue() instanceof String) {
          // Saxon function is expecting type other than String, but EXQuery TypedValue is of type String so conversion is necessary:
          if (!itemType.isPlainType())
            throw new RestXqServiceException("Cannot convert TypedValue because type it is not atomic");
          if (((AtomicType) itemType).isAbstract())
            throw new RestXqServiceException("Cannot convert TypedValue because type is an abtract type");
          if (((AtomicType) itemType).isNamespaceSensitive())
            throw new RestXqServiceException("Cannot convert TypedValue because type is namespace-sensitive");
          try {
            StringConverter converter = ((AtomicType) itemType).getStringConverter(config.getConversionRules());
            AtomicValue av = converter.convertString((String) tv.getValue()).asAtomic();
            return XdmAtomicValue.makeAtomicValue(av);
          } catch (ValidationException e) {
            throw new RestXqServiceException(e.getMessage(), e);
          }
        } else
          // EXQuery TypedValue is non String so convert to Saxon equivalent value: 
          return XdmAtomicValue.makeAtomicValue(tv.getValue()); 
      } catch (IllegalArgumentException e) {
        throw new RestXqServiceException("Could not convert typed value of type \"" + tv.getType() + "\"", e);
      } 
    }
    return XdmValue.makeValue(valueList);
  }
  
}