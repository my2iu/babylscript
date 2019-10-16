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
import java.util.HashMap;
import java.util.Map;
import org.apache.harmony.Character;


/**
*
* This file is derived from Rhino's org.mozilla.javascript.TokenStream
* 
*/
public class BabylTokenizer
{
    // For managing a string buffer
    private char[] stringBuffer = new char[128];
    private int stringBufferTop;
    protected void setStringBufferTop(int top) { stringBufferTop = top; }
    protected int getStringBufferTop() { return stringBufferTop;}
    protected String getStringFromBuffer()
    {
        return new String(stringBuffer, 0, stringBufferTop);
    }

    protected void addToString(int c)
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

    // Interns a string
    protected String internString(String str) {return (String)ts.allStrings.intern(str); }

    // The value of a string/number that was tokenized
    protected void setString(String str) {ts.string = str;}
    protected void setNumber(double num) {ts.number = num;}
    protected void setLanguage(TokenStream.LanguageMode lang) {ts.setLanguage(lang);}

    // Input stream management
    TokenCharStream in;

    ParserErrorReportingBase parser;
    TokenStream ts;
    DecimalNumberReader numberReader;
    public BabylTokenizer(ParserErrorReportingBase p, TokenCharStream in, TokenStream ts, DecimalNumberReader numberReader)
    {
        this.parser = p;
        this.ts = ts;
        this.in = in;
        this.numberReader = numberReader;
    }

    protected Map<String, Integer> keywordLookup = new HashMap<String, Integer>();

    protected int stringToKeyword(String name)
    {
        if (keywordLookup.containsKey(name))
            return keywordLookup.get(name) & 0xff;
        return Token.EOF;
    }
    
    static int englishStringToKeyword(String name)
    {
        if (englishKeywordLookup.containsKey(name))
            return englishKeywordLookup.get(name) & 0xff;
        return Token.EOF;
    }

