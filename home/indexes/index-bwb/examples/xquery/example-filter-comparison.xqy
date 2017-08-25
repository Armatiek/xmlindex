xquery version "3.1";

(: ComparisonExpression op context item: :)
let $results-1 := (//label[. = 'Titre'])[1]

(: ComparisonExpression op attribuut: :)
let $results-2:= (//artikel[@label-id = '1043924'])[1]

(: BooleanExpression van ComparisonExpressions op attributen: :) 
let $results-3:= (//artikel[@bron = 'Stcrt.2006-250' and @effect="wijziging" and @ondertekening_bron = '2006-12-20'])[1]

(: BooleanExpression van ComparisonExpression op attributen met operator >= op typed value: :) 
let $results-4:= (//artikel[@bron = 'Stcrt.2006-250' and @inwerking >= xs:date('2006-12-20')])[1]

(: ComparisonExpression op child element: :)
let $results-5:= //lid[lidnr = '67']

(: ComparisonExpression op combinatie van attributen en child elementen: :)
let $results-6:= //lid[lidnr = '3' and @label-id='1043954L3']

return $results-1