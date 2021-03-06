xquery version "3.1";

declare namespace ide="http://www.armatiek.nl/xmlindex/ide";
declare namespace rest="http://exquery.org/ns/restxq";
declare namespace request="http://exquery.org/ns/request";
declare namespace file="http://expath.org/ns/file";
declare namespace output="http://www.w3.org/2010/xslt-xquery-serialization";
declare namespace err="http://www.w3.org/2005/xqt-errors";
declare namespace xmi="http://www.armatiek.nl/xmlindex/functions";
declare namespace config="http://www.armatiek.nl/xmlindex/config";
declare namespace session="http://www.armatiek.nl/xmlindex/param/session";
declare namespace json="http://www.armatiek.nl/xmlindex/functions/json";

declare variable $config:development-mode as xs:boolean external;
declare variable $config:home-dir as xs:string external;
declare variable $config:index-dir as xs:string external;
declare variable $session:session as item()? external := ();

(:~
 : This function generates the home page.
 : @return HTML page
 :)
declare
  %rest:path("/")
  %rest:GET
  %output:method("html")
  %output:omit-xml-declaration("yes")
function ide:home() as element(html) {
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
function ide:open($path as xs:string*) as xs:string? {
  file:read-text($config:home-dir || file:dir-separator() || $path, 'UTF-8')
};

declare
  %rest:path("/save")
  %rest:POST
  %rest:form-param("path", "{$path}")
  %rest:form-param("code", "{$code}")
function ide:save($path as xs:string*, $code as xs:string*) as empty-sequence() {
  file:write-text($config:home-dir || file:dir-separator() || $path, $code, "UTF-8")
};

declare
  %rest:path("/run")
  %rest:POST
  %rest:form-param("code", "{$code}")
  %rest:form-param("path", "{$path}")
  %output:method("text")
function ide:run($code as xs:string*, $path as xs:string*) as xs:string {
  try {
    let $source-node as document-node() := document { <root/> }
    let $result := transform(
      map {
        "stylesheet-location": "xsl/run.xsl",
        "source-node": $source-node,
        "stylesheet-params": map {
          QName("", "home-dir") : $config:home-dir,
          QName("", "code") : $code,
          QName("", "path") : $path,
          QName("http://www.armatiek.nl/xmlindex/param/session", "session") : $session:session
        },
        "cache" : not($config:development-mode)
      })
    return $result?output
  } catch * {
    ide:get-json-error($err:code, $err:description, $err:module, $err:line-number, $err:column-number)
  }
};

declare
  %rest:path("/explain")
  %rest:POST
  %rest:form-param("code", "{$code}")
  %output:method("text")
function ide:explain($code as xs:string*) as xs:string {
  try {
    let $source-node as document-node() := document { <root/> }
    let $result := transform(
      map {
        "stylesheet-location": "xsl/explain.xsl",
        "source-node": $source-node,
        "stylesheet-params": map {
          QName("", "code") : $code,
          QName("http://www.armatiek.nl/xmlindex/param/session", "session") : $session:session
        },
        "cache" : not($config:development-mode)
      })
    return $result?output
  } catch * {
    ide:get-json-error($err:code, $err:description, $err:module, $err:line-number, $err:column-number)
  }
};

declare
  %rest:path("/filesystem")
  %rest:GET
  %rest:query-param("id", "{$id}")
  %output:method("text")
function ide:filesystem($id as xs:string*) as xs:string {
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
function ide:createfolder($path as xs:string*) as empty-sequence() {
  let $dir as xs:string := $config:home-dir || file:dir-separator() || $path
  return if (not(file:exists($dir))) then file:create-dir($dir) else ()
};

declare
  %rest:path("/createfile")
  %rest:POST
  %rest:form-param("path", "{$path}")
function ide:createfile($path as xs:string*) as empty-sequence() {
  let $file as xs:string := $config:home-dir || file:dir-separator() || $path
  return if (not(file:exists($file))) then file:write-text($file, '', 'UTF-8') else ()
};

declare
  %rest:path("/deletefile")
  %rest:POST
  %rest:form-param("path", "{$path}")
function ide:deletefile($path as xs:string*) as empty-sequence() {
  let $file as xs:string := $config:home-dir || file:dir-separator() || $path
  return if (file:is-file($file)) then file:delete($file, false()) else ()
};

declare
  %rest:path("/uploaddocument")
  %rest:POST("{$parts}")
  %rest:form-param("filename", "{$file-name}")
  %output:method("text")
function ide:uploaddocument($parts as item()*, $file-name as xs:string*) as empty-sequence() {
  xmi:add-document($file-name[1], $parts[1])
};

declare
  %rest:path("/createindex")
  %rest:POST
  %rest:form-param("name", "{$name}")
  %rest:form-param("maxTermLength", "{$max-term-length}")
  %rest:form-param("compression", "{$compression}")
  %output:method("text")
function ide:createindex($name as xs:string*, $max-term-length as xs:integer*, $compression as xs:string*) as xs:string? {
  try {
    let $new-index-dir as xs:string := $config:index-dir || file:dir-separator() || $name
    return xmi:create-index($new-index-dir, $max-term-length, $compression)
  } catch * {
    "Error creating index: " || $err:description
  }
};

declare function ide:get-json-error($code as xs:string?, $description as xs:string?, $module as xs:string?, 
  $line as xs:integer?, $col as xs:integer?) as xs:string? {
  let $err-xml as element(err:error) := 
    <err:error>
      <err:code>{$code}</err:code>
      <err:description>{$description}</err:description>
      <err:module>{$module}</err:module>
      <err:line-number>{$line}</err:line-number>
      <err:column-number>{$col}</err:column-number>
    </err:error>
  
  let $output-parameters as element(output:serialization-parameters) := 
    <output:serialization-parameters>
      <output:method value="xml"/>
      <output:indent value="yes"/>
      <output:omit-xml-declaration value="yes"/>
    </output:serialization-parameters>  
  
  return
    '{' ||
      '"time": 0,' ||
      '"errCode": "' || json:escape($code) || '",' || 
      '"errDescription": "' || json:escape($description) || '",' ||
      '"errModule": "' || json:escape($module) || '",' ||
      '"errLine": ' || $line || ',' ||
      '"errColumn": ' || $col || ',' ||
      '"result": "' || json:escape(serialize($err-xml, $output-parameters)) ||  '",' ||
    '}'
};

()