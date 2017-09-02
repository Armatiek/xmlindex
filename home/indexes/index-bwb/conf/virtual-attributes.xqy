xquery version "3.1";

declare namespace map="http://www.w3.org/2005/xpath-functions/map";
declare namespace va="http://www.armatiek.nl/xmlindex/virtualattribute";

import module namespace functx = "http://www.functx.com" at "functx-1.0.xq";

declare function va:file-name($elem as element(), $params as map(xs:string, item())) as xs:string? {
  $params?name
};

declare function va:file-path($elem as element(), $params as map(xs:string, item())) as xs:string? {
  $params?path
};

declare function va:file-creation-time($elem as element(), $params as map(xs:string, item())) as xs:dateTime? {
  $params?creation-time
};

declare function va:file-last-modified-time($elem as element(), $params as map(xs:string, item())) as xs:dateTime? {
  $params?last-modified-time
};

declare function va:file-last-access-time($elem as element(), $params as map(xs:string, item())) as xs:dateTime? {
  $params?last-access-time
};

declare function va:file-is-symbolic-link($elem as element(), $params as map(xs:string, item())) as xs:boolean? {
  $params?is-symbolic-link
};

declare function va:file-size($elem as element(), $params as map(xs:string, item())) as xs:long? {
  $params?size
};

declare function va:toestand-expression-id($elem as element()) as xs:string {
  concat($elem/@bwb-id, '_', $elem/@inwerkingtreding, '_', functx:substring-after-last($elem/@bwb-ng-vast-deel, '/'))
};

declare function va:expression-bwb-id($elem as element()) as xs:string {
  xs:string($elem/root()/work/@label)
};

declare function va:expression-inwerkingtredingsdatum($elem as element()) as xs:date {
  xs:date($elem/metadata/datum_inwerkingtreding)
};

declare function va:expression-einddatum($elem as element()) as xs:date {
  xs:date($elem/metadata/einddatum)
};

declare function va:expression-zichtdatum-start($elem as element()) as xs:date {
  xs:date($elem/metadata/zichtdatum_start)
};

declare function va:expression-zichtdatum-eind($elem as element()) as xs:date {
  xs:date($elem/metadata/zichtdatum_eind)
};

declare function va:citeertitel($elem as element()) as xs:string {
  string-join($elem/wetgeving/citeertitel//text(), ' ')
};

()