package nl.armatiek.xmlindex.convertor;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import nl.armatiek.xmlindex.plugins.convertor.FileConvertor;

public class XSLTConvertor implements FileConvertor {
  
  private Processor processor;
  private XsltExecutable executable;
  
  public XSLTConvertor(Processor processor, File xslFile) throws SaxonApiException {
    this.processor = processor;
    XsltCompiler comp = processor.newXsltCompiler();
    executable = comp.compile(new StreamSource(xslFile));
  }

  @Override
  public void convert(InputStream is, String systemId, OutputStream xmlStream) throws Exception {
    Xslt30Transformer transformer = executable.load30();
    Destination destination = processor.newSerializer(xmlStream);
    transformer.applyTemplates(new StreamSource(is, systemId), destination);
  }
  
}