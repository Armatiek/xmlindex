xquery version "3.1";

declare namespace page = "http://mydomain.org/modules/web-page";

(:~
 : This function generates the Hello World homepage.
 : @return a HTML page
 :)
declare
  %rest:path("/")
  %rest:GET
  %output:method("xhtml")
  %output:omit-xml-declaration("yes")
  function page:homepage() as element(Q{http://www.w3.org/1999/xhtml}html) {
  <html xmlns="http://www.w3.org/1999/xhtml">
    <head>
      <title>Hello world!</title>
    </head>
    <body>
      <h2>Hello world!!</h2>
    </body>
  </html>
};

()