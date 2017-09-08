xquery version "3.1";

declare namespace map="http://www.w3.org/2005/xpath-functions/map";
declare namespace va="http://www.armatiek.nl/xmlindex/virtualattribute";

declare function va:file-name($elem as element(), $params as map(xs:string, item())?) as xs:string? {
  $params?name
};

declare function va:file-extension($elem as element(), $params as map(xs:string, item())?) as xs:string? {
  $params?extension
};

declare function va:file-path($elem as element(), $params as map(xs:string, item())?) as xs:string? {
  $params?path
};

declare function va:file-is-file($elem as element(), $params as map(xs:string, item())?) as xs:boolean? {
  $params?is-file
};

declare function va:file-parent-path($elem as element(), $params as map(xs:string, item())?) as xs:string? {
  $params?parent-path
};

declare function va:file-creation-time($elem as element(), $params as map(xs:string, item())?) as xs:dateTime? {
  $params?creation-time
};

declare function va:file-last-modified-time($elem as element(), $params as map(xs:string, item())?) as xs:dateTime? {
  $params?last-modified-time
};

declare function va:file-last-access-time($elem as element(), $params as map(xs:string, item())?) as xs:dateTime? {
  $params?last-access-time
};

declare function va:file-is-symbolic-link($elem as element(), $params as map(xs:string, item())?) as xs:boolean? {
  $params?is-symbolic-link
};

declare function va:file-size($elem as element(), $params as map(xs:string, item())?) as xs:long? {
  $params?size
};

()