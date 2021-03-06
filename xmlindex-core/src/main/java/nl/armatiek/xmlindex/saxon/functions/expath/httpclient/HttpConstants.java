/****************************************************************************/
/*  File:       HttpConstants.java                                          */
/*  Author:     F. Georges - fgeorges.org                                   */
/*  Date:       2009-02-02                                                  */
/*  Tags:                                                                   */
/*      Copyright (c) 2009 Florent Georges (see end of file.)               */
/* ------------------------------------------------------------------------ */


package nl.armatiek.xmlindex.saxon.functions.expath.httpclient;

/**
 * Centralize some constants.
 *
 * @author Florent Georges
 */
public class HttpConstants
{
    public final static String   HTTP_NS_PREFIX = "http";
    public final static String   HTTP_NS_URI = "http://expath.org/ns/http";
    public final static String   HTTP_CLIENT_NS_PREFIX = "hc";
    public final static String   HTTP_CLIENT_NS_URI = "http://expath.org/ns/http-client";
    public final static String[] BOTH_NS_URIS = { HTTP_NS_URI, HTTP_CLIENT_NS_URI };
    public final static String   HTTP_1_0 = "1.0";
    public final static String   HTTP_1_1 = "1.1";
    public final static String[] MULTIPART_ATTRS = { "media-type", "boundary" };
    public final static String[] HEADER_ATTRS = { "name", "value" };
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
