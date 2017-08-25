xquery version "3.1";

declare namespace va="http://www.armatiek.nl/xmlindex/virtualattribute";

let $results as element(toestand)* := subsequence(//toestand[@va:citeertitel = "pensio?n AND uitkerin*"], 1, 20)

return 
  <results hits="{count($results)}"> {
    for $t in $results return
      <toestand> {
        <bwb-id> {xs:string($t/@bwb-id)} </bwb-id>,
        <citeertitel> {xs:string($t/wetgeving/citeertitel)} </citeertitel>
      } </toestand>
  } </results>