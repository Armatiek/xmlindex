package nl.armatiek.xmlindex.saxon.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryExecutable;

public class XQueryModuleCompiler {

  public static XQueryExecutable compile(XQueryCompiler compiler, File moduleFile) throws IOException, SaxonApiException {
    String moduleCode = FileUtils.readFileToString(moduleFile, StandardCharsets.UTF_8);
    moduleCode = moduleCode.replaceAll("\\(:.*?:\\)", ""); // Strip comments
    moduleCode = moduleCode.replaceAll("import\\s+module", ""); // Make sure import module statements don't match 
    String prefix = null;
    String moduleNamespace = null;
    Matcher matcher = Pattern.compile("module\\s+namespace\\s+([\\w+-\\.])\\s*=\\s*[\\\"|'](.*?)[\\\"|']").matcher(moduleCode);
    if (matcher.find()) {
      prefix = matcher.group(1);
      moduleNamespace = matcher.group(2);
    }
    String mainQuery = "import module namespace " + prefix + " = '" + moduleNamespace + "' at '" + moduleFile.getName() + "'; ()";
    compiler.setBaseURI(moduleFile.toURI());
    return compiler.compile(mainQuery);
  }
  
}