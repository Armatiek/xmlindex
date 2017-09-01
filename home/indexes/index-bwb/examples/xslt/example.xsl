<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  exclude-result-prefixes="#all"
  version="3.0">
  
  <xsl:template match="/">
    <resultaat>
      <titels>
        <xsl:apply-templates select="subsequence(//citeertitel[starts-with(., 'Wijzigingswet')], 1, 100)"/>
      </titels>
      <xsl:sequence select="(//kop[label='Titre']/parent::titeldeel)[1]"/>
      <xsl:sequence select="//toestand[@bwb-id = 'BWBR0001822']"/>
    </resultaat>
  </xsl:template>
  
  <xsl:template match="citeertitel">
    <titel>
      <inwerking>
        <xsl:value-of select="@inwerking"/>
      </inwerking>
      <value>
        <xsl:value-of select="."/>
      </value>
    </titel>
  </xsl:template>

</xsl:stylesheet>