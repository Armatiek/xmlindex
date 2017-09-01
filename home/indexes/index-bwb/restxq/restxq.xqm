xquery version "3.1";

import module namespace jci = "http://www.armatiek.nl/xmlindex/juriconnect" at "juriconnect.xqm";
import module namespace bwb = "http://www.armatiek.nl/xmlindex/bwb" at "bwb.xqm";

declare namespace svc="http://www.armatiek.nl/xmlindex/service";
declare namespace rest="http://exquery.org/ns/restxq";
declare namespace http="http://expath.org/ns/http-client";
declare namespace request="http://exquery.org/ns/request";
declare namespace output="http://www.w3.org/2010/xslt-xquery-serialization";
declare namespace config="http://www.armatiek.nl/xmlindex/config";
declare namespace xix="http://www.armatiek.nl/xmlindex/functions";

declare variable $config:development-mode as xs:boolean external;

declare function svc:process(
  $area as xs:string*, 
  $work as xs:string*, 
  $expression as xs:string*, 
  $manifestation as xs:string*
  $item as xs:string*) as element()? {
  if ($area) then
    svc:process-work($work, $expression, $manifestation, $item)
  else
    ()
};

declare function svc:process-work(
  $work as xs:string*, 
  $expression as xs:string*, 
  $manifestation as xs:string*, 
  $item as xs:string*) as element()? {
  if ($work) then
    let $work-elem as element(work)? := xix:index-root()//work[@label = $work]
    return 
      if ($work-elem) then
        svc:process-expression($work-elem, $expression, $manifestation, $item)
      else
        ()
  else 
    ()
};

declare function svc:process-expression(
  $work-elem as element(work), 
  $expression as xs:string?, 
  $manifestation as xs:string*, 
  $item as xs:string*) as element()? {
  if ($expression) then
    let $expression-elem as element(expression)? := $work-elem/expression[@label = $expression]
    return 
      if ($expression-elem) then
        svc:process-manifestation($expression-elem, $manifestation, $item)
      else
        ()
  else 
    ()
};

declare function svc:process-manifestation(
  $expression-elem as element(expression), 
  $manifestation as xs:string*, 
  $item as xs:string*) as element()? {
  if ($manifestation) then
    let $manifestation-elem as element(manifestation)? := $expression-elem/manifestation[@label = $manifestation]
    return 
      if ($manifestation-elem) then
        svc:process-item($manifestation-elem, $item)
      else
        ()
  else 
    ()
};

declare function svc:process-item(
  $manifestation-elem as element(item), 
  $item as xs:string*) as element()? {
  if ($item) then
    let $item-elem as element(item)? := $manifestation-elem/item[@label = $item]
    return 
      if ($item-elem) then
        $item-elem
      else
        ()
  else 
    ()
};

declare
  %rest:path("/{$area}/{$work}/{$expression}/{$manifestation}/{$item}")
  %rest:GET
  %output:method("xml")
function svc:frbr-path(
  $area as xs:string*, 
  $work as xs:string*, 
  $expression as xs:string*, 
  $manifestation as xs:string*,
  $item as xs:string*) as element()? {
  svc:process($area, $work, $expression, $manifestation, $item)
};

(:~
 : Deze functie retourneert alle regelinonderdelen waar de juriconnect $jci naar verwijst
 : @return de regelingonderdelen in XML formaat
 :)
declare
  %rest:path("/juriconnect")
  %rest:GET
  %rest:query-param("jci", "{$jci}")
  %output:method("xml")
function svc:juriconnect($jci as xs:string*) as element()? {
  let $toestand as element(toestand)? := bwb:get-toestand(xix:index-root(), $jci)
  return 
    if (not(jci:has-location-string($jci))) then
      $toestand
    else
      let $result as element()* := bwb:get-onderdelen($toestand, $jci, false())
      return
        <result jci="{$jci}" aantal-onderdelen="{count($result)}"> {
          bwb:get-onderdelen($toestand, $jci, false())
        } </result>  
};


declare 
  %rest:path("/error404")
  %rest:GET
function svc:error404() {
  <rest:response>
    <http:response status="404" message="I was not found.">
      <http:header name="Content-Language" value="en"/>
      <http:header name="Content-Type" value="text/html; charset=utf-8"/>
    </http:response>
  </rest:response>
};

(:~
 : This function generates the demo page.
 : @return HTML page
 :)
declare
  %rest:path("/")
  %rest:GET
  %output:method("html")
  %output:encoding("UTF-8")
  %output:omit-xml-declaration("yes")
function svc:demo() as element(html) {
  <html>
    <head>
      <title>Juriconnect service demo</title>
      <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    </head>
    <body>
      <form action="juriconnect">
        Juriconnect:<br/><br/>
        <input name="jci" action="juriconnect" method="GET" size="100" value="jci1.3:c:BWBR0001830&amp;hoofdstuk=1&amp;artikel=1&amp;o=b&amp;o=1°&amp;z=2003-02-03&amp;g=2003-02-02"/>
        <br/><br/>
        AOW artikel 17c:<br/>jci1.3:c:BWBR0002221&amp;hoofdstuk=III&amp;paragraaf=2&amp;artikel=17c&amp;z=2017-08-01&amp;g=2017-08-01<br/>
        Meerdere artikelen 1:<br/>jci1.3:c:BWBR0008994&amp;artikel=1&amp;z=1998-01-01&amp;g=1998-01-01
        <br/><br/>
        <input type="submit" value="Submit"/>
      </form>
    </body>
  </html>
};

()