package nl.armatiek.xmlindex.test;

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.Type;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.conf.IndexConfig;
import nl.armatiek.xmlindex.conf.TypedValueDef;
import nl.armatiek.xmlindex.conf.VirtualAttributeDef;

public abstract class TestBase {
  
  protected void configure(XMLIndex index) throws SaxonApiException, IOException {
    IndexConfig config = index.getConfiguration();
    
    TypedValueDef tvd = new TypedValueDef(index, Type.ATTRIBUTE, new QName("inwerking"), BuiltInAtomicType.DATE);
    if (!config.getTypedValueConfig().exists(tvd.getNodeType(), tvd.getQName()))
      config.getTypedValueConfig().add(tvd);
    
    if (!config.getVirtualAttributeConfig().exists("expression-id"))
      config.getVirtualAttributeConfig().add(
        new VirtualAttributeDef(index, new QName("toestand"), "expression-id", 
        new QName(Definitions.NAMESPACE_VIRTUALATTR, "toestand-expression-id"), BuiltInAtomicType.STRING));
    
    if (!config.getVirtualAttributeConfig().exists("bwb-id"))
      config.getVirtualAttributeConfig().add(
        new VirtualAttributeDef(index, new QName("expression"), "bwb-id", 
        new QName(Definitions.NAMESPACE_VIRTUALATTR, "expression-bwb-id"), BuiltInAtomicType.STRING));
    
    if (!config.getVirtualAttributeConfig().exists("inwerkingtredingsdatum"))
      config.getVirtualAttributeConfig().add(
        new VirtualAttributeDef(index, new QName("expression"), "inwerkingtredingsdatum", 
        new QName(Definitions.NAMESPACE_VIRTUALATTR, "expression-inwerkingtredingsdatum"), BuiltInAtomicType.DATE));
    
    if (!config.getVirtualAttributeConfig().exists("einddatum"))
      config.getVirtualAttributeConfig().add(
        new VirtualAttributeDef(index, new QName("expression"), "einddatum", 
        new QName(Definitions.NAMESPACE_VIRTUALATTR, "expression-einddatum"), BuiltInAtomicType.DATE));
    
    if (!config.getVirtualAttributeConfig().exists("zichtdatum-start"))
      config.getVirtualAttributeConfig().add(
        new VirtualAttributeDef(index, new QName("expression"), "zichtdatum-start", 
        new QName(Definitions.NAMESPACE_VIRTUALATTR, "expression-zichtdatum-start"), BuiltInAtomicType.DATE));
    
    if (!config.getVirtualAttributeConfig().exists("zichtdatum-eind"))
      config.getVirtualAttributeConfig().add(
        new VirtualAttributeDef(index, new QName("expression"), "zichtdatum-eind", 
        new QName(Definitions.NAMESPACE_VIRTUALATTR, "expression-zichtdatum-eind"), BuiltInAtomicType.DATE));
    
    if (!config.getVirtualAttributeConfig().exists("citeertitel"))
      config.getVirtualAttributeConfig().add(
        new VirtualAttributeDef(index, new QName("toestand"), "citeertitel", 
        new QName(Definitions.NAMESPACE_VIRTUALATTR, "citeertitel"), BuiltInAtomicType.STRING, new StandardAnalyzer(), 
        new StandardAnalyzer()));
    
  }

}
