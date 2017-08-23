module namespace bwb = "http://www.armatiek.nl/xmlindex/bwb";

declare namespace map="http://www.w3.org/2005/xpath-functions/map";
declare namespace va = "http://www.armatiek.nl/xmlindex/virtualattribute";
declare namespace xix="http://www.armatiek.nl/xmlindex/functions";

import module namespace jci = "http://www.armatiek.nl/xmlindex/juriconnect" at "juriconnect.xqm";

declare variable $bwb:label-map as map(xs:string, xs:string+) := 
  map {
    "aanwijzing"    : ('aanwijzing'),
    "afdeling"      : ('afdeling', 'afdeeling'),
    "artikel"       : ('artikel', 'artikelen', 'art', 'article', 'articles', 'articolo', 'artigo', 'artículo', 'ad artikel', 'ad article', 'ad articles', 'atikel'),
    "bijlage"       : ('bijlage', 'bijlagen', 'bijlage:', 'addendum', 'annex', 'annexe', 'anexo', 'anlage', 'appendix', 'attachment', 'aanhangsel', 'supplement'),
    "boek"          : ('boek'),
    "deel"          : ('deel', 'onderdeel', 'sectie', 'part', 'parte', 'partie', 'section'),
    "hoofdstuk"     : ('hoofdstuk', 'chapter', 'chapitre', 'capitulo', 'hoofstuk'),
    "inhoudsopgave" : ('inhoudsopgave'),
    "paragraaf"     : ('paragraaf', '§', 'paragraph', 'par'),
    "sub-paragraaf" : ('sub-paragraaf', 'subparagraaf'),
    "titeldeel"     : ('titeldeel', 'titel', 'title', 'titre', 'titulo'),
    "lid"           : ('lid'),
    "o"             : ('li')  
  };

declare function bwb:get-toestand(
  $context-node as node(), 
  $bwb-id as xs:string, 
  $geldigheidsdatum as xs:date?, 
  $zichtdatum as xs:date?) 
  as element(toestand)* 
{
  let $work as element(work)? := $context-node//work[@label = $bwb-id]
  let $z as xs:date := if (exists($zichtdatum)) then $zichtdatum else current-date()
  let $g as xs:date := if (exists($geldigheidsdatum)) then $geldigheidsdatum else current-date()
  let $expr as element(expression)? :=
    $work//expression[
      (@va:bwb-id = $bwb-id) and
      (@va:inwerkingtredingsdatum le $g) and
      (@va:einddatum ge $g) and
      (@va:zichtdatum-start le $z) and
      (@va:zichtdatum-eind ge $z)]    
  let $expression as element(expression)? :=  
    if ($expr) then
      $expr
    else
      let $expressions := 
        for $e in $work//expression 
          order by $e/metadata/datum_inwerkingtreding descending, $e/metadata/zichtdatum_start descending
          return $e
      return $expressions[1]  
  let $expression-id as xs:string? := concat($expression/parent::work/@label, '_', $expression/@label)
  return $context-node//toestand[@va:expression-id = $expression-id]
};

declare function bwb:get-toestand(
  $context-node as node(), 
  $jci as xs:string) 
  as element(toestand)*
{
  let $bwb-id as xs:string? := jci:get-bwb-id($jci)
  let $geldigheidsdatum as xs:date? := jci:get-geldigheidsdatum($jci)
  let $zichtdatum as xs:date? := jci:get-zichtdatum($jci)
  return bwb:get-toestand($context-node, $bwb-id, $geldigheidsdatum, $zichtdatum)
};

declare function bwb:normalize-nummer(
  $nummer as xs:string?) 
  as xs:string?
{
  (:
    1/ normalize space
    2/ vervang leading en trailing non-alfanumerieke karakters (uitgezonderd °)
    3/ vervang spaties door underscores
  :)
  translate(replace(normalize-space($nummer), '^[^a-zA-Z0-9°]+|[^a-zA-Z0-9°\*]+$', ''), ' ', '_')
};

declare function bwb:normalize-onderdeel-nummer(
  $nummer as xs:string?)
  as xs:string?
{
  (:
    1/ normalize space
    2/ vervang leading en trailing non-alfanumerieke karakters (uitgezonderd °) en specifieke leading woorden
    3/ vervang spaties door underscores
  :)
  translate(replace(normalize-space($nummer), '^((aantekening|bedrijfstak|bedrijfstakken|bijlage|categorie|category|classe|division|groep|nr|optie|section|stap|voorbeeld)|[^a-zA-Z0-9°])+|[^a-zA-Z0-9°\*]+$', '', 'i'), ' ', '_')
};
  
