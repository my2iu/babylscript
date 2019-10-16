/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Roger Lawrence
 *   Mike McCabe
 *   Igor Bukanov
 *   Ethan Hugg
 *   Bob Jervis
 *   Terry Lucas
 *   Milen Nankov
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript;

import java.io.IOException;

import org.mozilla.javascript.Token;

/**
 *
 * This file is derived from Rhino's org.mozilla.javascript.TokenStream
 * 
 */
public class XMLTokenizer
{
    ParserErrorReportingBase parser;
    TokenCharStream in;
    TokenStream ts;

    // For managing a string buffer
    private char[] stringBuffer = new char[128];
    private int stringBufferTop;
    private String getStringFromBuffer()
    {
        return new String(stringBuffer, 0, stringBufferTop);
    }

    private void addToString(int c)
    {
        int N = stringBufferTop;
        if (N == stringBuffer.length) {
            char[] tmp = new char[stringBuffer.length * 2];
            System.arraycopy(stringBuffer, 0, tmp, 0, N);
            stringBuffer = tmp;
        }
        stringBuffer[N] = (char)c;
        stringBufferTop = N + 1;
    }

    private boolean xmlIsAttribute;
    private boolean xmlIsTagContent;
    private int xmlOpenTagsCount;

    public XMLTokenizer(ParserErrorReportingBase parser, TokenCharStream in, TokenStream ts)
    {
        this.parser = parser;
        this.in = in;
        this.ts = ts;
    }

    //
    // XML Stuff
    //

    public boolean isXMLAttribute()
    {
        return xmlIsAttribute;
    }

    public int getFirstXMLToken() throws IOException
    {
        xmlOpenTagsCount = 0;
        xmlIsAttribute = false;
        xmlIsTagContent = false;
        in.ungetChar('<');
        return getNextXMLToken();
    }

    public int getNextXMLToken() throws IOException
    {
        stringBufferTop = 0; // remember the XML

        for (int c = in.getChar(); c != TokenCharStream.EOF_CHAR; c = in.getChar()) {
            if (xmlIsTagContent) {
                switch (c) {
                case '>':
                    addToString(c);
                    xmlIsTagContent = false;
                    xmlIsAttribute = false;
                    break;
                case '/':
                    addToString(c);
                    if (in.peekChar() == '>') {
                        c = in.getChar();
                        addToString(c);
                        xmlIsTagContent = false;
                        xmlOpenTagsCount--;
                    }
                    break;
                case '{':
                    in.ungetChar(c);
                    ts.string = getStringFromBuffer();
                    return Token.XML;
                case '\'':
                case '"':
                    addToString(c);
                    if (!readQuotedString(c)) return Token.ERROR;
                    break;
                case '=':
                    addToString(c);
                    xmlIsAttribute = true;
                    break;
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                    addToString(c);
                    break;
                default:
                    addToString(c);
                    xmlIsAttribute = false;
                    break;
                }

                if (!xmlIsTagContent && xmlOpenTagsCount == 0) {
                    ts.string = getStringFromBuffer();
                    return Token.XMLEND;
                }
            } else {
                switch (c) {
                case '<':
                    addToString(c);
                    c = in.peekChar();
                    switch (c) {
                    case '!':
                        c = in.getChar(); // Skip !
                        addToString(c);
                        c = in.peekChar();
                        switch (c) {
                        case '-':
                            c = in.getChar(); // Skip -
                            addToString(c);
                            c = in.getChar();
                            if (c == '-') {
                                addToString(c);
                                if(!readXmlComment()) return Token.ERROR;
                            } else {
                                // throw away the string in progress
                                stringBufferTop = 0;
                                ts.string = null;
                                parser.addError("msg.XML.bad.form");
                                return Token.ERROR;
                            }
                            break;
                        case '[':
                            c = in.getChar(); // Skip [
                            addToString(c);
                            if (in.getChar() == 'C' &&
                                    in.getChar() == 'D' &&
                                    in.getChar() == 'A' &&
                                    in.getChar() == 'T' &&
                                    in.getChar() == 'A' &&
                                    in.getChar() == '[')
                            {
                                addToString('C');
                                addToString('D');
                                addToString('A');
                                addToString('T');
                                addToString('A');
                                addToString('[');
                                if (!readCDATA()) return Token.ERROR;

                            } else {
                                // throw away the string in progress
                                stringBufferTop = 0;
                                ts.string = null;
                                parser.addError("msg.XML.bad.form");
                                return Token.ERROR;
                            }
                            break;
                        default:
                            if(!readEntity()) return Token.ERROR;
                            break;
                        }
                        break;
                    case '?':
                        c = in.getChar(); // Skip ?
                        addToString(c);
                        if (!readPI()) return Token.ERROR;
                        break;
                    case '/':
                        // End tag
                        c = in.getChar(); // Skip /
                        addToString(c);
                        if (xmlOpenTagsCount == 0) {
                            // throw away the string in progress
                            stringBufferTop = 0;
                            ts.string = null;
                            parser.addError("msg.XML.bad.form");
                            return Token.ERROR;
                        }
                        xmlIsTagContent = true;
                        xmlOpenTagsCount--;
                        break;
                    default:
                        // Start tag
                        xmlIsTagContent = true;
                        xmlOpenTagsCount++;
                        break;
                    }
                    break;
                case '{':
                    in.ungetChar(c);
                    ts.string = getStringFromBuffer();
                    return Token.XML;
                default:
                    addToString(c);
                    break;
                }
            }
        }

        stringBufferTop = 0; // throw away the string in progress
        ts.string = null;
        parser.addError("msg.XML.bad.form");
        return Token.ERROR;
    }

