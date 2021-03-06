/****************************************************************************/
/*  File:       EXPathFunctionDefinition.java                               */
/*  Author:     F. Georges - H2O Consulting                                 */
/*  Date:       2009-08-09                                                  */
/*  Tags:                                                                   */
/*      Copyright (c) 2009-2015 Florent Georges (see end of file.)          */
/* ------------------------------------------------------------------------ */


package nl.armatiek.xmlindex.saxon.functions.expath.httpclient.expath.pkg.saxon;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ExtensionFunctionDefinition;


/**
 * TODO: Doc...
 *
 * @author Florent Georges
 */
public abstract class EXPathFunctionDefinition
        extends ExtensionFunctionDefinition
{
    public abstract void setConfiguration(Configuration pool);
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