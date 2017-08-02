package nl.armatiek.xmlindex.restxq;

import javax.xml.namespace.QName;

import org.exquery.restxq.RestXqService;
import org.exquery.restxq.RestXqServiceRegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestXqServiceRegistryLogger implements RestXqServiceRegistryListener {

  private static final Logger logger = LoggerFactory.getLogger(RestXqServiceRegistryLogger.class);

  @Override
  public void registered(final RestXqService service) {
    logger.info("Registered RESTXQ Resource Function: " + getIdentifier(service));
  }

  @Override
  public void deregistered(final RestXqService service) {
    logger.info("De-registered RESTXQ Resource Function: " + getIdentifier(service));
  }

  private String qnameToClarkNotation(final QName qname) {
    if (qname.getNamespaceURI() == null) {
      return qname.getLocalPart();
    } else {
      return "{" + qname.getNamespaceURI() + "}" + qname.getLocalPart();
    }
  }

  private String getIdentifier(final RestXqService service) {
    final StringBuilder builder = new StringBuilder();
    builder.append(service.getResourceFunction().getXQueryLocation());
    builder.append(',');
    builder.append(qnameToClarkNotation(service.getResourceFunction().getFunctionSignature().getName()));
    builder.append('#');
    builder.append(service.getResourceFunction().getFunctionSignature().getArgumentCount());
    return builder.toString();
  }

}