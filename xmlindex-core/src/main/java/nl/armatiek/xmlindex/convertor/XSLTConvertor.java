package nl.armatiek.xmlindex.convertor;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.plugins.convertor.PluggableFileConvertor;

public class XSLTConvertor extends PluggableFileConvertor {
  
  private static final String PARAMNAME_XSLPATH = "xsl-path";
  
  private Processor processor;
  private XsltExecutable executable;
  
  public XSLTConvertor(String name) throws SaxonApiException {
    super(name);
  }
  
  @Override
  public void init(Map<String, String> params) throws Exception {
    super.init(params);
    processor = new Processor(false);
    String xslPath = params.get(PARAMNAME_XSLPATH);
    if (xslPath == null)
      throw new XMLIndexException("Mandatory parameter \"" + PARAMNAME_XSLPATH + "\" not set");
    XsltCompiler comp = processor.newXsltCompiler();
    executable = comp.compile(new StreamSource(xslPath));
  }

  @Override
  public void convert(InputStream is, String systemId, OutputStream xmlStream) throws Exception {
    Xslt30Transformer transformer = executable.load30();
    setStylesheetParameters(transformer);
    Destination destination = processor.newSerializer(xmlStream);
    transformer.applyTemplates(new StreamSource(is, systemId), destination);
  }
  
  private void setStylesheetParameters(Xslt30Transformer transformer) throws SaxonApiException {
    if (params == null || params.isEmpty())
      return;
    Map<QName, XdmValue> styleParams = new HashMap<QName, XdmValue>();
    Iterator<Entry<String, String>> iter =  params.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<String, String> entry = iter.next();
      QName name = new QName(entry.getKey());
      XdmValue value = XdmValue.makeValue(entry.getValue());
      styleParams.put(name, value);
    }
    transformer.setStylesheetParameters(styleParams);
  }
  
}