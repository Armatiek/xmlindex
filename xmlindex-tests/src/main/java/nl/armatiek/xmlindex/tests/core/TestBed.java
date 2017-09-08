package nl.armatiek.xmlindex.tests.core;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;

public class TestBed {
  
  private static final Logger logger = LoggerFactory.getLogger(TestBed.class);
  
  private void executeTest(Session session) throws Exception {
    // session.addDocument("geo-test", new File("C:\\projects\\validprojects\\XMLIndex2\\home\\gml.xml"));
    session.addDocument("geo-test", new File("D:\\countrywide\\nl\\Wijk2015.gml"), null);
    session.commit();
  }
  
  private void runTest(String indexPath) throws Exception {
    XMLIndex index = new XMLIndex("Test", Paths.get(indexPath));
    index.open();
    try {
      Session session = index.aquireSession();
      try {
        logger.info("Starting test ...");
        StopWatch sw = new StopWatch();
        sw.start();
        executeTest(session);
        logger.info("Test execution time: " + sw.toString());
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
      TestBed test = new TestBed();
      test.runTest(args[0]);
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
  }

}