declare function bwb:normalize-label(
  $label as xs:string?)
  as xs:string?
{
  (:
    1/ normalize-space en lower-case, 
    2/ verwijder telwoorden (Eerste, Tweede, ...)
    3/ verwijder niet letter karakters en spaties
    4/ normalize-space
  :)
  let $label-1 as xs:string? := lower-case(normalize-space($label))
  let $label-2 as xs:string? := replace($label-1, '^(eerste|tweede|derde|vierde|vijfde|zesde|zevende|achtste|negende|tiende|elfde|twaalfde|dertiende|veertiende|vijftiende|zestiende|zeventiende|achttiende|negentiende|twintigste)', '')
  let $label-3 as xs:string? := replace($label-2, '[^a-zA-Z\s]', '')
  return normalize-space($label-3)
};

declare function bwb:jci-onderdeel-naam(
  $element-naam as xs:string,
  $kop-label as xs:string?)
  as xs:string?
{
  let $label as xs:string? := bwb:normalize-label($kop-label)
  return
    if ($element-naam = 'lid') then
      (: Speciaal geval: lid (lid elementen hebben geen label) :)
      'lid'
    else if ($element-naam = 'li') then
      (: Speciaal geval: li (li elementen hebben geen label): :)
      'o'
    else if ($element-naam = 'officiele-inhoudsopgave') then
      (: Speciaal geval: officiele-inhoudsopgave mapped altijd naar inhoudsopgave :)
      'inhoudsopgave'
    else if ($element-naam = 'sub-paragraaf') then
      (: Speciaal geval: sub-paragraaf, anders worden labels 'paragraaf', '§' etc onder sub-paragraaf gemapped op paragraaf :)
      if ($label = $bwb:label-map("afdeling")) then
        'afdeling'
      else if ($label = $bwb:label-map("titeldeel")) then
        'titeldeel'
      else
        'sub-paragraaf'
    else if ($label = $bwb:label-map("aanwijzing")) then 'aanwijzing'
    else if ($label = $bwb:label-map("afdeling")) then 'afdeling'
    else if ($label = $bwb:label-map("artikel")) then 'artikel'
    else if ($label = $bwb:label-map("bijlage")) then 'bijlage'
    else if ($label = $bwb:label-map("boek")) then 'boek'
    else if ($label = $bwb:label-map("deel")) then 'deel'
    else if ($label = $bwb:label-map("hoofdstuk")) then 'hoofdstuk'
    else if ($label = $bwb:label-map("paragraaf")) then 'paragraaf'
    else if ($label = $bwb:label-map("titeldeel")) then 'titeldeel'
    (: Als label niet kon worden gemapped, dan uitgaan van XML elementnaam: :)
    else if ($element-naam = $jci:jci-onderdeelnamen) then $element-naam 
    (: Anders kon geen onderdeel naam worden herleid :)
    else () 
}; 

declare function bwb:jci-onderdeel-nummer(
  $element as element())
  as xs:string?
{
  if ($element/self::lid) then
    bwb:normalize-nummer($element/lidnr[1])
  else if ($element/self::li) then
    bwb:normalize-onderdeel-nummer($element/li.nr[1])
  else  (: TODO :)
    bwb:normalize-nummer($element/kop/nr[1])
};  

declare function bwb:get-onderdelen(
  $context-elems as element()*, 
  $location-params as xs:string*, 
  $index as xs:integer,
  $exact as xs:boolean) 
  as element()*
{
  if ($context-elems and ($index le count($location-params))) then
    let $part as xs:string? := $location-params[$index]
    let $nv as xs:string* := tokenize($part, '=')
    let $name as xs:string? := lower-case($nv[1])
    let $nr as xs:string? := if (count($nv) gt 1) then translate($nv[2], '_', ' ') else ()
    let $candidate-elems as element()* := $context-elems//*[local-name() = ($jci:elementen-met-label, $jci:elementen-zonder-label)]
    let $descendants-with-matching-name as element()* := $candidate-elems[bwb:jci-onderdeel-naam(local-name(), kop/label) = $name]
    let $descendants-with-matching-name-nr as element()* := $descendants-with-matching-name[bwb:jci-onderdeel-nummer(.) = $nr] 
    return bwb:get-onderdelen($descendants-with-matching-name-nr, $location-params, $index+1, $exact)
  else if ($exact and ($index le count($location-params))) then 
    ()
  else 
    $context-elems
};

declare function bwb:get-onderdelen(
  $context-elem as element(), 
  $jci as xs:string,
  $exact as xs:boolean)
  as element()*
{
  let $location-params as xs:string* := jci:get-location-parts($jci)
  let $index as xs:integer := 1
  return bwb:get-onderdelen(xix:copy-to-memory($context-elem), $location-params, $index, $exact)
};