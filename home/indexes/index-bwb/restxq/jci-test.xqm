xquery version "3.1";

import module namespace jci = "http://www.armatiek.nl/xmlindex/juriconnect" at "juriconnect.xqm";
import module namespace bwb = "http://www.armatiek.nl/xmlindex/bwb" at "bwb.xqm";

let $jci as xs:string := "jci1.3:c:BWBR0001830&amp;hoofdstuk=1&amp;artikel=1&amp;o=b&amp;o=1°&amp;z=2003-02-03&amp;g=2003-02-02"
let $toestand as element(toestand)? := bwb:get-toestand(/, $jci)
return bwb:get-onderdelen($toestand, $jci, false())
(: return $toestand :)