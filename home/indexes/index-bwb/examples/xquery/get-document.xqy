xquery version "3.1";

declare namespace xix="http://www.armatiek.nl/xmlindex/functions";

let $uri as xs:string := "BWBR0001821\1998-01-01_0\xml\BWBR0001821_1998-01-01_0.xml"

return 
  xix:document($uri)