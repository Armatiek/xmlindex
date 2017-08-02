package nl.armatiek.xmlindex.restxq.adapter;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;

public class TypeAdapter {

  // Saxon XQuery Type <-> EXQuery Type
  private final static BidiMap<ItemType, org.exquery.xquery.Type> mappings = new DualHashBidiMap<ItemType, org.exquery.xquery.Type>();
  static {
    /*
    mappings.put(Type.NODE, org.exquery.xquery.Type.NODE);
    mappings.put(Type.ELEMENT, org.exquery.xquery.Type.ELEMENT);
    mappings.put(Type.ATTRIBUTE, org.exquery.xquery.Type.ATTRIBUTE);
    mappings.put(Type.TEXT, org.exquery.xquery.Type.TEXT);
    mappings.put(Type.PROCESSING_INSTRUCTION, org.exquery.xquery.Type.PROCESSING_INSTRUCTION);
    mappings.put(Type.COMMENT, org.exquery.xquery.Type.COMMENT);
    mappings.put(Type.DOCUMENT, org.exquery.xquery.Type.DOCUMENT);
    */
    mappings.put(AnyItemType.getInstance(), org.exquery.xquery.Type.ITEM);
    mappings.put(AnyItemType.getInstance(), org.exquery.xquery.Type.ANY_TYPE); // ?
    mappings.put(BuiltInAtomicType.ANY_ATOMIC, org.exquery.xquery.Type.ANY_SIMPLE_TYPE);
    mappings.put(BuiltInAtomicType.UNTYPED_ATOMIC, org.exquery.xquery.Type.UNTYPED); // ?
    mappings.put(BuiltInAtomicType.STRING, org.exquery.xquery.Type.STRING);
    mappings.put(BuiltInAtomicType.BOOLEAN, org.exquery.xquery.Type.BOOLEAN);
    mappings.put(BuiltInAtomicType.QNAME, org.exquery.xquery.Type.QNAME);
    mappings.put(BuiltInAtomicType.ANY_URI, org.exquery.xquery.Type.ANY_URI);
    mappings.put(BuiltInAtomicType.BASE64_BINARY, org.exquery.xquery.Type.BASE64_BINARY);
    mappings.put(BuiltInAtomicType.HEX_BINARY, org.exquery.xquery.Type.HEX_BINARY);
    mappings.put(BuiltInAtomicType.NOTATION, org.exquery.xquery.Type.NOTATION);
    mappings.put(BuiltInAtomicType.INTEGER, org.exquery.xquery.Type.INTEGER);
    mappings.put(BuiltInAtomicType.DECIMAL, org.exquery.xquery.Type.DECIMAL);
    mappings.put(BuiltInAtomicType.FLOAT, org.exquery.xquery.Type.FLOAT);
    mappings.put(BuiltInAtomicType.DOUBLE, org.exquery.xquery.Type.DOUBLE);
    mappings.put(BuiltInAtomicType.NON_POSITIVE_INTEGER, org.exquery.xquery.Type.NON_POSITIVE_INTEGER);
    mappings.put(BuiltInAtomicType.NEGATIVE_INTEGER, org.exquery.xquery.Type.NEGATIVE_INTEGER);
    mappings.put(BuiltInAtomicType.LONG, org.exquery.xquery.Type.LONG);
    mappings.put(BuiltInAtomicType.INT, org.exquery.xquery.Type.INT);
    mappings.put(BuiltInAtomicType.SHORT, org.exquery.xquery.Type.SHORT);
    mappings.put(BuiltInAtomicType.BYTE, org.exquery.xquery.Type.BYTE);
    mappings.put(BuiltInAtomicType.NON_NEGATIVE_INTEGER, org.exquery.xquery.Type.NON_NEGATIVE_INTEGER);
    mappings.put(BuiltInAtomicType.UNSIGNED_LONG, org.exquery.xquery.Type.UNSIGNED_LONG);
    mappings.put(BuiltInAtomicType.UNSIGNED_SHORT, org.exquery.xquery.Type.UNSIGNED_SHORT);
    mappings.put(BuiltInAtomicType.UNSIGNED_BYTE, org.exquery.xquery.Type.UNSIGNED_BYTE);
    mappings.put(BuiltInAtomicType.POSITIVE_INTEGER, org.exquery.xquery.Type.POSITIVE_INTEGER);
    mappings.put(BuiltInAtomicType.DATE_TIME, org.exquery.xquery.Type.DATE_TIME);
    mappings.put(BuiltInAtomicType.DATE, org.exquery.xquery.Type.DATE);
    mappings.put(BuiltInAtomicType.TIME, org.exquery.xquery.Type.TIME);
    mappings.put(BuiltInAtomicType.DURATION, org.exquery.xquery.Type.DURATION);
    mappings.put(BuiltInAtomicType.YEAR_MONTH_DURATION, org.exquery.xquery.Type.YEAR_MONTH_DURATION);
    mappings.put(BuiltInAtomicType.DAY_TIME_DURATION, org.exquery.xquery.Type.DAY_TIME_DURATION);
    mappings.put(BuiltInAtomicType.G_YEAR, org.exquery.xquery.Type.G_YEAR);
    mappings.put(BuiltInAtomicType.G_MONTH, org.exquery.xquery.Type.G_MONTH);
    mappings.put(BuiltInAtomicType.G_DAY, org.exquery.xquery.Type.G_DAY);
    mappings.put(BuiltInAtomicType.G_YEAR_MONTH, org.exquery.xquery.Type.G_YEAR_MONTH);
    mappings.put(BuiltInAtomicType.G_MONTH_DAY, org.exquery.xquery.Type.G_MONTH_DAY);
    mappings.put(BuiltInAtomicType.TOKEN, org.exquery.xquery.Type.TOKEN);
    mappings.put(BuiltInAtomicType.NORMALIZED_STRING, org.exquery.xquery.Type.NORMALIZED_STRING);
    mappings.put(BuiltInAtomicType.LANGUAGE, org.exquery.xquery.Type.LANGUAGE);
    mappings.put(BuiltInAtomicType.NMTOKEN, org.exquery.xquery.Type.NM_TOKEN);
    mappings.put(BuiltInAtomicType.NAME, org.exquery.xquery.Type.NAME);
    mappings.put(BuiltInAtomicType.NCNAME, org.exquery.xquery.Type.NC_NAME);
    mappings.put(BuiltInAtomicType.ID, org.exquery.xquery.Type.ID);
    mappings.put(BuiltInAtomicType.IDREF, org.exquery.xquery.Type.ID_REF);
    mappings.put(BuiltInAtomicType.ENTITY, org.exquery.xquery.Type.ENTITY);
  }

  public static org.exquery.xquery.Type toExQueryType(ItemType itemType) {
    if (!itemType.isAtomicType()) {
      throw new UnsupportedOperationException("toExQueryType");
    }
    if (itemType instanceof NodeTest) {
      // NodeTest nt = (NodeTest) itemType;
      throw new UnsupportedOperationException("toExQueryType");
    }
    org.exquery.xquery.Type exQueryType = (org.exquery.xquery.Type) mappings.get(itemType);
    if (exQueryType == null)
      exQueryType = org.exquery.xquery.Type.ANY_TYPE;
    return exQueryType;
  }

  /*
  public static ItemType toSaxonType(org.exquery.xquery.Type type) {
    ItemType saxonType = (ItemType) mappings.getKey(type);
    if (saxonType == null)
      saxonType = AnyItemType.getInstance();
    return saxonType;
  }
  */
  
}