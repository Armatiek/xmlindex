xquery version "3.1";

declare namespace va="http://www.armatiek.nl/xmlindex/virtualattribute";

(: Zoek in manifest files naar expressions op basis van virtueel attribuut bwb-id: :)
let $results-1 := //expression[@va:bwb-id = 'BWBR0001840']

(: Zoek in manifest files naar expressions op basis van virtueel attributen bwb-id en inwerkingtredingsdatum: :)
let $results-2 := //expression[@va:bwb-id = 'BWBR0001840' and @va:inwerkingtredingsdatum = xs:date('2008-07-15')]

return $results-1