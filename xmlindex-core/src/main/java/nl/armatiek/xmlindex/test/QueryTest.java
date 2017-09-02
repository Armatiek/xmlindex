package nl.armatiek.xmlindex.test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;

public class QueryTest {
  
  private static final Logger logger = LoggerFactory.getLogger(QueryTest.class);
  
  private void runTest(String indexPath, String xqueryPath) throws IOException, SaxonApiException {
    XMLIndex index = new XMLIndex("Test", Paths.get(indexPath));
    index.open();
    try {
      Session session = index.aquireSession();
      try {
        File queryFile = new File(xqueryPath);
        String xquery = FileUtils.readFileToString(queryFile, "UTF-8");
        for (int i=0; i<1; i++) {
          Serializer out = index.getSaxonProcessor().newSerializer(new File("D:/temp/out.xml")); // 
          out.setOutputProperty(Serializer.Property.METHOD, "xml");
          out.setOutputProperty(Serializer.Property.INDENT, "yes");
          logger.info("Executing XQuery...");
          StopWatch sw = new StopWatch();
          sw.start();
          session.query(new StringReader(xquery), queryFile.toURI(), out, null, null, null);
          logger.info("XQuery execution time: " + sw.toString());
          sw.stop();
        }
      } finally {
        index.returnSession(session);
      }
    } finally {
      index.close();
    }
  }

  public static void main(String[] args) {
    try {
      QueryTest test = new QueryTest();
      test.runTest(args[0], args[1]);
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
  }

}