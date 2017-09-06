xquery version "3.1";

declare namespace map="http://www.w3.org/2005/xpath-functions/map";
declare namespace sva="http://www.armatiek.nl/xmlindex/standard-virtualattribute";

declare function sva:file-name($elem as element(), $params as map(xs:string, item())?) as xs:string? {
  $params?name
};

declare function sva:file-path($elem as element(), $params as map(xs:string, item())?) as xs:string? {
  $params?path
};

declare function sva:file-parent-path($elem as element(), $params as map(xs:string, item())?) as xs:string? {
  $params?parent-path
};

declare function sva:file-creation-time($elem as element(), $params as map(xs:string, item())?) as xs:dateTime? {
  $params?creation-time
};

declare function sva:file-last-modified-time($elem as element(), $params as map(xs:string, item())?) as xs:dateTime? {
  $params?last-modified-time
};

declare function sva:file-last-access-time($elem as element(), $params as map(xs:string, item())?) as xs:dateTime? {
  $params?last-access-time
};

declare function sva:file-is-symbolic-link($elem as element(), $params as map(xs:string, item())?) as xs:boolean? {
  $params?is-symbolic-link
};

declare function sva:file-size($elem as element(), $params as map(xs:string, item())?) as xs:long? {
  $params?size
};

()