    final int getToken() throws IOException
    {
        int c;

        for (;;) {
            // Eat whitespace, possibly sensitive to newlines.
            for (;;) {
                c = in.getChar();
                if (c == TokenCharStream.EOF_CHAR) {
                    return Token.EOF;
                } else if (c == '\n') {
                    in.setDirtyLine(false);
                    return Token.EOL;
                } else if (!isJSSpace(c)) {
                    if (c != '-') {
                        in.setDirtyLine(true);
                    }
                    break;
                }
            }

            if (c == '@') return Token.XMLATTR;

            // identifier/keyword/instanceof?
            // watch out for starting with a <backslash>
            boolean identifierStart;
            boolean isUnicodeEscapeStart = false;
            if (c == '\\') {
                c = in.getChar();
                if (c == 'u') {
                    identifierStart = true;
                    isUnicodeEscapeStart = true;
                    setStringBufferTop(0);
                } else {
                    identifierStart = false;
                    in.ungetChar(c);
                    c = '\\';
                }
            } else {
                    identifierStart = isIdentifierStart(c);
                if (identifierStart) {
                    setStringBufferTop(0);
                    addToString(c);
                }
            }

            if (identifierStart) {
                boolean containsEscape = isUnicodeEscapeStart;
                for (;;) {
                    if (isUnicodeEscapeStart) {
                        // strictly speaking we should probably push-back
                        // all the bad characters if the <backslash>uXXXX
                        // sequence is malformed. But since there isn't a
                        // correct context(is there?) for a bad Unicode
                        // escape sequence in an identifier, we can report
                        // an error here.
                        int escapeVal = 0;
                        for (int i = 0; i != 4; ++i) {
                            c = in.getChar();
                            escapeVal = Kit.xDigitToInt(c, escapeVal);
                            // Next check takes care about c < 0 and bad escape
                            if (escapeVal < 0) { break; }
                        }
                        if (escapeVal < 0) {
                            parser.addError("msg.invalid.escape");
                            return Token.ERROR;
                        }
                        addToString(escapeVal);
                        isUnicodeEscapeStart = false;
                    } else {
                        c = in.getChar();
                        if (c == '\\') {
                            c = in.getChar();
                            if (c == 'u') {
                                isUnicodeEscapeStart = true;
                                containsEscape = true;
                            } else {
                                parser.addError("msg.illegal.character");
                                return Token.ERROR;
                            }
                        } else {
                            if (c == TokenCharStream.EOF_CHAR
                                || !isIdentifierPart(c))
                            {
                                break;
                            }
                            addToString(c);
                        }
                    }
                }
                in.ungetChar(c);

                String str = getStringFromBuffer();
                if (!containsEscape) {
                    // OPT we shouldn't have to make a string (object!) to
                    // check if it's a keyword.

                    // Return the corresponding token if it's a keyword
                    int result = stringToKeyword(str);
                    if (result != Token.EOF) {
                        if ((result == Token.LET || result == Token.YIELD) && 
                            parser.compilerEnv.getLanguageVersion() 
                               < Context.VERSION_1_7)
                        {
                            // LET and YIELD are tokens only in 1.7 and later
                            setString(result == Token.LET ? "let" : "yield");
                            result = Token.NAME;
                        }
                        if (result != Token.RESERVED) {
                            return result;
                        } else if (!parser.compilerEnv.
                                        isReservedKeywordAsIdentifier())
                        {
                            return result;
                        } else {
                            // If implementation permits to use future reserved
                            // keywords in violation with the EcmaScript,
                            // treat it as name but issue warning
                            parser.addWarning("msg.reserved.keyword", str);
                        }
                    }
                }
                setString(internString(str));
                return Token.NAME;
            }

            // is it a number?
            int match = numberReader.matchNumber(c, in, parser);
            if (match != Token.EMPTY)
            {
                if (match == Token.NUMBER) 
                {
                	setString(numberReader.readString.toString());
                    setNumber(numberReader.readValue);
                }
                return match;
            }

            // is it a string?
            if (isStringDelimiter(c)) {
                // We attempt to accumulate a string the fast way, by
                // building it directly out of the reader.  But if there
                // are any escaped characters in the string, we revert to
                // building it out of a StringBuffer.

                int [] quoteChars = getMatchingStringDelimiter(c);
                setStringBufferTop(0);

                // Turn of normalization so we can put the raw character stream into the string
                in.setIsNormalizeChars(false);  
                
                c = in.getChar();
            strLoop: while (!intArrayContains(quoteChars, c) && !intArrayContains(quoteChars, TokenCharStream.normalizeChar(c))) {
                    if (c == '\n' || c == TokenCharStream.EOF_CHAR) {
                        in.ungetChar(c);
                        parser.addError("msg.unterminated.string.lit");
                        in.setIsNormalizeChars(true);  
                        return Token.ERROR;
                    }

                    if (c == '\\') {
                        // We've hit an escaped character
                        int escapeVal;

                        c = in.getChar();
                        switch (c) {
                        case 'b': c = '\b'; break;
                        case 'f': c = '\f'; break;
                        case 'n': c = '\n'; break;
                        case 'r': c = '\r'; break;
                        case 't': c = '\t'; break;

                        // \v a late addition to the ECMA spec,
                        // it is not in Java, so use 0xb
                        case 'v': c = 0xb; break;

                        case 'u':
                            // Get 4 hex digits; if the u escape is not
                            // followed by 4 hex digits, use 'u' + the
                            // literal character sequence that follows.
                            int escapeStart = getStringBufferTop();
                            addToString('u');
                            escapeVal = 0;
                            for (int i = 0; i != 4; ++i) {
                                c = in.getChar();
                                escapeVal = Kit.xDigitToInt(c, escapeVal);
                                if (escapeVal < 0) {
                                    continue strLoop;
                                }
                                addToString(c);
                            }
                            // prepare for replace of stored 'u' sequence
                            // by escape value
                            setStringBufferTop(escapeStart);
                            c = escapeVal;
                            break;
                        case 'x':
                            // Get 2 hex digits, defaulting to 'x'+literal
                            // sequence, as above.
                            c = in.getChar();
                            escapeVal = Kit.xDigitToInt(c, 0);
                            if (escapeVal < 0) {
                                addToString('x');
                                continue strLoop;
                            } else {
                                int c1 = c;
                                c = in.getChar();
                                escapeVal = Kit.xDigitToInt(c, escapeVal);
                                if (escapeVal < 0) {
                                    addToString('x');
                                    addToString(c1);
                                    continue strLoop;
                                } else {
                                    // got 2 hex digits
                                    c = escapeVal;
                                }
                            }
                            break;

                        case '\n':
                            // Remove line terminator after escape to follow
                            // SpiderMonkey and C/C++
                            c = in.getChar();
                            continue strLoop;

                        default:
                            if ('0' <= c && c < '8') {
                                int val = c - '0';
                                c = in.getChar();
                                if ('0' <= c && c < '8') {
                                    val = 8 * val + c - '0';
                                    c = in.getChar();
                                    if ('0' <= c && c < '8' && val <= 037) {
                                        // c is 3rd char of octal sequence only
                                        // if the resulting val <= 0377
                                        val = 8 * val + c - '0';
                                        c = in.getChar();
                                    }
                                }
                                in.ungetChar(c);
                                c = val;
                            }
                        }
                    }
                    addToString(c);
                    c = in.getChar();
                }

                in.setIsNormalizeChars(true);  
                String str = getStringFromBuffer();
                setString(internString(str));
                return Token.STRING;
            }

            int matchSymb = matchSymbol(c);
            if (matchSymb != Token.EMPTY)
                return matchSymb;
        }
    }

