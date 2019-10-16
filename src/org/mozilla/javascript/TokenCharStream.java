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
import java.io.Reader;
import java.util.HashMap;

import org.mozilla.javascript.Kit;
import org.mozilla.javascript.ScriptRuntime;
import org.apache.harmony.Character;

/**
* Manage the character stream for a TokenStream
* 
* This file is derived from Rhino's org.mozilla.javascript.TokenStream
* 
*/
public class TokenCharStream
{
    /*
     * For chars - because we need something out-of-range
     * to check.  (And checking EOF by exception is annoying.)
     * Note distinction from EOF token type!
     */
    final static int
        EOF_CHAR = -1;

    public TokenCharStream(String sourceString,
             int lineno)
    {
       this.lineno = lineno;
       {
           if (sourceString == null) Kit.codeBug();
           this.sourceString = sourceString;
           this.sourceEnd = sourceString.length();
       }
       this.sourceCursor = 0;
    }

    // stuff other than whitespace since start of line
    private boolean dirtyLine;
    
    // Room to backtrace from to < on failed match of the last - in <!--
    private final int[] ungetBuffer = new int[3];
    private int ungetCursor;

    boolean hitEOF = false;

    private int lineStart = 0;
    int lineno;
    private int lineEndChar = -1;
    
    private boolean isNormalizeChars = true;

    private String sourceString;
    private char[] sourceBuffer;
    private int sourceEnd;
    private int sourceCursor;

    public boolean getDirtyLine() 
    {
       return dirtyLine; 
    }
    public void setDirtyLine(boolean dirtyLine) 
    {
       this.dirtyLine = dirtyLine;
    }

    public boolean getIsNormalizeChars() 
    {
       return isNormalizeChars; 
    }
    public void setIsNormalizeChars(boolean isNormalize) 
    {
       isNormalizeChars = isNormalize;
    }

    public void ungetChar(int c)
    {
        // can not unread past across line boundary
        if (ungetCursor != 0 && ungetBuffer[ungetCursor - 1] == '\n')
            Kit.codeBug();
        ungetBuffer[ungetCursor++] = c;
    }
    public boolean matchChar(int test) throws IOException
    {
        int c = getCharIgnoreLineEnd();
        if (c == test) {
            return true;
        } else {
            ungetCharIgnoreLineEnd(c);
            return false;
        }
    }
    public int peekChar() throws IOException
    {
        int c = getChar();
        ungetChar(c);
        return c;
    }
    public int getChar() throws IOException
    {
        if (ungetCursor != 0) {
            return ungetBuffer[--ungetCursor];
        }

        for(;;) {
            int c;
            if (sourceString != null) {
                if (sourceCursor == sourceEnd) {
                    hitEOF = true;
                    return EOF_CHAR;
                }
                c = sourceString.charAt(sourceCursor++);
            } else {
                if (sourceCursor == sourceEnd) {
                    if (!fillSourceBuffer()) {
                        hitEOF = true;
                        return EOF_CHAR;
                    }
                }
                c = sourceBuffer[sourceCursor++];
            }

            if (lineEndChar >= 0) {
                if (lineEndChar == '\r' && c == '\n') {
                    lineEndChar = '\n';
                    continue;
                }
                lineEndChar = -1;
                lineStart = sourceCursor - 1;
                lineno++;
            }

            if (c <= 127) {
                if (c == '\n' || c == '\r') {
                    lineEndChar = c;
                    c = '\n';
                }
            } else {
                if (isJSFormatChar(c)) {
                    continue;
                }
                if (ScriptRuntime.isJSLineTerminator(c)) {
                    lineEndChar = c;
                    c = '\n';
                }
            }
            
            if (isNormalizeChars)
                c = normalizeChar(c);
            return c;
        }
    }
    public int getCharIgnoreLineEnd() throws IOException
    {
        if (ungetCursor != 0) {
            return ungetBuffer[--ungetCursor];
        }

        for(;;) {
            int c;
            if (sourceString != null) {
                if (sourceCursor == sourceEnd) {
                    hitEOF = true;
                    return EOF_CHAR;
                }
                c = sourceString.charAt(sourceCursor++);
            } else {
                if (sourceCursor == sourceEnd) {
                    if (!fillSourceBuffer()) {
                        hitEOF = true;
                        return EOF_CHAR;
                    }
                }
                c = sourceBuffer[sourceCursor++];
            }

            if (c <= 127) {
                if (c == '\n' || c == '\r') {
                    lineEndChar = c;
                    c = '\n';
                }
            } else {
                if (isJSFormatChar(c)) {
                    continue;
                }
                if (ScriptRuntime.isJSLineTerminator(c)) {
                    lineEndChar = c;
                    c = '\n';
                }
            }
            if (isNormalizeChars)
                c = normalizeChar(c);
            return c;
        }
    }
    
