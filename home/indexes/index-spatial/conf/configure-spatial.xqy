xquery version "3.1";

declare namespace map="http://www.w3.org/2005/xpath-functions/map";
declare namespace xmi="http://www.armatiek.nl/xmlindex/functions";

let $class-name as xs:string := 'nl.armatiek.xmlindex.extensions.SpatialIndex'
let $params as map(xs:string, xs:string) := 
  map { 
    'fieldName' : 'shape', 
    'prefixTreeMaxLevels' : '11' }

return
  if (not(xmi:pluggable-index-exists($class-name))) then
    xmi:add-pluggable-index($class-name, $params)
  else
    false()