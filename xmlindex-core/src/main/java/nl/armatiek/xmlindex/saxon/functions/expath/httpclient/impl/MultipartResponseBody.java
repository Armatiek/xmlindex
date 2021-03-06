/****************************************************************************/
/*  File:       MultipartResponseBody.java                                  */
/*  Author:     F. Georges - fgeorges.org                                   */
/*  Date:       2009-02-04                                                  */
/*  Tags:                                                                   */
/*      Copyright (c) 2009 Florent Georges (see end of file.)               */
/* ------------------------------------------------------------------------ */


package nl.armatiek.xmlindex.saxon.functions.expath.httpclient.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.stream.EntityState;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeTokenStream;

import nl.armatiek.xmlindex.saxon.functions.expath.httpclient.ContentType;
import nl.armatiek.xmlindex.saxon.functions.expath.httpclient.HeaderSet;
import nl.armatiek.xmlindex.saxon.functions.expath.httpclient.HttpClientException;
import nl.armatiek.xmlindex.saxon.functions.expath.httpclient.HttpConnection;
import nl.armatiek.xmlindex.saxon.functions.expath.httpclient.HttpResponseBody;
import nl.armatiek.xmlindex.saxon.functions.expath.httpclient.model.Result;
import nl.armatiek.xmlindex.saxon.functions.expath.httpclient.model.TreeBuilder;
import nl.armatiek.xmlindex.saxon.functions.expath.tools.ToolsException;

/**
 * A multipart body in the response.
 *
 * @author Florent Georges
 */
public class MultipartResponseBody
        implements HttpResponseBody
{
    public MultipartResponseBody(Result result, InputStream in, ContentType type, HttpConnection conn)
            throws HttpClientException
    {
        myContentType = type;
        myParts = new ArrayList<HttpResponseBody>();
        Header h = conn.getResponseHeaders().getFirstHeader("Content-Type");
        if ( h == null ) {
            throw new HttpClientException("No content type");
        }
        myBoundary = type.getBoundary();
        if ( myBoundary == null ) {
            throw new HttpClientException("No boundary");
        }
        try {
            analyzeParts(result, in, h.getValue());
        }
        catch ( IOException ex ) {
            throw new HttpClientException("error reading the response stream", ex);
        }
    }

    @Override
    public void outputBody(TreeBuilder b)
            throws HttpClientException
    {
        try {
            b.startElem("multipart");
            b.attribute("media-type", myContentType.getValue());
            b.attribute("boundary", myBoundary);
            b.startContent();
            for ( HttpResponseBody part : myParts ) {
                part.outputBody(b);
            }
            b.endElem();
        }
        catch ( ToolsException ex ) {
            throw new HttpClientException("Error building the body", ex);
        }
    }

    private void analyzeParts(Result result, InputStream in, String type)
            throws IOException
                 , HttpClientException
    {
        MimeTokenStream parser = new MimeTokenStream();
        parser.parseHeadless(in, type);
        try {
            HeaderSet headers = null;
            for ( EntityState state = parser.getState();
                  state != EntityState.T_END_OF_STREAM;
                  state = parser.next() )
            {
                if ( state == EntityState.T_START_HEADER ) {
                    headers = new HeaderSet();
                }
                handleParserState(result, parser, headers);
            }
        }
        catch ( MimeException ex ) {
            throw new HttpClientException("The response content is ill-formed.", ex);
        }
    }

    private void handleParserState(Result result, MimeTokenStream parser, HeaderSet headers)
            throws HttpClientException
    {
        EntityState state = parser.getState();
        if ( LOG.isDebugEnabled() ) {
            LOG.debug(MimeTokenStream.stateToString(state));
        }
        switch ( state ) {
            // It seems that in a headless parsing, END_HEADER appears
            // right after START_MESSAGE (without the corresponding
            // START_HEADER).  So if headers == null, we can just ignore
            // this state.
            case T_END_HEADER:
                // TODO: Just ignore anyway...?
                break;
            case T_FIELD:
                Field f = parser.getField();
                if ( LOG.isDebugEnabled() ) {
                    LOG.debug("  field: " + f);
                }
                headers.add(f.getName(), parseFieldBody(f));
                break;
            case T_BODY:
                if ( LOG.isDebugEnabled() ) {
                    LOG.debug("  body desc: " + parser.getBodyDescriptor());
                }
                HttpResponseBody b = makeResponsePart(result, headers, parser);
                myParts.add(b);
                break;
            // START_HEADER is handled in the calling analyzeParts()
            case T_START_HEADER:
            case T_END_BODYPART:
            case T_END_MESSAGE:
            case T_END_MULTIPART:
            case T_EPILOGUE:
            case T_PREAMBLE:
            case T_START_BODYPART:
            case T_START_MESSAGE:
            case T_START_MULTIPART:
                // ignore
                break;
            // In a first time, take a very defensive approach, and
            // throw an error for all unexpected states, even if we
            // should discover slowly that we should probably just
            // ignore some of them.
            default:
                String s = MimeTokenStream.stateToString(state);
                throw new HttpClientException("Unknown parsing state: " + s);
        }
    }

    private String parseFieldBody(Field f)
            throws HttpClientException
    {
//        try {
            // WHy did I use AbstractField in the first place?
            String b = f.getBody() /* AbstractField.parse(f.getRaw()).getBody() */;
            if ( LOG.isDebugEnabled() ) {
                LOG.debug("Field: " + f.getName() + ": [" + b + "]");
            }
            return b;
//        }
//        catch ( MimeException ex ) {
//            LOG.error("Field value parsing error (" + f + ")", ex);
//            throw new HttpClientException("Field value parsing error (" + f + ")", ex);
//        }
    }

    private HttpResponseBody makeResponsePart(Result result, HeaderSet headers, MimeTokenStream parser)
            throws HttpClientException
    {
        Header h = headers.getFirstHeader("Content-Type");
        if ( h == null ) {
            throw new HttpClientException("impossible to find the content type");
        }
        ContentType type = new ContentType(h);
        switch ( BodyFactory.parseType(type) ) {
            case XML: {
                // TODO: 'content_type' is the header Content-Type without any
                // param (i.e. "text/xml".)  Should we keep this, or put the
                // whole header (i.e. "text/xml; charset=utf-8")? (and for
                // other types as well...)
                Reader in = parser.getReader();
                return new XmlResponseBody(result, in, type, headers, false);
            }
            case HTML: {
                Reader in = parser.getReader();
                return new XmlResponseBody(result, in, type, headers, true);
            }
            case TEXT: {
                Reader in = parser.getReader();
                return new TextResponseBody(result, in, type, headers);
            }
            case BINARY: {
                InputStream in = parser.getInputStream();
                return new BinaryResponseBody(result, in, type, headers);
            }
            default:
                throw new HttpClientException("INTERNAL ERROR: cannot happen");
        }
    }

    private List<HttpResponseBody> myParts;
    private ContentType myContentType;
    private String myBoundary;
    private static final Log LOG = LogFactory.getLog(MultipartResponseBody.class);
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