    protected int matchSymbol(int c) throws IOException
    {
        switch (c) {
        case ';': return Token.SEMI;
        case '[': return Token.LB;
        case ']': return Token.RB;
        case '{': return Token.LC;
        case '}': return Token.RC;
        case '(': return Token.LP;
        case ')': return Token.RP;
        case ',': return Token.COMMA;
        case '?': return Token.HOOK;
        case ':':
            if (in.matchChar(':')) {
                return Token.COLONCOLON;
            } else {
                return Token.COLON;
            }
        case '.':
            if (in.matchChar('.')) {
                return Token.DOTDOT;
            } else if (in.matchChar('(')) {
                return Token.DOTQUERY;
            } else {
                return Token.DOT;
            }

        case '|':
            if (in.matchChar('|')) {
                return Token.OR;
            } else if (in.matchChar('=')) {
                return Token.ASSIGN_BITOR;
            } else {
                return Token.BITOR;
            }

        case '^':
            if (in.matchChar('=')) {
                return Token.ASSIGN_BITXOR;
            } else {
                return Token.BITXOR;
            }

        case '&':
            if (in.matchChar('&')) {
                return Token.AND;
            } else if (in.matchChar('=')) {
                return Token.ASSIGN_BITAND;
            } else {
                return Token.BITAND;
            }

        case '=':
            if (in.matchChar('=')) {
                if (in.matchChar('='))
                    return Token.SHEQ;
                else
                    return Token.EQ;
            } else {
                return Token.ASSIGN;
            }

        case '!':
            if (in.matchChar('=')) {
                if (in.matchChar('='))
                    return Token.SHNE;
                else
                    return Token.NE;
            } else {
                return Token.NOT;
            }

        case '<':
            /* NB:treat HTML begin-comment as comment-till-eol */
            if (in.matchChar('!')) {
                if (in.matchChar('-')) {
                    if (in.matchChar('-')) {
                        in.skipLine();
                        return Token.EMPTY;
                    }
                    in.ungetCharIgnoreLineEnd('-');
                }
                in.ungetCharIgnoreLineEnd('!');
            }
            if (in.matchChar('<')) {
                if (in.matchChar('=')) {
                    return Token.ASSIGN_LSH;
                } else {
                    return Token.LSH;
                }
            } else {
                if (in.matchChar('=')) {
                    return Token.LE;
                } else {
                    return Token.LT;
                }
            }

        case '>':
            if (in.matchChar('>')) {
                if (in.matchChar('>')) {
                    if (in.matchChar('=')) {
                        return Token.ASSIGN_URSH;
                    } else {
                        return Token.URSH;
                    }
                } else {
                    if (in.matchChar('=')) {
                        return Token.ASSIGN_RSH;
                    } else {
                        return Token.RSH;
                    }
                }
            } else {
                if (in.matchChar('=')) {
                    return Token.GE;
                } else {
                    return Token.GT;
                }
            }

        case '*':
            if (in.matchChar('=')) {
                return Token.ASSIGN_MUL;
            } else {
                return Token.MUL;
            }

        case '/':
            // is it a // comment?
            if (in.matchChar('/')) {
                in.skipLine();
                return Token.EMPTY;
            }
            if (in.matchChar('*')) {
                boolean lookForSlash = false;
                for (;;) {
                    c = in.getChar();
                    if (c == TokenCharStream.EOF_CHAR) {
                        parser.addError("msg.unterminated.comment");
                        return Token.ERROR;
                    } else if (c == '*') {
                        lookForSlash = true;
                    } else if (c == '/') {
                        if (lookForSlash) {
                            return Token.EMPTY;
                        }
                    } else {
                        lookForSlash = false;
                    }
                }
            }

            if (in.matchChar('=')) {
                return Token.ASSIGN_DIV;
            } else {
                return Token.DIV;
            }

        case '%':
            if (in.matchChar('=')) {
                return Token.ASSIGN_MOD;
            } else {
                return Token.MOD;
            }

        case '~':
            return Token.BITNOT;

        case '+':
            if (in.matchChar('=')) {
                return Token.ASSIGN_ADD;
            } else if (in.matchChar('+')) {
                return Token.INC;
            } else {
                return Token.ADD;
            }

        case '-':
            if (in.matchChar('=')) {
                c = Token.ASSIGN_SUB;
            } else if (in.matchChar('-')) {
                if (!in.getDirtyLine()) {
                    // treat HTML end-comment after possible whitespace
                    // after line start as comment-utill-eol
                    if (in.matchChar('>')) {
                        in.skipLine();
                        return Token.EMPTY;
                    }
                }
                if (in.matchChar('-'))
                {
                    // Change of language mode
                    c = scanLanguageMode();
                }
                else
                    c = Token.DEC;
            } else {
                c = Token.SUB;
            }
            in.setDirtyLine(true);
            return c;

        default:
            parser.addError("msg.illegal.character");
            return Token.ERROR;
        }
    }

