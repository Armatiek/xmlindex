/****************************************************************************/
/*  File:       TreeBuilder.java                                            */
/*  Author:     F. Georges - H2O Consulting                                 */
/*  Date:       2011-03-10                                                  */
/*  Tags:                                                                   */
/*      Copyright (c) 2011 Florent Georges (see end of file.)               */
/* ------------------------------------------------------------------------ */


package nl.armatiek.xmlindex.saxon.functions.expath.httpclient.model;

import nl.armatiek.xmlindex.saxon.functions.expath.httpclient.HeaderSet;
import nl.armatiek.xmlindex.saxon.functions.expath.httpclient.HttpClientException;

/**
 * A generic interface to build a tree, independent on any processor.
 *
 * @author Florent Georges
 */
public interface TreeBuilder
        extends nl.armatiek.xmlindex.saxon.functions.expath.tools.model.TreeBuilder
{
    public void outputHeaders(HeaderSet headers)
            throws HttpClientException;
}


/* ------------------------------------------------------------------------ */
/*  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS COMMENT.               */
/*                                                                          */
/*  The contents of this file are subject to the Mozilla Public License     */
/*  Version 1.0 (the "License"); you may not use this file except in        */
/*  compliance with the License. You may obtain a copy of the License at    */
/*  http://www.mozilla.org/MPL/.                                            */
/*                                                                          */
/*  Software distributed under the License is distributed on an "AS IS"     */
/*  basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See    */
/*  the License for the specific language governing rights and limitations  */
/*  under the License.                                                      */
/*                                                                          */
/*  The Original Code is: all this file.                                    */
/*                                                                          */
/*  The Initial Developer of the Original Code is Florent Georges.          */
/*                                                                          */
/*  Contributor(s): none.                                                   */
/* ------------------------------------------------------------------------ */
