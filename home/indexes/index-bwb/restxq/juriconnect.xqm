module namespace  jci = "http://www.armatiek.nl/xmlindex/juriconnect";

declare namespace fn = "http://www.w3.org/2005/xpath-functions";

declare variable $jci:date-pattern as xs:string := "\d{4}-\d{2}-\d{2}";
declare variable $jci:jci-onderdeelnamen as xs:string+ := ('aanwijzing', 'afdeling', 'artikel', 'bijlage', 'boek', 
  'deel', 'hoofdstuk', 'inhoudsopgave', 'paragraaf', 'sub-paragraaf', 'titeldeel', 'lid', 'o');
  
declare variable $jci:elementen-met-label as xs:string* := ('afdeling', 'artikel', 'bijlage', 'boek', 'circulaire.divisie', 
  'deel', 'divisie', 'hoofdstuk', 'lid', 'li', 'officiele-inhoudsopgave', 'paragraaf', 'sub-paragraaf', 'titeldeel', 'verdragtekst');
declare variable $jci:elementen-zonder-label as xs:string* := ('lid', 'li');

declare function jci:get-bwb-id($jci as xs:string) as xs:string? {
  tokenize($jci, '[:&amp;]')[3]
};

declare function jci:get-geldigheidsdatum($jci as xs:string) as xs:date? {
  jci:get-datum($jci, "g")
};

declare function jci:get-zichtdatum($jci as xs:string) as xs:date? {
  jci:get-datum($jci, "z")
};

declare function jci:get-location-parts($jci as xs:string) as xs:string* {
  let $jci-stripped as xs:string := substring-after($jci, ':c:')
  let $parts as xs:string* := tokenize($jci-stripped, '&amp;')
  return $parts[position() gt 1][not(matches(., '^(z=|g=|s=|e=)'))]
};

declare function jci:get-location-string($jci as xs:string) as xs:string* {
  string-join(jci:get-location-parts($jci), '&amp;')
};

declare function jci:has-location-string($jci as xs:string) as xs:boolean {
  exists(jci:get-location-parts($jci))
};

declare function jci:remove-dates($jci as xs:string) as xs:string {
  replace($jci, '&amp;[z|g|s|e]=\d{4}-\d{2}-\d{2}', '')
};

declare function jci:get-regeling-jci($context as node()?, $version as xs:string) as xs:string? {
  $context/root()/toestand/wetgeving/meta-data/jcis/jci[@versie = $version]/@verwijzing cast as xs:string
};

declare function jci:get-datum($jci as xs:string, $param-name as xs:string) as xs:date? {
  let $reg-ex as xs:string := concat("&amp;", $param-name, "=(", $jci:date-pattern, ")")
  let $date-str as xs:string? := fn:analyze-string($jci, $reg-ex)/fn:match/fn:group[@nr='1'] cast as xs:string
  return if ($date-str) then xs:date($date-str) else ()
};