xquery version "3.1";

declare namespace map="http://www.w3.org/2005/xpath-functions/map";
declare namespace va="http://www.armatiek.nl/xmlindex/virtualattribute";

(: 
Take into acccount that when an index is reindexed (for instance when adding a typed value definition), 
the root of the passed element node is the document node of the complete index. When a new document is 
added or an existing document is overwritten, the root is the document node of the new in memory 
document.
:)

(:

declare function va:my-function($elem as element()) as xs:string* {
  xs:string($elem/my-element1/my-element2/@my-attribute)
};

declare function va:my-fts-function($elem as element()) as xs:string* {
  string-join($elem//text(), ' ')
};

declare function va:my-file-creation-time($elem as element(), $params as map(xs:string, item())) as xs:dateTime? {
  $params?creation-time
};

:)

()