xquery version "3.0";

declare namespace va="http://www.armatiek.nl/xmlindex/virtualattribute";
declare namespace fx="http://www.armatiek.nl/xmlindex/functions";
declare namespace local="urn:local";

import module namespace functx = "http://www.functx.com" at "functx-1.0.xq";

(: declare variable $bwb-id as xs:string external; :)

declare function local:get-toestand($context-node as node(), $bwb-id as xs:string, $inwerkingtredingsdatum as xs:date, $einddatum as xs:date) as element(toestand)* {
  let $current-date := current-date()
  let $manifest-work as element(work)? := fx:copy-to-memory($context-node//work[@label = $bwb-id])
  let $expression as element(expression)? := 
    $manifest-work/expression[
      (metadata/datum_inwerkingtreding >= $inwerkingtredingsdatum) and 
      (metadata/einddatum <= $einddatum) and
      (metadata/zichtdatum_start <= $current-date) and
      (metadata/zichtdatum_eind >= $current-date)]
  let $expression-id as xs:string := concat($expression/parent::work/@label, '_', $expression/@label)
  return $context-node//toestand[@bwb-id = $bwb-id][$expression-id = concat(@bwb-id, '_', @inwerkingtreding, '_', functx:substring-after-last(@bwb-ng-vast-deel, '/'))]
};

declare function local:get-toestand-va($context-node as node(), $bwb-id as xs:string, $inwerkingtredingsdatum as xs:date, $einddatum as xs:date) as element(toestand)* {
  let $current-date := current-date()
  let $expression as element(expression)? := 
    $context-node//expression[
      (@va:bwb-id = $bwb-id) and 
      (@va:inwerkingtredingsdatum >= $inwerkingtredingsdatum) and 
      (@va:einddatum <= $einddatum) and
      (@va:zichtdatum-start <= $current-date) and
      (@va:zichtdatum-eind >= $current-date)]
  let $expression-id as xs:string := concat($expression/parent::work/@label, '_', $expression/@label)
  return $context-node//toestand[@va:expression-id = $expression-id]
};

let $bwb-id as xs:string := 'BWBR0001888'
let $inwerkingtredingsdatum as xs:date := xs:date('2004-01-01')
let $einddatum as xs:date := xs:date('2004-02-29')

return
  <result> {
    local:get-toestand-va(root(), $bwb-id, $inwerkingtredingsdatum, $einddatum)
  } </result>
  