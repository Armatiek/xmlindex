<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
  xmlns="http://www.w3.org/1999/xhtml"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"  
  xmlns:xix="http://www.armatiek.nl/xmlindex/functions"
  xmlns:json="http://www.armatiek.nl/xmlindex/functions/json"
  xmlns:output="http://www.w3.org/2010/xslt-xquery-serialization"
  xmlns:err="http://www.w3.org/2005/xqt-errors"
  exclude-result-prefixes="#all"
  version="3.0">
  
  <xsl:output method="text"/>
  
  <xsl:param name="code" as="xs:string?"/>
  
  <xsl:variable name="output-parameters" as="element(output:serialization-parameters)">
    <output:serialization-parameters>
      <output:method value="xml"/>
      <output:indent value="yes"/>
      <output:omit-xml-declaration value="yes"/>
    </output:serialization-parameters>  
  </xsl:variable>
  
  <xsl:template match="/">
    <xsl:variable name="results" as="item()*">
      <xsl:choose>
        <xsl:when test="contains($code, 'xsl:stylesheet')">
          <xsl:sequence select="xix:explain-transformation($code, true())"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:sequence select="xix:explain-query($code, true())"/>  
        </xsl:otherwise>
      </xsl:choose>  
    </xsl:variable>
    <xsl:text>{ "time" : 0</xsl:text> 
    <xsl:if test="$results[1]/self::err:error">
      <xsl:apply-templates select="$results[1]"/>
    </xsl:if>
    <xsl:text>, "result" : "</xsl:text>
    <xsl:sequence select="json:escape(serialize($results[1], $output-parameters))"/>  
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
  
</xsl:stylesheet>