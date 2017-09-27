package nl.armatiek.xmlindex.storage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.om.StructuredQName;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.error.XMLIndexException;

public class NameStore {
  
  private static final Logger logger = LoggerFactory.getLogger(NameStore.class);
 
  private boolean isOpen;
  private File nameFile;
  private File prefixFile;
  private ConcurrentHashMap<Integer, Name> codeToNameMapping;
  private ConcurrentHashMap<Name, Integer> nameToCodeMapping;
  private ConcurrentHashMap<Integer, String> codeToPrefixMapping;
  private ConcurrentHashMap<String, Integer> prefixToCodeMapping;
  private Name name;
  
  public NameStore(XMLIndex index) {
    File nameDir = new File(index.getIndexPath().toFile(), Definitions.FOLDERNAME_NAMESTORE);
    if (!nameDir.exists())
      nameDir.mkdirs();
    this.nameFile = new File(nameDir, Definitions.FILENAME_NAMES);
    this.prefixFile = new File(nameDir, Definitions.FILENAME_PREFIXES);
    this.name = new Name("", "");
  }
  
  private void readNameFile() throws IOException {
    List<String> lines = null;
    if (nameFile.exists())
      lines = FileUtils.readLines(nameFile, "UTF-8");
    if (lines == null || lines.size() == 0) {
      putName("", Definitions.ELEMNAME_ROOT);
    } else {
      for (String line : lines) {
        if (StringUtils.isBlank(line))
          continue;
        String[] parts = line.split(";");
        int code = Integer.parseInt(parts[0]);
        String namespaceUri = parts[1];
        String localPart = parts[2];
        localPart = localPart.equals("_") ? "" : localPart;
        Name name = new Name(namespaceUri, localPart);
        codeToNameMapping.put(code, name);
        nameToCodeMapping.put(name, code);
      }
    }
  }
  
  private void readPrefixFile() throws IOException {
    if (!prefixFile.exists())
      return;
    List<String> lines = FileUtils.readLines(prefixFile, "UTF-8");
    for (String line : lines) {
      if (StringUtils.isBlank(line))
        continue;
      String[] parts = line.split(";");
      int code = Integer.parseInt(parts[0]);
      String prefix = parts[1];
      codeToPrefixMapping.put(code, prefix);
      prefixToCodeMapping.put(prefix, code);
    }
  }
  
  public void open() throws IOException {
    if (this.isOpen)
      throw new XMLIndexException("Error opening NameStore. NameStore is already open.");
    logger.info("Opening name store ...");
    this.codeToNameMapping = new ConcurrentHashMap<Integer, Name>();
    this.nameToCodeMapping = new ConcurrentHashMap<Name, Integer>();
    this.codeToPrefixMapping = new ConcurrentHashMap<Integer, String>();
    this.prefixToCodeMapping = new ConcurrentHashMap<String, Integer>();
    this.readNameFile();
    this.readPrefixFile();
    this.isOpen = true;
  }
  
  public void close() {
    if (!isOpen)
      throw new XMLIndexException("Error closing NameStore. NameStore is not open.");
    logger.info("Closing name store ...");
    isOpen = false;
    this.codeToNameMapping = null;
    this.nameToCodeMapping = null;
    this.codeToPrefixMapping = null;
    this.prefixToCodeMapping = null;
    logger.info("Name store closed");
  }
  
  public boolean isOpen() {
    return this.isOpen;
  }
  
  public StructuredQName getStructuredQName(int nameCode, int prefixCode) {
    String prefix = (prefixCode == -1) ? "" : codeToPrefixMapping.get(prefixCode);
    Name name = codeToNameMapping.get(nameCode);
    String namespaceUri = StringUtils.defaultString(name.namespaceUri);
    return new StructuredQName(prefix, namespaceUri, name.localPart);
  }
  
  public StructuredQName getStructuredQName(int nameCode) {
    Name name = codeToNameMapping.get(nameCode);
    String namespaceUri = StringUtils.defaultString(name.namespaceUri);
    return new StructuredQName("", namespaceUri, name.localPart);
  }
  
  public int getNameCode(String namespaceUri, String localPart) {
    Integer nameCode = nameToCodeMapping.get(new Name(namespaceUri, localPart));
    if (nameCode == null)
      return -1;
    return nameCode.intValue();
  }
  
  public int putName(String namespaceUri, String localPart) throws IOException {
    name.namespaceUri = namespaceUri;
    name.localPart = localPart;
    Integer code = nameToCodeMapping.get(name);
    if (code == null) {
      synchronized (nameFile) {
        code = nameToCodeMapping.size();
        String line = Integer.toString(code) + ";" + namespaceUri + ";" + localPart;
        FileUtils.writeStringToFile(nameFile, line + '\n', "UTF-8", true);
        Name newName = name.clone();
        codeToNameMapping.put(code, newName);
        nameToCodeMapping.put(newName, code);
      }
    }
    return code;
  }
  
  public int putPrefix(String prefix) throws IOException {
    Integer code = prefixToCodeMapping.get(prefix);
    if (code == null) {
      synchronized (prefixFile) {
        code = prefixToCodeMapping.size();
        String line = Integer.toString(code) + ";" + prefix;
        FileUtils.writeStringToFile(prefixFile, line + '\n', "UTF-8", true);
        codeToPrefixMapping.put(code, prefix);
        prefixToCodeMapping.put(prefix, code);
      }
    }
    return code;
  }
  
  public boolean hasVirtualAttributeURI(int nameCode) {
    Name name = codeToNameMapping.get(nameCode);
    return StringUtils.equals(Definitions.NAMESPACE_VIRTUALATTR, name.namespaceUri);
  }
  
  public boolean isBuiltInVirtualAttributeName(int nameCode) {
    Name name = codeToNameMapping.get(nameCode);
    return StringUtils.equals(Definitions.NAMESPACE_VIRTUALATTR, name.namespaceUri) && name.localPart.startsWith("file-");
  }
  
  public boolean isNonBuiltInVirtualAttributeName(int nameCode) {
    Name name = codeToNameMapping.get(nameCode);
    return StringUtils.equals(Definitions.NAMESPACE_VIRTUALATTR, name.namespaceUri) && !name.localPart.startsWith("file-");
  }
  
  public static final class Name {
    
    public String namespaceUri;
    public String localPart;
  
    public Name(String namespaceUri, String localPart) {
      this.namespaceUri = namespaceUri;
      this.localPart = localPart; 
    }

    @Override
    public boolean equals(Object obj) {
      Name otherName = (Name) obj;
      return StringUtils.equals(namespaceUri, otherName.namespaceUri) && 
          StringUtils.equals(localPart, otherName.localPart);
    }

    @Override
    public int hashCode() {
      String str = StringUtils.defaultString(namespaceUri) + "_" + localPart;
      return str.hashCode();
    }
    
    public Name clone() {
      return new Name(namespaceUri, localPart);
    }
    
  }
   
}