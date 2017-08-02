package nl.armatiek.xmlindex.restxq.typedvalue;

import org.exquery.xdm.type.AbstractTypedValue;
import org.exquery.xquery.Type;

import net.sf.saxon.om.NodeInfo;

public class DocumentTypedValue extends AbstractTypedValue<NodeInfo> {

  public DocumentTypedValue(final NodeInfo document) {
    super(Type.DOCUMENT, document);
  }

}