    static HashMap<String, TokenStream.LanguageMode> languageModeCodes;
    static {
        languageModeCodes  = new HashMap<String, TokenStream.LanguageMode>();
        languageModeCodes.put("\u0639\u0631\u0628\u064a", TokenStream.LanguageMode.ar);
        languageModeCodes.put("ar", TokenStream.LanguageMode.ar);
        languageModeCodes.put("de", TokenStream.LanguageMode.de);
        languageModeCodes.put("en", TokenStream.LanguageMode.en);
        languageModeCodes.put("eo", TokenStream.LanguageMode.eo);
        languageModeCodes.put("es", TokenStream.LanguageMode.es);
        languageModeCodes.put("fr", TokenStream.LanguageMode.fr);
        languageModeCodes.put("pt", TokenStream.LanguageMode.pt);
        languageModeCodes.put("test", TokenStream.LanguageMode.test);
        languageModeCodes.put("ro", TokenStream.LanguageMode.ro);
        languageModeCodes.put("zh", TokenStream.LanguageMode.zh);
        languageModeCodes.put("\u4e2d\u6587", TokenStream.LanguageMode.zh);
        languageModeCodes.put("\u7b80\u4f53", TokenStream.LanguageMode.zh);
        languageModeCodes.put("hi", TokenStream.LanguageMode.hi);
        languageModeCodes.put("\u0939\u093f", TokenStream.LanguageMode.hi);
        languageModeCodes.put("ja", TokenStream.LanguageMode.ja);
        languageModeCodes.put("\u65e5\u672c\u8a9e", TokenStream.LanguageMode.ja);
        languageModeCodes.put("ru", TokenStream.LanguageMode.ru);
        languageModeCodes.put("\u0440\u0443", TokenStream.LanguageMode.ru);
        languageModeCodes.put("bn", TokenStream.LanguageMode.bn);
        languageModeCodes.put("\u09AC\u09BE\u0982", TokenStream.LanguageMode.bn);
        languageModeCodes.put("ko", TokenStream.LanguageMode.ko);
        languageModeCodes.put("\uD55C\uAD6D\uC5B4", TokenStream.LanguageMode.ko);
        languageModeCodes.put("tr", TokenStream.LanguageMode.tr);
        languageModeCodes.put("sw", TokenStream.LanguageMode.sw);
        languageModeCodes.put("it", TokenStream.LanguageMode.it);
        languageModeCodes.put("id", TokenStream.LanguageMode.id);
        languageModeCodes.put("ms", TokenStream.LanguageMode.id);
        languageModeCodes.put("pl", TokenStream.LanguageMode.pl);
        languageModeCodes.put("nl", TokenStream.LanguageMode.nl);
    }
    
