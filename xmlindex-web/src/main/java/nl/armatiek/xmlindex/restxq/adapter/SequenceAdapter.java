package nl.armatiek.xmlindex.restxq.adapter;

import java.util.Iterator;

import org.exquery.xquery.Sequence;
import org.exquery.xquery.Type;
import org.exquery.xquery.TypedValue;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

public class SequenceAdapter implements Sequence<XdmItem> {

  private final XdmValue value;
  private final Configuration config;
  
  public SequenceAdapter(final XdmValue value, final Configuration config) {
    this.value = value;
    this.config = config;
  }

  @Override
  public Iterator<TypedValue<XdmItem>> iterator() {
    
    return new Iterator<TypedValue<XdmItem>>() {

      private XdmSequenceIterator iterator;

      private XdmSequenceIterator getIterator() {
        if (iterator == null)
          iterator = value.iterator();
        return iterator;
      }

      @Override
      public boolean hasNext() {
        return getIterator().hasNext();
      }

      @Override
      public TypedValue<XdmItem> next() {
        return createTypedValue(getIterator().next());
      }

      @Override
      public void remove() { }
      
    };
    
  }

  private TypedValue<XdmItem> createTypedValue(final XdmItem item) {
    
    return new TypedValue<XdmItem>() {
      
      @Override
      public Type getType() {
        return TypeAdapter.toExQueryType(net.sf.saxon.type.Type.getItemType(item.getUnderlyingValue(), 
            config.getTypeHierarchy()));
      }

      @Override
      public XdmItem getValue() {
        return item;
      }
      
    };
    
  }

  @Override
  public TypedValue<XdmItem> head() {
    if (value.size() == 0) {
      return null;
    } else {
      return createTypedValue(value.itemAt(0));
    }
  }

  @Override
  public Sequence<XdmItem> tail() {
    return new SequenceAdapter(value.itemAt(value.size()-1), config);
  }

  public XdmValue getSaxonXdmValue() {
    return value;
  }
  
}