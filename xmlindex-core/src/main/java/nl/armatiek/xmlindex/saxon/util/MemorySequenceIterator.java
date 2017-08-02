package nl.armatiek.xmlindex.saxon.util;

import java.util.ArrayList;
import java.util.Iterator;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import nl.armatiek.xmlindex.error.XMLIndexException;

public class MemorySequenceIterator implements SequenceIterator {

  private ArrayList<Item> items = new ArrayList<Item>();
  private Iterator<Item> itemsIter;
  
  public MemorySequenceIterator(SequenceIterator iter) {
    if (iter == null) {
      itemsIter = items.iterator();
      return;
    }
    try {
      Item item;
      while ((item = iter.next()) != null) 
        items.add(item);
      itemsIter = items.iterator();
    } catch (XPathException xpe) {
      throw new XMLIndexException("Error constructing MemorySequenceIterator", xpe);
    }
  }
  
  @Override
  public Item next() throws XPathException {
    return itemsIter.next();
  }

  @Override
  public void close() { }

  @Override
  public int getProperties() {
    return 0;
  }
  
  public boolean isEmpty() {
    return items.isEmpty();
  }

}