xquery version "3.0";

declare namespace va="http://www.armatiek.nl/xmlindex/virtualattribute";

(: 
  Take into acccount that when an index is reindexed (for instance when adding a typed value definition), 
  the root of the passed element node is the document node of the complete index. When a new document is 
  added or an existing document is overwritten, the root is the document node of the new document.
:)

(:
declare function va:my-fts-function($elem as element()) as xs:string* {
  string-join($elem//text(), ' ')
};
:)

()