    /**
     *
     */
    private boolean readQuotedString(int quote) throws IOException
    {
        for (int c = in.getChar(); c != TokenCharStream.EOF_CHAR; c = in.getChar()) {
            addToString(c);
            if (c == quote) return true;
        }

        stringBufferTop = 0; // throw away the string in progress
        ts.string = null;
        parser.addError("msg.XML.bad.form");
        return false;
    }

    /**
     *
     */
    private boolean readXmlComment() throws IOException
    {
        for (int c = in.getChar(); c != TokenCharStream.EOF_CHAR;) {
            addToString(c);
            if (c == '-' && in.peekChar() == '-') {
                c = in.getChar();
                addToString(c);
                if (in.peekChar() == '>') {
                    c = in.getChar(); // Skip >
                    addToString(c);
                    return true;
                } else {
                    continue;
                }
            }
            c = in.getChar();
        }

        stringBufferTop = 0; // throw away the string in progress
        ts.string = null;
        parser.addError("msg.XML.bad.form");
        return false;
    }

    /**
     *
     */
    private boolean readCDATA() throws IOException
    {
        for (int c = in.getChar(); c != TokenCharStream.EOF_CHAR;) {
            addToString(c);
            if (c == ']' && in.peekChar() == ']') {
                c = in.getChar();
                addToString(c);
                if (in.peekChar() == '>') {
                    c = in.getChar(); // Skip >
                    addToString(c);
                    return true;
                } else {
                    continue;
                }
            }
            c = in.getChar();
        }

        stringBufferTop = 0; // throw away the string in progress
        ts.string = null;
        parser.addError("msg.XML.bad.form");
        return false;
    }

    /**
     *
     */
    private boolean readEntity() throws IOException
    {
        int declTags = 1;
        for (int c = in.getChar(); c != TokenCharStream.EOF_CHAR; c = in.getChar()) {
            addToString(c);
            switch (c) {
            case '<':
                declTags++;
                break;
            case '>':
                declTags--;
                if (declTags == 0) return true;
                break;
            }
        }

        stringBufferTop = 0; // throw away the string in progress
        ts.string = null;
        parser.addError("msg.XML.bad.form");
        return false;
    }

    /**
     *
     */
    private boolean readPI() throws IOException
    {
        for (int c = in.getChar(); c != TokenCharStream.EOF_CHAR; c = in.getChar()) {
            addToString(c);
            if (c == '?' && in.peekChar() == '>') {
                c = in.getChar(); // Skip >
                addToString(c);
                return true;
            }
        }

        stringBufferTop = 0; // throw away the string in progress
        ts.string = null;
        parser.addError("msg.XML.bad.form");
        return false;
    }

}
