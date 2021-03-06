<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema" 
  xmlns:file="http://expath.org/ns/file"
  exclude-result-prefixes="#all"
  version="3.0">
  
  <xsl:output method="html" version="5.0"/>
  
  <xsl:param name="app-path" as="xs:string"/>
  <xsl:param name="home-dir" as="xs:string"/>
  
  <xsl:template match="/">
    <xsl:call-template name="body"/>
  </xsl:template>
  
  <xsl:template name="body">
    <html lang="en">
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1"/>
        <meta name="description" content="XSLWeb IDE"/>
        <meta name="author" content="Armatiek BV, Maarten Kroon"/>
        <link href="{$app-path}/ide/css/ide.css" rel="stylesheet" />
        <link href="{$app-path}/jquery-ui/jquery-ui.min.css" rel="stylesheet"/>
        <link href="{$app-path}/jquery-ui-iconfont/jquery-ui.icon-font.min.css" rel="stylesheet"/>
        <link href="{$app-path}/codemirror/lib/codemirror.css" rel="stylesheet"/>
        <link href="{$app-path}/codemirror/addon/display/fullscreen.css" rel="stylesheet"/>
        <link href="{$app-path}/codemirror/addon/dialog/dialog.css" rel="stylesheet"/>
        <link href="{$app-path}/codemirror/addon/search/matchesonscrollbar.css" rel="stylesheet"/>
        <link href="{$app-path}/codemirror/addon/fold/foldgutter.css" rel="stylesheet"/>
        <link href="{$app-path}/codemirror/theme/xq-dark.css" rel="stylesheet"/>
        <link href="{$app-path}/codemirror/theme/xq-light.css" rel="stylesheet"/>
        <link href="{$app-path}/jstree/themes/default/style.min.css" rel="stylesheet"/>
        <link href="{$app-path}/jquery-layout/css/layout-default.css" rel="stylesheet"/>
        <link href="{$app-path}/dropzone/css/dropzone.min.css" rel="stylesheet"/>
      </head>
      <body> 
        <div id="newIndexDlg" title="Create new index">
          <form>
            <fieldset class="ui-helper-reset">
              <label for="indexName">Name of new index:</label>
              <input type="text" name="indexName" id="indexName" class="ui-widget-content ui-corner-all"/>
              <label for="maxTermLength">Maximum term length:</label>
              <input type="number" min="32" name="maxTermLength" id="maxTermLength" class="ui-widget-content ui-corner-all" value="1024"/>
              <label for="compression">Index compression:</label>
              <select name="compression" id="compression" class="ui-widget-content ui-corner-all">
                <option value="NO_COMPRESSION" selected="selected">No compression</option>
                <option value="BEST_SPEED">Best speed</option>
                <option value="BEST_COMPRESSION">Best compression</option>
              </select>
            </fieldset>
          </form>
        </div>
        <div id="newFileDlg" title="Create new file">
          <form>
            <fieldset class="ui-helper-reset">
              <label for="fileName">Name of new file:</label>
              <input type="text" name="fileName" id="fileName" class="ui-widget-content ui-corner-all"/>
            </fieldset>
          </form>
        </div>
        <div id="newFolderDlg" title="Create new folder">
          <form>
            <fieldset class="ui-helper-reset">
              <label for="folderName">Name of new folder:</label>
              <input type="text" name="folderName" id="folderName" class="ui-widget-content ui-corner-all"/>
            </fieldset>
          </form>
        </div>
        <!--
        <div id="batchImportDlg" title="Batch import">
          <form>
            <fieldset class="ui-helper-reset">
              <label for="indexName">Name of new index:</label>
              <input type="text" name="indexName" id="indexName" class="ui-widget-content ui-corner-all"/>
              <label for="startDir">Start directory (on server):</label>
              <input name="startDir" id="startDir" class="ui-widget-content ui-corner-all"/>
              
              
              
              <label for="compression">Index compression:</label>
              <select name="compression" id="compression" class="ui-widget-content ui-corner-all">
                <option value="NO_COMPRESSION" selected="selected">No compression</option>
                <option value="BEST_SPEED">Best speed</option>
                <option value="BEST_COMPRESSION">Best compression</option>
              </select>
            </fieldset>
          </form>
        </div>
        -->
        
        <div id="saveConfirmDlg" title="Save">
          <p><span class="ui-icon ui-icon-alert" style="float:left; margin-right:10px;"></span>Do you want to save changes?</p>
        </div>
        
        <div id="uploadDlg" title="Upload files to index">
          <form action="{$app-path}/uploaddocument" class="dropzone" id="dropzone"></form>
        </div>
        
        <div id="optional-container">
          <div id="tabs" class="ui-layout-center">
            <div id="toolbar" style="margin-bottom:4px">
              <fieldset>
                <label for="index">Index: </label>
                <select name="index" id="index">
                  <xsl:for-each select="file:list(concat($home-dir, file:dir-separator(), 'indexes'))">
                    <option value="{.}">
                      <xsl:value-of select="."/>
                    </option>
                  </xsl:for-each>
                </select>
                <button id="newIndexBtn" style="margin-left:10px">New index</button>
                <button id="saveBtn" style="margin-left:5px">Save</button>
                <button id="runBtn">Run</button>
                <button id="explainBtn">Explain</button>
                <button id="uploadBtn">Upload</button>
                <button id="searchBtn" style="margin-left:5px">Search</button>
                <button id="replaceBtn">Replace</button>
              </fieldset>
            </div>
            <ul style="-moz-border-radius-bottomleft: 0; -moz-border-radius-bottomright: 0;"></ul>
            <div class="ui-layout-content ui-widget-content ui-corner-bottom" style="border-top: 0; padding: 0;"></div>
          </div>
          
          <div class="ui-layout-west">
            <div class="myScrollableBlock">
              <div id="filesystem"/>
            </div>
          </div>
          
          <div class="ui-layout-south">
            <div id="statusPanel">Status: OK</div>
            <textarea id="resultsTextArea"></textarea>
          </div>
          
        </div>
        
        <script src="{$app-path}/jquery/jquery-3.2.1.min.js"></script>
        <script src="{$app-path}/jquery-ui/jquery-ui.min.js"></script>
        <script src="{$app-path}/jquery-layout/js/jquery.layout.js"></script>
        <script src="{$app-path}/codemirror/lib/codemirror.js"></script>
        <script src="{$app-path}/codemirror/mode/xml/xml.js"></script>
        <script src="{$app-path}/codemirror/mode/xquery/xquery.js"></script>
        <script src="{$app-path}/codemirror/mode/css/css.js"></script>
        <script src="{$app-path}/codemirror/mode/javascript/javascript.js"></script>
        <script src="{$app-path}/codemirror/mode/dtd/dtd.js"></script>
        <script src="{$app-path}/codemirror/mode/meta.js"></script>
        <script src="{$app-path}/codemirror/addon/selection/active-line.js"></script>
        <script src="{$app-path}/codemirror/addon/display/fullscreen.js"></script>
        <script src="{$app-path}/codemirror/addon/hint/show-hint.js"></script>
        <script src="{$app-path}/codemirror/addon/hint/xml-hint.js"></script>
        <script src="{$app-path}/codemirror/addon/dialog/dialog.js"></script>
        <script src="{$app-path}/codemirror/addon/search/searchcursor.js"></script>
        <script src="{$app-path}/codemirror/addon/search/search.js"></script>
        <script src="{$app-path}/codemirror/addon/scroll/annotatescrollbar.js"></script>
        <script src="{$app-path}/codemirror/addon/search/matchesonscrollbar.js"></script>
        <script src="{$app-path}/codemirror/addon/search/jump-to-line.js"></script>
        <script src="{$app-path}/codemirror/addon/fold/foldcode.js"></script>
        <script src="{$app-path}/codemirror/addon/fold/foldgutter.js"></script>
        <script src="{$app-path}/codemirror/addon/fold/brace-fold.js"></script>
        <script src="{$app-path}/codemirror/addon/fold/xml-fold.js"></script>
        <script src="{$app-path}/codemirror/addon/fold/indent-fold.js"></script>
        <script src="{$app-path}/codemirror/addon/fold/comment-fold.js"></script>
        <script src="{$app-path}/jstree/jstree.min.js"></script>
        <script src="{$app-path}/dropzone/js/dropzone.min.js"></script>
        <script src="{$app-path}/ide/js/ide.js"></script>
      </body>
    </html>
  </xsl:template>
  
</xsl:stylesheet>