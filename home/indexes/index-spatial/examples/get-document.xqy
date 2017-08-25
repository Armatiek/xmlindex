xquery version "3.1";

declare namespace xix="http://www.armatiek.nl/xmlindex/functions";

let $uri as xs:string := "Landsgrens.xml"

return 
  xix:document($uri)