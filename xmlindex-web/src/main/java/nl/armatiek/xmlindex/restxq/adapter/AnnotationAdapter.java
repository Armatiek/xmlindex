package nl.armatiek.xmlindex.restxq.adapter;

import java.util.List;

import javax.xml.namespace.QName;

import org.exquery.xquery.Literal;
import org.exquery.xquery3.Annotation;
import org.exquery.xquery3.FunctionSignature;

import net.sf.saxon.query.XQueryFunction;
import net.sf.saxon.value.AtomicValue;

public class AnnotationAdapter implements Annotation {

  private QName name;
  private LiteralValueAdapter literals[];
  private FunctionSignature functionSignature;

  public AnnotationAdapter(final net.sf.saxon.query.Annotation annotation, final XQueryFunction func) {
    this(annotation, new FunctionSignatureAdapter(func));
  }
  
  public AnnotationAdapter(final net.sf.saxon.query.Annotation annotation, final FunctionSignatureAdapter functionSignatureAdapter) {
    this.name = annotation.getAnnotationQName().toJaxpQName();
    List<AtomicValue> params = annotation.getAnnotationParameters();
    this.literals = new LiteralValueAdapter[params.size()];
    for (int i=0; i<params.size(); i++) 
      literals[i] = new LiteralValueAdapter(params.get(i));
    this.functionSignature = functionSignatureAdapter;
  }

  @Override
  public QName getName() {
    return name;
  }

  @Override
  public Literal[] getLiterals() {
    return literals;
  }

  @Override
  public FunctionSignature getFunctionSignature() {
    return functionSignature;
  }
  
  public void setFunctionSignature(FunctionSignature sig) {
    this.functionSignature = sig; 
  }
  
}