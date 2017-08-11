<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
  xmlns="http://www.w3.org/1999/xhtml"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"  
  xmlns:map="http://www.w3.org/2005/xpath-functions/map"
  xmlns:xhtml="http://www.w3.org/1999/xhtml"
  xmlns:xix="http://www.armatiek.nl/xmlindex/functions"
  xmlns:json="http://www.armatiek.nl/xmlindex/functions/json"
  xmlns:output="http://www.w3.org/2010/xslt-xquery-serialization"
  xmlns:err="http://www.w3.org/2005/xqt-errors"
  xmlns:local="urn:local"
  exclude-result-prefixes="#all"
  version="3.0">
  
  <xsl:output method="text"/>
  
  <xsl:param name="home-dir" as="xs:string"/>
  <xsl:param name="code" as="xs:string?"/>
  <xsl:param name="path" as="xs:string?"/>
  
  <xsl:variable name="output-parameters" as="element(output:serialization-parameters)">
    <output:serialization-parameters>
      <output:method value="xml"/>
      <output:indent value="yes"/>
      <output:omit-xml-declaration value="yes"/>
    </output:serialization-parameters>  
  </xsl:variable>
  
  <xsl:template match="/">  
    <xsl:variable name="base-uri" select="xs:anyURI(local:path-to-file-uri(concat($home-dir, '/', $path)))" as="xs:anyURI"/>
    <xsl:variable name="results" as="item()*">
      <xsl:choose>
        <xsl:when test="contains($code, 'xsl:stylesheet')">
          <xsl:sequence select="xix:transform-adhoc($code, $base-uri, (), false(), true())"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:sequence select="xix:query-adhoc($code, $base-uri, (), false(), true())"/>  
        </xsl:otherwise>
      </xsl:choose>  
    </xsl:variable>
    <xsl:text>{ "time" : </xsl:text> 
    <xsl:value-of select="$results[1]"/>
    <xsl:if test="$results[2]/self::err:error">
      <xsl:apply-templates select="$results[2]"/>
    </xsl:if>
    <xsl:text>, "result" : "</xsl:text>
    <xsl:sequence select="json:escape(serialize($results[2], $output-parameters))"/>  
    <xsl:text>"}</xsl:text>
  </xsl:template>
  
  <xsl:template match="err:code">
    <xsl:text>, "errCode" : "</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>"</xsl:text>
  </xsl:template>
  
  <xsl:template match="err:description">
    <xsl:text>, "errDescription" : "</xsl:text>
    <xsl:value-of select="json:escape(.)"/>
    <xsl:text>"</xsl:text>
  </xsl:template>
  
  <xsl:template match="err:module">
    <xsl:text>, "errModule" : "</xsl:text>
    <xsl:value-of select="json:escape(.)"/>
    <xsl:text>"</xsl:text>
  </xsl:template>
  
  <xsl:template match="err:line-number">
    <xsl:text>, "errLine" : </xsl:text>
    <xsl:value-of select="."/>
  </xsl:template>
  
  <xsl:template match="err:column-number">
    <xsl:text>, "errColumn" : </xsl:text>
    <xsl:value-of select="."/>
  </xsl:template>
  
  <xsl:function name="local:path-to-file-uri" as="xs:string">
    <xsl:param name="path" as="xs:string"/>
    <xsl:variable name="protocol-prefix" as="xs:string">
      <xsl:choose>
        <xsl:when test="starts-with($path, '\\')">file://</xsl:when> <!-- UNC path -->
        <xsl:when test="matches($path, '[a-zA-Z]:[\\/]')">file:///</xsl:when> <!-- Windows drive path -->
        <xsl:when test="starts-with($path, '/')">file://</xsl:when> <!-- Unix path -->
        <xsl:otherwise>file://</xsl:otherwise>
      </xsl:choose>  
    </xsl:variable>
    <xsl:variable name="norm-path" select="translate($path, '\', '/')" as="xs:string"/>
    <xsl:variable name="path-parts" select="tokenize($norm-path, '/')" as="xs:string*"/>
    <xsl:variable name="encoded-path" select="string-join(for $p in $path-parts return encode-for-uri($p), '/')" as="xs:string"/>
    <xsl:value-of select="concat($protocol-prefix, $encoded-path)"/>        
  </xsl:function>
  
</xsl:stylesheet>