xquery version "3.1";

import module namespace jci = "http://www.armatiek.nl/xmlindex/juriconnect" at "juriconnect.xqm";
import module namespace bwb = "http://www.armatiek.nl/xmlindex/bwb" at "bwb.xqm";

declare namespace svc="http://www.armatiek.nl/xmlindex/service";
declare namespace rest="http://exquery.org/ns/restxq";
declare namespace request="http://exquery.org/ns/request";
declare namespace output="http://www.w3.org/2010/xslt-xquery-serialization";
declare namespace config="http://www.armatiek.nl/xmlindex/config";
declare namespace xix="http://www.armatiek.nl/xmlindex/functions";

declare variable $config:development-mode as xs:boolean external;

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
        <input type="submit" value="Submit"/>
      </form>
    </body>
  </html>
};

()