    private int scanLanguageMode() throws IOException
    {
        setStringBufferTop(0);
        while(true)
        {
            int c = in.getChar();
            if (c == '-')
            {
                if (in.matchChar('-') && in.matchChar('-'))
                    break;
                parser.addError("msg.unknown.language.mode");
                return Token.ERROR;
            }
            
            if (!Character.isJavaIdentifierPart((char)c))
            {
                parser.addError("msg.unknown.language.mode");
                return Token.ERROR;
            }
        
            addToString(c);
        }
        String langCode = getStringFromBuffer();

        TokenStream.LanguageMode mode = languageModeCodes.get(langCode);
        if (mode != null) 
        {
            setLanguage(mode);
            return Token.LANGMODE;
        }
        
        parser.addError("msg.unknown.language.mode");
        return Token.ERROR;
    }

    public static class DecimalNumberReader
    {
        protected double readValue;
        protected StringBuilder readString = new StringBuilder(128);
        protected StringBuilder stringBuffer = new StringBuilder(128);
        protected char decimalSeparator;
        protected char tertiaryDecimalSeparator;
        protected char altNumbers0Base;
        protected char altDecimalSeparator;

        public DecimalNumberReader(char decimalSeparator, char tertiaryDecimalSeparator, char altNumbers0Base, char altDecimalSeparator)
        {
            this.decimalSeparator = decimalSeparator;
            this.tertiaryDecimalSeparator = tertiaryDecimalSeparator;
            this.altNumbers0Base = altNumbers0Base;
            this.altDecimalSeparator = altDecimalSeparator;
        }
        public DecimalNumberReader(char decimalSeparator, char altNumbers0Base, char altDecimalSeparator)
        {
            this(decimalSeparator, decimalSeparator, altNumbers0Base, altDecimalSeparator);
        }
        public DecimalNumberReader(char decimalSeparator)
        {
            this(decimalSeparator, '0', decimalSeparator);
        }
        public DecimalNumberReader()
        {
            this('.');
        }
        
        // Checks if the next token in the stream is a number (where c is the next
        // character in the stream). Returns Token.EMPTY (for no match), Token.ERROR,
        // or Token.NUMBER. If it returns Token.EMPTY, then the state of the stream
        // will not be modified, but it will be modified in the other cases.
        public int matchNumber(int c, TokenCharStream in, ParserErrorReportingBase parser) throws IOException {
            // is it a number?
            if (!isDigit(c) && !(isDecimalSeparator(c) && isDigit(in.peekChar()))) 
                return Token.EMPTY;

            stringBuffer.setLength(0);
            readString.setLength(0);
            int base = 10;

            c = toJSNumberChar(c);
            if (c == '0') {
                readString.append((char)c);
                c = in.getChar();
                if (c == 'x' || c == 'X') {
                    readString.append((char)c);
                    base = 16;
                    c = in.getChar();
                } else if (isDigit(c)) {
                    base = 8;
                } else {
                    stringBuffer.append((char)'0');
                }
            }

            if (base == 16) {
                while (0 <= Kit.xDigitToInt(c, 0)) {
                    stringBuffer.append((char)c);
                    readString.append((char)c);
                    c = in.getChar();
                }
            } else {
                while ('0' <= c && c <= '9') {
                    /*
                     * We permit 08 and 09 as decimal numbers, which
                     * makes our behavior a superset of the ECMA
                     * numeric grammar.  We might not always be so
                     * permissive, so we warn about it.
                     */
                    if (base == 8 && c >= '8') {
                        parser.addWarning("msg.bad.octal.literal",
                                c == '8' ? "8" : "9");
                        base = 10;
                    }
                    readString.append((char)c);
                    stringBuffer.append((char)c);
                    c = in.getChar();
                    c = toJSNumberChar(c);
                }
            }

            boolean isInteger = true;

            if (base == 10 && (isDecimalSeparator(c) || c == 'e' || c == 'E')) {
                isInteger = false;
                if (isDecimalSeparator(c)) {
                    readString.append((char)'.');
                    stringBuffer.append((char)'.');
                    c = in.getChar();
                    while (isDigit(c)) {
                        c = toJSNumberChar(c);
                        readString.append((char)c);
                        stringBuffer.append((char)c);
                        c = in.getChar();
                    } ;
                }
                if (c == 'e' || c == 'E') {
                    c = toJSNumberChar(c);
                    readString.append((char)c);
                    stringBuffer.append((char)c);
                    c = in.getChar();
                    if (c == '+' || c == '-') {
                        readString.append((char)c);
                        stringBuffer.append((char)c);
                        c = in.getChar();
                    }
                    if (!isDigit(c)) {
                        parser.addError("msg.missing.exponent");
                        return Token.ERROR;
                    }
                    do {
                        c = toJSNumberChar(c);
                        readString.append((char)c);
                        stringBuffer.append((char)c);
                        c = in.getChar();
                    } while (isDigit(c));
                }
            }
            in.ungetChar(c);
            String numString = stringBuffer.toString();

            double dval;
            if (base == 10 && !isInteger) {
                try {
                    // Use Java conversion to number from string...
                    dval = Double.valueOf(numString).doubleValue();
                }
                catch (NumberFormatException ex) {
                    parser.addError("msg.caught.nfe");
                    return Token.ERROR;
                }
            } else {
                dval = ScriptRuntime.stringToNumber(numString, 0, base);
            }

            readValue = dval;
            return Token.NUMBER;
        }

