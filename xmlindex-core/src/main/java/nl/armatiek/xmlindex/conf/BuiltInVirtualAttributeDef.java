package nl.armatiek.xmlindex.conf;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XQueryEvaluator;
import nl.armatiek.xmlindex.XMLIndex;

public class BuiltInVirtualAttributeDef extends VirtualAttributeDef2 {

  public BuiltInVirtualAttributeDef(XMLIndex index, Processor processor, 
      XQueryEvaluator eval) throws SaxonApiException {
    super(index);
    XPathCompiler compiler = processor.newXPathCompiler();
    XPathExecutable exec = compiler.compilePattern("/");
    selector = exec.load();
  }
  
  public void addVirtualAttribute(VirtualAttributeDef vad) {
    virtualAttributes.add(vad);
  }
  
}