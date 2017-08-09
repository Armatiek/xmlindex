<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema" 
  xmlns:json="http://www.armatiek.nl/xmlindex/functions/json"
  xmlns:file="http://expath.org/ns/file"
  xmlns:functx="http://www.functx.com"
  exclude-result-prefixes="#all"
  version="3.0">
  
  <xsl:output method="text"/>
  
  <xsl:param name="home-dir" as="xs:string"/>
  <xsl:param name="id" as="xs:string"/>
  
  <xsl:variable name="quote" as="xs:string">"</xsl:variable>
  <xsl:variable name="apos" as="xs:string">'</xsl:variable>
  
  <xsl:include href="functx-1.0.xsl"/>
  
  <xsl:template match="/">  
    <xsl:variable name="dir" select="concat($home-dir, '/', if ($id = '#') then () else $id)"/>
    <xsl:variable name="prefix-path" select="if ($id = ('', '#')) then () else json:escape(concat($id, '/'))" as="xs:string?"/>
    
    <xsl:text>[</xsl:text>
    <xsl:variable name="nodes" as="xs:string*">
      <xsl:for-each select="file:children($dir)">
        <xsl:sort select="." data-type="text"/>
        <xsl:variable name="path" select="concat($dir, '/', .)" as="xs:string"/>
        <xsl:if test="file:is-dir($path)">
          <xsl:value-of select="concat('{', $quote, 'id', $quote, ':', $quote, $prefix-path, ., $quote, ',', $quote, 'text', $quote, ':', $quote, 
            file:name(.), $quote, ',', $quote, 'type', $quote, ':', $quote, 'folder', $quote, ',', $quote, 'children', $quote, if (count(file:children($path)) gt 0) then ':true' else ':false', '}')"/>
        </xsl:if>
      </xsl:for-each>
      <!-- Files: -->
      <xsl:for-each select="file:children($dir)">
        <xsl:sort select="." data-type="text"/>
        <xsl:variable name="path" select="concat($dir, '/', .)" as="xs:string"/>
        <xsl:if test="file:is-file($path)">
          <xsl:value-of select="concat('{', $quote, 'id', $quote, ':', $quote, $prefix-path, ., $quote, ',', $quote, 'text', $quote, ':', $quote, 
            file:name(.), $quote, ',', $quote, 'type', $quote, ':', $quote, 'file', $quote, ',', $quote, 'children', $quote, ':false', '}')"/>
        </xsl:if>
      </xsl:for-each>  
    </xsl:variable>
    <xsl:sequence select="string-join($nodes, ',')"/>
    <xsl:text>]</xsl:text>
  </xsl:template>
  
</xsl:stylesheet>