        protected int toJSNumberChar(int c)
        {
            if (altNumbers0Base <= c && c <= altNumbers0Base + 9)
                return c - altNumbers0Base + '0';
            return c;
        }
        
        protected boolean isDigit(int c)
        {
            return ('0' <= c && c <= '9')
                    || (altNumbers0Base <= c && c <= altNumbers0Base + 9);
        }
        
        protected boolean isDecimalSeparator(int c)
        {
            return c == decimalSeparator || c == altDecimalSeparator || c == tertiaryDecimalSeparator;
        }
    }
 
    protected boolean isStringDelimiter(int ch)
    {
        return (ch == '\'' || ch == '\"' || ch == '\u2018' || ch == '\u2019' || ch == '\u201c' || ch == '\u201d');
    }
    
    private final static int[] SINGLE_QUOTE_MATCH = new int[] {'\''};
    private final static int[] DOUBLE_QUOTE_MATCH = new int[] {'\"'};
    private final static int[] LEFT_GUILLEMET_MATCH = new int[] {'\u00BB'};
    private final static int[] RIGHT_GUILLEMET_MATCH = new int[] {'\u00AB'};
    private final static int[] LEFTRIGHT_SINGLE_QUOTE_MATCH = new int[] {'\u2018', '\u2019', '\''};
    private final static int[] LEFTRIGHT_DOUBLE_QUOTE_MATCH = new int[] {'\u201c', '\u201d', '\"'};
    private final static int[] LOW_SINGLE_QUOTE_MATCH = new int[] {'\u201a', '\u201b'};
    private final static int[] LOW_DOUBLE_QUOTE_MATCH = new int[] {'\u201c', '\u201d'};
    private final static int[] LEFT_SINGLE_ARROW_MATCH = new int[] {'\u2039'};
    private final static int[] RIGHT_SINGLE_ARROW_MATCH = new int[] {'\u203a'};
    private final static int[] LEFT_CJK_DOUBLE_ARROW_MATCH = new int[] {'\u300b'};
    private final static int[] LEFT_CORNER_MATCH = new int[] {'\u300d'};
    private final static int[] LEFT_WHITE_CORNER_MATCH = new int[] {'\u300f'};
    private final static int[] LEFT_VERTICAL_CORNER_MATCH = new int[] {'\ufe42'};
    private static boolean intArrayContains(int[] array, int val)
    {
        for (int n: array)
            if (val == n)
                return true;
        return false;
    }
    
