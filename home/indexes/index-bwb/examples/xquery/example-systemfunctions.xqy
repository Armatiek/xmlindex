xquery version "3.1";

(: ExistsExpression op attribuut: :)
let $results-1 := (//deel[exists(@label-id)])[1]

(: ExistsExpression op attribuut: :)
let $results-2 := (//deel[@label-id])[1]

(: ExistsExpression op child element: :)
let $results-3 := (//kop[subtitel])[1]

(: Contains: :)
let $results-4 := (//citeertitel[contains(., 'motorrijtuigenbelasting')])[1]

(: StartsWith: :)
let $results-5 := (//citeertitel[starts-with(., 'Uitvoeringsbeschi')])[1]

(: EndsWith: :)
let $results-6 := (//citeertitel[ends-with(., 'beschouwd')])[1]

return $results-5