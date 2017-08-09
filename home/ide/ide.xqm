xquery version "3.1";

declare namespace rest="http://exquery.org/ns/restxq";
declare namespace page="http://www.armatiek.nl/web-page";
declare namespace config="http://www.armatiek.nl/xmlindex/config";
declare namespace request="http://exquery.org/ns/request";
declare namespace file="http://expath.org/ns/file";

declare variable $config:development-mode as xs:boolean external;
declare variable $config:home-dir as xs:string external;

(:~
 : This function generates the home page.
 : @return HTML page
 :)
declare
  %rest:path("/")
  %rest:GET
  %output:method("html")
  %output:omit-xml-declaration("no")
function page:home() as element(html) {
  let $source-node as document-node() := document { <root/> }
  let $result := transform(
    map {
      "stylesheet-location": "xsl/main.xsl",
      "source-node": $source-node,
      "stylesheet-params": map {
        QName("", "app-path") : concat(rest:base-uri(), "/static"),
        QName("", "home-dir") : $config:home-dir
      },
      "cache" : not($config:development-mode)
    })
  return $result?output/*
};

declare
  %rest:path("/open")
  %rest:GET
  %rest:query-param("path", "{$path}")
  %output:method("text")
function page:open($path as xs:string*) as xs:string? {
  file:read-text($config:home-dir || file:dir-separator() || $path, 'UTF-8')
};

declare
  %rest:path("/save")
  %rest:POST
  %rest:form-param("path", "{$path}")
  %rest:form-param("code", "{$code}")
function page:save($path as xs:string*, $code as xs:string*) as empty-sequence() {
  file:write-text($config:home-dir || file:dir-separator() || $path, $code, "UTF-8")
};

declare
  %rest:path("/run")
  %rest:GET
  %output:method("xml")
  %output:omit-xml-declaration("yes")
function page:run() as element()? {
};

declare
  %rest:path("/filesystem")
  %rest:GET
  %rest:query-param("id", "{$id}")
  %output:method("text")
function page:filesystem($id as xs:string*) as xs:string {
  let $source-node as document-node() := document { <root/> }
  let $result := transform(
    map {
      "stylesheet-location": "xsl/filesystem.xsl",
      "source-node": $source-node,
      "stylesheet-params": map {
        QName("", "home-dir") : $config:home-dir,
        QName("", "id") : $id
      },
      "cache" : not($config:development-mode)
    })
  return $result?output
};

declare
  %rest:path("/createfolder")
  %rest:POST
  %rest:form-param("path", "{$path}")
function page:createfolder($path as xs:string*) as empty-sequence() {
  let $dir as xs:string := $config:home-dir || file:dir-separator() || $path
  return if (not(file:exists($dir))) then file:create-dir($dir) else ()
};

declare
  %rest:path("/createfile")
  %rest:POST
  %rest:form-param("path", "{$path}")
function page:createfile($path as xs:string*) as empty-sequence() {
  let $file as xs:string := $config:home-dir || file:dir-separator() || $path
  return if (not(file:exists($file))) then file:write-text($file, '', 'UTF-8') else ()
};

declare
  %rest:path("/deletefile")
  %rest:POST
  %rest:form-param("path", "{$path}")
function page:deletefile($path as xs:string*) as empty-sequence() {
  let $file as xs:string := $config:home-dir || file:dir-separator() || $path
  return if (file:is-file($file)) then file:delete($file, false()) else ()
};

declare
  %rest:path("/uploaddocument")
  %rest:GET
  %output:method("text")
function page:uploaddocument() as element(html)? {

};

()