package nl.armatiek.xmlindex.restxq.adapter;

import javax.xml.namespace.QName;

import org.exquery.xquery.FunctionArgument;
import org.exquery.xquery3.Annotation;
import org.exquery.xquery3.FunctionSignature;

import net.sf.saxon.expr.instruct.UserFunctionParameter;
import net.sf.saxon.query.AnnotationList;
import net.sf.saxon.query.XQueryFunction;
import net.sf.saxon.value.SequenceType;

public class FunctionSignatureAdapter implements FunctionSignature {

  private XQueryFunction func;
  private QName name;
  private int argumentCount;
  private FunctionParameterSequenceTypeAdapter[] arguments;
  private AnnotationAdapter[] annotations;

  public FunctionSignatureAdapter(XQueryFunction func) {
    this.func = func;
    this.name = func.getFunctionName().toJaxpQName();
    this.argumentCount = func.getNumberOfArguments();
    
    final SequenceType[] argumentTypes = func.getArgumentTypes();
    final UserFunctionParameter[] params = func.getParameterDefinitions();
    if (argumentCount > 0) {
      arguments = new FunctionParameterSequenceTypeAdapter[argumentTypes.length];
      for (int i=0; i<argumentCount; i++)
        arguments[i] = new FunctionParameterSequenceTypeAdapter(params[i], argumentTypes[i]);
    }

    final AnnotationList annotations = func.getAnnotations();
    if (annotations != null) {
      this.annotations = new AnnotationAdapter[annotations.size()];
      for (int i=0; i<annotations.size(); i++)
        this.annotations[i] = new AnnotationAdapter(annotations.get(i), this);
    } 
  }

  @Override
  public String toString() {
    String str;
    if (name.getPrefix() != null) {
      str = name.getPrefix() + ":" + name.getLocalPart();
    } else {
      str = name.toString(); // clark-notation
    }
    str += "#" + getArgumentCount();
    return str;
  }

  @Override
  public QName getName() {
    return name;
  }

  @Override
  public int getArgumentCount() {
    return argumentCount;
  }

  @Override
  public Annotation[] getAnnotations() {
    return annotations;
  }

  @Override
  public FunctionArgument[] getArguments() {
    return arguments;
  }
  
  public XQueryFunction getXQueryFunction() {
    return func;
  }
  
}