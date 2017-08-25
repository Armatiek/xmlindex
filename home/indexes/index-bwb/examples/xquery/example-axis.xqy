xquery version "3.1";

let $descendants as element()* := //plaatsbepaling | subsequence(//tekstcorrectie, 1, 5)

let $count as xs:integer := count($descendants/ancestor::wetgeving)

return 
  <results count="{$count}"> {
    $descendants
  } </results>