    protected int[] getMatchingStringDelimiter(int ch)
    {
        if (ch == '\'') return SINGLE_QUOTE_MATCH;
        else if (ch == '\"') return DOUBLE_QUOTE_MATCH;
        else if (ch == '\u00AB') return LEFT_GUILLEMET_MATCH;  // double angle
        else if (ch == '\u00BB') return RIGHT_GUILLEMET_MATCH;
        else if (ch == '\u2018') return LEFTRIGHT_SINGLE_QUOTE_MATCH;  // left-right single quotation
        else if (ch == '\u2019') return LEFTRIGHT_SINGLE_QUOTE_MATCH;
        else if (ch == '\u201c') return LEFTRIGHT_DOUBLE_QUOTE_MATCH;  // left-right double quotation
        else if (ch == '\u201d') return LEFTRIGHT_DOUBLE_QUOTE_MATCH;
        else if (ch == '\u201a') return LOW_SINGLE_QUOTE_MATCH;  // low single quotation
        else if (ch == '\u201e') return LOW_DOUBLE_QUOTE_MATCH;  // low double quotation
        else if (ch == '\u203a') return LEFT_SINGLE_ARROW_MATCH;  // single angle
        else if (ch == '\u2039') return RIGHT_SINGLE_ARROW_MATCH;  
        else if (ch == '\u300a') return LEFT_CJK_DOUBLE_ARROW_MATCH;  // double angle
        else if (ch == '\u300c') return LEFT_CORNER_MATCH;  // corner
        else if (ch == '\u300e') return LEFT_WHITE_CORNER_MATCH;  // white corner
        else if (ch == '\ufe41') return LEFT_VERTICAL_CORNER_MATCH;  // vertical corner
        return DOUBLE_QUOTE_MATCH;
    }
    protected boolean isIdentifierStart(int ch)
    {
        return Character.isJavaIdentifierStart((char)ch);
    }
    protected boolean isIdentifierPart(int ch)
    {
        return Character.isJavaIdentifierPart((char)ch);
    }
    
    /* As defined in ECMA.  jsscan.c uses C isspace() (which allows
     * \v, I think.)  note that code in getChar() implicitly accepts
     * '\r' == \u000D as well.
     */
    static boolean isJSSpace(int c)
    {
        if (c <= 127) {
            return c == 0x20 || c == 0x9 || c == 0xC || c == 0xB;
        } else {
            return c == 0xA0
                || Character.getType((char)c) == Character.SPACE_SEPARATOR;
        }
    }

    //
    // Keyword table
    //
    
    // #string_id_map#
    // The following assumes that Token.EOF == 0
    protected static final int
         Id_break         = Token.BREAK,
         Id_case          = Token.CASE,
         Id_continue      = Token.CONTINUE,
         Id_default       = Token.DEFAULT,
         Id_delete        = Token.DELPROP,
         Id_do            = Token.DO,
         Id_else          = Token.ELSE,
         Id_export        = Token.EXPORT,
         Id_false         = Token.FALSE,
         Id_for           = Token.FOR,
         Id_function      = Token.FUNCTION,
         Id_if            = Token.IF,
         Id_in            = Token.IN,
         Id_let           = Token.LET,
         Id_new           = Token.NEW,
         Id_null          = Token.NULL,
         Id_return        = Token.RETURN,
         Id_switch        = Token.SWITCH,
         Id_this          = Token.THIS,
         Id_true          = Token.TRUE,
         Id_typeof        = Token.TYPEOF,
         Id_var           = Token.VAR,
         Id_void          = Token.VOID,
         Id_while         = Token.WHILE,
         Id_with          = Token.WITH,
         Id_yield         = Token.YIELD,

         // the following are #ifdef RESERVE_JAVA_KEYWORDS in jsscan.c
         Id_abstract      = Token.RESERVED,
         Id_boolean       = Token.RESERVED,
         Id_byte          = Token.RESERVED,
         Id_catch         = Token.CATCH,
         Id_char          = Token.RESERVED,
         Id_class         = Token.RESERVED,
         Id_const         = Token.CONST,
         Id_debugger      = Token.DEBUGGER,
         Id_double        = Token.RESERVED,
         Id_enum          = Token.RESERVED,
         Id_extends       = Token.RESERVED,
         Id_final         = Token.RESERVED,
         Id_finally       = Token.FINALLY,
         Id_float         = Token.RESERVED,
         Id_goto          = Token.RESERVED,
         Id_implements    = Token.RESERVED,
         Id_import        = Token.IMPORT,
         Id_instanceof    = Token.INSTANCEOF,
         Id_int           = Token.RESERVED,
         Id_interface     = Token.RESERVED,
         Id_long          = Token.RESERVED,
         Id_native        = Token.RESERVED,
         Id_package       = Token.RESERVED,
         Id_private       = Token.RESERVED,
         Id_protected     = Token.RESERVED,
         Id_public        = Token.RESERVED,
         Id_short         = Token.RESERVED,
         Id_static        = Token.RESERVED,
         Id_super         = Token.RESERVED,
         Id_synchronized  = Token.RESERVED,
         Id_throw         = Token.THROW,
         Id_throws        = Token.RESERVED,
         Id_transient     = Token.RESERVED,
         Id_try           = Token.TRY,
         Id_volatile      = Token.RESERVED;