    public void ungetCharIgnoreLineEnd(int c)
    {
        ungetBuffer[ungetCursor++] = c;
    }
    
    public void skipLine() throws IOException
    {
        // skip to end of line
        int c;
        while ((c = getChar()) != EOF_CHAR && c != '\n') { }
        ungetChar(c);
    }

    public final int getOffset()
    {
        int n = sourceCursor - lineStart;
        if (lineEndChar >= 0) { --n; }
        return n;
    }

    // Output is not normalized
    public final String getLine()
    {
        if (sourceString != null) {
            // String case
            int lineEnd = sourceCursor;
            if (lineEndChar >= 0) {
                --lineEnd;
            } else {
                for (; lineEnd != sourceEnd; ++lineEnd) {
                    int c = sourceString.charAt(lineEnd);
                    if (ScriptRuntime.isJSLineTerminator(c)) {
                        break;
                    }
                }
            }
            return sourceString.substring(lineStart, lineEnd);
        } else {
            // Reader case
            int lineLength = sourceCursor - lineStart;
            if (lineEndChar >= 0) {
                --lineLength;
            } else {
                // Read until the end of line
                for (;; ++lineLength) {
                    int i = lineStart + lineLength;
                    if (i == sourceEnd) {
                        try {
                            if (!fillSourceBuffer()) { break; }
                        } catch (IOException ioe) {
                            // ignore it, we're already displaying an error...
                            break;
                        }
                        // i recalculuation as fillSourceBuffer can move saved
                        // line buffer and change lineStart
                        i = lineStart + lineLength;
                    }
                    int c = sourceBuffer[i];
                    if (ScriptRuntime.isJSLineTerminator(c)) {
                        break;
                    }
                }
            }
            return new String(sourceBuffer, lineStart, lineLength);
        }
    }

    private boolean fillSourceBuffer() throws IOException
    {
        if (sourceString != null) Kit.codeBug();
//        if (sourceEnd == sourceBuffer.length) {
//            if (lineStart != 0) {
//                System.arraycopy(sourceBuffer, lineStart, sourceBuffer, 0,
//                                 sourceEnd - lineStart);
//                sourceEnd -= lineStart;
//                sourceCursor -= lineStart;
//                lineStart = 0;
//            } else {
//                char[] tmp = new char[sourceBuffer.length * 2];
//                System.arraycopy(sourceBuffer, 0, tmp, 0, sourceEnd);
//                sourceBuffer = tmp;
//            }
//        }
//        int n = sourceReader.read(sourceBuffer, sourceEnd,
//                                  sourceBuffer.length - sourceEnd);
//        if (n < 0) {
//            return false;
//        }
//        sourceEnd += n;
        return true;
    }

