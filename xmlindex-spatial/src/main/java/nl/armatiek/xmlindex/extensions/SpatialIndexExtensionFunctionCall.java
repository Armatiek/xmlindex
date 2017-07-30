package nl.armatiek.xmlindex.extensions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;

public class SpatialIndexExtensionFunctionCall extends CustomIndexExtensionFunctionCall {
  
  @Override
  public BooleanValue call(XPathContext context, Sequence[] arguments) throws XPathException {            
    return BooleanValue.TRUE;
  }
  
}