    static Map<String, Integer> englishKeywordLookup;
    static {
        englishKeywordLookup = new HashMap<String, Integer>();
        englishKeywordLookup.put("if", Id_if);
        englishKeywordLookup.put("in", Id_in);
        englishKeywordLookup.put("do", Id_do);
        englishKeywordLookup.put("for", Id_for);
        englishKeywordLookup.put("int", Id_int);
        englishKeywordLookup.put("let", Id_let);
        englishKeywordLookup.put("new", Id_new);
        englishKeywordLookup.put("try", Id_try);
        englishKeywordLookup.put("var", Id_var);
        englishKeywordLookup.put("byte", Id_byte);
        englishKeywordLookup.put("case", Id_case);
        englishKeywordLookup.put("char", Id_char);
        englishKeywordLookup.put("else", Id_else);
        englishKeywordLookup.put("enum", Id_enum);
        englishKeywordLookup.put("goto", Id_goto);
        englishKeywordLookup.put("long", Id_long);
        englishKeywordLookup.put("null", Id_null);
        englishKeywordLookup.put("true", Id_true);
        englishKeywordLookup.put("this", Id_this);
        englishKeywordLookup.put("void", Id_void);
        englishKeywordLookup.put("with", Id_with);
        englishKeywordLookup.put("class", Id_class);
        englishKeywordLookup.put("break", Id_break);
        englishKeywordLookup.put("yield", Id_yield);
        englishKeywordLookup.put("while", Id_while);
        englishKeywordLookup.put("false", Id_false);
        englishKeywordLookup.put("const", Id_const);
        englishKeywordLookup.put("final", Id_final);
        englishKeywordLookup.put("float", Id_float);
        englishKeywordLookup.put("short", Id_short);
        englishKeywordLookup.put("super", Id_super);
        englishKeywordLookup.put("throw", Id_throw);
        englishKeywordLookup.put("catch", Id_catch);
        englishKeywordLookup.put("native", Id_native);
        englishKeywordLookup.put("delete", Id_delete);
        englishKeywordLookup.put("return", Id_return);
        englishKeywordLookup.put("throws", Id_throws);
        englishKeywordLookup.put("import", Id_import);
        englishKeywordLookup.put("double", Id_double);
        englishKeywordLookup.put("static", Id_static);
        englishKeywordLookup.put("public", Id_public);
        englishKeywordLookup.put("switch", Id_switch);
        englishKeywordLookup.put("export", Id_export);
        englishKeywordLookup.put("typeof", Id_typeof);
        englishKeywordLookup.put("package", Id_package);
        englishKeywordLookup.put("default", Id_default);
        englishKeywordLookup.put("finally", Id_finally);
        englishKeywordLookup.put("boolean", Id_boolean);
        englishKeywordLookup.put("private", Id_private);
        englishKeywordLookup.put("extends", Id_extends);
        englishKeywordLookup.put("abstract", Id_abstract);
        englishKeywordLookup.put("continue", Id_continue);
        englishKeywordLookup.put("debugger", Id_debugger);
        englishKeywordLookup.put("function", Id_function);
        englishKeywordLookup.put("volatile", Id_volatile);
        englishKeywordLookup.put("interface", Id_interface);
        englishKeywordLookup.put("protected", Id_protected);
        englishKeywordLookup.put("transient", Id_transient);
        englishKeywordLookup.put("implements", Id_implements);
        englishKeywordLookup.put("instanceof", Id_instanceof);
        englishKeywordLookup.put("synchronized", Id_synchronized);
    }
}