    static HashMap<Integer, Integer> widthNormalizationTable;
    static
    {
        final int [] normalizationTableAsArray = {
                '\uFF5F', '\u2985', 
                '\uFF60', '\u2986',

                '\uFF61', '\u3002',
                '\uFF62', '\u300C',
                '\uFF63', '\u300D',
                '\uFF64', '\u3001',

                '\uFF65', '\u30FB',
                '\uFF66', '\u30F2',
                '\uFF67', '\u30A1',
                '\uFF68', '\u30A3',
                '\uFF69', '\u30A5',
                '\uFF6A', '\u30A7',
                '\uFF6B', '\u30A9',
                '\uFF6C', '\u30E3',
                '\uFF6D', '\u30E5',
                '\uFF6E', '\u30E7',
                '\uFF6F', '\u30C3',
                '\uFF70', '\u30FC',
                '\uFF71', '\u30A2',
                '\uFF72', '\u30A4',
                '\uFF73', '\u30A6',
                '\uFF74', '\u30A8',
                '\uFF75', '\u30AA',
                '\uFF76', '\u30AB',
                '\uFF77', '\u30AD',
                '\uFF78', '\u30AF',
                '\uFF79', '\u30B1',
                '\uFF7A', '\u30B3',
                '\uFF7B', '\u30B5',
                '\uFF7C', '\u30B7',
                '\uFF7D', '\u30B9',
                '\uFF7E', '\u30BB',
                '\uFF7F', '\u30BD',
                '\uFF80', '\u30BF',
                '\uFF81', '\u30C1',
                '\uFF82', '\u30C4',
                '\uFF83', '\u30C6',
                '\uFF84', '\u30C8',
                '\uFF85', '\u30CA',
                '\uFF86', '\u30CB',
                '\uFF87', '\u30CC',
                '\uFF88', '\u30CD',
                '\uFF89', '\u30CE',
                '\uFF8A', '\u30CF',
                '\uFF8B', '\u30D2',
                '\uFF8C', '\u30D5',
                '\uFF8D', '\u30D8',
                '\uFF8E', '\u30DB',
                '\uFF8F', '\u30DE',
                '\uFF90', '\u30DF',
                '\uFF91', '\u30E0',
                '\uFF92', '\u30E1',
                '\uFF93', '\u30E2',
                '\uFF94', '\u30E4',
                '\uFF95', '\u30E6',
                '\uFF96', '\u30E8',
                '\uFF97', '\u30E9',
                '\uFF98', '\u30EA',
                '\uFF99', '\u30EB',
                '\uFF9A', '\u30EC',
                '\uFF9B', '\u30ED',
                '\uFF9C', '\u30EF',
                '\uFF9D', '\u30F3',
                '\uFF9E', '\u3099',
                '\uFF9F', '\u309A',

                '\uFFA0', '\u3164',
                '\uFFA1', '\u3131',
                '\uFFA2', '\u3132',
                '\uFFA3', '\u3133',
                '\uFFA4', '\u3134',
                '\uFFA5', '\u3135',
                '\uFFA6', '\u3136',
                '\uFFA7', '\u3137',
                '\uFFA8', '\u3138',
                '\uFFA9', '\u3139',
                '\uFFAA', '\u313A',
                '\uFFAB', '\u313B',
                '\uFFAC', '\u313C',
                '\uFFAD', '\u313D',
                '\uFFAE', '\u313E',
                '\uFFAF', '\u313F',
                '\uFFB0', '\u3140',
                '\uFFB1', '\u3141',
                '\uFFB2', '\u3142',
                '\uFFB3', '\u3143',
                '\uFFB4', '\u3144',
                '\uFFB5', '\u3145',
                '\uFFB6', '\u3146',
                '\uFFB7', '\u3147',
                '\uFFB8', '\u3148',
                '\uFFB9', '\u3149',
                '\uFFBA', '\u314A',
                '\uFFBB', '\u314B',
                '\uFFBC', '\u314C',
                '\uFFBD', '\u314D',
                '\uFFBE', '\u314E',

                '\uFFC2', '\u314F',
                '\uFFC3', '\u3150',
                '\uFFC4', '\u3151',
                '\uFFC5', '\u3152',
                '\uFFC6', '\u3153',
                '\uFFC7', '\u3154',

                '\uFFCA', '\u3155',
                '\uFFCB', '\u3156',
                '\uFFCC', '\u3157',
                '\uFFCD', '\u3158',
                '\uFFCE', '\u3159',
                '\uFFCF', '\u315A',

                '\uFFD2', '\u315B',
                '\uFFD3', '\u315C',
                '\uFFD4', '\u315D',
                '\uFFD5', '\u315E',
                '\uFFD6', '\u315F',
                '\uFFD7', '\u3160',

                '\uFFDA', '\u3161',
                '\uFFDB', '\u3162',
                '\uFFDC', '\u3163',

                '\uFFE0', '\u00A2',
                '\uFFE1', '\u00A3',
                '\uFFE2', '\u00AC',
                '\uFFE3', '\u00AF',
                '\uFFE4', '\u00A6',
                '\uFFE5', '\u00A5',
                '\uFFE6', '\u20A9',

                '\uFFE8', '\u2502',
                '\uFFE9', '\u2190',
                '\uFFEA', '\u2191',
                '\uFFEB', '\u2192',
                '\uFFEC', '\u2193',
                '\uFFED', '\u25A0',
                '\uFFEE', '\u25CB',
        }; 

        widthNormalizationTable = new HashMap<Integer, Integer>();
        for (int n = 0; n < normalizationTableAsArray.length; n += 2)
            widthNormalizationTable.put(normalizationTableAsArray[n], normalizationTableAsArray[n+1]);
    }

    // Normalizes a character (e.g. converts half-width/full-width forms)
    // Usually, we can assume that Unicode strings are already normalized, but
    // just to be safe, we will still normalize out half-width/full-width forms
    public static int normalizeChar(int c)
    {
        if (c >= '\uff01' && c <= '\uff5e')
            c -= '\uff01' - 33;
        else if (widthNormalizationTable.containsKey(c))
            c = widthNormalizationTable.get(c);
        return c;
    }
    
    private static boolean isJSFormatChar(int c)
    {
        return c > 127 && Character.getType((char)c) == Character.FORMAT;
    }
 }