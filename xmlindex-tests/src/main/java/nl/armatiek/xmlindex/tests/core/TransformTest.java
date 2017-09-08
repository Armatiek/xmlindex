package nl.armatiek.xmlindex.tests.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;

public class TransformTest {
  
  private static final Logger logger = LoggerFactory.getLogger(TransformTest.class);
  
  private void runTest(String indexPath, String xsltPath) throws IOException, SaxonApiException {
    XMLIndex index = new XMLIndex("Test", Paths.get(indexPath));
    index.open();
    try {
      Session session = index.aquireSession();
      try {
        Serializer out = index.getSaxonProcessor().newSerializer(System.out); // new File("D:/temp/out.xml")
        out.setOutputProperty(Serializer.Property.METHOD, "xml");
        out.setOutputProperty(Serializer.Property.INDENT, "yes");
        logger.info("Executing transformation...");
        StopWatch sw = new StopWatch();
        sw.start();
        session.transform(new StreamSource(new File(xsltPath)), out, null, null, null);
        logger.info("Transformation execution time: " + sw.toString());
        sw.stop();
      } finally {
        index.returnSession(session);
      }
    } finally {
      index.close();
    }
  }

  public static void main(String[] args) {
    try {
      TransformTest test = new TransformTest();
      test.runTest(args[0], args[1]);
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
  }

}
